package com.example.miscompras.data.local.model;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;

/** Proyección de lectura: producto del catálogo. */
public class ProductRow {

    @ColumnInfo(name = "id")
    public long id;

    @ColumnInfo(name = "name")
    public String name = "";

    @ColumnInfo(name = "icon")
    public String icon = "🛍️";

    @ColumnInfo(name = "categoryId")
    public long categoryId;

    @ColumnInfo(name = "categoryName")
    public String categoryName = "";

    @ColumnInfo(name = "unit")
    public String unit = "";

    @ColumnInfo(name = "estimatedPrice")
    public double estimatedPrice;

    @ColumnInfo(name = "currentStock")
    public double currentStock;

    @ColumnInfo(name = "minimumStock")
    public double minimumStock;

    @ColumnInfo(name = "usualQuantity")
    public double usualQuantity = 1d;

    @ColumnInfo(name = "isFavorite")
    public boolean isFavorite;

    @ColumnInfo(name = "isActive")
    public boolean isActive;

    @Nullable
    @ColumnInfo(name = "lastPrice")
    public Double lastPrice;

    @Nullable
    @ColumnInfo(name = "averagePrice")
    public Double averagePrice;

    @ColumnInfo(name = "purchases")
    public int purchases;

    public boolean isLowStock() {
        return currentStock <= minimumStock;
    }
}
