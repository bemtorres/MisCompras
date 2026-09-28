package com.example.miscompras.data.repository;

import androidx.lifecycle.LiveData;

import com.example.miscompras.data.DataExecutors;
import com.example.miscompras.data.local.AppDatabase;
import com.example.miscompras.data.local.entity.Category;
import com.example.miscompras.data.local.entity.Product;
import com.example.miscompras.data.local.model.PriceRow;
import com.example.miscompras.data.local.model.ProductPriceStats;
import com.example.miscompras.data.local.model.ProductRow;
import com.example.miscompras.domain.Pricing;
import com.example.miscompras.utils.Validators;

import java.util.List;

public class ProductRepository {

    /** Entrega el resultado en el hilo principal. */
    public interface Callback<T> {
        void onResult(T value);
    }

    private final AppDatabase database;

    public ProductRepository(AppDatabase database) {
        this.database = database;
    }

    public LiveData<List<ProductRow>> observeProducts() {
        return database.productDao().observeAll();
    }

    public LiveData<List<ProductRow>> observeActiveProducts() {
        return database.productDao().observeActive();
    }

    public LiveData<List<ProductRow>> observeProduct(long productId) {
        return database.productDao().observeById(productId);
    }

    public LiveData<List<Category>> observeCategories() {
        return database.categoryDao().observeAll();
    }

    public LiveData<ProductPriceStats> observePriceStats(long productId) {
        return database.priceHistoryDao().observeStats(productId);
    }

    public LiveData<List<PriceRow>> observePriceHistory(long productId) {
        return database.priceHistoryDao().observeForProduct(productId);
    }

    public List<ProductRow> searchSync(String query) {
        return database.productDao().searchSync(query == null ? "" : query.trim());
    }

    public void save(Product product, Callback<Long> callback) {
        if (product == null) {
            return;
        }
        long now = System.currentTimeMillis();
        product.name = product.name == null ? "" : product.name.trim();
        product.estimatedPrice = Math.max(0d, product.estimatedPrice);
        product.currentStock = Math.max(0d, product.currentStock);
        product.minimumStock = Math.max(0d, product.minimumStock);
        product.usualQuantity = product.usualQuantity <= 0d ? 1d : product.usualQuantity;
        DataExecutors.io(() -> {
            long id;
            if (product.id <= 0L) {
                product.createdAt = now;
                product.updatedAt = now;
                id = database.productDao().insert(product);
            } else {
                product.updatedAt = now;
                database.productDao().update(product);
                id = product.id;
            }
            if (callback != null) {
                DataExecutors.main(() -> callback.onResult(id));
            }
        });
    }

    public void setFavorite(long productId, boolean favorite) {
        DataExecutors.io(() -> database.productDao().setFavorite(productId, favorite, System.currentTimeMillis()));
    }

    public void setStock(long productId, double stock) {
        DataExecutors.io(() -> database.productDao().setStock(productId, Math.max(0d, stock), System.currentTimeMillis()));
    }

    public void delete(long productId, Callback<Boolean> callback) {
        DataExecutors.io(() -> {
            database.productDao().deleteById(productId);
            if (callback != null) {
                DataExecutors.main(() -> callback.onResult(true));
            }
        });
    }

    public void load(long productId, Callback<Product> callback) {
        DataExecutors.io(() -> {
            Product product = database.productDao().findEntitySync(productId);
            DataExecutors.main(() -> callback.onResult(product));
        });
    }

    public void loadRecentPrices(int limit, Callback<List<PriceRow>> callback) {
        DataExecutors.io(() -> {
            List<PriceRow> rows = database.priceHistoryDao().recentSync(limit);
            DataExecutors.main(() -> callback.onResult(rows));
        });
    }

    /** Precio de referencia para el siguiente cálculo: promedio histórico o último precio. */
    public double suggestedPrice(ProductRow row) {
        if (row == null) {
            return 0d;
        }
        if (row.averagePrice != null && row.averagePrice > 0d) {
            return Pricing.round(row.averagePrice);
        }
        if (row.lastPrice != null && row.lastPrice > 0d) {
            return row.lastPrice;
        }
        return row.estimatedPrice;
    }

    public boolean isValid(Product product) {
        return product != null
                && Validators.isValidName(product.name)
                && Validators.isValidPrice(product.estimatedPrice)
                && product.categoryId > 0L;
    }
}
