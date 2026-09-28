package com.example.miscompras.data.repository;

import androidx.lifecycle.LiveData;

import com.example.miscompras.data.DataExecutors;
import com.example.miscompras.data.local.AppDatabase;
import com.example.miscompras.data.local.entity.PriceHistory;
import com.example.miscompras.data.local.entity.Product;
import com.example.miscompras.data.local.entity.PurchaseHistory;
import com.example.miscompras.data.local.entity.ShoppingList;
import com.example.miscompras.data.local.entity.ShoppingListItem;
import com.example.miscompras.data.local.model.ItemRow;
import com.example.miscompras.data.local.model.ListSummary;
import com.example.miscompras.domain.DiscountType;
import com.example.miscompras.domain.ListStatus;
import com.example.miscompras.domain.ListType;
import com.example.miscompras.domain.Pricing;
import com.example.miscompras.utils.Dates;
import com.example.miscompras.utils.Validators;

import java.util.ArrayList;
import java.util.List;

public class ShoppingRepository {

    public interface Callback<T> {
        void onResult(T value);
    }

    /** Resultado de una operación de escritura con validación. */
    public static class Result {
        public final boolean success;
        public final String message;

        private Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static Result ok() {
            return new Result(true, null);
        }

        public static Result error(String message) {
            return new Result(false, message);
        }
    }

    private final AppDatabase database;

    public ShoppingRepository(AppDatabase database) {
        this.database = database;
    }

    // ------------------------------------------------------------------ lecturas

    public LiveData<List<ListSummary>> observeAllLists() {
        return database.shoppingListDao().observeAll();
    }

    public LiveData<List<ListSummary>> observeCompletedLists() {
        return database.shoppingListDao().observeCompleted();
    }

    public LiveData<ListSummary> observeActiveList() {
        return database.shoppingListDao().observeActive();
    }

    public LiveData<ListSummary> observeList(long listId) {
        return database.shoppingListDao().observeById(listId);
    }

    public LiveData<List<ItemRow>> observeItems(long listId) {
        return database.shoppingListItemDao().observeByList(listId);
    }

    public void loadItems(long listId, Callback<List<ItemRow>> callback) {
        DataExecutors.io(() -> {
            List<ItemRow> items = database.shoppingListItemDao().findByListSync(listId);
            DataExecutors.main(() -> callback.onResult(items));
        });
    }

    public void loadPending(long listId, Callback<List<ItemRow>> callback) {
        DataExecutors.io(() -> {
            List<ItemRow> items = database.shoppingListItemDao().findPendingSync(listId);
            DataExecutors.main(() -> callback.onResult(items));
        });
    }

    public void loadItem(long itemId, Callback<ItemRow> callback) {
        DataExecutors.io(() -> {
            ItemRow row = database.shoppingListItemDao().findRowByIdSync(itemId);
            DataExecutors.main(() -> callback.onResult(row));
        });
    }

    public void countPending(long listId, Callback<Integer> callback) {
        DataExecutors.io(() -> {
            List<ItemRow> pending = database.shoppingListItemDao().findPendingSync(listId);
            DataExecutors.main(() -> callback.onResult(pending.size()));
        });
    }

    // ------------------------------------------------------------------ escritura de listas

    /** Crea una lista planificada, opcionalmente con productos sugeridos. */
    public void createList(String name, ListType type, long plannedDate, List<Product> suggested, Callback<Long> callback) {
        if (!Validators.isValidName(name)) {
            DataExecutors.main(() -> callback.onResult(-1L));
            return;
        }
        long now = System.currentTimeMillis();
        DataExecutors.io(() -> {
            ShoppingList list = new ShoppingList(name.trim(), type == null ? ListType.WEEKLY : type, ListStatus.PLANNED, plannedDate);
            list.createdAt = now;
            list.updatedAt = now;
            long listId = database.shoppingListDao().insert(list);
            double estimatedTotal = 0d;
            if (suggested != null) {
                int position = 0;
                for (Product product : suggested) {
                    double quantity = Math.max(1d, product.usualQuantity);
                    double price = Math.max(0d, product.estimatedPrice);
                    ShoppingListItem item = new ShoppingListItem();
                    item.shoppingListId = listId;
                    item.productId = product.id;
                    item.quantity = quantity;
                    item.estimatedUnitPrice = price;
                    item.estimatedSubtotal = Pricing.subtotal(quantity, price);
                    item.position = position++;
                    item.addedAt = now;
                    database.shoppingListItemDao().insert(item);
                    estimatedTotal += item.estimatedSubtotal;
                }
            }
            database.shoppingListDao().updateTotals(listId, estimatedTotal, 0d, 0d, now);
            DataExecutors.main(() -> callback.onResult(listId));
        });
    }

