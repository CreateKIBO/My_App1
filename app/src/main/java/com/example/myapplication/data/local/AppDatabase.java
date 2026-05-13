package com.example.myapplication.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.myapplication.util.SessionManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
    entities = {
        TaskEntity.class,
        UserEntity.class,
        RewardTransactionEntity.class,
        ShopItemEntity.class,
        PomodoroSessionEntity.class,
        ForgettingCurveItemEntity.class,
        ReviewTaskEntity.class
    },
    version = 11,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract TaskDao taskDao();
    public abstract UserDao userDao();
    public abstract RewardTransactionDao rewardTransactionDao();
    public abstract ShopItemDao shopItemDao();
    public abstract PomodoroSessionDao pomodoroSessionDao();
    public abstract ForgettingCurveItemDao forgettingCurveItemDao();
    public abstract ReviewTaskDao reviewTaskDao();

    private static volatile AppDatabase INSTANCE;
    private static volatile CountDownLatch initLatch = new CountDownLatch(1);
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(4);

    static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE tasks ADD COLUMN priority INTEGER NOT NULL DEFAULT 1");
        }
    };

    static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE shop_items ADD COLUMN source TEXT NOT NULL DEFAULT 'shop'");
        }
    };

    public static void awaitInitialization() throws InterruptedException {
        initLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);
    }

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "schedule_planner_db"
                    )
                    .addMigrations(MIGRATION_9_10, MIGRATION_10_11)
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
            try {
                AppDatabase db = getInstance(context);

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

                addPropItems(db);
            } finally {
                initLatch.countDown();
            }
        });
    }

    private static void addPropItems(AppDatabase db) {
        ShopItemEntity freezeCard = new ShopItemEntity();
        freezeCard.setType("PROP");
        freezeCard.setName("冻结卡");
        freezeCard.setDescription("保持连续打卡不被中断，隔天未完成时自动使用");
        freezeCard.setPrice(50);
        freezeCard.setIconResName("ic_freeze_card");
        freezeCard.setColorHex("#3B82F6");
        freezeCard.setColorHexDark("#2563EB");
        freezeCard.setEmoji("❄️");
        db.shopItemDao().insert(freezeCard);
    }
}
