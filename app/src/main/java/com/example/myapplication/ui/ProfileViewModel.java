package com.example.myapplication.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.RewardTransactionEntity;
import com.example.myapplication.data.local.ShopItemEntity;
import com.example.myapplication.data.local.UserEntity;
import com.example.myapplication.util.RewardCalculator;
import com.example.myapplication.util.SessionManager;

import java.util.List;

public class ProfileViewModel extends AndroidViewModel {

    private final AppDatabase db;
    private final SessionManager sessionManager;
    private final long userId;

    private final LiveData<UserEntity> user;
    private final LiveData<List<RewardTransactionEntity>> recentRewards;
    private final LiveData<Integer> completedCount;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        sessionManager = new SessionManager(application);
        userId = sessionManager.getLocalUserId();

        user = db.userDao().observeUser(userId);
        recentRewards = db.rewardTransactionDao().getRecentForUser(userId, 20);
        completedCount = db.taskDao().getTotalCompletedCount(userId);
    }

    public LiveData<UserEntity> getUser() { return user; }
    public LiveData<List<RewardTransactionEntity>> getRecentRewards() { return recentRewards; }
    public LiveData<Integer> getCompletedCount() { return completedCount; }

    public ShopItemEntity getAvatarItem(int avatarId) {
        return db.shopItemDao().getItemById(avatarId);
    }

    public ShopItemEntity getEquippedAvatarItem(int avatarId) {
        return db.shopItemDao().getItemById(avatarId);
    }

    public ShopItemEntity getEquippedThemeItem(int themeId) {
        return db.shopItemDao().getItemById(themeId);
    }

    public int getXpForNextLevel(int level) {
        return RewardCalculator.getXpForNextLevel(level);
    }

    public int getXpProgressInLevel(int totalXp) {
        return RewardCalculator.getXpProgressInLevel(totalXp);
    }
}
