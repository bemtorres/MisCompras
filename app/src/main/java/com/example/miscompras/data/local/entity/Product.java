package com.example.miscompras.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "products", foreignKeys = @ForeignKey(
        entity = Category.class,
        parentColumns = "id",
        childColumns = "categoryId",
        onDelete = ForeignKey.RESTRICT),
        indices = {@Index("categoryId"), @Index("name")})
public class Product {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name = "";

    @NonNull
    public String icon = "🛍️";

    public long categoryId;

    @NonNull
    public String unit = "unidad";

    public double estimatedPrice;

    public double currentStock;

    public double minimumStock;

    public double usualQuantity = 1d;

    public boolean isFavorite;

    public boolean isActive = true;

    public long createdAt;

    public long updatedAt;

    public Product() {
    }
}
