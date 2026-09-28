package com.example.miscompras.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.miscompras.domain.DiscountType;
import com.example.miscompras.domain.Pricing;
import com.example.miscompras.utils.Dates;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Carga inicial del catálogo completo desde assets/supermarket_catalog.json (25 categorías y productos con icono),
 * además de compras históricas y lista planificada inicial.
 */
final class Seed {

    private Seed() {
    }

    private static final class Line {
        String product;
        double quantity;
        double estimated;
        double real;
        DiscountType discountType = DiscountType.NONE;
        double discountValue;
        boolean notFound;
        String reason = "";
    }

    private static final class Purchase {
        final String listName;
        final String listType;
        final int daysAgo;
        final List<Line> lines = new ArrayList<>();

        Purchase(String listName, String listType, int daysAgo) {
            this.listName = listName;
            this.listType = listType;
            this.daysAgo = daysAgo;
        }

        Purchase line(String product, double quantity, double estimated, double real) {
            return line(product, quantity, estimated, real, DiscountType.NONE, 0d);
        }

        Purchase line(String product, double quantity, double estimated, double real, DiscountType type, double value) {
            Line line = new Line();
            line.product = product;
            line.quantity = quantity;
            line.estimated = estimated;
            line.real = real;
            line.discountType = type;
            line.discountValue = value;
            lines.add(line);
            return this;
        }

        Purchase missing(String product, double quantity, double estimated, String reason) {
            Line line = new Line();
            line.product = product;
            line.quantity = quantity;
            line.estimated = estimated;
            line.notFound = true;
            line.reason = reason;
            lines.add(line);
            return this;
        }
    }

    /** Lista activa que aparece en el panel de inicio. */
    private static final String[][] PLANNED = {
            {"Leche entera", "2", "1190"},
            {"Pan marraqueta", "4", "1990"},
            {"Huevos blancos", "1", "3290"},
            {"Arroz blanco", "1", "1490"},
            {"Café instantáneo", "1", "3990"},
            {"Papel higiénico", "1", "5990"},
            {"Detergente líquido", "1", "7990"},
            {"Manzana", "2", "1990"},
    };

    private static final String INSERT_CATEGORY =
            "INSERT INTO categories (id, name, icon, colorHex) VALUES (?, ?, ?, ?)";
    private static final String INSERT_PRODUCT =
            "INSERT INTO products (id, name, icon, categoryId, unit, estimatedPrice, currentStock, minimumStock, "
                    + "usualQuantity, isFavorite, isActive, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_LIST =
            "INSERT INTO shopping_lists (id, name, type, status, plannedDate, completedDate, estimatedTotal, "
                    + "realTotal, totalDiscount, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_ITEM =
            "INSERT INTO shopping_list_items (id, shoppingListId, productId, quantity, estimatedUnitPrice, realUnitPrice, "
                    + "finalUnitPrice, discountType, discountValue, estimatedSubtotal, realSubtotal, isPurchased, notFound, "
                    + "notFoundReason, substituteProductId, position, addedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_PRICE =
            "INSERT INTO price_history (id, productId, shoppingListId, quantity, price, discount, finalPrice, date) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_PURCHASE =
            "INSERT INTO purchase_history (id, shoppingListId, purchaseDate, itemCount, estimatedTotal, total, discount) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_TOTALS =
            "UPDATE shopping_lists SET estimatedTotal = ?, realTotal = ?, totalDiscount = ?, updatedAt = ? WHERE id = ?";

    private static final class Ids {
        long item = 1L;
        long price = 1L;
        long purchase = 1L;
    }

    static void populate(SupportSQLiteDatabase db, Context context) {
        long now = System.currentTimeMillis();
        Map<String, Long> productIds = insertCatalogFromJson(db, context, now);
        Ids ids = new Ids();
        long listId = 1L;
        List<Purchase> purchases = purchases();
        for (Purchase purchase : purchases) {
            insertPurchase(db, purchase, productIds, listId++, ids);
        }
        insertPlannedList(db, productIds, listId, ids, now);
    }

