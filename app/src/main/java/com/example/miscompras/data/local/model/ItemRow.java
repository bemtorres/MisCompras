package com.example.miscompras.data.local.model;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;

/** Proyección de lectura: ítem de una lista con los datos de su producto. */
public class ItemRow {

    @ColumnInfo(name = "id")
    public long id;

    @ColumnInfo(name = "shoppingListId")
    public long shoppingListId;

    @ColumnInfo(name = "productId")
    public long productId;

    @ColumnInfo(name = "productName")
    public String productName = "";

    @ColumnInfo(name = "productIcon")
    public String productIcon = "🛍️";

    @ColumnInfo(name = "categoryName")
    public String categoryName = "";

    @ColumnInfo(name = "unit")
    public String unit = "";

    @ColumnInfo(name = "quantity")
    public double quantity = 1d;

    @ColumnInfo(name = "estimatedUnitPrice")
    public double estimatedUnitPrice;

    @Nullable
    @ColumnInfo(name = "realUnitPrice")
    public Double realUnitPrice;

    @ColumnInfo(name = "discountType")
    public String discountType = "NONE";

    @ColumnInfo(name = "discountValue")
    public double discountValue;

    @Nullable
    @ColumnInfo(name = "finalUnitPrice")
    public Double finalUnitPrice;

    @ColumnInfo(name = "estimatedSubtotal")
    public double estimatedSubtotal;

    @ColumnInfo(name = "realSubtotal")
    public double realSubtotal;

    @ColumnInfo(name = "isPurchased")
    public boolean isPurchased;

    @ColumnInfo(name = "notFound")
    public boolean notFound;

    @ColumnInfo(name = "notFoundReason")
    public String notFoundReason = "";

    @ColumnInfo(name = "substituteName")
    public String substituteName = "";

    @ColumnInfo(name = "position")
    public int position;

    public double computeEffectiveUnit() {
        if (finalUnitPrice != null) {
            return finalUnitPrice;
        }
        if (realUnitPrice != null) {
            return realUnitPrice;
        }
        return estimatedUnitPrice;
    }
}
