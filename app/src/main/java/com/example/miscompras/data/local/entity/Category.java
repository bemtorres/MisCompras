package com.example.miscompras.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories", indices = {@Index(value = "name", unique = true)})
public class Category {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name = "";

    @NonNull
    public String icon = "🛒";

    @NonNull
    public String colorHex = "#0E6B45";

    public Category() {
    }

    @androidx.room.Ignore
    public Category(@NonNull String name, @NonNull String colorHex) {
        this.name = name;
        this.colorHex = colorHex;
        this.icon = "🛒";
    }

    @androidx.room.Ignore
    public Category(@NonNull String name, @NonNull String icon, @NonNull String colorHex) {
        this.name = name;
        this.icon = icon;
        this.colorHex = colorHex;
    }
}
