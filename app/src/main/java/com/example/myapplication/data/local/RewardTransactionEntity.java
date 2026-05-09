package com.example.myapplication.data.local;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "reward_transactions",
    foreignKeys = {
        @ForeignKey(entity = UserEntity.class, parentColumns = "id", childColumns = "userId", onDelete = ForeignKey.CASCADE)
    },
    indices = {
        @Index("userId")
    }
)
public class RewardTransactionEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "userId")
    private long userId;

    @ColumnInfo(name = "taskId")
    private Long taskId;

    @ColumnInfo(name = "type")
    private String type;

    @ColumnInfo(name = "amount")
    private int amount;

    @ColumnInfo(name = "reason")
    private String reason;

    @ColumnInfo(name = "timestamp")
    private long timestamp;

    public RewardTransactionEntity() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}