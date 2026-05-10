package com.example.myapplication.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import androidx.appcompat.widget.SwitchCompat;

import com.example.myapplication.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class NotificationHelper {

    private static final String PREFS_NAME = "notification_prefs";
    private static final String KEY_DAILY = "daily_reminder";
    private static final String KEY_TASK = "task_complete";
    private static final String KEY_STREAK = "streak_reminder";

    public static BottomSheetDialog showNotificationDialog(Context context) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = View.inflate(context, R.layout.dialog_notification, null);
        dialog.setContentView(view);

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        SwitchCompat switchDaily = view.findViewById(R.id.switch_daily_reminder);
        SwitchCompat switchTask = view.findViewById(R.id.switch_task_complete);
        SwitchCompat switchStreak = view.findViewById(R.id.switch_streak_reminder);

        switchDaily.setChecked(prefs.getBoolean(KEY_DAILY, true));
        switchTask.setChecked(prefs.getBoolean(KEY_TASK, true));
        switchStreak.setChecked(prefs.getBoolean(KEY_STREAK, true));

        switchDaily.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean(KEY_DAILY, checked).apply());
        switchTask.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean(KEY_TASK, checked).apply());
        switchStreak.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean(KEY_STREAK, checked).apply());

        dialog.show();
        return dialog;
    }

    public static boolean isDailyReminderEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_DAILY, true);
    }

    public static boolean isTaskCompleteEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_TASK, true);
    }

    public static boolean isStreakReminderEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_STREAK, true);
    }
}