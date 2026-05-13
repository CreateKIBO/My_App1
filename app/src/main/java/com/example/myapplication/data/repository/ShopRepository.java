package com.example.myapplication.data.repository;

import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.RewardTransactionEntity;
import com.example.myapplication.data.local.ShopItemDao;
import com.example.myapplication.data.local.ShopItemEntity;
import com.example.myapplication.data.local.UserDao;
import com.example.myapplication.data.local.UserEntity;
import com.example.myapplication.util.RewardCalculator;

import java.util.concurrent.ExecutorService;

public class ShopRepository {

    public static final int RESULT_SUCCESS = 1;
    public static final int RESULT_INSUFFICIENT_COINS = 2;
    public static final int RESULT_ALREADY_OWNED = 3;
    public static final int RESULT_ERROR = 4;

    public interface PurchaseCallback {
        void onResult(int resultCode);
    }

    private final ShopItemDao shopItemDao;
    private final UserDao userDao;
    private final AppDatabase db;
    private final ExecutorService executor;

    public ShopRepository(AppDatabase database, ExecutorService executor) {
        this.shopItemDao = database.shopItemDao();
        this.userDao = database.userDao();
        this.db = database;
        this.executor = executor;
    }

    public androidx.lifecycle.LiveData<java.util.List<ShopItemEntity>> getItemsByType(String type) {
        return shopItemDao.getShopItemsByType(type);
    }

    public void purchaseItem(long userId, long itemId, int price, PurchaseCallback callback) {
        executor.execute(() -> {
            UserEntity user = userDao.getUserById(userId);
            if (user == null) {
                if (callback != null) callback.onResult(RESULT_ERROR);
                return;
            }

            ShopItemEntity item = shopItemDao.getItemById(itemId);
            if (item == null) {
                if (callback != null) callback.onResult(RESULT_ERROR);
                return;
            }

            // Check if already owned
            String unlocked = RewardCalculator.TYPE_AVATAR.equals(item.getType())
                    ? user.getUnlockedAvatars() : user.getUnlockedThemes();
            if (unlocked != null && !unlocked.isEmpty()) {
                for (String part : unlocked.split(",")) {
                    if (part.trim().equals(String.valueOf(itemId))) {
                        if (callback != null) callback.onResult(RESULT_ALREADY_OWNED);
                        return;
                    }
                }
            }

            if (user.getCurrentCoins() < price) {
                if (callback != null) callback.onResult(RESULT_INSUFFICIENT_COINS);
                return;
            }

            // Deduct coins
            user.setCurrentCoins(user.getCurrentCoins() - price);

            // Add to unlocked list
            if (unlocked == null || unlocked.isEmpty()) {
                unlocked = String.valueOf(itemId);
            } else {
                unlocked = unlocked + "," + itemId;
            }
            if (RewardCalculator.TYPE_AVATAR.equals(item.getType())) {
                user.setUnlockedAvatars(unlocked);
            } else {
                user.setUnlockedThemes(unlocked);
            }
            userDao.update(user);

            // Record transaction
            RewardTransactionEntity tx = new RewardTransactionEntity();
            tx.setUserId(userId);
            tx.setTaskId(null);
            tx.setType(RewardCalculator.TX_SPEND);
            tx.setAmount(-price);
            tx.setReason("购买: " + item.getName());
            tx.setTimestamp(System.currentTimeMillis());
            db.rewardTransactionDao().insert(tx);

            if (callback != null) callback.onResult(RESULT_SUCCESS);
        });
    }

    public void equipItem(long userId, long itemId, String type) {
        executor.execute(() -> {
            if (RewardCalculator.TYPE_AVATAR.equals(type)) {
                userDao.setAvatarId(userId, (int) itemId);
            } else {
                userDao.setThemeId(userId, (int) itemId);
            }
        });
    }
}