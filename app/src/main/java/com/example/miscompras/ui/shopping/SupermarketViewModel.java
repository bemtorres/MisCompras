package com.example.miscompras.ui.shopping;

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
import com.example.miscompras.domain.Pricing;
import com.example.miscompras.utils.Money;

import java.util.List;

/** Modo supermercado: recorre los productos y registra precios reales, descuentos y faltantes. */
public class SupermarketViewModel extends AndroidViewModel {

    private final ShoppingRepository shopping;
    private long listId;
    private LiveData<ListSummary> list;
    private LiveData<List<ItemRow>> items;
    private final MutableLiveData<String> messages = new MutableLiveData<>();
    private final MutableLiveData<Boolean> finished = new MutableLiveData<>(false);
    private int index;

    public SupermarketViewModel(@NonNull Application application) {
        super(application);
        shopping = DataGraph.shopping(application);
    }

    public void init(long listId) {
        if (this.listId == listId && items != null) {
            return;
        }
        this.listId = listId;
        list = shopping.observeList(listId);
        items = shopping.observeItems(listId);
        shopping.startShopping(listId);
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

    public LiveData<String> getMessages() {
        return messages;
    }

    public LiveData<Boolean> getFinished() {
        return finished;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = Math.max(0, index);
    }

    /** Primer producto pendiente de la lista. */
    public ItemRow firstPending(List<ItemRow> rows) {
        if (rows == null) {
            return null;
        }
        for (int i = 0; i < rows.size(); i++) {
            if (!rows.get(i).isPurchased && !rows.get(i).notFound) {
                return rows.get(i);
            }
        }
        return null;
    }

    public void setQuantity(long itemId, double quantity) {
        if (quantity <= 0d) {
            messages.setValue("La cantidad mínima es 1");
            return;
        }
        shopping.updateQuantity(itemId, quantity, this::publish);
    }

    public void registerPurchase(ItemRow row, double realPrice, DiscountType discountType, double discountValue) {
        shopping.registerPurchase(row.id, realPrice, discountType, discountValue, result -> {
            if (result.success) {
                index++;
            }
            publish(result);
        });
    }

    public void useEstimate(ItemRow row) {
        double price = row.realUnitPrice == null || row.realUnitPrice <= 0d ? row.estimatedUnitPrice : row.realUnitPrice;
        registerPurchase(row, price, DiscountType.NONE, 0d);
    }

    public void registerNotFound(long itemId, String reason) {
        shopping.registerNotFound(itemId, reason, result -> {
            if (result.success) {
                index++;
            }
            publish(result);
        });
    }

    public void clear(long itemId) {
        shopping.clearItem(itemId, this::publish);
    }

    public void setSubstitute(long itemId, String name) {
        shopping.setSubstituteByName(itemId, name, result -> {
            if (result.success) {
                messages.setValue(name == null || name.trim().isEmpty() ? "Sustituto quitado" : "Sustituto: " + name.trim());
            }
            publish(result);
        });
    }

    public void finish(ShoppingRepository.Callback<ShoppingRepository.Result> callback) {
        shopping.finishList(listId, result -> {
            if (result.success) {
                finished.setValue(true);
                messages.setValue(Money.format(lastTotal) + " · compra guardada");
            }
            callback.onResult(result);
        });
    }

    private double lastTotal;

    public void rememberTotal(ListSummary summary) {
        if (summary != null) {
            lastTotal = summary.realTotal > 0d ? summary.realTotal : summary.estimatedTotal;
        }
    }

    public double subtotal(ItemRow row, double realPrice, DiscountType discountType, double discountValue) {
        if (row == null) {
            return 0d;
        }
        return Pricing.subtotal(row.quantity, Pricing.finalUnitPrice(realPrice, discountType, discountValue));
    }

    private void publish(ShoppingRepository.Result result) {
        if (result != null && !result.success && result.message != null) {
            messages.setValue(result.message);
        }
    }
}