    private static Map<String, Long> insertCatalogFromJson(SupportSQLiteDatabase db, Context context, long now) {
        Map<String, Long> productIds = new HashMap<>();
        long productIdCounter = 1L;
        long categoryIdCounter = 1L;

        try {
            InputStream is = context.getAssets().open("supermarket_catalog.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, StandardCharsets.UTF_8);

            JSONObject root = new JSONObject(jsonStr);
            JSONArray categories = root.getJSONArray("categories");

            for (int i = 0; i < categories.length(); i++) {
                JSONObject catObj = categories.getJSONObject(i);
                long catId = catObj.optLong("id", categoryIdCounter++);
                String catName = catObj.getString("name");
                String catIcon = catObj.optString("icon", "🛒");
                String catColor = catObj.optString("colorHex", "#0E6B45");

                db.execSQL(INSERT_CATEGORY, new Object[]{catId, catName, catIcon, catColor});

                JSONArray products = catObj.optJSONArray("products");
                if (products != null) {
                    for (int j = 0; j < products.length(); j++) {
                        JSONObject prodObj = products.getJSONObject(j);
                        long prodId = productIdCounter++;
                        String prodName = prodObj.getString("name");
                        String prodIcon = prodObj.optString("icon", "🛍️");
                        String unit = prodObj.optString("unit", "Unidad");
                        double price = prodObj.optDouble("estimatedPrice", 1000d);

                        boolean isFavorite = (j < 3); // Destacar los primeros como favoritos
                        double usual = unit.equalsIgnoreCase("Kilo") ? 1d : 1d;

                        db.execSQL(INSERT_PRODUCT, new Object[]{
                                prodId,
                                prodName,
                                prodIcon,
                                catId,
                                unit,
                                price,
                                1d, // currentStock
                                1d, // minimumStock
                                usual,
                                isFavorite ? 1 : 0,
                                1, // isActive
                                now,
                                now
                        });
                        productIds.put(prodName, prodId);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return productIds;
    }

    private static void insertPurchase(SupportSQLiteDatabase db, Purchase purchase, Map<String, Long> productIds, long listId, Ids ids) {
        long date = Dates.daysAgo(purchase.daysAgo);
        db.execSQL(INSERT_LIST, new Object[]{
                listId, purchase.listName, purchase.listType, "COMPLETED", date, date,
                0d, 0d, 0d, date, date
        });

        double estimatedTotal = 0d;
        double realTotal = 0d;
        double discountTotal = 0d;
        int itemCount = 0;
        int position = 0;

        for (Line line : purchase.lines) {
            Long productId = productIds.get(line.product);
            if (productId == null) {
                continue;
            }
            double estimatedSubtotal = Pricing.subtotal(line.quantity, line.estimated);
            estimatedTotal += estimatedSubtotal;
            itemCount++;

            Double realUnit = null;
            Double finalUnit = null;
            double realSubtotal = 0d;
            int isPurchased = 0;
            int notFound = 0;
            String reason = "";
            DiscountType discountType = DiscountType.NONE;
            double discountValue = 0d;

            if (line.notFound) {
                notFound = 1;
                reason = line.reason;
            } else {
                isPurchased = 1;
                discountType = line.discountType;
                discountValue = line.discountValue;
                double unit = Pricing.finalUnitPrice(line.real, discountType, discountValue);
                realSubtotal = Pricing.subtotal(line.quantity, unit);
                realUnit = line.real;
                finalUnit = unit;
                realTotal += realSubtotal;
                discountTotal += Pricing.discountAmount(line.quantity, line.real, discountType, discountValue);

                db.execSQL(INSERT_PRICE, new Object[]{
                        ids.price++, productId, listId, line.quantity, line.real,
                        Pricing.discountAmount(line.real, discountType, discountValue), unit, date
                });
            }

            db.execSQL(INSERT_ITEM, new Object[]{
                    ids.item++, listId, productId, line.quantity, line.estimated, realUnit, finalUnit,
                    discountType.name(), discountValue, estimatedSubtotal, realSubtotal, isPurchased, notFound,
                    reason, null, position++, date
            });
        }

        db.execSQL(UPDATE_TOTALS, new Object[]{estimatedTotal, realTotal, discountTotal, date, listId});
        db.execSQL(INSERT_PURCHASE, new Object[]{
                ids.purchase++, listId, date, itemCount, estimatedTotal, realTotal, discountTotal
        });
    }

    private static void insertPlannedList(SupportSQLiteDatabase db, Map<String, Long> productIds, long listId, Ids ids, long now) {
        long plannedDate = Dates.startOfDay(now) + 3L * 24L * 60L * 60L * 1000L;
        db.execSQL(INSERT_LIST, new Object[]{
                listId, "Compra semanal", "WEEKLY", "PLANNED", plannedDate, null,
                0d, 0d, 0d, now, now
        });

        double estimatedTotal = 0d;
        int position = 0;
        for (String[] row : PLANNED) {
            Long productId = productIds.get(row[0]);
            if (productId == null) {
                continue;
            }
            double quantity = Double.parseDouble(row[1]);
            double price = Double.parseDouble(row[2]);
            double subtotal = Pricing.subtotal(quantity, price);
            estimatedTotal += subtotal;
            db.execSQL(INSERT_ITEM, new Object[]{
                    ids.item++, listId, productId, quantity, price, null, null,
                    DiscountType.NONE.name(), 0d, subtotal, 0d, 0, 0, "", null, position++, now
            });
        }
        db.execSQL(UPDATE_TOTALS, new Object[]{estimatedTotal, 0d, 0d, now, listId});
    }

    private static List<Purchase> purchases() {
        List<Purchase> purchases = new ArrayList<>();
        purchases.add(new Purchase("Compra mensual grande", "MONTHLY", 28)
                .line("Leche entera", 4, 1190d, 1190d)
                .line("Pan marraqueta", 8, 1990d, 1950d)
                .line("Arroz blanco", 2, 1490d, 1490d)
                .line("Huevos blancos", 2, 3290d, 3290d)
                .line("Café instantáneo", 1, 3990d, 4190d)
                .line("Detergente líquido", 2, 7990d, 7990d, DiscountType.PERCENT, 15d)
                .line("Papel higiénico", 2, 5990d, 5990d)
                .line("Aceite vegetal", 2, 2390d, 2290d)
                .line("Coca-Cola", 4, 1990d, 1990d)
                .line("Atún en agua", 6, 1490d, 1490d)
                .line("Manzana", 3, 1990d, 2090d)
                .line("Tomate", 2, 1690d, 1690d)
                .line("Shampoo", 1, 3990d, 3990d)
                .missing("Alimento seco para perro", 1, 9990d, "OUT_OF_STOCK"));

        purchases.add(new Purchase("Compra semanal", "WEEKLY", 21)
                .line("Leche entera", 2, 1190d, 1190d)
                .line("Pan marraqueta", 4, 1990d, 1990d)
                .line("Huevos blancos", 1, 3290d, 3190d)
                .line("Arroz blanco", 1, 1490d, 1490d)
                .line("Café instantáneo", 1, 3990d, 3990d)
                .line("Sprite", 4, 1890d, 1790d, DiscountType.PERCENT, 10d)
                .line("Manzana", 2, 1990d, 1990d)
                .line("Detergente líquido", 1, 7990d, 7990d)
                .missing("Palta", 1, 4990d, "OUT_OF_STOCK"));

        purchases.add(new Purchase("Compra semanal", "WEEKLY", 14)
                .line("Leche entera", 2, 1190d, 1150d)
                .line("Pan marraqueta", 6, 1990d, 1990d)
                .line("Huevos blancos", 1, 3290d, 3290d)
                .line("Café instantáneo", 1, 3990d, 3500d, DiscountType.FIXED, 490d)
                .line("Papel higiénico", 1, 5990d, 5990d)
                .line("Coca-Cola", 4, 1990d, 1990d)
                .line("Agua mineral", 4, 990d, 990d)
                .line("Azúcar blanca", 1, 1290d, 1290d)
                .line("Lavalozas", 1, 1990d, 1790d, DiscountType.PERCENT, 10d)
                .line("Tomate", 1, 1690d, 1690d));

        purchases.add(new Purchase("Compra semanal", "WEEKLY", 7)
                .line("Leche entera", 2, 1190d, 1190d)
                .line("Pan marraqueta", 4, 1990d, 1990d)
                .line("Huevos blancos", 1, 3290d, 3290d)
                .line("Arroz blanco", 1, 1490d, 1490d)
                .line("Café instantáneo", 1, 3990d, 3990d)
                .line("Coca-Cola", 4, 1990d, 1890d, DiscountType.PERCENT, 10d)
                .line("Manzana", 2, 1990d, 1890d)
                .line("Helados", 1, 3490d, 3190d, DiscountType.PERCENT, 10d)
                .line("Cerveza", 2, 4990d, 4490d, DiscountType.PERCENT, 10d)
                .line("Papel higiénico", 1, 5990d, 5990d)
                .missing("Detergente líquido", 1, 7990d, "OUT_OF_STOCK"));
        return purchases;
    }
}
