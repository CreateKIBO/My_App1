package com.example.myapplication.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.myapplication.data.local.RewardTransactionEntity;
import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.ShopItemEntity;
import com.example.myapplication.data.local.UserEntity;
import com.example.myapplication.data.repository.ShopRepository;
import com.example.myapplication.util.RewardCalculator;

import java.util.HashSet;
import java.util.Set;

public class ShopViewModel extends BaseViewModel {

    private final ShopRepository shopRepository;

    private final MutableLiveData<String> selectedType = new MutableLiveData<>();
    private final LiveData<java.util.List<ShopItemEntity>> shopItems;
    private final LiveData<UserEntity> currentUser;

    private final LiveData<String> unlockedAvatarsLiveData;
    private final LiveData<String> unlockedThemesLiveData;
    private final LiveData<Set<Long>> currentOwnedIds;
    private final LiveData<Long> currentEquippedId;

    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<ShopItemEntity> purchaseSuccess = new MutableLiveData<>();
    private final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    public ShopViewModel(@NonNull Application application) {
        super(application);
        shopRepository = new ShopRepository(db, AppDatabase.databaseWriteExecutor);

        selectedType.setValue(RewardCalculator.TYPE_AVATAR);
        shopItems = Transformations.switchMap(selectedType, type ->
                shopRepository.getItemsByType(type));
        currentUser = db.userDao().observeUser(userId);

        unlockedAvatarsLiveData = Transformations.map(currentUser, user -> {
            if (user == null) return "0";
            String s = user.getUnlockedAvatars();
            return s != null ? s : "0";
        });
        unlockedThemesLiveData = Transformations.map(currentUser, user -> {
            if (user == null) return "0";
            String s = user.getUnlockedThemes();
            return s != null ? s : "0";
        });

        currentOwnedIds = Transformations.switchMap(selectedType, type -> {
            if (RewardCalculator.TYPE_PROP.equals(type)) {
                MutableLiveData<Set<Long>> empty = new MutableLiveData<>();
                empty.setValue(new HashSet<>());
                return empty;
            }
            if (RewardCalculator.TYPE_THEME.equals(type)) {
                return Transformations.map(unlockedThemesLiveData, this::csvToSet);
            }
            return Transformations.map(unlockedAvatarsLiveData, this::csvToSet);
        });

        currentEquippedId = Transformations.switchMap(selectedType, type -> {
            if (RewardCalculator.TYPE_PROP.equals(type)) {
                MutableLiveData<Long> none = new MutableLiveData<>();
                none.setValue(-1L);
                return none;
            }
            if (RewardCalculator.TYPE_THEME.equals(type)) {
                return Transformations.map(currentUser, user -> user != null ? (long) user.getThemeId() : 0L);
            }
            return Transformations.map(currentUser, user -> user != null ? (long) user.getAvatarId() : 0L);
        });
    }

    private Set<Long> csvToSet(String csv) {
        Set<Long> set = new HashSet<>();
        if (csv == null || csv.isEmpty()) return set;
        for (String part : csv.split(",")) {
            try { set.add(Long.parseLong(part.trim())); } catch (NumberFormatException ignored) {}
        }
        return set;
    }

    public void setSelectedType(String type) { selectedType.setValue(type); }
    public LiveData<java.util.List<ShopItemEntity>> getShopItems() { return shopItems; }
    public LiveData<UserEntity> getCurrentUser() { return currentUser; }
    public LiveData<Set<Long>> getCurrentOwnedIds() { return currentOwnedIds; }
    public LiveData<Long> getCurrentEquippedId() { return currentEquippedId; }
    public LiveData<String> getMessage() { return message; }
    public LiveData<ShopItemEntity> getPurchaseSuccess() { return purchaseSuccess; }

    public void purchaseItem(long itemId, int price, ShopItemEntity item) {
        shopRepository.purchaseItem(userId, itemId, price, resultCode -> {
            String msg;
            switch (resultCode) {
                case ShopRepository.RESULT_SUCCESS:
                    msg = "购买成功！";
                    mainHandler.post(() -> {
                        message.setValue(msg);
                        purchaseSuccess.setValue(item);
                    });
                    break;
                case ShopRepository.RESULT_INSUFFICIENT_COINS:
                    msg = "金币不足！";
                    break;
                case ShopRepository.RESULT_ALREADY_OWNED:
                    msg = "已经拥有该物品";
                    break;
                default:
                    msg = "购买失败";
                    break;
            }
            if (resultCode != ShopRepository.RESULT_SUCCESS) {
                mainHandler.post(() -> message.setValue(msg));
            }
        });
    }

    public void purchaseProp(long itemId, int price) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            UserEntity user = db.userDao().getUserById(userId);
            if (user == null || user.getCurrentCoins() < price) {
                mainHandler.post(() -> message.setValue("金币不足！"));
                return;
            }
            user.setCurrentCoins(user.getCurrentCoins() - price);
            user.setFreezeCount(user.getFreezeCount() + 1);
            db.userDao().update(user);

            // Record transaction
            RewardTransactionEntity tx = new RewardTransactionEntity();
            tx.setUserId(userId);
            tx.setTaskId(null);
            tx.setType(RewardCalculator.TX_SPEND);
            tx.setAmount(-price);
            tx.setReason("购买: 冻结卡");
            tx.setTimestamp(System.currentTimeMillis());
            db.rewardTransactionDao().insert(tx);

            mainHandler.post(() -> {
                message.setValue("购买成功！获得 1 张冻结卡");
                // Create a synthetic ShopItemEntity for the acquire animation
                ShopItemEntity propItem = new ShopItemEntity();
                propItem.setType("PROP");
                propItem.setName("冻结卡");
                propItem.setEmoji("❄️");
                propItem.setColorHex("#3B82F6");
                purchaseSuccess.setValue(propItem);
            });
        });
    }

    public void equipItem(long itemId, String type) {
        shopRepository.equipItem(userId, itemId, type);
    }
}