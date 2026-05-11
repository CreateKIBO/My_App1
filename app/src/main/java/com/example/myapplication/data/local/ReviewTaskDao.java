package com.example.myapplication.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ReviewTaskDao {

    @Insert
    long insert(ReviewTaskEntity task);

    @Insert
    void insertAll(List<ReviewTaskEntity> tasks);

    @Update
    void update(ReviewTaskEntity task);

    @Query("SELECT * FROM review_tasks WHERE userId = :userId AND reviewDate = :date AND isCompleted = 0 ORDER BY step ASC")
    LiveData<List<ReviewTaskEntity>> getPendingByDate(long userId, String date);

    @Query("SELECT * FROM review_tasks WHERE userId = :userId AND isCompleted = 0 AND reviewDate <= :today ORDER BY reviewDate ASC")
    LiveData<List<ReviewTaskEntity>> getOverdueReviews(long userId, String today);

    @Query("SELECT * FROM review_tasks WHERE userId = :userId ORDER BY reviewDate DESC")
    LiveData<List<ReviewTaskEntity>> getByUserId(long userId);

    @Query("SELECT COUNT(*) FROM review_tasks WHERE userId = :userId AND reviewDate = :date AND isCompleted = 0")
    LiveData<Integer> getPendingCountByDate(long userId, String date);

    @Query("SELECT * FROM review_tasks WHERE curveItemId = :curveItemId ORDER BY step ASC")
    List<ReviewTaskEntity> getByCurveItemId(long curveItemId);

    @Query("DELETE FROM review_tasks WHERE curveItemId = :curveItemId")
    void deleteByCurveItemId(long curveItemId);
}
