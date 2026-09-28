package com.example.miscompras.data.local.model;

import androidx.room.ColumnInfo;

/** Fila del resumen de una lista de compra (totales + conteos). */
public class ListSummary {

    @ColumnInfo(name = "id")
    public long id;

    @ColumnInfo(name = "name")
    public String name = "";

    @ColumnInfo(name = "type")
    public String type = "WEEKLY";

    @ColumnInfo(name = "status")
    public String status = "PLANNED";

    @ColumnInfo(name = "plannedDate")
    public long plannedDate;

    @ColumnInfo(name = "completedDate")
    public Long completedDate;

    @ColumnInfo(name = "estimatedTotal")
    public double estimatedTotal;

    @ColumnInfo(name = "realTotal")
    public double realTotal;

    @ColumnInfo(name = "totalDiscount")
    public double totalDiscount;

    @ColumnInfo(name = "itemCount")
    public int itemCount;

    @ColumnInfo(name = "purchasedCount")
    public int purchasedCount;

    @ColumnInfo(name = "notFoundCount")
    public int notFoundCount;
}
