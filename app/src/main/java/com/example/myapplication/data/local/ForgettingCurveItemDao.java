package com.example.myapplication.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ForgettingCurveItemDao {

    @Insert
    long insert(ForgettingCurveItemEntity item);

    @Update
    void update(ForgettingCurveItemEntity item);

    @Query("SELECT * FROM forgetting_curve_items WHERE userId = :userId ORDER BY nextReviewDate ASC")
    LiveData<List<ForgettingCurveItemEntity>> getByUserId(long userId);

    @Query("SELECT * FROM forgetting_curve_items WHERE userId = :userId AND nextReviewDate <= :today AND isMastered = 0 ORDER BY nextReviewDate ASC")
    LiveData<List<ForgettingCurveItemEntity>> getPendingReviews(long userId, String today);

    @Query("SELECT * FROM forgetting_curve_items WHERE userId = :userId AND isMastered = 1 ORDER BY nextReviewDate DESC")
    LiveData<List<ForgettingCurveItemEntity>> getMasteredItems(long userId);

    @Query("SELECT COUNT(*) FROM forgetting_curve_items WHERE userId = :userId AND isMastered = 0")
    LiveData<Integer> getActiveCount(long userId);

    @Query("SELECT COUNT(*) FROM forgetting_curve_items WHERE userId = :userId AND isMastered = 1")
    LiveData<Integer> getMasteredCount(long userId);

    @Query("SELECT * FROM forgetting_curve_items WHERE id = :id")
    ForgettingCurveItemEntity getById(long id);

    @Query("SELECT * FROM forgetting_curve_items WHERE taskId = :taskId")
    ForgettingCurveItemEntity getByTaskId(long taskId);
}
