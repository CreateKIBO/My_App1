package com.example.myapplication.ui;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.ShopItemEntity;
import com.example.myapplication.data.local.UserDao;
import com.example.myapplication.data.local.UserEntity;
import com.example.myapplication.data.repository.ShopRepository;
import com.example.myapplication.util.RewardCalculator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class AvatarEditViewModel extends BaseViewModel {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final LiveData<UserEntity> currentUser;
    private final LiveData<List<ShopItemEntity>> avatarItems;
    private final MutableLiveData<String> message = new MutableLiveData<>();

    private final ShopRepository shopRepository;

    public AvatarEditViewModel(@NonNull Application application) {
        super(application);

        shopRepository = new ShopRepository(db, AppDatabase.databaseWriteExecutor);

        currentUser = db.userDao().observeUser(userId);
        avatarItems = db.shopItemDao().getItemsByType(RewardCalculator.TYPE_AVATAR);
    }

    public LiveData<UserEntity> getCurrentUser() {
        return currentUser;
    }

    public LiveData<List<ShopItemEntity>> getAvatarItems() {
        return avatarItems;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    /**
     * Equip a shop avatar by itemId. Clears customAvatarPath.
     */
    public void equipAvatar(long itemId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            UserDao userDao = db.userDao();
            userDao.setAvatarId(userId, (int) itemId);
            userDao.setCustomAvatarPath(userId, null);
            mainHandler.post(() -> message.setValue("已装备"));
        });
    }

    /**
     * Equip the custom avatar. Sets avatarId to -1 (sentinel), keeps customAvatarPath.
     */
    public void equipCustomAvatar() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.userDao().setAvatarId(userId, -1);
            mainHandler.post(() -> message.setValue("已装备自定义头像"));
        });
    }

    /**
     * Purchase an avatar item.
     */
    public void purchaseAvatar(long itemId, int price) {
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
                    msg = "已经拥有该头像";
                    break;
                default:
                    msg = "购买失败";
                    break;
            }
            mainHandler.post(() -> message.setValue(msg));
        });
    }

    /**
     * Update the user's display name.
     */
    public void updateDisplayName(String name) {
        if (name == null || name.trim().isEmpty()) return;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.userDao().setDisplayName(userId, name.trim());
            mainHandler.post(() -> message.setValue("昵称已更新"));
        });
    }

    /**
     * Save a custom avatar file from source path to internal storage.
     * @param sourcePath The source file path (from content URI copy or picked file).
     * @return The saved file path, or null on failure.
     */
    public String saveCustomAvatar(String sourcePath) {
        if (sourcePath == null) return null;

        File avatarsDir = new File(getApplication().getFilesDir(), "avatars");
        if (!avatarsDir.exists()) {
            avatarsDir.mkdirs();
        }

        String fileName = "custom_" + userId + "_" + System.currentTimeMillis() + ".jpg";
        File destFile = new File(avatarsDir, fileName);

        try (InputStream in = new FileInputStream(sourcePath);
             OutputStream out = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            return null;
        }

        // Save path to database
        String absolutePath = destFile.getAbsolutePath();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.userDao().setCustomAvatarPath(userId, absolutePath);
        });

        return absolutePath;
    }

    /**
     * Save a custom avatar from an InputStream (for content URI).
     * @param inputStream The input stream from the content resolver.
     * @return The saved file path, or null on failure.
     */
    public String saveCustomAvatarFromStream(InputStream inputStream) {
        if (inputStream == null) return null;

        File avatarsDir = new File(getApplication().getFilesDir(), "avatars");
        if (!avatarsDir.exists()) {
            avatarsDir.mkdirs();
        }

        String fileName = "custom_" + userId + "_" + System.currentTimeMillis() + ".jpg";
        File destFile = new File(avatarsDir, fileName);

        try (OutputStream out = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            return null;
        } finally {
            try { inputStream.close(); } catch (IOException ignored) {}
        }

        // Save path to database
        String absolutePath = destFile.getAbsolutePath();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.userDao().setCustomAvatarPath(userId, absolutePath);
        });

        return absolutePath;
    }

    /**
     * Delete the custom avatar file and clear the path. Falls back to warrior avatar.
     */
    public void deleteCustomAvatar() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            UserEntity user = db.userDao().getUserById(userId);
            if (user != null) {
                String customPath = user.getCustomAvatarPath();
                if (customPath != null && !customPath.isEmpty()) {
                    File file = new File(customPath);
                    if (file.exists()) {
                        file.delete();
                    }
                }
                db.userDao().setCustomAvatarPath(userId, null);

                // If custom avatar was equipped, fallback to first free avatar
                if (user.getAvatarId() == -1) {
                    // Find warrior (first free avatar) from unlocked list
                    String unlocked = user.getUnlockedAvatars();
                    if (unlocked != null && !unlocked.isEmpty()) {
                        String[] ids = unlocked.split(",");
                        if (ids.length > 0) {
                            try {
                                long firstId = Long.parseLong(ids[0].trim());
                                db.userDao().setAvatarId(userId, (int) firstId);
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
            mainHandler.post(() -> message.setValue("已删除自定义头像"));
        });
    }

    /**
     * Check if a specific avatar item is owned by the current user.
     */
    public boolean isAvatarOwned(long itemId, UserEntity user) {
        if (user == null) return false;
        String unlocked = user.getUnlockedAvatars();
        if (unlocked == null || unlocked.isEmpty()) return false;
        for (String part : unlocked.split(",")) {
            try {
                if (Long.parseLong(part.trim()) == itemId) return true;
            } catch (NumberFormatException ignored) {}
        }
        return false;
    }

    /**
     * Get a shop item by its ID (blocking, call on executor).
     */
    public ShopItemEntity getShopItemById(long itemId) {
        return db.shopItemDao().getItemById(itemId);
    }

    /**
     * Clean up old custom avatar files in the avatars directory (except the current one).
     */
    public void cleanupOldAvatars(String currentPath) {
        File avatarsDir = new File(getApplication().getFilesDir(), "avatars");
        if (!avatarsDir.exists()) return;

        File[] files = avatarsDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (currentPath != null && file.getAbsolutePath().equals(currentPath)) {
                continue;
            }
            // Only delete files matching our naming pattern that are old
            if (file.getName().startsWith("custom_" + userId + "_")) {
                file.delete();
            }
        }
    }
}
