package com.example.myapplication.ui;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.CountDownTimer;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.ForgettingCurveItemEntity;
import com.example.myapplication.data.local.PomodoroSessionEntity;
import com.example.myapplication.data.local.ReviewTaskEntity;
import com.example.myapplication.data.local.TaskEntity;
import com.example.myapplication.data.local.UserEntity;
import com.example.myapplication.data.repository.RewardRepository;
import com.example.myapplication.util.DateUtils;
import com.example.myapplication.util.NotificationHelper;
import com.example.myapplication.util.RewardCalculator;

import java.util.Calendar;
import java.util.List;

public class FocusViewModel extends BaseViewModel {

    // Pomodoro state
    public enum TimerState { IDLE, RUNNING, PAUSED, BREAK }

    private final MutableLiveData<TimerState> timerState = new MutableLiveData<>(TimerState.IDLE);
    private final MutableLiveData<Long> remainingSeconds = new MutableLiveData<>(0L);
    private final MutableLiveData<Integer> totalSeconds = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> completedTodayCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> todayFocusMinutes = new MutableLiveData<>(0);
    private final MutableLiveData<String> message = new MutableLiveData<>();

    private CountDownTimer countDownTimer;
    private TimerState pausedState = null; // state before pause (RUNNING or BREAK)
    private int workDurationMinutes = 25;
    private int breakDurationMinutes = 5;
    private int consecutivePomodoros = 0;
    private String currentTaskTitle = "";

    // Forgetting curve state
    private final MutableLiveData<List<ForgettingCurveItemEntity>> activeItems = new MutableLiveData<>();
    private final MutableLiveData<List<ReviewTaskEntity>> todayReviews = new MutableLiveData<>();
    private final MutableLiveData<Integer> masteredCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> activeCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> dueTodayCount = new MutableLiveData<>(0);

    // Observer references for cleanup
    private Observer<List<ForgettingCurveItemEntity>> curveObserver;
    private Observer<List<ReviewTaskEntity>> reviewObserver;

    private static final String PREFS_NAME = "pomodoro_prefs";
    private static final String KEY_WORK_DURATION = "work_duration";
    private static final String KEY_BREAK_DURATION = "break_duration";

    public FocusViewModel(@NonNull Application application) {
        super(application);

        loadPomodoroSettings();
        loadPomodoroStats();
        loadForgettingCurveData();
    }

    // ═══ Pomodoro ═══

    public LiveData<TimerState> getTimerState() { return timerState; }
    public LiveData<Long> getRemainingSeconds() { return remainingSeconds; }
    public LiveData<Integer> getTotalSeconds() { return totalSeconds; }
    public LiveData<Integer> getCompletedTodayCount() { return completedTodayCount; }
    public LiveData<Integer> getTodayFocusMinutes() { return todayFocusMinutes; }
    public LiveData<String> getMessage() { return message; }
    public void clearMessage() { message.setValue(null); }

    public int getWorkDurationMinutes() { return workDurationMinutes; }
    public int getBreakDurationMinutes() { return breakDurationMinutes; }

    public void setWorkDurationMinutes(int minutes) {
        workDurationMinutes = minutes;
        getApplication().getSharedPreferences(PREFS_NAME, 0)
                .edit().putInt(KEY_WORK_DURATION, minutes).apply();
    }

    public void setBreakDurationMinutes(int minutes) {
        breakDurationMinutes = minutes;
        getApplication().getSharedPreferences(PREFS_NAME, 0)
                .edit().putInt(KEY_BREAK_DURATION, minutes).apply();
    }

    public void setCurrentTaskTitle(String title) { currentTaskTitle = title; }

