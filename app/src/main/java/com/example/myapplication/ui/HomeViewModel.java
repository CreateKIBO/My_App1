package com.example.myapplication.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.RewardTransactionEntity;
import com.example.myapplication.data.local.TaskEntity;
import com.example.myapplication.data.local.UserEntity;
import com.example.myapplication.util.DateUtils;
import com.example.myapplication.util.RewardCalculator;

import java.util.List;

public class HomeViewModel extends BaseViewModel {

    private final MutableLiveData<String> selectedDate = new MutableLiveData<>();
    private final LiveData<List<TaskEntity>> tasks;
    private final LiveData<Integer> completedCount;
    private final LiveData<Integer> totalCount;
    private final LiveData<UserEntity> user;

    public HomeViewModel(@NonNull Application application) {
        super(application);

        selectedDate.setValue(DateUtils.getTodayString());

        tasks = Transformations.switchMap(selectedDate, date ->
                db.taskDao().getTasksByDate(userId, date));
        completedCount = Transformations.switchMap(selectedDate, date ->
                db.taskDao().getCompletedCountForDate(userId, date));
        totalCount = Transformations.switchMap(selectedDate, date ->
                db.taskDao().getTotalCountForDate(userId, date));
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
            if (current == null) return;

            if (current.isCompleted()) {
                undoComplete(current);
            } else {
                doComplete(current);
            }
        });
    }

    private void doComplete(TaskEntity task) {
        task.setCompleted(true);
        task.setUpdatedAt(System.currentTimeMillis());
        task.setCompletedAt(System.currentTimeMillis());

        RewardCalculator.Reward reward = RewardCalculator.calculateTaskReward(task.getCategory());
        task.setCoinsEarned(reward.coins);
        task.setXpEarned(reward.xp);
        db.taskDao().update(task);

        UserEntity u = db.userDao().getUserById(userId);
        if (u != null) {
            u.setCurrentCoins(u.getCurrentCoins() + reward.coins);
            u.setTotalXp(u.getTotalXp() + reward.xp);
            db.userDao().update(u);
        }

        RewardTransactionEntity tx = new RewardTransactionEntity();
        tx.setUserId(userId);
        tx.setTaskId(task.getId());
        tx.setType("TASK_COMPLETE");
        tx.setAmount(reward.coins);
        tx.setReason("完成任务: " + task.getTitle());
        tx.setTimestamp(System.currentTimeMillis());
        db.rewardTransactionDao().insert(tx);

        String today = DateUtils.getTodayString();
        int completed = db.taskDao().getCompletedCountForDateSync(userId, today);
        int total = db.taskDao().getTotalCountForDateSync(userId, today);

        if (completed == total && total > 0) {
            RewardCalculator.Reward bonus = RewardCalculator.getDayCompleteBonus();
            UserEntity user2 = db.userDao().getUserById(userId);
            if (user2 != null) {
                user2.setCurrentCoins(user2.getCurrentCoins() + bonus.coins);
                user2.setTotalXp(user2.getTotalXp() + bonus.xp);
                db.userDao().update(user2);
            }
            updateStreak();
        }
    }

    private void undoComplete(TaskEntity task) {
        int coinsToReverse = task.getCoinsEarned();
        int xpToReverse = task.getXpEarned();

        task.setCompleted(false);
        task.setUpdatedAt(System.currentTimeMillis());
        task.setCompletedAt(0L);
        task.setCoinsEarned(0);
        task.setXpEarned(0);
        db.taskDao().update(task);

        UserEntity u = db.userDao().getUserById(userId);
        if (u != null) {
            u.setCurrentCoins(Math.max(0, u.getCurrentCoins() - coinsToReverse));
            u.setTotalXp(Math.max(0, u.getTotalXp() - xpToReverse));
            db.userDao().update(u);
        }
    }

    private void updateStreak() {
        UserEntity u = db.userDao().getUserById(userId);
        if (u == null) return;

        String today = DateUtils.getTodayString();
        String lastActive = u.getLastActiveDate();

        if (today.equals(lastActive)) return;

        if (lastActive == null) {
            u.setCurrentStreak(1);
        } else if (DateUtils.getYesterdayString().equals(lastActive)) {
            u.setCurrentStreak(u.getCurrentStreak() + 1);
        } else {
            if (u.getFreezeCount() > 0) {
                u.setFreezeCount(u.getFreezeCount() - 1);
                u.setCurrentStreak(u.getCurrentStreak() + 1);
            } else {
                u.setCurrentStreak(1);
            }
        }

        if (u.getCurrentStreak() > u.getLongestStreak()) {
            u.setLongestStreak(u.getCurrentStreak());
        }
        u.setLastActiveDate(today);

        RewardCalculator.Reward streakBonus = RewardCalculator.calculateStreakBonus(u.getCurrentStreak());
        if (streakBonus.coins > 0) {
            u.setCurrentCoins(u.getCurrentCoins() + streakBonus.coins);
            u.setTotalXp(u.getTotalXp() + streakBonus.xp);

            RewardTransactionEntity tx = new RewardTransactionEntity();
            tx.setUserId(userId);
            tx.setType("STREAK_BONUS");
            tx.setAmount(streakBonus.coins);
            tx.setReason("连续" + u.getCurrentStreak() + "天奖励");
            tx.setTimestamp(System.currentTimeMillis());
            db.rewardTransactionDao().insert(tx);
        }

        db.userDao().update(u);
    }

    public void onTaskAddedToday() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            String today = DateUtils.getTodayString();
            int completed = db.taskDao().getCompletedCountForDateSync(userId, today);
            int total = db.taskDao().getTotalCountForDateSync(userId, today);

            if (completed < total) {
                UserEntity u = db.userDao().getUserById(userId);
                if (u != null && today.equals(u.getLastActiveDate())) {
                    if (u.getCurrentStreak() > 0) {
                        u.setCurrentStreak(u.getCurrentStreak() - 1);
                    }
                    u.setLastActiveDate(DateUtils.getYesterdayString());
                    db.userDao().update(u);
                }
            }
        });
    }

    public void checkAndUpdateStreak() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            UserEntity u = db.userDao().getUserById(userId);
            if (u == null) return;

            String today = DateUtils.getTodayString();
            String lastActive = u.getLastActiveDate();
            if (lastActive == null || today.equals(lastActive)) return;

            if (DateUtils.getYesterdayString().equals(lastActive)) return;

            u.setCurrentStreak(0);
            db.userDao().update(u);
        });
    }
}