    public void addItem(long listId, long productId, double quantity, double estimatedUnitPrice, Callback<Result> callback) {
        if (!Validators.isValidQuantity(quantity) || !Validators.isValidPrice(estimatedUnitPrice)) {
            deliver(callback, Result.error("Cantidad o precio inválido"));
            return;
        }
        long now = System.currentTimeMillis();
        DataExecutors.io(() -> {
            if (database.shoppingListItemDao().countForProductSync(listId, productId) > 0) {
                DataExecutors.main(() -> callback.onResult(Result.error("El producto ya está en la lista")));
                return;
            }
            ShoppingListItem item = new ShoppingListItem();
            item.shoppingListId = listId;
            item.productId = productId;
            item.quantity = quantity;
            item.estimatedUnitPrice = Math.max(0d, estimatedUnitPrice);
            item.estimatedSubtotal = Pricing.subtotal(item.quantity, item.estimatedUnitPrice);
            item.position = database.shoppingListItemDao().nextPositionSync(listId);
            item.addedAt = now;
            database.shoppingListItemDao().insert(item);
            recalculate(listId, now);
            DataExecutors.main(() -> callback.onResult(Result.ok()));
        });
    }

    public void addItem(long listId, ItemRow row, double quantity, double estimatedUnitPrice, Callback<Result> callback) {
        addItem(listId, row.productId, quantity, estimatedUnitPrice, callback);
    }

    public void updateQuantity(long itemId, double quantity, Callback<Result> callback) {
        if (!Validators.isValidQuantity(quantity)) {
            deliver(callback, Result.error("La cantidad debe ser mayor a 0"));
            return;
        }
        DataExecutors.io(() -> {
            ShoppingListItem item = database.shoppingListItemDao().findEntitySync(itemId);
            if (item == null) {
                DataExecutors.main(() -> callback.onResult(Result.error("El producto ya no existe")));
                return;
            }
            item.quantity = quantity;
            item.estimatedSubtotal = Pricing.subtotal(quantity, item.estimatedUnitPrice);
            database.shoppingListItemDao().update(item);
            recalculate(item.shoppingListId, System.currentTimeMillis());
            DataExecutors.main(() -> callback.onResult(Result.ok()));
        });
    }

    public void updateEstimatedPrice(long itemId, double estimatedUnitPrice, Callback<Result> callback) {
        if (!Validators.isValidPrice(estimatedUnitPrice)) {
            deliver(callback, Result.error("El precio no puede ser negativo"));
            return;
        }
        DataExecutors.io(() -> {
            ShoppingListItem item = database.shoppingListItemDao().findEntitySync(itemId);
            if (item == null) {
                DataExecutors.main(() -> callback.onResult(Result.error("El producto ya no existe")));
                return;
            }
            item.estimatedUnitPrice = estimatedUnitPrice;
            item.estimatedSubtotal = Pricing.subtotal(item.quantity, estimatedUnitPrice);
            database.shoppingListItemDao().update(item);
            recalculate(item.shoppingListId, System.currentTimeMillis());
            DataExecutors.main(() -> callback.onResult(Result.ok()));
        });
    }

    public void updateProductPrice(long listId, long productId, double estimatedUnitPrice) {
        DataExecutors.io(() -> {
            Product product = database.productDao().findEntitySync(productId);
            if (product != null) {
                product.estimatedPrice = Math.max(0d, estimatedUnitPrice);
                database.productDao().update(product);
            }
            recalculate(listId, System.currentTimeMillis());
        });
    }

