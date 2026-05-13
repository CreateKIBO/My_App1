package com.example.myapplication.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.myapplication.data.local.RewardTransactionEntity;
import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.ShopItemEntity;
import com.example.myapplication.data.local.UserEntity;
import com.example.myapplication.util.RewardCalculator;

import java.util.List;

public class ProfileViewModel extends BaseViewModel {

    private final LiveData<UserEntity> user;
    private final LiveData<List<RewardTransactionEntity>> recentRewards;
    private final LiveData<Integer> completedCount;

    public ProfileViewModel(@NonNull Application application) {
        super(application);

        user = db.userDao().observeUser(userId);
        recentRewards = db.rewardTransactionDao().getRecentForUser(userId, 20);
        completedCount = db.taskDao().getTotalCompletedCount(userId);
    }

    public LiveData<UserEntity> getUser() { return user; }
    public LiveData<List<RewardTransactionEntity>> getRecentRewards() { return recentRewards; }
    public LiveData<Integer> getCompletedCount() { return completedCount; }

    public ShopItemEntity getShopItemById(long id) {
        return db.shopItemDao().getItemById(id);
    }

    public int getXpForNextLevel(int level) {
        return RewardCalculator.getXpForNextLevel(level);
    }

    public int getXpProgressInLevel(int totalXp) {
        return RewardCalculator.getXpProgressInLevel(totalXp);
    }
}
