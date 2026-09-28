package com.example.miscompras.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.example.miscompras.domain.DiscountType;
import com.example.miscompras.domain.ItemStatus;
import com.example.miscompras.domain.NotFoundReason;

@Entity(tableName = "shopping_list_items",
        foreignKeys = {
                @ForeignKey(entity = ShoppingList.class, parentColumns = "id", childColumns = "shoppingListId", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Product.class, parentColumns = "id", childColumns = "productId", onDelete = ForeignKey.CASCADE)
        },
        indices = {@Index("shoppingListId"), @Index("productId")})
public class ShoppingListItem {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long shoppingListId;

    public long productId;

    public double quantity = 1d;

    public double estimatedUnitPrice;

    public Double realUnitPrice;

    public DiscountType discountType = DiscountType.NONE;

    public double discountValue;

    public Double finalUnitPrice;

    public double estimatedSubtotal;

    public double realSubtotal;

    public boolean isPurchased;

    public boolean notFound;

    @NonNull
    public String notFoundReason = "";

    public Long substituteProductId;

    public int position;

    public long addedAt;

    public ShoppingListItem() {
    }

    @androidx.room.Ignore
    public ItemStatus status() {
        if (notFound) {
            return ItemStatus.NOT_FOUND;
        }
        return isPurchased ? ItemStatus.PURCHASED : ItemStatus.PENDING;
    }

    @androidx.room.Ignore
    public NotFoundReason reason() {
        return NotFoundReason.fromName(notFoundReason.isEmpty() ? null : notFoundReason);
    }

    public void clearPurchase() {
        isPurchased = false;
        notFound = false;
        notFoundReason = "";
    }
}