    public void setSubstitute(long itemId, long substituteProductId, Callback<Result> callback) {
        DataExecutors.io(() -> {
            ShoppingListItem item = database.shoppingListItemDao().findEntitySync(itemId);
            if (item == null) {
                DataExecutors.main(() -> callback.onResult(Result.error("El producto ya no existe")));
                return;
            }
            item.substituteProductId = substituteProductId <= 0L ? null : substituteProductId;
            database.shoppingListItemDao().update(item);
            DataExecutors.main(() -> callback.onResult(Result.ok()));
        });
    }

    /** Registra el producto que se compró en lugar del planificado (lo crea si no existe). */
    public void setSubstituteByName(long itemId, String name, Callback<Result> callback) {
        DataExecutors.io(() -> {
            ShoppingListItem item = database.shoppingListItemDao().findEntitySync(itemId);
            if (item == null) {
                DataExecutors.main(() -> callback.onResult(Result.error("El producto ya no existe")));
                return;
            }
            String trimmed = name == null ? "" : name.trim();
            if (trimmed.isEmpty()) {
                item.substituteProductId = null;
                database.shoppingListItemDao().update(item);
                DataExecutors.main(() -> callback.onResult(Result.ok()));
                return;
            }
            long substituteId = 0L;
            for (Product product : database.productDao().allSync()) {
                if (product.name.equalsIgnoreCase(trimmed)) {
                    substituteId = product.id;
                    break;
                }
            }
            if (substituteId == 0L) {
                Product original = database.productDao().findEntitySync(item.productId);
                Product substitute = new Product();
                substitute.name = trimmed;
                substitute.unit = original == null ? "unidad" : original.unit;
                substitute.categoryId = original == null ? 1L : original.categoryId;
                substitute.estimatedPrice = item.estimatedUnitPrice;
                substitute.usualQuantity = item.quantity;
                substitute.currentStock = 0d;
                substitute.minimumStock = 1d;
                substitute.isActive = true;
                substitute.createdAt = System.currentTimeMillis();
                substitute.updatedAt = substitute.createdAt;
                substituteId = database.productDao().insert(substitute);
            }
            item.substituteProductId = substituteId;
            database.shoppingListItemDao().update(item);
            DataExecutors.main(() -> callback.onResult(Result.ok()));
        });
    }

