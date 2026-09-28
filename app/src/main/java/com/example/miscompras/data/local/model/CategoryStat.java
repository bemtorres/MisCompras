package com.example.miscompras.data.local.model;

import androidx.room.ColumnInfo;

/** Gasto acumulado por categoría. */
public class CategoryStat {

    @ColumnInfo(name = "name")
    public String name = "";

    @ColumnInfo(name = "colorHex")
    public String colorHex = "#0E6B45";

    @ColumnInfo(name = "total")
    public double total;
}
