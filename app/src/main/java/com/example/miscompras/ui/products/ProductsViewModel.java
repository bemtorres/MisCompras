package com.example.miscompras.ui.products;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.miscompras.data.DataGraph;
import com.example.miscompras.data.local.entity.Category;
import com.example.miscompras.data.local.entity.Product;
import com.example.miscompras.data.local.model.ProductPriceStats;
import com.example.miscompras.data.local.model.ProductRow;
import com.example.miscompras.data.repository.ProductRepository;

import java.util.List;

/** Catálogo de productos: filtro, favoritos, stock e historial de precios. */
public class ProductsViewModel extends AndroidViewModel {

    private final ProductRepository products;
    private final MutableLiveData<String> messages = new MutableLiveData<>();

    public ProductsViewModel(@NonNull Application application) {
        super(application);
        products = DataGraph.products(application);
    }

    public LiveData<List<ProductRow>> getProducts() {
        return products.observeActiveProducts();
    }

    public LiveData<List<Category>> getCategories() {
        return products.observeCategories();
    }

    public LiveData<String> getMessages() {
        return messages;
    }

    public LiveData<ProductPriceStats> getStats(long productId) {
        return products.observePriceStats(productId);
    }

    public void toggleFavorite(ProductRow row) {
        products.setFavorite(row.id, !row.isFavorite);
    }

    public void changeStock(ProductRow row, double delta) {
        products.setStock(row.id, Math.max(0d, row.currentStock + delta));
    }

    public void setStock(ProductRow row, double stock) {
        products.setStock(row.id, Math.max(0d, stock));
    }

    public void delete(ProductRow row) {
        products.delete(row.id, deleted -> messages.setValue(deleted
                ? "Producto eliminado"
                : "No se puede eliminar: el producto ya se usó en una compra"));
    }

    public void load(long productId, ProductRepository.Callback<Product> callback) {
        products.load(productId, callback);
    }
}