    public void moveItem(long itemId, int direction, Callback<Result> callback) {
        DataExecutors.io(() -> {
            ShoppingListItem item = database.shoppingListItemDao().findEntitySync(itemId);
            if (item == null) {
                DataExecutors.main(() -> callback.onResult(Result.error("El producto ya no existe")));
                return;
            }
            List<ShoppingListItem> items = database.shoppingListItemDao().findEntitiesSync(item.shoppingListId);
            int index = -1;
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).id == itemId) {
                    index = i;
                    break;
                }
            }
            int target = index + direction;
            if (index < 0 || target < 0 || target >= items.size()) {
                DataExecutors.main(() -> callback.onResult(Result.error("No se puede mover más")));
                return;
            }
            ShoppingListItem other = items.get(target);
            int itemPosition = item.position;
            item.position = other.position;
            other.position = itemPosition;
            database.shoppingListItemDao().update(item);
            database.shoppingListItemDao().update(other);
            DataExecutors.main(() -> callback.onResult(Result.ok()));
        });
    }

    public void deleteItem(long itemId, Callback<Result> callback) {
        DataExecutors.io(() -> {
            ShoppingListItem item = database.shoppingListItemDao().findEntitySync(itemId);
            if (item == null) {
                DataExecutors.main(() -> callback.onResult(Result.ok()));
                return;
            }
            database.shoppingListItemDao().deleteById(itemId);
            recalculate(item.shoppingListId, System.currentTimeMillis());
            DataExecutors.main(() -> callback.onResult(Result.ok()));
        });
    }

    // ------------------------------------------------------------------ modo compra

    public void startShopping(long listId) {
        DataExecutors.io(() -> {
            ShoppingList list = database.shoppingListDao().findEntitySync(listId);
            if (list != null && list.status == ListStatus.PLANNED) {
                database.shoppingListDao().updateStatus(listId, ListStatus.IN_PROGRESS.name(), null, System.currentTimeMillis());
            }
        });
    }

    /** Registra el precio real (y descuento) de un producto comprado. */
    public void registerPurchase(long itemId, double realUnitPrice, DiscountType discountType, double discountValue, Callback<Result> callback) {
        if (!Validators.isValidPrice(realUnitPrice)) {
            deliver(callback, Result.error("El precio no puede ser negativo"));
            return;
        }
        if (!Validators.isValidDiscount(discountType, discountValue)) {
            deliver(callback, Result.error("Descuento inválido"));
            return;
        }
        long now = System.currentTimeMillis();
        DataExecutors.io(() -> {
            ShoppingListItem item = database.shoppingListItemDao().findEntitySync(itemId);
            if (item == null) {
                DataExecutors.main(() -> callback.onResult(Result.error("El producto ya no existe")));
                return;
            }
            DiscountType type = discountType == null ? DiscountType.NONE : discountType;
            double finalUnit = Pricing.finalUnitPrice(realUnitPrice, type, discountValue);
            item.realUnitPrice = realUnitPrice;
            item.discountType = type;
            item.discountValue = discountValue;
            item.finalUnitPrice = finalUnit;
            item.realSubtotal = Pricing.subtotal(item.quantity, finalUnit);
            item.isPurchased = true;
            item.notFound = false;
            item.notFoundReason = "";
            database.shoppingListItemDao().update(item);
            recalculate(item.shoppingListId, now);
            DataExecutors.main(() -> callback.onResult(Result.ok()));
        });
    }

    public void registerNotFound(long itemId, String reason, Callback<Result> callback) {
        long now = System.currentTimeMillis();
        DataExecutors.io(() -> {
            ShoppingListItem item = database.shoppingListItemDao().findEntitySync(itemId);
            if (item == null) {
                DataExecutors.main(() -> callback.onResult(Result.error("El producto ya no existe")));
                return;
            }
            item.isPurchased = false;
            item.notFound = true;
            item.notFoundReason = reason == null ? "" : reason;
            database.shoppingListItemDao().update(item);
            recalculate(item.shoppingListId, now);
            DataExecutors.main(() -> callback.onResult(Result.ok()));
        });
    }

    public void clearItem(long itemId, Callback<Result> callback) {
        long now = System.currentTimeMillis();
        DataExecutors.io(() -> {
            ShoppingListItem item = database.shoppingListItemDao().findEntitySync(itemId);
            if (item == null) {
                DataExecutors.main(() -> callback.onResult(Result.ok()));
                return;
            }
            item.clearPurchase();
            item.realUnitPrice = null;
            item.finalUnitPrice = null;
            item.realSubtotal = 0d;
            item.discountType = DiscountType.NONE;
            item.discountValue = 0d;
            database.shoppingListItemDao().update(item);
            recalculate(item.shoppingListId, now);
            DataExecutors.main(() -> callback.onResult(Result.ok()));
        });
    }

    /** Finaliza la compra: guarda historial de precios, descuenta stock y cierra la lista. */
    public void finishList(long listId, Callback<Result> callback) {
        long now = System.currentTimeMillis();
        DataExecutors.io(() -> {
            ShoppingList list = database.shoppingListDao().findEntitySync(listId);
            if (list == null) {
                DataExecutors.main(() -> callback.onResult(Result.error("La lista ya no existe")));
                return;
            }
            List<ShoppingListItem> items = database.shoppingListItemDao().findEntitiesSync(listId);
            double estimatedTotal = 0d;
            double realTotal = 0d;
            double discountTotal = 0d;
            int purchasedCount = 0;

            for (ShoppingListItem item : items) {
                estimatedTotal += item.estimatedSubtotal;
                if (!item.isPurchased) {
                    continue;
                }
                purchasedCount++;
                realTotal += item.realSubtotal;
                discountTotal += Pricing.discountAmount(item.quantity, item.realUnitPrice == null ? 0d : item.realUnitPrice, item.discountType, item.discountValue);

                PriceHistory history = new PriceHistory();
                history.productId = item.productId;
                history.shoppingListId = listId;
                history.quantity = item.quantity;
                history.price = item.realUnitPrice == null ? 0d : item.realUnitPrice;
                history.discount = Pricing.discountAmount(item.realUnitPrice == null ? 0d : item.realUnitPrice, item.discountType, item.discountValue);
                history.finalPrice = item.finalUnitPrice == null ? 0d : item.finalUnitPrice;
                history.date = now;
                database.priceHistoryDao().insert(history);

                // inventario: se descuenta lo efectivamente comprado
                Product product = database.productDao().findEntitySync(item.productId);
                if (product != null) {
                    product.currentStock = Math.max(0d, product.currentStock - item.quantity);
                    product.updatedAt = now;
                    database.productDao().update(product);
                }
            }

            PurchaseHistory summary = new PurchaseHistory();
            summary.shoppingListId = listId;
            summary.purchaseDate = now;
            summary.itemCount = purchasedCount;
            summary.estimatedTotal = estimatedTotal;
            summary.total = realTotal;
            summary.discount = discountTotal;
            database.shoppingListDao().insertPurchaseHistory(summary);

            database.shoppingListDao().updateTotals(listId, estimatedTotal, realTotal, discountTotal, now);
            database.shoppingListDao().updateStatus(listId, ListStatus.COMPLETED.name(), now, now);
            DataExecutors.main(() -> callback.onResult(Result.ok()));
        });
    }

    public void cancelList(long listId, Callback<Result> callback) {
        DataExecutors.io(() -> {
            database.shoppingListDao().updateStatus(listId, ListStatus.CANCELLED.name(), null, System.currentTimeMillis());
            DataExecutors.main(() -> callback.onResult(Result.ok()));
        });
    }

    public void deleteList(long listId, Callback<Result> callback) {
        DataExecutors.io(() -> {
            database.shoppingListItemDao().deleteByList(listId);
            database.priceHistoryDao().deleteByList(listId);
            database.shoppingListDao().deleteById(listId);
            DataExecutors.main(() -> callback.onResult(Result.ok()));
        });
    }

    public void updateHeader(long listId, String name, ListType type, long plannedDate) {
        if (!Validators.isValidName(name)) {
            return;
        }
        DataExecutors.io(() -> database.shoppingListDao().updateHeader(
                listId, name.trim(), (type == null ? ListType.WEEKLY : type).name(), plannedDate, System.currentTimeMillis()));
    }

    /** Recalcula los totales de la lista a partir de sus ítems. */
    private void recalculate(long listId, long now) {
        List<ShoppingListItem> items = database.shoppingListItemDao().findEntitiesSync(listId);
        double estimatedTotal = 0d;
        double realTotal = 0d;
        double discountTotal = 0d;
        for (ShoppingListItem item : items) {
            estimatedTotal += item.estimatedSubtotal;
            if (item.isPurchased) {
                realTotal += item.realSubtotal;
                discountTotal += Pricing.discountAmount(item.quantity, item.realUnitPrice == null ? 0d : item.realUnitPrice, item.discountType, item.discountValue);
            }
        }
        database.shoppingListDao().updateTotals(listId, Pricing.round(estimatedTotal), Pricing.round(realTotal), Pricing.round(discountTotal), now);
    }

    public void suggestProductsForNewList(Callback<List<Product>> callback) {
        DataExecutors.io(() -> {
            List<Product> products = database.productDao().habitualSync(8);
            List<Product> result = new ArrayList<>();
            for (Product product : products) {
                if (product.currentStock > product.usualQuantity) {
                    continue; // hay stock suficiente: no sugerimos compra
                }
                product.usualQuantity = Math.max(1d, Math.ceil(product.usualQuantity - product.currentStock));
                result.add(product);
            }
            if (result.isEmpty()) {
                result = database.productDao().habitualSync(5);
            }
            List<Product> suggestions = result;
            DataExecutors.main(() -> callback.onResult(suggestions));
        });
    }

    private void deliver(Callback<Result> callback, Result result) {
        if (callback != null) {
            DataExecutors.main(() -> callback.onResult(result));
        }
    }

    public static long todayStart() {
        return Dates.startOfDay(System.currentTimeMillis());
    }
}
