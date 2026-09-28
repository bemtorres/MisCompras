package com.example.miscompras.data.local.model;

import androidx.room.ColumnInfo;

/** Resumen del historial de precios de un producto. */
public class ProductPriceStats {

    @ColumnInfo(name = "average")
    public double average;

    @ColumnInfo(name = "min")
    public double min;

    @ColumnInfo(name = "max")
    public double max;

    @ColumnInfo(name = "lastPrice")
    public double lastPrice;

    @ColumnInfo(name = "count")
    public int count;

    @ColumnInfo(name = "totalSpent")
    public double totalSpent;
}
