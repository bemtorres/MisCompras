package com.example.miscompras.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.miscompras.data.local.entity.PurchaseHistory;
import com.example.miscompras.data.local.entity.ShoppingList;
import com.example.miscompras.data.local.model.ListSummary;
import com.example.miscompras.data.local.model.MonthTotal;

import java.util.List;

@Dao
public interface ShoppingListDao {

    String LIST_PROJECTION = "SELECT l.id AS id, l.name AS name, l.type AS type, l.status AS status, "
            + "l.plannedDate AS plannedDate, l.completedDate AS completedDate, l.estimatedTotal AS estimatedTotal, "
            + "l.realTotal AS realTotal, l.totalDiscount AS totalDiscount, "
            + "(SELECT COUNT(*) FROM shopping_list_items i WHERE i.shoppingListId = l.id) AS itemCount, "
            + "(SELECT COUNT(*) FROM shopping_list_items i WHERE i.shoppingListId = l.id AND i.isPurchased = 1) AS purchasedCount, "
            + "(SELECT COUNT(*) FROM shopping_list_items i WHERE i.shoppingListId = l.id AND i.notFound = 1) AS notFoundCount "
            + "FROM shopping_lists l ";

    @Query(LIST_PROJECTION + "ORDER BY l.plannedDate DESC, l.id DESC")
    LiveData<List<ListSummary>> observeAll();

    @Query(LIST_PROJECTION + "WHERE l.status IN ('PLANNED', 'IN_PROGRESS') ORDER BY l.updatedAt DESC LIMIT 1")
    LiveData<ListSummary> observeActive();

    @Query(LIST_PROJECTION + "WHERE l.id = :id")
    LiveData<ListSummary> observeById(long id);

    @Query(LIST_PROJECTION + "WHERE l.status = 'COMPLETED' ORDER BY COALESCE(l.completedDate, l.plannedDate) DESC")
    LiveData<List<ListSummary>> observeCompleted();

    @Query(LIST_PROJECTION + "WHERE l.status = 'COMPLETED' ORDER BY COALESCE(l.completedDate, l.plannedDate) DESC")
    List<ListSummary> completedSync();

    @Query(LIST_PROJECTION + "ORDER BY l.plannedDate DESC, l.id DESC")
    List<ListSummary> allSync();

    @Query("SELECT * FROM shopping_lists WHERE id = :id")
    ShoppingList findEntitySync(long id);

    @Query("SELECT * FROM shopping_lists WHERE status IN ('PLANNED', 'IN_PROGRESS') ORDER BY updatedAt DESC LIMIT 1")
    ShoppingList findActiveSync();

    @Query("SELECT COUNT(*) FROM shopping_lists")
    int countSync();

    @Query("SELECT strftime('%Y-%m', COALESCE(completedDate, plannedDate)/1000, 'unixepoch', 'localtime') AS period, "
            + "COALESCE(SUM(realTotal), 0) AS total, COALESCE(SUM(estimatedTotal), 0) AS estimated, "
            + "COALESCE(SUM(totalDiscount), 0) AS discount, COUNT(*) AS purchases, "
            + "MIN(COALESCE(completedDate, plannedDate)) AS startMillis, MAX(COALESCE(completedDate, plannedDate)) AS endMillis "
            + "FROM shopping_lists WHERE status = 'COMPLETED' GROUP BY period ORDER BY period DESC LIMIT :limit")
    List<MonthTotal> monthsSync(int limit);

    @Insert
    long insert(ShoppingList list);

    @Update
    void update(ShoppingList list);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertPurchaseHistory(PurchaseHistory history);

    @Query("UPDATE shopping_lists SET status = :status, completedDate = :completedDate, updatedAt = :updatedAt WHERE id = :id")
    void updateStatus(long id, String status, Long completedDate, long updatedAt);

    @Query("UPDATE shopping_lists SET name = :name, type = :type, plannedDate = :plannedDate, updatedAt = :updatedAt WHERE id = :id")
    void updateHeader(long id, String name, String type, long plannedDate, long updatedAt);

    @Query("UPDATE shopping_lists SET estimatedTotal = :estimated, realTotal = :real, totalDiscount = :discount, updatedAt = :updatedAt WHERE id = :id")
    void updateTotals(long id, double estimated, double real, double discount, long updatedAt);

    @Query("DELETE FROM shopping_lists WHERE id = :id")
    void deleteById(long id);
}
