package com.example.myapplication.data.local;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "review_tasks",
        foreignKeys = @ForeignKey(
                entity = UserEntity.class,
                parentColumns = "id",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("userId"), @Index("curveItemId"), @Index("reviewDate")}
)
public class ReviewTaskEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long userId;
    private long curveItemId;
    private String title;
    private String reviewDate;
    private int step;
    private boolean isCompleted;
    private Long completedAt;

    public ReviewTaskEntity() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public long getCurveItemId() { return curveItemId; }
    public void setCurveItemId(long curveItemId) { this.curveItemId = curveItemId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getReviewDate() { return reviewDate; }
    public void setReviewDate(String reviewDate) { this.reviewDate = reviewDate; }

    public int getStep() { return step; }
    public void setStep(int step) { this.step = step; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public Long getCompletedAt() { return completedAt; }
    public void setCompletedAt(Long completedAt) { this.completedAt = completedAt; }
}
