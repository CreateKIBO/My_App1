package com.example.myapplication.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.myapplication.data.local.AppDatabase;
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

            int completed = db.taskDao().getCompletedCountForDateSync(userId, current.getDate());
            int total = db.taskDao().getTotalCountForDateSync(userId, current.getDate());
            if (completed == total && total > 0) {
                RewardCalculator.Reward bonus = RewardCalculator.getDayCompleteBonus();
                rewardRepo.awardTaskCompletionSync(userId, -1, bonus.coins, bonus.xp, "全部完成奖励");
            }

            updateStreakSync(rewardRepo);
        });
    }

    private void updateStreakSync(RewardRepository rewardRepo) {
        UserEntity user = db.userDao().getUserById(userId);
        if (user == null) return;

        String today = DateUtils.getTodayString();
        String lastActive = user.getLastActiveDate();
        if (today.equals(lastActive)) return;

        if (DateUtils.getYesterdayString().equals(lastActive)) {
            user.setCurrentStreak(user.getCurrentStreak() + 1);
        } else {
            user.setCurrentStreak(1);
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
