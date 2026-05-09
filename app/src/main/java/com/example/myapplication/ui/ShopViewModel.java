package com.example.myapplication.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.ShopItemEntity;
import com.example.myapplication.data.local.UserEntity;
import com.example.myapplication.data.repository.ShopRepository;
import com.example.myapplication.util.RewardCalculator;
import com.example.myapplication.util.SessionManager;

import java.util.HashSet;
import java.util.Set;

public class ShopViewModel extends AndroidViewModel {

    private final ShopRepository shopRepository;
    private final SessionManager sessionManager;
    private final long userId;

    private final MutableLiveData<String> selectedType = new MutableLiveData<>();
    private final LiveData<java.util.List<ShopItemEntity>> shopItems;
    private final LiveData<UserEntity> currentUser;

    private final LiveData<String> unlockedAvatarsLiveData;
    private final LiveData<String> unlockedThemesLiveData;
    private final LiveData<Set<Long>> currentOwnedIds;
    private final LiveData<Long> currentEquippedId;

    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    public ShopViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        shopRepository = new ShopRepository(db, AppDatabase.databaseWriteExecutor);
        sessionManager = new SessionManager(application);
        userId = sessionManager.getLocalUserId();

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
            if (RewardCalculator.TYPE_THEME.equals(type)) {
                return Transformations.map(unlockedThemesLiveData, this::csvToSet);
            }
            return Transformations.map(unlockedAvatarsLiveData, this::csvToSet);
        });

        currentEquippedId = Transformations.switchMap(selectedType, type -> {
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

    public void purchaseItem(long itemId, int price) {
        shopRepository.purchaseItem(userId, itemId, price, resultCode -> {
            String msg;
            switch (resultCode) {
                case ShopRepository.RESULT_SUCCESS:
                    msg = "购买成功！";
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
            mainHandler.post(() -> message.setValue(msg));
        });
    }

    public void equipItem(long itemId, String type) {
        shopRepository.equipItem(userId, itemId, type);
    }
}