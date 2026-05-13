package com.example.myapplication.util;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.View;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class NotificationHelper {

    private static final String PREFS_NAME = "notification_prefs";
    private static final String KEY_DAILY = "daily_reminder";
    private static final String KEY_TASK = "task_complete";
    private static final String KEY_STREAK = "streak_reminder";
    private static final String CHANNEL_ID = "task_reminders";

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

    public static void ensureChannel(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "日程提醒",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("日程即将开始时发送提醒");
        channel.enableVibration(true);
        nm.createNotificationChannel(channel);
    }

    public static void scheduleReminder(Context context, String title, String date, int startTimeMinutes) {
        if (context == null) return;
        ensureChannel(context);

        Calendar cal = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            cal.setTime(sdf.parse(date));
        } catch (Exception e) {
            return;
        }
        cal.set(Calendar.HOUR_OF_DAY, startTimeMinutes / 60);
        cal.set(Calendar.MINUTE, startTimeMinutes % 60);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        long triggerTime = cal.getTimeInMillis() - 5 * 60 * 1000L;
        if (triggerTime <= System.currentTimeMillis()) return;

        String timeStr = String.format(Locale.getDefault(), "%02d:%02d", startTimeMinutes / 60, startTimeMinutes % 60);

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(ReminderReceiver.EXTRA_TITLE, title);
        intent.putExtra(ReminderReceiver.EXTRA_TIME, timeStr);

        int requestCode = (title + date + startTimeMinutes).hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }

    public static void showReminderNotification(Context context, String title, String time) {
        ensureChannel(context);

        Intent launchIntent = new Intent(context, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String content = time != null
                ? "日程「" + title + "」将在5分钟后开始 (" + time + ")"
                : "日程「" + title + "」将在5分钟后开始";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_focus)
                .setContentTitle("日迹 — 日程提醒")
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManagerCompat.from(context).notify((int) System.currentTimeMillis(), builder.build());
    }
}
