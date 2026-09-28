package com.example.miscompras.data.local.model;

import androidx.room.ColumnInfo;

/** Proyección de lectura: registro de historial de precio. */
public class PriceRow {

    @ColumnInfo(name = "id")
    public long id;

    @ColumnInfo(name = "productId")
    public long productId;

    @ColumnInfo(name = "productName")
    public String productName = "";

    @ColumnInfo(name = "quantity")
    public double quantity;

    @ColumnInfo(name = "price")
    public double price;

    @ColumnInfo(name = "discount")
    public double discount;

    @ColumnInfo(name = "finalPrice")
    public double finalPrice;

    @ColumnInfo(name = "date")
    public long date;
}
