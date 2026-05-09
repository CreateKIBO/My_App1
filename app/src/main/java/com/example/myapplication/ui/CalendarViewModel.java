package com.example.myapplication.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.repository.TaskRepository;
import com.example.myapplication.util.DateUtils;
import com.example.myapplication.util.SessionManager;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CalendarViewModel extends AndroidViewModel {

    private final TaskRepository taskRepository;
    private final SessionManager sessionManager;

    private final MutableLiveData<int[]> currentMonth = new MutableLiveData<>();
    private final LiveData<List<String>> datesWithTasks;
    private final MutableLiveData<Set<String>> taskDatesSet = new MutableLiveData<>(new HashSet<>());

    public CalendarViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        taskRepository = new TaskRepository(db, AppDatabase.databaseWriteExecutor);
        sessionManager = new SessionManager(application);

        long userId = sessionManager.getLocalUserId();
        datesWithTasks = taskRepository.getDatesWithTasks(userId);

        Calendar cal = Calendar.getInstance();
        currentMonth.setValue(new int[]{cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)});
    }

    public MutableLiveData<int[]> getCurrentMonth() {
        return currentMonth;
    }

    public LiveData<List<String>> getDatesWithTasksLiveData() {
        return datesWithTasks;
    }

    public MutableLiveData<Set<String>> getTaskDatesSet() {
        return taskDatesSet;
    }

    public void goToNextMonth() {
        int[] month = currentMonth.getValue();
        if (month == null) return;
        Calendar cal = Calendar.getInstance();
        cal.set(month[0], month[1], 1);
        cal.add(Calendar.MONTH, 1);
        currentMonth.setValue(new int[]{cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)});
    }

    public void goToPrevMonth() {
        int[] month = currentMonth.getValue();
        if (month == null) return;
        Calendar cal = Calendar.getInstance();
        cal.set(month[0], month[1], 1);
        cal.add(Calendar.MONTH, -1);
        currentMonth.setValue(new int[]{cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)});
    }

    public void updateTaskDates(List<String> dates) {
        Set<String> set = new HashSet<>(dates);
        taskDatesSet.setValue(set);
    }
}