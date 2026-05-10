package com.example.myapplication.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.RewardTransactionEntity;
import com.example.myapplication.data.local.TaskEntity;
import com.example.myapplication.data.local.UserEntity;
import com.example.myapplication.data.repository.RewardRepository;
import com.example.myapplication.data.repository.TaskRepository;
import com.example.myapplication.data.repository.UserRepository;
import com.example.myapplication.util.DateUtils;
import com.example.myapplication.util.RewardCalculator;
import com.example.myapplication.util.SessionManager;

import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final AppDatabase db;
    private final SessionManager sessionManager;
    private final long userId;

    private final MutableLiveData<String> selectedDate = new MutableLiveData<>();
    private final LiveData<List<TaskEntity>> tasks;
    private final LiveData<Integer> completedCount;
    private final LiveData<Integer> totalCount;
    private final LiveData<UserEntity> user;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        sessionManager = new SessionManager(application);
        userId = sessionManager.getLocalUserId();

        TaskRepository taskRepository = new TaskRepository(db, AppDatabase.databaseWriteExecutor);

        selectedDate.setValue(DateUtils.getTodayString());

        tasks = Transformations.switchMap(selectedDate, date ->
                taskRepository.getTasksByDate(userId, date));
        completedCount = Transformations.switchMap(selectedDate, date ->
                taskRepository.getCompletedCount(userId, date));
        totalCount = Transformations.switchMap(selectedDate, date ->
                taskRepository.getTotalCount(userId, date));
        user = db.userDao().observeUser(userId);
    }

    public void setSelectedDate(String date) { selectedDate.setValue(date); }
    public String getSelectedDate() { return selectedDate.getValue(); }
    public LiveData<List<TaskEntity>> getTasks() { return tasks; }
    public LiveData<Integer> getCompletedCount() { return completedCount; }
    public LiveData<Integer> getTotalCount() { return totalCount; }
    public LiveData<UserEntity> getUser() { return user; }

    public void completeTask(TaskEntity task) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            TaskEntity current = db.taskDao().getTaskById(task.getId());
            if (current == null || current.isCompleted()) return;

            current.setCompleted(true);
            current.setUpdatedAt(System.currentTimeMillis());
            current.setCompletedAt(System.currentTimeMillis());

            RewardCalculator.Reward reward = RewardCalculator.calculateTaskReward(current.getCategory());
            current.setCoinsEarned(reward.coins);
            current.setXpEarned(reward.xp);
            db.taskDao().update(current);

            RewardRepository rewardRepo = new RewardRepository(db);
            rewardRepo.awardTaskCompletionSync(userId, current.getId(), reward.coins, reward.xp,
                    "完成任务: " + current.getTitle());

            // Check if all tasks for today are now completed
            String today = DateUtils.getTodayString();
            int completed = db.taskDao().getCompletedCountForDateSync(userId, today);
            int total = db.taskDao().getTotalCountForDateSync(userId, today);

            if (completed == total && total > 0) {
                // All tasks done — award bonus and update streak
                RewardCalculator.Reward bonus = RewardCalculator.getDayCompleteBonus();
                rewardRepo.awardTaskCompletionSync(userId, -1, bonus.coins, bonus.xp, "全部完成奖励");
                updateStreakSync(rewardRepo);
            }
        });
    }

    /**
     * Called when a new task is added for today — if streak was counted today
     * but now there are incomplete tasks, cancel today's streak.
     */
    public void onTaskAddedToday() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            String today = DateUtils.getTodayString();
            int completed = db.taskDao().getCompletedCountForDateSync(userId, today);
            int total = db.taskDao().getTotalCountForDateSync(userId, today);

            // If there are incomplete tasks and lastActiveDate is today, cancel today's streak
            if (completed < total) {
                UserEntity user = db.userDao().getUserById(userId);
                if (user != null && today.equals(user.getLastActiveDate())) {
                    // Revert: if streak was incremented today, undo it
                    if (user.getCurrentStreak() > 0) {
                        user.setCurrentStreak(user.getCurrentStreak() - 1);
                    }
                    // Reset lastActiveDate to yesterday so streak can be re-earned
                    user.setLastActiveDate(DateUtils.getYesterdayString());
                    db.userDao().update(user);
                }
            }
        });
    }

    public void checkAndUpdateStreak() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            UserEntity user = db.userDao().getUserById(userId);
            if (user == null) return;

            String today = DateUtils.getTodayString();
            String lastActive = user.getLastActiveDate();
            if (lastActive == null || today.equals(lastActive)) return;

            // Check if streak is broken (gap > 1 day)
            if (DateUtils.getYesterdayString().equals(lastActive)) {
                // Last active yesterday — streak is still alive, no action needed
                return;
            }

            // Gap > 1 day — streak is broken
            if (user.getFreezeCount() > 0) {
                user.setFreezeCount(user.getFreezeCount() - 1);
                // Freeze preserves streak but doesn't increment
            } else {
                user.setCurrentStreak(0);
            }
            // Don't update lastActiveDate — that only happens when a task is completed
            db.userDao().update(user);
        });
    }

    private void updateStreakSync(RewardRepository rewardRepo) {
        UserEntity user = db.userDao().getUserById(userId);
        if (user == null) return;

        String today = DateUtils.getTodayString();
        String lastActive = user.getLastActiveDate();

        if (today.equals(lastActive)) {
            // Already active today — streak already counted
            return;
        }

        if (lastActive == null) {
            // First time ever
            user.setCurrentStreak(1);
        } else if (DateUtils.getYesterdayString().equals(lastActive)) {
            // Consecutive day — increment streak
            user.setCurrentStreak(user.getCurrentStreak() + 1);
        } else {
            // Gap > 1 day — check freeze card
            if (user.getFreezeCount() > 0) {
                user.setFreezeCount(user.getFreezeCount() - 1);
                user.setCurrentStreak(user.getCurrentStreak() + 1);
                RewardTransactionEntity freezeTx = new RewardTransactionEntity();
                freezeTx.setUserId(userId);
                freezeTx.setTaskId(null);
                freezeTx.setType("FREEZE");
                freezeTx.setAmount(0);
                freezeTx.setReason("使用冻结卡保持连续");
                freezeTx.setTimestamp(System.currentTimeMillis());
                db.rewardTransactionDao().insert(freezeTx);
            } else {
                // No freeze — reset streak
                user.setCurrentStreak(1);
            }
        }

        if (user.getCurrentStreak() > user.getLongestStreak()) {
            user.setLongestStreak(user.getCurrentStreak());
        }
        user.setLastActiveDate(today);

        RewardCalculator.Reward streakBonus = RewardCalculator.calculateStreakBonus(user.getCurrentStreak());
        if (streakBonus.coins > 0) {
            rewardRepo.awardTaskCompletionSync(userId, -1, streakBonus.coins, streakBonus.xp,
                    "连续" + user.getCurrentStreak() + "天奖励");
        }

        db.userDao().update(user);
    }
}
