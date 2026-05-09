package com.example.myapplication.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.myapplication.util.SessionManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
    entities = {
        TaskEntity.class,
        UserEntity.class,
        RewardTransactionEntity.class,
        ShopItemEntity.class
    },
    version = 4,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract TaskDao taskDao();
    public abstract UserDao userDao();
    public abstract RewardTransactionDao rewardTransactionDao();
    public abstract ShopItemDao shopItemDao();

    private static volatile AppDatabase INSTANCE;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(4);

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "schedule_planner_db"
                    )
                    .fallbackToDestructiveMigration()
                    .addCallback(new Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            initializeData(context);
                        }

                        @Override
                        public void onDestructiveMigration(@NonNull SupportSQLiteDatabase db) {
                            super.onDestructiveMigration(db);
                            initializeData(context);
                        }
                    })
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    private static void initializeData(Context context) {
        databaseWriteExecutor.execute(() -> {
            AppDatabase db = getInstance(context);

            // Insert shop items first to get their IDs
            ShopItemEntity[] items = ShopItemEntity.getDefaultItems();
            StringBuilder freeAvatarIds = new StringBuilder();
            StringBuilder freeThemeIds = new StringBuilder();
            long firstAvatarId = 0;
            long firstThemeId = 0;

            for (ShopItemEntity item : items) {
                long itemId = db.shopItemDao().insert(item);
                if (item.getPrice() == 0) {
                    if ("AVATAR".equals(item.getType())) {
                        if (firstAvatarId == 0) firstAvatarId = itemId;
                        if (freeAvatarIds.length() > 0) freeAvatarIds.append(",");
                        freeAvatarIds.append(itemId);
                    } else {
                        if (firstThemeId == 0) firstThemeId = itemId;
                        if (freeThemeIds.length() > 0) freeThemeIds.append(",");
                        freeThemeIds.append(itemId);
                    }
                }
            }

            // Create user with correct default avatar/theme
            UserEntity user = new UserEntity();
            user.setDisplayName("我");
            user.setCurrentCoins(0);
            user.setTotalCoins(0);
            user.setTotalXp(0);
            user.setLevel(1);
            user.setCurrentStreak(0);
            user.setAvatarId((int) firstAvatarId);
            user.setThemeId((int) firstThemeId);
            user.setUnlockedAvatars(freeAvatarIds.length() > 0 ? freeAvatarIds.toString() : "");
            user.setUnlockedThemes(freeThemeIds.length() > 0 ? freeThemeIds.toString() : "");
            user.setCreatedAt(System.currentTimeMillis());
            long userId = db.userDao().insert(user);
            new SessionManager(context).setLocalUserId(userId);
        });
    }
}
