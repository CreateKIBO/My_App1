package com.example.myapplication.data.repository;

import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.UserDao;
import com.example.myapplication.data.local.UserEntity;
import com.example.myapplication.util.DateUtils;

import java.util.concurrent.ExecutorService;

public class UserRepository {

    private final UserDao userDao;
    private final ExecutorService executor;

    public UserRepository(AppDatabase database, ExecutorService executor) {
        this.userDao = database.userDao();
        this.executor = executor;
    }

    public long createDefaultUser() {
        UserEntity user = new UserEntity();
        user.setDisplayName("我");
        user.setCurrentCoins(0);
        user.setTotalCoins(0);
        user.setTotalXp(0);
        user.setLevel(1);
        user.setCurrentStreak(0);
        user.setAvatarId(1);
        user.setThemeId(9);
        user.setUnlockedAvatars("1");
        user.setUnlockedThemes("9");
        user.setLastActiveDate(DateUtils.getTodayString());
        user.setCreatedAt(System.currentTimeMillis());
        return userDao.insert(user);
    }

    public UserEntity getUserById(long userId) {
        return userDao.getUserById(userId);
    }

    public void updateStreak(long userId) {
        executor.execute(() -> {
            UserEntity user = userDao.getUserById(userId);
            if (user == null) return;

            String today = DateUtils.getTodayString();
            String lastDate = user.getLastActiveDate();

            if (today.equals(lastDate)) return;

            if (lastDate != null && lastDate.equals(DateUtils.getYesterdayString())) {
                user.setCurrentStreak(user.getCurrentStreak() + 1);
            } else {
                user.setCurrentStreak(1);
            }
            user.setLastActiveDate(today);
            userDao.update(user);
        });
    }
}
