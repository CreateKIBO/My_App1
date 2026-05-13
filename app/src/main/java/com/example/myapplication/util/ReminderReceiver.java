package com.example.myapplication.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.example.myapplication.R;

public class ReminderReceiver extends BroadcastReceiver {

    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_TIME = "extra_time";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra(EXTRA_TITLE);
        String time = intent.getStringExtra(EXTRA_TIME);
        if (title == null) title = "日程提醒";

        NotificationHelper.showReminderNotification(context, title, time);
    }
}
