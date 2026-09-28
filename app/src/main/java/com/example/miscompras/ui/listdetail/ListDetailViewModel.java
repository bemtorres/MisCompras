package com.example.miscompras.ui.listdetail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.miscompras.data.DataGraph;
import com.example.miscompras.data.local.model.ItemRow;
import com.example.miscompras.data.local.model.ListSummary;
import com.example.miscompras.data.repository.ShoppingRepository;
import com.example.miscompras.domain.DiscountType;

import java.util.List;

public class ListDetailViewModel extends AndroidViewModel {

    private final ShoppingRepository shopping;
    private long listId;

    private LiveData<ListSummary> list;
    private LiveData<List<ItemRow>> items;
    private final MutableLiveData<Event<String>> messages = new MutableLiveData<>();

    public ListDetailViewModel(@NonNull Application application) {
        super(application);
        shopping = DataGraph.shopping(application);
    }

    /** Mensajes de una sola vez (resultados de operaciones). */
    public static class Event<T> {
        private final T value;
        private boolean handled;

        Event(T value) {
            this.value = value;
            this.handled = false;
        }

        public T consume() {
            if (handled) {
                return null;
            }
            handled = true;
            return value;
        }
    }

    public void init(long listId) {
        if (this.listId == listId && list != null) {
            return;
        }
        this.listId = listId;
        list = shopping.observeList(listId);
        items = shopping.observeItems(listId);
    }

    public long getListId() {
        return listId;
    }

    public LiveData<ListSummary> getList() {
        return list;
    }

    public LiveData<List<ItemRow>> getItems() {
        return items;
    }

    public LiveData<Event<String>> getMessages() {
        return messages;
    }

    public void notify(String message) {
        messages.setValue(new Event<>(message));
    }

    // --------------------------------------------------------------- acciones

    public void updateQuantity(long itemId, double quantity) {
        shopping.updateQuantity(itemId, quantity, this::publish);
    }

    public void updateEstimate(long itemId, double price, boolean updateCatalog) {
        shopping.updateEstimatedPrice(itemId, price, result -> {
            if (updateCatalog) {
                shopping.updateProductPrice(listId, findProductId(itemId), price);
            }
            publish(result);
        });
    }

    private long findProductId(long itemId) {
        List<ItemRow> current = items == null ? null : items.getValue();
        if (current != null) {
            for (ItemRow row : current) {
                if (row.id == itemId) {
                    return row.productId;
                }
            }
        }
        return 0L;
    }

    public void saveItem(long itemId, double quantity, double estimatedPrice, Double realPrice,
                         DiscountType discountType, double discountValue, boolean purchased,
                         boolean notFound, String notFoundReason, String substituteName) {
        shopping.updateQuantity(itemId, quantity, result -> {
            if (!result.success) {
                publish(result);
                return;
            }
            shopping.updateEstimatedPrice(itemId, estimatedPrice, result2 -> {
                if (!result2.success) {
                    publish(result2);
                    return;
                }
                if (purchased && realPrice != null) {
                    shopping.registerPurchase(itemId, realPrice, discountType, discountValue, result3 -> {
                        if (!result3.success) {
                            publish(result3);
                            return;
                        }
                        applySubstitute(itemId, substituteName, purchased, result3);
                    });
                } else if (notFound) {
                    shopping.registerNotFound(itemId, notFoundReason, result4 ->
                            applySubstitute(itemId, substituteName, false, result4));
                } else {
                    shopping.clearItem(itemId, result5 ->
                            applySubstitute(itemId, substituteName, false, result5));
                }
            });
        });
    }

    private void applySubstitute(long itemId, String substituteName, boolean purchased,
                                 ShoppingRepository.Result result) {
        if (purchased && substituteName != null && !substituteName.trim().isEmpty()) {
            shopping.setSubstituteByName(itemId, substituteName.trim(), this::publish);
        } else {
            publish(result);
        }
    }

    public void quickTogglePurchased(ItemRow row) {
        if (row.isPurchased || row.notFound) {
            shopping.clearItem(row.id, this::publish);
            return;
        }
        double price = row.realUnitPrice == null || row.realUnitPrice <= 0d ? row.estimatedUnitPrice : row.realUnitPrice;
        shopping.registerPurchase(row.id, price, DiscountType.NONE, 0d, result -> {
            if (result.success) {
                notify("Comprado con precio estimado " + com.example.miscompras.utils.Money.format(price * row.quantity)
                        + " · toca para ajustar");
            }
            publish(result);
        });
    }

    public void deleteItem(long itemId) {
        shopping.deleteItem(itemId, result -> {
            if (result.success) {
                notify("Producto eliminado");
            }
            publish(result);
        });
    }

    public void moveItem(long itemId, int direction) {
        shopping.moveItem(itemId, direction, this::publish);
    }

    public void setSubstitute(long itemId, String name) {
        shopping.setSubstituteByName(itemId, name, result -> {
            if (result.success) {
                notify(name == null || name.trim().isEmpty() ? "Sustituto quitado" : "Sustituto: " + name.trim());
            }
            publish(result);
        });
    }

    public void startShopping() {
        shopping.startShopping(listId);
    }

    public void finish(ShoppingRepository.Callback<ShoppingRepository.Result> callback) {
        shopping.finishList(listId, callback);
    }

    public void cancel(ShoppingRepository.Callback<ShoppingRepository.Result> callback) {
        shopping.cancelList(listId, callback);
    }

    public void countPending(ShoppingRepository.Callback<Integer> callback) {
        shopping.countPending(listId, callback);
    }

    public void delete(ShoppingRepository.Callback<ShoppingRepository.Result> callback) {
        shopping.deleteList(listId, callback);
    }

    private void publish(ShoppingRepository.Result result) {
        if (result != null && !result.success && result.message != null) {
            notify(result.message);
        }
    }
}
