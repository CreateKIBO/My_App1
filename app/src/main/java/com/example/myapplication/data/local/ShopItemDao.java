package com.example.myapplication.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ShopItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ShopItemEntity item);

    @Query("SELECT * FROM shop_items WHERE type = :type")
    LiveData<List<ShopItemEntity>> getItemsByType(String type);

    @Query("SELECT * FROM shop_items WHERE id = :itemId")
    ShopItemEntity getItemById(long itemId);
}