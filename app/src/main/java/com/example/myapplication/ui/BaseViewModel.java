package com.example.myapplication.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.util.SessionManager;

public abstract class BaseViewModel extends AndroidViewModel {

    protected final AppDatabase db;
    protected final SessionManager sessionManager;
    protected final long userId;

    public BaseViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        sessionManager = new SessionManager(application);
        userId = sessionManager.getLocalUserId();
    }
}
