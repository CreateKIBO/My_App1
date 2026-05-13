package com.example.myapplication.data.local;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks", indices = {@Index(value = {"userId", "date"})})
public class TaskEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "userId")
    private long userId;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "date")
    private String date; // yyyy-MM-dd

    @ColumnInfo(name = "startTime")
    private int startTime; // minutes from midnight (0-1439)

    @ColumnInfo(name = "endTime")
    private int endTime;

    @ColumnInfo(name = "category")
    private String category; // Work, Study, Exercise, Personal, Other

    @ColumnInfo(name = "priority", defaultValue = "1")
    private int priority; // 0=低, 1=中, 2=高

    @ColumnInfo(name = "isCompleted", defaultValue = "0")
    private boolean isCompleted;

    @ColumnInfo(name = "completedAt")
    private Long completedAt;

    @ColumnInfo(name = "coinsEarned", defaultValue = "0")
    private int coinsEarned;

    @ColumnInfo(name = "xpEarned", defaultValue = "0")
    private int xpEarned;

    @ColumnInfo(name = "createdAt")
    private long createdAt;

    @ColumnInfo(name = "updatedAt")
    private long updatedAt;

    public TaskEntity() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public int getStartTime() { return startTime; }
    public void setStartTime(int startTime) { this.startTime = startTime; }

    public int getEndTime() { return endTime; }
    public void setEndTime(int endTime) { this.endTime = endTime; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public Long getCompletedAt() { return completedAt; }
    public void setCompletedAt(Long completedAt) { this.completedAt = completedAt; }

    public int getCoinsEarned() { return coinsEarned; }
    public void setCoinsEarned(int coinsEarned) { this.coinsEarned = coinsEarned; }

    public int getXpEarned() { return xpEarned; }
    public void setXpEarned(int xpEarned) { this.xpEarned = xpEarned; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
