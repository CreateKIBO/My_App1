package com.example.myapplication.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.TaskEntity;
import com.example.myapplication.data.repository.TaskRepository;
import com.example.myapplication.util.DateUtils;
import com.example.myapplication.util.NotificationHelper;
import com.example.myapplication.util.RewardCalculator;

public class AddEditTaskViewModel extends BaseViewModel {

    private final TaskRepository taskRepository;
    private final MutableLiveData<TaskEntity> taskLiveData = new MutableLiveData<>();

    public AddEditTaskViewModel(@NonNull Application application) {
        super(application);
        taskRepository = new TaskRepository(db, AppDatabase.databaseWriteExecutor);
    }

    public MutableLiveData<TaskEntity> getTaskLiveData() {
        return taskLiveData;
    }

    public void loadTask(long taskId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            TaskEntity task = taskRepository.getTaskByIdSync(taskId);
            taskLiveData.postValue(task);
        });
    }

    private final MutableLiveData<Long> savedTaskId = new MutableLiveData<>();

    public MutableLiveData<Long> getSavedTaskId() {
        return savedTaskId;
    }

    public void saveTask(String title, String description, String date,
                         int startTime, int endTime, String category, long existingTaskId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (existingTaskId == -1) {
                TaskEntity task = new TaskEntity();
                task.setUserId(userId);
                task.setTitle(title);
                task.setDescription(description);
                task.setDate(date);
                task.setStartTime(startTime);
                task.setEndTime(endTime);
                task.setCategory(category);
                task.setCompleted(false);
                task.setCompletedAt(null);
                RewardCalculator.Reward reward = RewardCalculator.calculateTaskReward(category);
                task.setCoinsEarned(reward.coins);
                task.setXpEarned(reward.xp);
                task.setCreatedAt(System.currentTimeMillis());
                task.setUpdatedAt(System.currentTimeMillis());
                long id = taskRepository.insertTaskSync(task);
                savedTaskId.postValue(id);
                NotificationHelper.scheduleReminder(getApplication(), title, date, startTime);
            } else {
                TaskEntity task = taskRepository.getTaskByIdSync(existingTaskId);
                if (task != null) {
                    task.setTitle(title);
                    task.setDescription(description);
                    task.setDate(date);
                    task.setStartTime(startTime);
                    task.setEndTime(endTime);
                    task.setCategory(category);
                    RewardCalculator.Reward reward = RewardCalculator.calculateTaskReward(category);
                    task.setCoinsEarned(reward.coins);
                    task.setXpEarned(reward.xp);
                    taskRepository.updateTask(task);
                    NotificationHelper.scheduleReminder(getApplication(), title, date, startTime);
                }
            }
        });
    }
}