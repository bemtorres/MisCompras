package com.example.miscompras.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "price_history",
        foreignKeys = {
                @ForeignKey(entity = Product.class, parentColumns = "id", childColumns = "productId", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = ShoppingList.class, parentColumns = "id", childColumns = "shoppingListId", onDelete = ForeignKey.CASCADE)
        },
        indices = {@Index("productId"), @Index("shoppingListId"), @Index("date")})
public class PriceHistory {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long productId;

    public long shoppingListId;

    public double quantity;

    public double price;

    public double discount;

    public double finalPrice;

    public long date;

    public PriceHistory() {
    }
}