    public void startTimer() {
        if (timerState.getValue() == TimerState.PAUSED) {
            resumeTimer();
            return;
        }
        // Cancel any existing timer before starting a new one
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        int durationSec;
        if (timerState.getValue() == TimerState.IDLE) {
            durationSec = workDurationMinutes * 60;
            timerState.setValue(TimerState.RUNNING);
        } else {
            durationSec = breakDurationMinutes * 60;
            timerState.setValue(TimerState.BREAK);
        }

        totalSeconds.setValue(durationSec);
        remainingSeconds.setValue((long) durationSec);

        countDownTimer = new CountDownTimer(durationSec * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingSeconds.setValue(millisUntilFinished / 1000);
            }

            @Override
            public void onFinish() {
                remainingSeconds.setValue(0L);
                onTimerFinished();
            }
        };
        countDownTimer.start();
    }

    public void pauseTimer() {
        if (countDownTimer != null) {
            pausedState = timerState.getValue(); // save state before pausing
            countDownTimer.cancel();
            countDownTimer = null;
        }
        timerState.setValue(TimerState.PAUSED);
    }

    private void resumeTimer() {
        Long remaining = remainingSeconds.getValue();
        if (remaining == null || remaining <= 0) return;

        countDownTimer = new CountDownTimer(remaining * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingSeconds.setValue(millisUntilFinished / 1000);
            }

            @Override
            public void onFinish() {
                remainingSeconds.setValue(0L);
                onTimerFinished();
            }
        };
        countDownTimer.start();

        // Restore correct state from saved pausedState
        timerState.setValue(pausedState != null ? pausedState : TimerState.RUNNING);
        pausedState = null;
    }

    public void resetTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        timerState.setValue(TimerState.IDLE);
        remainingSeconds.setValue(0L);
        totalSeconds.setValue(0);
        consecutivePomodoros = 0;
    }

    public void skipToBreak() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        onWorkSessionCompleted();
        startBreak();
    }

    private void onTimerFinished() {
        TimerState current = timerState.getValue();
        if (current == TimerState.RUNNING) {
            onWorkSessionCompleted();
            startBreak();
        } else if (current == TimerState.BREAK) {
            timerState.setValue(TimerState.IDLE);
            message.setValue("休息结束，准备下一个番茄钟！");
        }
    }

    private void onWorkSessionCompleted() {
        consecutivePomodoros++;
        long now = System.currentTimeMillis();

        PomodoroSessionEntity session = new PomodoroSessionEntity();
        session.setUserId(userId);
        session.setTaskTitle(currentTaskTitle);
        session.setDurationMinutes(workDurationMinutes);
        session.setStartedAt(now - workDurationMinutes * 60 * 1000L);
        session.setCompletedAt(now);
        session.setCompleted(true);
        session.setCoinsEarned(RewardCalculator.POMODORO_COINS);
        session.setXpEarned(RewardCalculator.POMODORO_XP);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.pomodoroSessionDao().insert(session);

            RewardRepository rewardRepo = new RewardRepository(db);
            rewardRepo.awardTaskCompletionSync(userId, -1,
                    RewardCalculator.POMODORO_COINS, RewardCalculator.POMODORO_XP,
                    "完成番茄钟" + (currentTaskTitle.isEmpty() ? "" : ": " + currentTaskTitle));

            loadPomodoroStatsSync();
        });

        message.setValue("番茄钟完成！+ " + RewardCalculator.POMODORO_XP + " XP, + " + RewardCalculator.POMODORO_COINS + " 金币");
    }

    private void startBreak() {
        int breakSec = breakDurationMinutes * 60;
        totalSeconds.setValue(breakSec);
        remainingSeconds.setValue((long) breakSec);
        timerState.setValue(TimerState.BREAK);

        countDownTimer = new CountDownTimer(breakSec * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingSeconds.setValue(millisUntilFinished / 1000);
            }

            @Override
            public void onFinish() {
                remainingSeconds.setValue(0L);
                timerState.setValue(TimerState.IDLE);
                message.setValue("休息结束，准备下一个番茄钟！");
            }
        };
        countDownTimer.start();
    }

    private void loadPomodoroSettings() {
        SharedPreferences prefs = getApplication().getSharedPreferences(PREFS_NAME, 0);
        workDurationMinutes = prefs.getInt(KEY_WORK_DURATION, 25);
        breakDurationMinutes = prefs.getInt(KEY_BREAK_DURATION, 5);
    }

    private void loadPomodoroStats() {
        AppDatabase.databaseWriteExecutor.execute(this::loadPomodoroStatsSync);
    }

    private void loadPomodoroStatsSync() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfDay = cal.getTimeInMillis();
        long endOfDay = startOfDay + 24 * 60 * 60 * 1000L;

        List<PomodoroSessionEntity> sessions = db.pomodoroSessionDao().getCompletedByUserIdSync(userId);
        // Filter today's completed sessions
        int count = 0;
        int minutes = 0;
        for (PomodoroSessionEntity s : sessions) {
            if (s.isCompleted() && s.getStartedAt() >= startOfDay && s.getStartedAt() < endOfDay) {
                count++;
                minutes += s.getDurationMinutes();
            }
        }
        completedTodayCount.postValue(count);
        todayFocusMinutes.postValue(minutes);
    }

    // ═══ Forgetting Curve ═══

    public LiveData<List<ForgettingCurveItemEntity>> getActiveItems() { return activeItems; }
    public LiveData<List<ReviewTaskEntity>> getTodayReviews() { return todayReviews; }
    public LiveData<Integer> getMasteredCount() { return masteredCount; }
    public LiveData<Integer> getActiveCount() { return activeCount; }
    public LiveData<Integer> getDueTodayCount() { return dueTodayCount; }

    private void loadForgettingCurveData() {
        // Remove old observers before registering new ones to prevent leaks on repeated calls
        if (curveObserver != null) {
            db.forgettingCurveItemDao().getByUserId(userId).removeObserver(curveObserver);
        }
        if (reviewObserver != null) {
            db.reviewTaskDao().getPendingByDate(userId, DateUtils.getTodayString()).removeObserver(reviewObserver);
        }

        String today = DateUtils.getTodayString();

        curveObserver = items -> {
            if (items != null) {
                List<ForgettingCurveItemEntity> active = new java.util.ArrayList<>();
                int mastered = 0;
                int dueToday = 0;
                for (ForgettingCurveItemEntity item : items) {
                    if (item.isMastered()) {
                        mastered++;
                    } else {
                        active.add(item);
                        if (item.getNextReviewDate() != null && item.getNextReviewDate().compareTo(today) <= 0) {
                            dueToday++;
                        }
                    }
                }
                activeItems.postValue(active);
                masteredCount.postValue(mastered);
                activeCount.postValue(active.size());
                dueTodayCount.postValue(dueToday);
            }
        };
        db.forgettingCurveItemDao().getByUserId(userId).observeForever(curveObserver);

        reviewObserver = reviews -> todayReviews.postValue(reviews);
        db.reviewTaskDao().getPendingByDate(userId, today).observeForever(reviewObserver);
    }

    public void completeReview(long curveItemId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            ForgettingCurveItemEntity item = db.forgettingCurveItemDao().getById(curveItemId);
            if (item == null || item.isMastered()) return;

            int nextStep = item.getReviewStep() + 1;
            item.setReviewStep(nextStep);
            item.setTotalReviews(item.getTotalReviews() + 1);

            if (nextStep >= RewardCalculator.REVIEW_INTERVALS.length) {
                item.setMastered(true);
            } else {
                // Calculate next review date
                String createdAt = item.getCreatedAt();
                Calendar cal = Calendar.getInstance();
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                    cal.setTime(sdf.parse(createdAt));
                } catch (Exception e) {
                    cal.setTime(new java.util.Date(System.currentTimeMillis()));
                }
                cal.add(Calendar.DAY_OF_YEAR, RewardCalculator.REVIEW_INTERVALS[nextStep]);
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                item.setNextReviewDate(sdf.format(cal.getTime()));

                // Create next review task
                ReviewTaskEntity reviewTask = new ReviewTaskEntity();
                reviewTask.setUserId(userId);
                reviewTask.setCurveItemId(curveItemId);
                reviewTask.setTitle("复习: " + item.getTitle());
                reviewTask.setReviewDate(item.getNextReviewDate());
                reviewTask.setStep(nextStep + 1);
                reviewTask.setCompleted(false);
                db.reviewTaskDao().insert(reviewTask);

                // Also create a schedule task for the next review date
                createReviewScheduleTask(item.getTitle(), item.getNextReviewDate());
            }

            db.forgettingCurveItemDao().update(item);

            // Mark the corresponding schedule task as completed
            TaskEntity scheduleTask = db.taskDao().getReviewTaskByDateAndTitle(
                    userId, DateUtils.getTodayString(), "复习: " + item.getTitle());
            if (scheduleTask != null && !scheduleTask.isCompleted()) {
                scheduleTask.setCompleted(true);
                scheduleTask.setCompletedAt(System.currentTimeMillis());
                db.taskDao().update(scheduleTask);
            }

            // Award review reward
            RewardRepository rewardRepo = new RewardRepository(db);
            rewardRepo.awardTaskCompletionSync(userId, -1,
                    RewardCalculator.REVIEW_COINS, RewardCalculator.REVIEW_XP,
                    "完成复习: " + item.getTitle());

            message.postValue("复习完成！+ " + RewardCalculator.REVIEW_XP + " XP");
        });
    }

    public void createForgettingCurveItem(long taskId, String title) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            String today = DateUtils.getTodayString();

            ForgettingCurveItemEntity item = new ForgettingCurveItemEntity();
            item.setUserId(userId);
            item.setTaskId(taskId);
            item.setTitle(title);
            item.setReviewStep(0);
            item.setTotalReviews(0);
            item.setMastered(false);
            item.setCreatedAt(today);

            // First review: 1 day later
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, RewardCalculator.REVIEW_INTERVALS[0]);
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            item.setNextReviewDate(sdf.format(cal.getTime()));

            long itemId = db.forgettingCurveItemDao().insert(item);

            // Create first review task
            ReviewTaskEntity reviewTask = new ReviewTaskEntity();
            reviewTask.setUserId(userId);
            reviewTask.setCurveItemId(itemId);
            reviewTask.setTitle("复习: " + title);
            reviewTask.setReviewDate(item.getNextReviewDate());
            reviewTask.setStep(1);
            reviewTask.setCompleted(false);
            db.reviewTaskDao().insert(reviewTask);

            // Create a schedule task for the first review date
            createReviewScheduleTask(title, item.getNextReviewDate());
        });
    }

    private void createReviewScheduleTask(String itemTitle, String reviewDate) {
        TaskEntity task = new TaskEntity();
        task.setUserId(userId);
        task.setTitle("复习: " + itemTitle);
        task.setDate(reviewDate);
        task.setCategory("Review");
        task.setStartTime(540); // 9:00 AM
        task.setEndTime(600);   // 10:00 AM
        task.setCompleted(false);
        task.setCreatedAt(System.currentTimeMillis());
        task.setUpdatedAt(System.currentTimeMillis());
        db.taskDao().insert(task);
        NotificationHelper.scheduleReminder(getApplication(), "复习: " + itemTitle, reviewDate, 540);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (curveObserver != null) {
            db.forgettingCurveItemDao().getByUserId(userId).removeObserver(curveObserver);
        }
        if (reviewObserver != null) {
            db.reviewTaskDao().getPendingByDate(userId, DateUtils.getTodayString()).removeObserver(reviewObserver);
        }
    }
}