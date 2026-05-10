package com.example.myapplication.data.local;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class UserEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "displayName", defaultValue = "我")
    private String displayName;

    @ColumnInfo(name = "avatarId", defaultValue = "0")
    private int avatarId;

    @ColumnInfo(name = "themeId", defaultValue = "0")
    private int themeId;

    @ColumnInfo(name = "totalCoins", defaultValue = "0")
    private int totalCoins;

    @ColumnInfo(name = "currentCoins", defaultValue = "0")
    private int currentCoins;

    @ColumnInfo(name = "totalXp", defaultValue = "0")
    private int totalXp;

    @ColumnInfo(name = "level", defaultValue = "1")
    private int level;

    @ColumnInfo(name = "currentStreak", defaultValue = "0")
    private int currentStreak;

    @ColumnInfo(name = "longestStreak", defaultValue = "0")
    private int longestStreak;

    @ColumnInfo(name = "lastActiveDate")
    private String lastActiveDate;

    @ColumnInfo(name = "unlockedAvatars", defaultValue = "0")
    private String unlockedAvatars;

    @ColumnInfo(name = "unlockedThemes", defaultValue = "0")
    private String unlockedThemes;

    @ColumnInfo(name = "freezeCount", defaultValue = "0")
    private int freezeCount;

    @ColumnInfo(name = "customAvatarPath", defaultValue = "NULL")
    private String customAvatarPath;

    @ColumnInfo(name = "createdAt")
    private long createdAt;

    public UserEntity() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public int getAvatarId() { return avatarId; }
    public void setAvatarId(int avatarId) { this.avatarId = avatarId; }

    public int getThemeId() { return themeId; }
    public void setThemeId(int themeId) { this.themeId = themeId; }

    public int getTotalCoins() { return totalCoins; }
    public void setTotalCoins(int totalCoins) { this.totalCoins = totalCoins; }

    public int getCurrentCoins() { return currentCoins; }
    public void setCurrentCoins(int currentCoins) { this.currentCoins = currentCoins; }

    public int getTotalXp() { return totalXp; }
    public void setTotalXp(int totalXp) { this.totalXp = totalXp; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public int getLongestStreak() { return longestStreak; }
    public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }

    public String getLastActiveDate() { return lastActiveDate; }
    public void setLastActiveDate(String lastActiveDate) { this.lastActiveDate = lastActiveDate; }

    public String getUnlockedAvatars() { return unlockedAvatars; }
    public void setUnlockedAvatars(String unlockedAvatars) { this.unlockedAvatars = unlockedAvatars; }

    public String getUnlockedThemes() { return unlockedThemes; }
    public void setUnlockedThemes(String unlockedThemes) { this.unlockedThemes = unlockedThemes; }

    public int getFreezeCount() { return freezeCount; }
    public void setFreezeCount(int freezeCount) { this.freezeCount = freezeCount; }

    public String getCustomAvatarPath() { return customAvatarPath; }
    public void setCustomAvatarPath(String customAvatarPath) { this.customAvatarPath = customAvatarPath; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}