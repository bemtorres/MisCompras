package com.example.miscompras.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.miscompras.data.local.entity.PriceHistory;
import com.example.miscompras.data.local.model.PriceRow;
import com.example.miscompras.data.local.model.ProductPriceStats;

import java.util.List;

@Dao
public interface PriceHistoryDao {

    String PRICE_PROJECTION = "SELECT ph.id AS id, ph.productId AS productId, p.name AS productName, "
            + "ph.quantity AS quantity, ph.price AS price, ph.discount AS discount, ph.finalPrice AS finalPrice, ph.date AS date "
            + "FROM price_history ph JOIN products p ON p.id = ph.productId ";

    @Query(PRICE_PROJECTION + "WHERE ph.productId = :productId ORDER BY ph.date DESC, ph.id DESC")
    LiveData<List<PriceRow>> observeForProduct(long productId);

    @Query(PRICE_PROJECTION + "ORDER BY ph.date DESC, ph.id DESC LIMIT :limit")
    List<PriceRow> recentSync(int limit);

    @Query("SELECT COALESCE(AVG(finalPrice), 0) AS \"average\", COALESCE(MIN(finalPrice), 0) AS \"min\", "
            + "COALESCE(MAX(finalPrice), 0) AS \"max\", "
            + "COALESCE((SELECT finalPrice FROM price_history WHERE productId = :productId ORDER BY date DESC LIMIT 1), 0) AS lastPrice, "
            + "COUNT(*) AS \"count\", COALESCE(SUM(finalPrice * quantity), 0) AS totalSpent "
            + "FROM price_history WHERE productId = :productId")
    LiveData<ProductPriceStats> observeStats(long productId);

    @Query("SELECT COALESCE(SUM(discount), 0) FROM price_history WHERE date BETWEEN :from AND :to")
    double sumDiscountSync(long from, long to);

    @Insert
    long insert(PriceHistory history);

    @Query("DELETE FROM price_history WHERE shoppingListId = :listId")
    void deleteByList(long listId);
}
