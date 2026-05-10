package com.example.myapplication.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface RewardTransactionDao {

    @Insert
    long insert(RewardTransactionEntity transaction);

    @Query("SELECT * FROM reward_transactions WHERE userId = :userId ORDER BY timestamp DESC")
    LiveData<List<RewardTransactionEntity>> getRecentForUser(long userId);

    @Query("SELECT * FROM reward_transactions WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<RewardTransactionEntity>> getRecentForUser(long userId, int limit);

    @Query("SELECT * FROM reward_transactions WHERE userId = :userId AND type = :type ORDER BY timestamp DESC LIMIT 1")
    RewardTransactionEntity getLatestByType(long userId, String type);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM reward_transactions WHERE userId = :userId AND type = :type AND timestamp >= :sinceTimestamp")
    int getSumByTypeSince(long userId, String type, long sinceTimestamp);
}
