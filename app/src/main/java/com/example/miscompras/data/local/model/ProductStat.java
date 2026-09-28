package com.example.miscompras.data.local.model;

import androidx.room.ColumnInfo;

/** Producto más comprado / con mayor gasto. */
public class ProductStat {

    @ColumnInfo(name = "name")
    public String name = "";

    @ColumnInfo(name = "quantity")
    public double quantity;

    @ColumnInfo(name = "total")
    public double total;
}
