package com.example.myapplication.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "schedule_planner_prefs";
    private static final String KEY_LOCAL_USER_ID = "local_user_id";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public long getLocalUserId() {
        return prefs.getLong(KEY_LOCAL_USER_ID, -1);
    }

    public void setLocalUserId(long id) {
        prefs.edit().putLong(KEY_LOCAL_USER_ID, id).apply();
    }
}
