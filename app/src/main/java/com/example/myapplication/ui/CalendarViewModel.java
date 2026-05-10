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
import com.example.myapplication.data.repository.TaskRepository;
import com.example.myapplication.util.DateUtils;
import com.example.myapplication.util.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CalendarViewModel extends AndroidViewModel {

    private final TaskRepository taskRepository;
    private final SessionManager sessionManager;
    private final long userId;

    // Current month being displayed [year, month(0-based)]
    private final MutableLiveData<int[]> currentMonth = new MutableLiveData<>();

    // Selected date string (yyyy-MM-dd)
    private final MutableLiveData<String> selectedDate = new MutableLiveData<>();

    // Dates that have tasks (from DB)
    private final LiveData<List<String>> datesWithTasks;
    private final MutableLiveData<Set<String>> taskDatesSet = new MutableLiveData<>(new HashSet<>());

    // Tasks for the selected date
    private final LiveData<List<TaskEntity>> selectedDateTasks;

    // Completed count and total count for selected date
    private final LiveData<Integer> completedCount;
    private final LiveData<Integer> totalCount;

    // Streak count from user data
    private final MutableLiveData<Integer> streakCount = new MutableLiveData<>(0);

    public CalendarViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        taskRepository = new TaskRepository(db, AppDatabase.databaseWriteExecutor);
        sessionManager = new SessionManager(application);
        userId = sessionManager.getLocalUserId();

        Calendar cal = Calendar.getInstance();
        currentMonth.setValue(new int[]{cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)});
        selectedDate.setValue(DateUtils.getTodayString());

        datesWithTasks = taskRepository.getDatesWithTasks(userId);

        selectedDateTasks = Transformations.switchMap(selectedDate, date ->
                taskRepository.getTasksByDate(userId, date));

        completedCount = Transformations.switchMap(selectedDate, date ->
                taskRepository.getCompletedCount(userId, date));

        totalCount = Transformations.switchMap(selectedDate, date ->
                taskRepository.getTotalCount(userId, date));

        // Load streak data
        loadStreakData(db);
    }

    private void loadStreakData(AppDatabase db) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            UserEntity user = db.userDao().getUserById(userId);
            if (user != null) {
                streakCount.postValue(user.getCurrentStreak());
            }
        });
    }

    // --- Month navigation ---

    public MutableLiveData<int[]> getCurrentMonth() {
        return currentMonth;
    }

    public void goToPrevMonth() {
        int[] month = currentMonth.getValue();
        if (month == null) return;
        Calendar cal = Calendar.getInstance();
        cal.set(month[0], month[1], 1);
        cal.add(Calendar.MONTH, -1);
        currentMonth.setValue(new int[]{cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)});
    }

    public void goToNextMonth() {
        int[] month = currentMonth.getValue();
        if (month == null) return;
        Calendar cal = Calendar.getInstance();
        cal.set(month[0], month[1], 1);
        cal.add(Calendar.MONTH, 1);
        currentMonth.setValue(new int[]{cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)});
    }

    public void goToToday() {
        Calendar cal = Calendar.getInstance();
        currentMonth.setValue(new int[]{cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)});
        selectedDate.setValue(DateUtils.getTodayString());
    }

    // --- Selected date ---

    public MutableLiveData<String> getSelectedDateLiveData() {
        return selectedDate;
    }

    public String getSelectedDate() {
        return selectedDate.getValue();
    }

    public void setSelectedDate(String date) {
        selectedDate.setValue(date);
    }

    // --- Task dates ---

    public LiveData<List<String>> getDatesWithTasksLiveData() {
        return datesWithTasks;
    }

    public MutableLiveData<Set<String>> getTaskDatesSet() {
        return taskDatesSet;
    }

    public void updateTaskDates(List<String> dates) {
        Set<String> set = new HashSet<>(dates);
        taskDatesSet.setValue(set);
    }

    // --- Tasks for selected date ---

    public LiveData<List<TaskEntity>> getTasksForDate() {
        return selectedDateTasks;
    }

    public LiveData<Integer> getCompletedCount() {
        return completedCount;
    }

    public LiveData<Integer> getTotalCount() {
        return totalCount;
    }

    // --- Streak ---

    public MutableLiveData<Integer> getStreakCount() {
        return streakCount;
    }

    // --- Formatting helpers ---

    public String getMonthYearLabel(int year, int month) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月", Locale.CHINESE);
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1);
        return sdf.format(cal.getTime());
    }

    public String getSelectedDateDisplayLabel(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat(DateUtils.DATE_FORMAT_STR, Locale.getDefault());
            Date date = inputFormat.parse(dateStr);
            if (date == null) return dateStr;
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            String[] weekdays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
            SimpleDateFormat displayFormat = new SimpleDateFormat("M月d日", Locale.CHINESE);
            return displayFormat.format(date) + " 周" + weekdays[cal.get(Calendar.DAY_OF_WEEK) - 1];
        } catch (Exception e) {
            return dateStr;
        }
    }
}
