package com.example.miscompras.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "purchase_history",
        foreignKeys = @ForeignKey(entity = ShoppingList.class, parentColumns = "id", childColumns = "shoppingListId", onDelete = ForeignKey.CASCADE),
        indices = {@Index("shoppingListId"), @Index("purchaseDate")})
public class PurchaseHistory {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long shoppingListId;

    public long purchaseDate;

    public int itemCount;

    public double estimatedTotal;

    public double total;

    public double discount;

    public PurchaseHistory() {
    }
}
