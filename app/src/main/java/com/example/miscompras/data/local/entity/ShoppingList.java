package com.example.miscompras.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.example.miscompras.domain.ListStatus;
import com.example.miscompras.domain.ListType;

@Entity(tableName = "shopping_lists", indices = {@Index("status"), @Index("plannedDate")})
public class ShoppingList {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name = "";

    public ListType type = ListType.WEEKLY;

    public ListStatus status = ListStatus.PLANNED;

    public long plannedDate;

    public Long completedDate;

    public double estimatedTotal;

    public double realTotal;

    public double totalDiscount;

    public long createdAt;

    public long updatedAt;

    public ShoppingList() {
    }

    @androidx.room.Ignore
    public ShoppingList(@NonNull String name, ListType type, ListStatus status, long plannedDate) {
        this.name = name;
        this.type = type;
        this.status = status;
        this.plannedDate = plannedDate;
    }
}
