package com.example.miscompras.data.local.model;

import androidx.room.ColumnInfo;

/** Proyección de lectura: totales de un mes. */
public class MonthTotal {

    @ColumnInfo(name = "period")
    public String period = "";

    @ColumnInfo(name = "total")
    public double total;

    @ColumnInfo(name = "estimated")
    public double estimated;

    @ColumnInfo(name = "discount")
    public double discount;

    @ColumnInfo(name = "purchases")
    public int purchases;

    @ColumnInfo(name = "startMillis")
    public long startMillis;

    @ColumnInfo(name = "endMillis")
    public long endMillis;
}
