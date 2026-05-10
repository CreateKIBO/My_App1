package com.example.myapplication.data.repository;

import androidx.lifecycle.LiveData;

import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.TaskDao;
import com.example.myapplication.data.local.TaskEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class TaskRepository {

    private final TaskDao taskDao;
    private final ExecutorService executor;

    public TaskRepository(AppDatabase database, ExecutorService executor) {
        this.taskDao = database.taskDao();
        this.executor = executor;
    }

    public LiveData<List<TaskEntity>> getTasksByDate(long userId, String date) {
        return taskDao.getTasksByDate(userId, date);
    }

    public LiveData<List<String>> getDatesWithTasks(long userId) {
        return taskDao.getDatesWithTasks(userId);
    }

    public LiveData<Integer> getCompletedCount(long userId, String date) {
        return taskDao.getCompletedCountForDate(userId, date);
    }

    public LiveData<Integer> getTotalCount(long userId, String date) {
        return taskDao.getTotalCountForDate(userId, date);
    }

    public void insertTask(TaskEntity task) {
        taskDao.insert(task);
    }

    public void updateTask(TaskEntity task) {
        executor.execute(() -> {
            task.setUpdatedAt(System.currentTimeMillis());
            taskDao.update(task);
        });
    }

    public void deleteTask(TaskEntity task) {
        executor.execute(() -> taskDao.delete(task));
    }

    public void deleteTaskById(long id) {
        executor.execute(() -> taskDao.deleteById(id));
    }

    public TaskEntity getTaskByIdSync(long id) {
        return taskDao.getTaskById(id);
    }

    public int getCompletedCountSync(long userId, String date) {
        return taskDao.getCompletedCountForDateSync(userId, date);
    }

    public int getTotalCountSync(long userId, String date) {
        return taskDao.getTotalCountForDateSync(userId, date);
    }
}
