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
import com.example.miscompras.data.repository.ProductRepository;
import com.example.miscompras.utils.Validators;

import java.util.List;

/** Formulario de producto: creación, edición, categorías y estadísticas de precio. */
public class ProductEditViewModel extends AndroidViewModel {

    private final ProductRepository products;
    private final MutableLiveData<Product> current = new MutableLiveData<>();
    private final MutableLiveData<String> messages = new MutableLiveData<>();

    public ProductEditViewModel(@NonNull Application application) {
        super(application);
        products = DataGraph.products(application);
    }

    public LiveData<List<Category>> getCategories() {
        return products.observeCategories();
    }

    public LiveData<Product> getProduct() {
        return current;
    }

    public LiveData<String> getMessages() {
        return messages;
    }

    public LiveData<ProductPriceStats> getStats(long productId) {
        return products.observePriceStats(productId);
    }

    public void load(long productId) {
        if (productId <= 0L || current.getValue() != null) {
            return;
        }
        products.load(productId, product -> {
            if (product != null) {
                current.setValue(product);
            }
        });
    }

    public void save(Product product, ProductRepository.Callback<Long> callback) {
        if (!Validators.isValidName(product.name)) {
            messages.setValue("Escribe un nombre");
            return;
        }
        if (product.estimatedPrice < 0d) {
            messages.setValue("El precio no puede ser negativo");
            return;
        }
        if (!products.isValid(product)) {
            messages.setValue("Revisa los datos del producto");
            return;
        }
        product.updatedAt = System.currentTimeMillis();
        if (product.createdAt == 0L) {
            product.createdAt = product.updatedAt;
        }
        products.save(product, id -> {
            messages.setValue("Producto guardado");
            if (callback != null) {
                callback.onResult(id);
            }
        });
    }

    public void delete(long productId, ProductRepository.Callback<Boolean> callback) {
        products.delete(productId, deleted -> {
            messages.setValue(deleted
                    ? "Producto eliminado"
                    : "No se puede eliminar: el producto ya se usó en una compra");
            if (callback != null) {
                callback.onResult(deleted);
            }
        });
    }
}
