package com.example.miscompras.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.miscompras.data.local.entity.Category;

import java.util.List;

@Dao
public interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY name ASC")
    LiveData<List<Category>> observeAll();

    @Query("SELECT * FROM categories ORDER BY name ASC")
    List<Category> allSync();

    @Query("SELECT * FROM categories WHERE id = :id")
    Category findByIdSync(long id);

    @Query("SELECT COUNT(*) FROM categories")
    int countSync();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Category category);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<Category> categories);

    @Update
    void update(Category category);

    @Delete
    void delete(Category category);
}
