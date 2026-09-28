package com.example.miscompras.ui.addproducts;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.miscompras.data.DataGraph;
import com.example.miscompras.data.local.entity.Category;
import com.example.miscompras.data.local.model.ProductRow;
import com.example.miscompras.data.repository.ProductRepository;
import com.example.miscompras.data.repository.ShoppingRepository;
import com.example.miscompras.domain.Pricing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Selección de productos y cantidades para agregar a una lista. */
public class AddProductsViewModel extends AndroidViewModel {

    private final ProductRepository products;
    private final ShoppingRepository shopping;

    private LiveData<List<ProductRow>> source;
    private LiveData<List<Category>> categories;
    private final MediatorLiveData<List<ProductRow>> filtered = new MediatorLiveData<>();
    private final MutableLiveData<Long> categoryFilter = new MutableLiveData<>(0L);
    private final MutableLiveData<String> query = new MutableLiveData<>("");
    private final MutableLiveData<String> messages = new MutableLiveData<>();
    private final Map<Long, Double> selection = new LinkedHashMap<>();

    public AddProductsViewModel(@NonNull Application application) {
        super(application);
        products = DataGraph.products(application);
        shopping = DataGraph.shopping(application);
        source = products.observeActiveProducts();
        categories = products.observeCategories();
        filtered.addSource(source, value -> applyFilters());
        filtered.addSource(categoryFilter, value -> applyFilters());
        filtered.addSource(query, value -> applyFilters());
    }

    public LiveData<List<ProductRow>> getProducts() {
        return filtered;
    }

    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    public LiveData<Long> getCategoryFilter() {
        return categoryFilter;
    }

    public LiveData<String> getQuery() {
        return query;
    }

    public LiveData<String> getMessages() {
        return messages;
    }

    public void setQuery(String text) {
        query.setValue(text == null ? "" : text);
    }

    public void setCategory(long categoryId) {
        categoryFilter.setValue(categoryId);
    }

    public double quantityOf(long productId) {
        Double value = selection.get(productId);
        return value == null ? 0d : value;
    }

    public boolean isSelected(long productId) {
        return quantityOf(productId) > 0d;
    }

    public int selectedCount() {
        return selection.size();
    }

    public double selectedTotal() {
        double total = 0d;
        for (Map.Entry<Long, Double> entry : selection.entrySet()) {
            ProductRow row = findRow(entry.getKey());
            if (row != null) {
                total += Pricing.subtotal(entry.getValue(), suggestedPrice(row));
            }
        }
        return Pricing.round(total);
    }

    /** Busca un producto en el catálogo completo, incluso si el filtro actual lo oculta. */
    private ProductRow findRow(long productId) {
        List<ProductRow> all = source == null ? null : source.getValue();
        if (all == null) {
            return null;
        }
        for (ProductRow row : all) {
            if (row.id == productId) {
                return row;
            }
        }
        return null;
    }

    public double suggestedPrice(ProductRow row) {
        return products.suggestedPrice(row);
    }

    public void toggle(long productId, boolean selected) {
        if (selected) {
            ProductRow row = findRow(productId);
            double suggested = row == null || row.usualQuantity <= 0d ? 1d : row.usualQuantity;
            selection.put(productId, suggested);
        } else {
            selection.remove(productId);
        }
    }

    public void setQuantity(long productId, double quantity) {
        if (quantity <= 0d) {
            selection.remove(productId);
        } else {
            selection.put(productId, quantity);
        }
    }

    private void applyFilters() {
        List<ProductRow> all = source == null ? null : source.getValue();
        if (all == null) {
            return;
        }
        long categoryId = categoryFilter.getValue() == null ? 0L : categoryFilter.getValue();
        String text = query.getValue() == null ? "" : query.getValue().trim().toLowerCase(Locale.getDefault());
        List<ProductRow> result = new ArrayList<>();
        for (ProductRow row : all) {
            if (categoryId > 0L && row.categoryId != categoryId) {
                continue;
            }
            if (!text.isEmpty() && !row.name.toLowerCase(Locale.getDefault()).contains(text)) {
                continue;
            }
            result.add(row);
        }
        filtered.setValue(result);
    }

    /** Agrega los productos seleccionados a la lista indicada. */
    public void addToList(long listId, Runnable onDone) {
        if (selection.isEmpty()) {
            messages.setValue("Selecciona al menos un producto");
            return;
        }
        Map<Long, Double> pending = new LinkedHashMap<>(selection);
        addNext(listId, pending, 0, onDone);
        selection.clear();
    }

    private void addNext(long listId, Map<Long, Double> pending, int index, Runnable onDone) {
        if (index >= pending.size()) {
            messages.setValue(pending.size() + " productos agregados");
            onDone.run();
            return;
        }
        Long productId = null;
        Double quantity = null;
        int i = 0;
        for (Map.Entry<Long, Double> entry : pending.entrySet()) {
            if (i == index) {
                productId = entry.getKey();
                quantity = entry.getValue();
                break;
            }
            i++;
        }
        if (productId == null || quantity == null) {
            addNext(listId, pending, index + 1, onDone);
            return;
        }
        ProductRow row = findRow(productId);
        double unitPrice = row == null ? 0d : suggestedPrice(row);
        shopping.addItem(listId, productId, quantity, unitPrice, result -> {
            if (!result.success) {
                messages.setValue(result.message == null ? "No se pudo agregar el producto" : result.message);
                onDone.run();
                return;
            }
            addNext(listId, pending, index + 1, onDone);
        });
    }
}
