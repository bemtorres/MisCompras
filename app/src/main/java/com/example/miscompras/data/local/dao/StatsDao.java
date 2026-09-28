package com.example.miscompras.data.local.dao;

import androidx.room.Dao;
import androidx.room.Query;

import com.example.miscompras.data.local.model.CategoryStat;
import com.example.miscompras.data.local.model.ProductStat;

import java.util.List;

@Dao
public interface StatsDao {

    @Query("SELECT p.name AS name, SUM(i.quantity) AS quantity, SUM(i.realSubtotal) AS total "
            + "FROM shopping_list_items i "
            + "JOIN products p ON p.id = i.productId "
            + "JOIN shopping_lists l ON l.id = i.shoppingListId "
            + "WHERE l.status = 'COMPLETED' AND i.isPurchased = 1 "
            + "GROUP BY i.productId ORDER BY quantity DESC LIMIT :limit")
    List<ProductStat> topByQuantitySync(int limit);

    @Query("SELECT p.name AS name, SUM(i.quantity) AS quantity, SUM(i.realSubtotal) AS total "
            + "FROM shopping_list_items i "
            + "JOIN products p ON p.id = i.productId "
            + "JOIN shopping_lists l ON l.id = i.shoppingListId "
            + "WHERE l.status = 'COMPLETED' AND i.isPurchased = 1 "
            + "GROUP BY i.productId ORDER BY total DESC LIMIT :limit")
    List<ProductStat> topBySpendSync(int limit);

    @Query("SELECT COALESCE(c.name, 'Otros') AS name, c.colorHex AS colorHex, SUM(i.realSubtotal) AS total "
            + "FROM shopping_list_items i "
            + "JOIN products p ON p.id = i.productId "
            + "JOIN shopping_lists l ON l.id = i.shoppingListId "
            + "LEFT JOIN categories c ON c.id = p.categoryId "
            + "WHERE l.status = 'COMPLETED' AND i.isPurchased = 1 "
            + "GROUP BY p.categoryId ORDER BY total DESC LIMIT :limit")
    List<CategoryStat> byCategorySync(int limit);

    @Query("SELECT p.name AS name, COUNT(*) AS quantity, 0.0 AS total "
            + "FROM shopping_list_items i "
            + "JOIN products p ON p.id = i.productId "
            + "JOIN shopping_lists l ON l.id = i.shoppingListId "
            + "WHERE i.notFound = 1 "
            + "GROUP BY i.productId ORDER BY quantity DESC LIMIT :limit")
    List<ProductStat> mostNotFoundSync(int limit);
}
