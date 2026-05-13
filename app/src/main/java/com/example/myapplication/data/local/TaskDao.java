package com.example.myapplication.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TaskDao {

    @Insert
    long insert(TaskEntity task);

    @Update
    void update(TaskEntity task);

    @Delete
    void delete(TaskEntity task);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND date = :date ORDER BY startTime ASC")
    LiveData<List<TaskEntity>> getTasksByDate(long userId, String date);

    @Query("SELECT * FROM tasks WHERE id = :id")
    TaskEntity getTaskById(long id);

    @Query("SELECT DISTINCT date FROM tasks WHERE userId = :userId ORDER BY date DESC")
    LiveData<List<String>> getDatesWithTasks(long userId);

    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :userId AND date = :date AND isCompleted = 1")
    LiveData<Integer> getCompletedCountForDate(long userId, String date);

    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :userId AND date = :date")
    LiveData<Integer> getTotalCountForDate(long userId, String date);

    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :userId AND date = :date AND isCompleted = 1")
    int getCompletedCountForDateSync(long userId, String date);

    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :userId AND date = :date")
    int getTotalCountForDateSync(long userId, String date);

    @Query("DELETE FROM tasks WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :userId AND isCompleted = 1")
    LiveData<Integer> getTotalCompletedCount(long userId);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND date = :date AND category = 'Review' AND title = :title LIMIT 1")
    TaskEntity getReviewTaskByDateAndTitle(long userId, String date, String title);

    @Query("SELECT date, COUNT(*) as count FROM tasks WHERE userId = :userId AND isCompleted = 1 AND date >= :monthStart AND date <= :monthEnd GROUP BY date")
    List<DateCount> getCompletedCountsForMonth(long userId, String monthStart, String monthEnd);
}
