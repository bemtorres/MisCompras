package com.example.miscompras.data;

import android.content.Context;

import com.example.miscompras.data.local.AppDatabase;
import com.example.miscompras.data.repository.ProductRepository;
import com.example.miscompras.data.repository.ShoppingRepository;
import com.example.miscompras.data.repository.StatsRepository;

/** Contenedor de dependencias: punto único de acceso a los repositorios. */
public final class DataGraph {

    private DataGraph() {
    }

    private static volatile ProductRepository productRepository;
    private static volatile ShoppingRepository shoppingRepository;
    private static volatile StatsRepository statsRepository;

    public static ProductRepository products(Context context) {
        if (productRepository == null) {
            synchronized (DataGraph.class) {
                if (productRepository == null) {
                    productRepository = new ProductRepository(AppDatabase.get(context));
                }
            }
        }
        return productRepository;
    }

    public static ShoppingRepository shopping(Context context) {
        if (shoppingRepository == null) {
            synchronized (DataGraph.class) {
                if (shoppingRepository == null) {
                    shoppingRepository = new ShoppingRepository(AppDatabase.get(context));
                }
            }
        }
        return shoppingRepository;
    }

    public static StatsRepository stats(Context context) {
        if (statsRepository == null) {
            synchronized (DataGraph.class) {
                if (statsRepository == null) {
                    statsRepository = new StatsRepository(AppDatabase.get(context));
                }
            }
        }
        return statsRepository;
    }
}
