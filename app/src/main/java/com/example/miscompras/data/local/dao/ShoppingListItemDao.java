package com.example.miscompras.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.miscompras.data.local.entity.ShoppingListItem;
import com.example.miscompras.data.local.model.ItemRow;

import java.util.List;

@Dao
public interface ShoppingListItemDao {

    String ITEM_PROJECTION = "SELECT i.id AS id, i.shoppingListId AS shoppingListId, i.productId AS productId, "
            + "p.name AS productName, p.icon AS productIcon, COALESCE(c.name, '') AS categoryName, p.unit AS unit, i.quantity AS quantity, "
            + "i.estimatedUnitPrice AS estimatedUnitPrice, i.realUnitPrice AS realUnitPrice, i.discountType AS discountType, "
            + "i.discountValue AS discountValue, i.finalUnitPrice AS finalUnitPrice, i.estimatedSubtotal AS estimatedSubtotal, "
            + "i.realSubtotal AS realSubtotal, i.isPurchased AS isPurchased, i.notFound AS notFound, "
            + "i.notFoundReason AS notFoundReason, COALESCE(sub.name, '') AS substituteName, i.position AS position "
            + "FROM shopping_list_items i "
            + "JOIN products p ON p.id = i.productId "
            + "LEFT JOIN categories c ON c.id = p.categoryId "
            + "LEFT JOIN products sub ON sub.id = i.substituteProductId ";

    @Query(ITEM_PROJECTION + "WHERE i.shoppingListId = :listId ORDER BY i.position ASC, i.id ASC")
    LiveData<List<ItemRow>> observeByList(long listId);

    @Query(ITEM_PROJECTION + "WHERE i.shoppingListId = :listId ORDER BY i.position ASC, i.id ASC")
    List<ItemRow> findByListSync(long listId);

    @Query(ITEM_PROJECTION + "WHERE i.shoppingListId = :listId AND i.isPurchased = 0 AND i.notFound = 0 ORDER BY i.position ASC, i.id ASC")
    List<ItemRow> findPendingSync(long listId);

    @Query(ITEM_PROJECTION + "WHERE i.shoppingListId = :listId AND i.isPurchased = 0 AND i.notFound = 0 ORDER BY i.position ASC, i.id ASC LIMIT 1")
    ItemRow findFirstPendingSync(long listId);

    @Query(ITEM_PROJECTION + "WHERE i.id = :itemId")
    ItemRow findRowByIdSync(long itemId);

    @Query("SELECT * FROM shopping_list_items WHERE id = :id")
    ShoppingListItem findEntitySync(long id);

    @Query("SELECT * FROM shopping_list_items WHERE shoppingListId = :listId")
    List<ShoppingListItem> findEntitiesSync(long listId);

    @Query("SELECT COUNT(*) FROM shopping_list_items WHERE shoppingListId = :listId AND productId = :productId")
    int countForProductSync(long listId, long productId);

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM shopping_list_items WHERE shoppingListId = :listId")
    int nextPositionSync(long listId);

    @Query("SELECT COUNT(*) FROM shopping_list_items WHERE shoppingListId = :listId")
    int countByListSync(long listId);

    @Query("SELECT COUNT(*) FROM shopping_list_items WHERE shoppingListId = :listId AND (isPurchased = 1 OR notFound = 1)")
    int countResolvedSync(long listId);

    @Query("SELECT COUNT(*) FROM shopping_list_items WHERE isPurchased = 0 AND notFound = 0")
    int countPendingGlobalSync();

    @Insert
    long insert(ShoppingListItem item);

    @Update
    void update(ShoppingListItem item);

    @Query("DELETE FROM shopping_list_items WHERE id = :id")
    void deleteById(long id);

    @Query("DELETE FROM shopping_list_items WHERE shoppingListId = :listId")
    void deleteByList(long listId);

    @Query("UPDATE shopping_list_items SET position = :position WHERE id = :id")
    void updatePosition(long id, int position);
}
