package com.example.myapplication.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(UserEntity user);

    @Update
    void update(UserEntity user);

    @Query("SELECT * FROM users WHERE id = :userId")
    UserEntity getUserById(long userId);

    @Query("SELECT * FROM users WHERE id = :userId")
    LiveData<UserEntity> observeUser(long userId);

    @Query("SELECT * FROM users LIMIT 1")
    UserEntity getFirstUser();

    @Query("UPDATE users SET currentCoins = currentCoins + :amount, totalCoins = totalCoins + :amount WHERE id = :userId")
    void addCoins(long userId, int amount);

    @Query("UPDATE users SET currentCoins = currentCoins - :amount WHERE id = :userId AND currentCoins >= :amount")
    int spendCoins(long userId, int amount);

    @Query("UPDATE users SET totalXp = totalXp + :amount WHERE id = :userId")
    void addXp(long userId, int amount);

    @Query("UPDATE users SET level = :level WHERE id = :userId")
    void setLevel(long userId, int level);

    @Query("UPDATE users SET currentStreak = :streak, longestStreak = CASE WHEN :streak > longestStreak THEN :streak ELSE longestStreak END WHERE id = :userId")
    void setStreak(long userId, int streak);

    @Query("UPDATE users SET lastActiveDate = :date WHERE id = :userId")
    void setLastActiveDate(long userId, String date);

    @Query("UPDATE users SET avatarId = :avatarId WHERE id = :userId")
    void setAvatarId(long userId, int avatarId);

    @Query("UPDATE users SET themeId = :themeId WHERE id = :userId")
    void setThemeId(long userId, int themeId);

    @Query("UPDATE users SET unlockedAvatars = :csv WHERE id = :userId")
    void setUnlockedAvatars(long userId, String csv);

    @Query("UPDATE users SET unlockedThemes = :csv WHERE id = :userId")
    void setUnlockedThemes(long userId, String csv);

    @Query("UPDATE users SET freezeCount = freezeCount + 1 WHERE id = :userId")
    void addFreezeCard(long userId);

    @Query("UPDATE users SET freezeCount = freezeCount - 1 WHERE id = :userId AND freezeCount > 0")
    void useFreezeCard(long userId);

    @Query("UPDATE users SET customAvatarPath = :path WHERE id = :userId")
    void setCustomAvatarPath(long userId, String path);

    @Query("UPDATE users SET displayName = :name WHERE id = :userId")
    void setDisplayName(long userId, String name);

    @Query("SELECT id FROM shop_items WHERE type = 'avatar' AND price = 0 ORDER BY id ASC LIMIT 1")
    Long getFirstFreeAvatarId();

    @Query("SELECT id FROM shop_items WHERE type = 'theme' AND price = 0 ORDER BY id ASC LIMIT 1")
    Long getFirstFreeThemeId();
}
