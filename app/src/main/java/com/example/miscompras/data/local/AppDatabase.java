package com.example.miscompras.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.miscompras.data.local.dao.CategoryDao;
import com.example.miscompras.data.local.dao.PriceHistoryDao;
import com.example.miscompras.data.local.dao.ProductDao;
import com.example.miscompras.data.local.dao.ShoppingListDao;
import com.example.miscompras.data.local.dao.ShoppingListItemDao;
import com.example.miscompras.data.local.dao.StatsDao;
import com.example.miscompras.data.local.entity.Category;
import com.example.miscompras.data.local.entity.PriceHistory;
import com.example.miscompras.data.local.entity.Product;
import com.example.miscompras.data.local.entity.PurchaseHistory;
import com.example.miscompras.data.local.entity.ShoppingList;
import com.example.miscompras.data.local.entity.ShoppingListItem;

@Database(
        entities = {
                Category.class,
                Product.class,
                ShoppingList.class,
                ShoppingListItem.class,
                PriceHistory.class,
                PurchaseHistory.class
        },
        version = 2,
        exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static final String DB_NAME = "mis_compras.db";

    private static volatile AppDatabase instance;

    public abstract CategoryDao categoryDao();

    public abstract ProductDao productDao();

    public abstract ShoppingListDao shoppingListDao();

    public abstract ShoppingListItemDao shoppingListItemDao();

    public abstract PriceHistoryDao priceHistoryDao();

    public abstract StatsDao statsDao();

    public static AppDatabase get(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = build(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private static AppDatabase build(Context appContext) {
        return Room.databaseBuilder(appContext, AppDatabase.class, DB_NAME)
                .addCallback(new SeedCallback(appContext))
                .fallbackToDestructiveMigration()
                .build();
    }

    private static class SeedCallback extends RoomDatabase.Callback {
        private final Context context;

        SeedCallback(Context context) {
            this.context = context;
        }

        @Override
        public void onCreate(androidx.sqlite.db.SupportSQLiteDatabase db) {
            super.onCreate(db);
            Seed.populate(db, context);
        }
    }
}
