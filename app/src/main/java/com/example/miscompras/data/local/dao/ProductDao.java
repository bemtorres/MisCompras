package com.example.miscompras.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.miscompras.data.local.entity.Product;
import com.example.miscompras.data.local.model.ProductRow;

import java.util.List;

@Dao
public interface ProductDao {

    String PRODUCT_PROJECTION = "SELECT p.id AS id, p.name AS name, p.icon AS icon, p.categoryId AS categoryId, "
            + "COALESCE(c.name, '') AS categoryName, p.unit AS unit, p.estimatedPrice AS estimatedPrice, "
            + "p.currentStock AS currentStock, p.minimumStock AS minimumStock, p.usualQuantity AS usualQuantity, "
            + "p.isFavorite AS isFavorite, p.isActive AS isActive, "
            + "(SELECT ph.finalPrice FROM price_history ph WHERE ph.productId = p.id ORDER BY ph.date DESC LIMIT 1) AS lastPrice, "
            + "(SELECT AVG(ph.finalPrice) FROM price_history ph WHERE ph.productId = p.id) AS averagePrice, "
            + "(SELECT COUNT(*) FROM price_history ph WHERE ph.productId = p.id) AS purchases "
            + "FROM products p LEFT JOIN categories c ON c.id = p.categoryId ";

    @Query(PRODUCT_PROJECTION + "ORDER BY p.isFavorite DESC, p.name ASC")
    LiveData<List<ProductRow>> observeAll();

    @Query(PRODUCT_PROJECTION + "WHERE p.isActive = 1 ORDER BY p.isFavorite DESC, p.name ASC")
    LiveData<List<ProductRow>> observeActive();

    @Query(PRODUCT_PROJECTION + "WHERE p.id = :id")
    LiveData<List<ProductRow>> observeById(long id);

    @Query(PRODUCT_PROJECTION + "WHERE p.isActive = 1 AND p.name LIKE '%' || :query || '%' ORDER BY p.isFavorite DESC, p.name ASC")
    List<ProductRow> searchSync(String query);

    @Query(PRODUCT_PROJECTION + "WHERE p.isActive = 1 AND p.name LIKE '%' || :query || '%' ORDER BY p.isFavorite DESC, p.name ASC")
    LiveData<List<ProductRow>> searchLive(String query);

    @Query("DELETE FROM products WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM products WHERE id = :id")
    Product findEntitySync(long id);

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY isFavorite DESC, name ASC")
    List<Product> activeSync();

    @Query("SELECT * FROM products ORDER BY isFavorite DESC, name ASC")
    List<Product> allSync();

    @Query("SELECT COUNT(*) FROM products")
    int countSync();

    @Insert
    long insert(Product product);

    @Update
    void update(Product product);

    @Delete
    void delete(Product product);

    @Query("UPDATE products SET isFavorite = :favorite, updatedAt = :updatedAt WHERE id = :id")
    void setFavorite(long id, boolean favorite, long updatedAt);

    @Query("UPDATE products SET currentStock = :stock, updatedAt = :updatedAt WHERE id = :id")
    void setStock(long id, double stock, long updatedAt);

    /** Productos que más se han comprado según el historial. */
    @Query("SELECT p.* FROM products p INNER JOIN "
            + "(SELECT productId, COUNT(*) AS times, MAX(date) AS lastDate FROM price_history GROUP BY productId "
            + "ORDER BY times DESC, lastDate DESC) ph ON ph.productId = p.id "
            + "ORDER BY ph.times DESC, ph.lastDate DESC LIMIT :limit")
    List<Product> mostPurchasedSync(int limit);

    /** Productos habituales cuya compra periodicidad sugiere reponer. */
    @Query("SELECT p.* FROM products p INNER JOIN "
            + "(SELECT productId, COUNT(*) AS times, MIN(date) AS firstDate, MAX(date) AS lastDate FROM price_history GROUP BY productId) ph "
            + "ON ph.productId = p.id WHERE p.isActive = 1 ORDER BY ph.times DESC, ph.lastDate DESC LIMIT :limit")
    List<Product> habitualSync(int limit);
}
