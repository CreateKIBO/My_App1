package com.example.myapplication.data.repository;

import androidx.lifecycle.LiveData;

import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.RewardTransactionDao;
import com.example.myapplication.data.local.RewardTransactionEntity;
import com.example.myapplication.data.local.UserDao;
import com.example.myapplication.data.local.UserEntity;
import com.example.myapplication.util.RewardCalculator;

import java.util.List;

public class RewardRepository {

    private final RewardTransactionDao rewardDao;
    private final UserDao userDao;
    private final AppDatabase db;

    public RewardRepository(AppDatabase database) {
        this.rewardDao = database.rewardTransactionDao();
        this.userDao = database.userDao();
        this.db = database;
    }

    public LiveData<List<RewardTransactionEntity>> getRecentTransactions(long userId, int limit) {
        return rewardDao.getRecentForUser(userId, limit);
    }

    /**
     * Must be called from a background thread. All operations are synchronous.
     */
    public void awardTaskCompletionSync(long userId, long taskId, int coins, int xp, String reason) {
        long now = System.currentTimeMillis();

        if (coins > 0) {
            RewardTransactionEntity coinTx = new RewardTransactionEntity();
            coinTx.setUserId(userId);
            coinTx.setTaskId(taskId > 0 ? taskId : null);
            coinTx.setType(RewardCalculator.TX_COIN);
            coinTx.setAmount(coins);
            coinTx.setReason(reason);
            coinTx.setTimestamp(now);
            rewardDao.insert(coinTx);
        }

        if (xp > 0) {
            RewardTransactionEntity xpTx = new RewardTransactionEntity();
            xpTx.setUserId(userId);
            xpTx.setTaskId(taskId > 0 ? taskId : null);
            xpTx.setType(RewardCalculator.TX_XP);
            xpTx.setAmount(xp);
            xpTx.setReason(reason);
            xpTx.setTimestamp(now + 1);
            rewardDao.insert(xpTx);
        }

        UserEntity user = userDao.getUserById(userId);
        if (user == null) return;

        int oldLevel = RewardCalculator.calculateLevel(user.getTotalXp());
        user.setTotalCoins(user.getTotalCoins() + coins);
        user.setCurrentCoins(user.getCurrentCoins() + coins);
        user.setTotalXp(user.getTotalXp() + xp);
        int newLevel = RewardCalculator.calculateLevel(user.getTotalXp());
        user.setLevel(newLevel);
        userDao.update(user);

        if (newLevel > oldLevel) {
            int bonusCoins = RewardCalculator.getLevelUpBonusCoins(newLevel);
            user.setCurrentCoins(user.getCurrentCoins() + bonusCoins);
            user.setTotalCoins(user.getTotalCoins() + bonusCoins);
            userDao.update(user);

            RewardTransactionEntity lvlTx = new RewardTransactionEntity();
            lvlTx.setUserId(userId);
            lvlTx.setTaskId(null);
            lvlTx.setType(RewardCalculator.TX_LEVEL_UP);
            lvlTx.setAmount(bonusCoins);
            lvlTx.setReason("升级到 Lv." + newLevel + " 奖励");
            lvlTx.setTimestamp(System.currentTimeMillis());
            rewardDao.insert(lvlTx);
        }
    }

    /**
     * Must be called from a background thread. All operations are synchronous.
     */
    public void reverseRewardSync(long userId, int coins, int xp) {
        UserEntity user = userDao.getUserById(userId);
        if (user == null) return;

        user.setTotalCoins(Math.max(0, user.getTotalCoins() - coins));
        user.setCurrentCoins(Math.max(0, user.getCurrentCoins() - coins));
        user.setTotalXp(Math.max(0, user.getTotalXp() - xp));
        user.setLevel(RewardCalculator.calculateLevel(user.getTotalXp()));
        userDao.update(user);
    }
}
