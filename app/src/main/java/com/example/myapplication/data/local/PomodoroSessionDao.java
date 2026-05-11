package com.example.myapplication.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PomodoroSessionDao {

    @Insert
    long insert(PomodoroSessionEntity session);

    @Query("SELECT * FROM pomodoro_sessions WHERE userId = :userId ORDER BY startedAt DESC")
    LiveData<List<PomodoroSessionEntity>> getByUserId(long userId);

    @Query("SELECT * FROM pomodoro_sessions WHERE userId = :userId AND isCompleted = 1 ORDER BY startedAt DESC")
    List<PomodoroSessionEntity> getCompletedByUserIdSync(long userId);

    @Query("SELECT * FROM pomodoro_sessions WHERE userId = :userId AND isCompleted = 1 ORDER BY startedAt DESC")
    LiveData<List<PomodoroSessionEntity>> getCompletedByUserId(long userId);

    @Query("SELECT COUNT(*) FROM pomodoro_sessions WHERE userId = :userId AND isCompleted = 1 AND startedAt >= :startOfDay AND startedAt < :endOfDay")
    LiveData<Integer> getTodayCompletedCount(long userId, long startOfDay, long endOfDay);

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM pomodoro_sessions WHERE userId = :userId AND isCompleted = 1 AND startedAt >= :startOfDay AND startedAt < :endOfDay")
    LiveData<Integer> getTodayTotalMinutes(long userId, long startOfDay, long endOfDay);

    @Query("SELECT COUNT(*) FROM pomodoro_sessions WHERE userId = :userId AND isCompleted = 1 AND startedAt >= :weekStart AND startedAt < :weekEnd")
    LiveData<Integer> getWeekCompletedCount(long userId, long weekStart, long weekEnd);
}
