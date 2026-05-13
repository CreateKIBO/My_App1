# Vibe_work Bug 修复计划 — Phase 1: 严重 Bug

> **For Hermes:** 此计划覆盖 10 个严重 Bug + 5 个高危 Bug，按优先级排列。
> **目标:** 修复崩溃/ANR/内存泄漏/数据损坏级别的 Bug。
> **架构:** Android MVVM + Room + ViewBinding，单 Activity 多 Fragment。
> **技术栈:** Java, Android Room, LiveData, CountDownTimer

---

## 项目文件结构速查

```
app/src/main/java/com/example/myapplication/
├── MainActivity.java                    ← 🔴 ANR
├── data/local/
│   ├── AppDatabase.java                 ← 🔴 destructive migration + initLatch
│   ├── UserEntity.java
│   ├── UserDao.java
│   └── TaskDao.java
├── data/repository/
│   └── UserRepository.java              ← 🔴 avatarId=1 硬编码
├── ui/
│   ├── FocusViewModel.java              ← 🔴 observeForever 泄漏 + Timer 状态
│   ├── HomeViewModel.java               ← 🔴 streak 回退逻辑
│   ├── StreakFragment.java              ← 🟠 31 次同步 SQL
│   ├── AvatarEditFragment.java          ← 🟠 图片读两次
│   └── CalendarViewModel.java           ← 🟠 MutableLiveData 泄露
└── util/
    ├── AvatarHelper.java
    ├── SessionManager.java
    └── ThemeManager.java
```

---

## 🔴 Phase 1: 严重 Bug（崩溃/数据丢失级）

### Task 1: 修复 MainActivity 主线程阻塞（ANR）

**Objective:** 将 `ensureLocalUser()` 中的主线程阻塞改为异步回调，消除 8 秒 ANR 风险。

**Files:**
- Modify: `app/src/main/java/com/example/myapplication/MainActivity.java`

**问题:** 第 195-237 行，`ensureLocalUser()` 在 `onCreate()` 中同步调用，内部用 `Thread.join(3000)` + `Thread.join(5000)` 阻塞主线程。

**修复方案:** 将整个 `ensureLocalUser()` 改为异步方法，使用 `AppDatabase.databaseWriteExecutor` + `runOnUiThread` 回调。不使用 `awaitInitialization()` 阻塞。

**Step 1: 移除 `awaitInitialization()` 调用**

删除第 197-199 行：
```java
// 删除这段
try {
    AppDatabase.awaitInitialization();
} catch (InterruptedException ignored) {}
```

**Step 2: 重写 `ensureLocalUser()` 为异步方法**

```java
private void ensureLocalUser() {
    AppDatabase.databaseWriteExecutor.execute(() -> {
        long existingId = sessionManager.getLocalUserId();
        AppDatabase db = AppDatabase.getInstance(this);

        if (existingId != -1) {
            // Verify the user still exists
            UserEntity user = db.userDao().getUserById(existingId);
            if (user != null) {
                // User exists, all good
                runOnUiThread(this::onUserReady);
                return;
            }
        }

        // No valid user ID or user was wiped — find or create
        UserEntity existing = db.userDao().getFirstUser();
        if (existing != null) {
            sessionManager.setLocalUserId(existing.getId());
        } else {
            long id = new UserRepository(db, AppDatabase.databaseWriteExecutor).createDefaultUser();
            sessionManager.setLocalUserId(id);
        }

        runOnUiThread(this::onUserReady);
    });
}

private void onUserReady() {
    NotificationHelper.ensureChannel(this);
    requestNotificationPermission();
    observeTheme();
}
```

**Step 3: 修改 `onCreate()` 调用方式**

```java
// onCreate() 中，删除原有行，替换为：
ensureLocalUser();
// 注意：ensureLocalUser 现在是异步的，NotificationHelper 等移到 onUserReady 回调中
NotificationHelper.ensureChannel(this);  // ← 删除此行
requestNotificationPermission();        // ← 删除此行
observeTheme();                         // ← 删除此行
```

改完之后的 `onCreate()` 末尾：
```java
        ensureLocalUser();
    }
```

**Step 4: 验证**

- `./gradlew assembleDebug` 编译通过
- 首次启动 → 正常创建用户
- 已存在用户启动 → 正常加载
- destructive migration 后启动 → 正常重建用户
- 不再在 onCreate 中有 `Thread.join()` 调用

---

### Task 2: 修复 FocusViewModel observeForever 内存泄漏

**Objective:** 确保 `observeForever()` 注册的 Observer 在 ViewModel 清除时被正确移除，防止内存泄漏。

**Files:**
- Modify: `app/src/main/java/com/example/myapplication/ui/FocusViewModel.java`

**问题:** `loadForgettingCurveData()` 中注册了 `observeForever()`，但如果在 ViewModel 生命周期内多次调用（例如 userId 变化），旧的 Observer 不会被移除。`onCleared()` 中有清理逻辑但只在 ViewModel 销毁时触发。

**修复方案:** 在注册新 Observer 前先移除旧 Observer。

**Step 1: 修改 `loadForgettingCurveData()`**

```java
private void loadForgettingCurveData() {
    String today = DateUtils.getTodayString();

    // Remove old observers before registering new ones
    if (curveObserver != null) {
        db.forgettingCurveItemDao().getByUserId(userId).removeObserver(curveObserver);
    }
    if (reviewObserver != null) {
        db.reviewTaskDao().getPendingByDate(userId, today).removeObserver(reviewObserver);
    }

    curveObserver = items -> {
        // ... 保持不变
    };
    db.forgettingCurveItemDao().getByUserId(userId).observeForever(curveObserver);

    reviewObserver = reviews -> todayReviews.postValue(reviews);
    db.reviewTaskDao().getPendingByDate(userId, today).observeForever(reviewObserver);
}
```

**Step 2: 验证**

- `./gradlew assembleDebug`
- App 运行 → 进入 Focus 页面 → 返回 → 再进入 → 不崩溃，无泄漏日志

---

### Task 3: 修复新用户无默认头像导致空指针

**Objective:** 确保新创建的用户始终有一个有效的默认头像 ID。

**Files:**
- Modify: `app/src/main/java/com/example/myapplication/data/repository/UserRepository.java`

**问题:** `createDefaultUser()` 硬编码 `avatarId=1`，但如果 ShopItem 表中 ID=1 不存在（destructive migration 后可能被重建为其他 ID），加载头像时会找不到资源。

**修复方案:** 不要硬编码 avatarId=1，改为查找实际的默认头像 ID（price=0 的第一个 AVATAR 类型 shop item）。

**Step 1: 修改 `UserRepository.createDefaultUser()`**

```java
public long createDefaultUser() {
    // Find the actual default avatar ID (first free avatar)
    ShopItemEntity defaultAvatar = userDao.getFirstFreeAvatar();
    int defaultAvatarId = (defaultAvatar != null) ? (int) defaultAvatar.getId() : 1;
    
    // Find the actual default theme ID (first free theme)  
    ShopItemEntity defaultTheme = userDao.getFirstFreeTheme();
    int defaultThemeId = (defaultTheme != null) ? (int) defaultTheme.getId() : 9;

    UserEntity user = new UserEntity();
    user.setDisplayName("我");
    user.setCurrentCoins(0);
    user.setTotalCoins(0);
    user.setTotalXp(0);
    user.setLevel(1);
    user.setCurrentStreak(0);
    user.setAvatarId(defaultAvatarId);
    user.setThemeId(defaultThemeId);
    user.setUnlockedAvatars(String.valueOf(defaultAvatarId));
    user.setUnlockedThemes(String.valueOf(defaultThemeId));
    user.setLastActiveDate(DateUtils.getTodayString());
    user.setCreatedAt(System.currentTimeMillis());
    return userDao.insert(user);
}
```

**Step 2: 在 UserDao 中添加查询方法**

```java
// 在 UserDao.java 中添加：
@Query("SELECT * FROM shop_items WHERE type = 'AVATAR' AND price = 0 LIMIT 1")
ShopItemEntity getFirstFreeAvatar();

@Query("SELECT * FROM shop_items WHERE type = 'THEME' AND price = 0 LIMIT 1")
ShopItemEntity getFirstFreeTheme();
```

**Step 3: 验证**

- `./gradlew assembleDebug`
- 首次启动 → 默认头像正常显示
- destructive migration 后 → 自动重建用户，头像正常

---

### Task 4: 修复 HomeViewModel 连签回退逻辑

**Objective:** 修复 `onTaskAddedToday()` 中新增任务后错误减去 streak 的问题。

**Files:**
- Modify: `app/src/main/java/com/example/myapplication/ui/HomeViewModel.java`

**问题:** `onTaskAddedToday()` (第 88-108 行) 中，当用户已全部完成今日任务（streak 已计入），再添加一个新任务后，会错误地将 streak 减 1。

**修复方案:** `onTaskAddedToday()` 不应该修改 streak。添加任务 ≠ 失去 streak。streak 只应在"当日未完成任何任务"或"断签"时变化。移除 `onTaskAddedToday()` 中的 streak 减法逻辑。

**Step 1: 简化 `onTaskAddedToday()`**

```java
/**
 * Called when a new task is added for today.
 * No longer modifies streak — adding tasks shouldn't undo earned streaks.
 */
public void onTaskAddedToday() {
    // Intentionally empty — streak is only managed by completeTask() and checkAndUpdateStreak().
    // Adding a new task does not invalidate a streak already earned today.
}
```

或者直接移除该方法及其所有调用点。

**Step 2: 查找调用点并移除**

搜索 `onTaskAddedToday` 调用：
- 可能在 `AddEditTaskFragment` 或 `AddEditTaskViewModel` 中

**Step 3: 验证**

- `./gradlew assembleDebug`
- 场景：用户完成全部任务 → streak +1 → 又添加新任务 → streak 应保持不变

---

### Task 5: 修复 StreakFragment 31 次同步 SQL 查询

**Objective:** 将每月逐日查询的单次 SQL 改为一次批量查询。

**Files:**
- Modify: `app/src/main/java/com/example/myapplication/ui/StreakFragment.java`
- Modify: `app/src/main/java/com/example/myapplication/data/local/TaskDao.java`

**问题:** `loadStreakData()` 第 119-124 行，对每月每一天执行一次 `getCompletedCountForDateSync()`，31 次独立同步查询。

**修复方案:** 在 TaskDao 添加批量查询方法，一次查回整月数据。

**Step 1: 在 TaskDao 添加批量查询**

```java
// 在 TaskDao.java 中添加：
@Query("SELECT date, COUNT(*) as count FROM tasks " +
       "WHERE userId = :userId AND isCompleted = 1 " +
       "AND date >= :monthStart AND date <= :monthEnd " +
       "GROUP BY date")
List<DateCount> getCompletedCountsForMonth(long userId, String monthStart, String monthEnd);

// 内部类
class DateCount {
    public String date;
    public int count;
}
```

**Step 2: 修改 StreakFragment 查询逻辑**

```java
// 在 loadStreakData() 中，替换逐日查询：
Calendar cal = Calendar.getInstance();
int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
int thisMonth = cal.get(Calendar.MONTH);
int thisYear = cal.get(Calendar.YEAR);
SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

// 构建月份范围
cal.set(thisYear, thisMonth, 1);
String monthStart = dateFormat.format(cal.getTime());
cal.set(thisYear, thisMonth, daysInMonth);
String monthEnd = dateFormat.format(cal.getTime());

// 一次批量查询
var dateCounts = taskDao.getCompletedCountsForMonth(userId, monthStart, monthEnd);

// 构建日期→完成数的 Map
Map<String, Integer> countMap = new java.util.HashMap<>();
for (var dc : dateCounts) {
    countMap.put(dc.date, dc.count);
}

// 统计有完成任务的日期数
int completedDays = 0;
cal.set(thisYear, thisMonth, 1);
for (int d = 1; d <= daysInMonth; d++) {
    cal.set(thisYear, thisMonth, d);
    String dateStr = dateFormat.format(cal.getTime());
    if (countMap.getOrDefault(dateStr, 0) > 0) {
        completedDays++;
    }
}
monthlyCompleted = completedDays;
```

**Step 3: 同样修改 buildHeatmapGrid()**

```java
// buildHeatmapGrid() 中也调用同一批量查询，避免逐日查
// 将 countMap 提取为方法返回值复用
```

**Step 4: 验证**

- `./gradlew assembleDebug`
- 进入连续打卡页面 → 正常显示热力图和月度统计
- 性能：从 31 次 SQL → 1 次 SQL

---

## 🟠 Phase 2: 高危 Bug

### Task 6: 修复 AvatarEditFragment 图片流读两次

**Files:**
- Modify: `app/src/main/java/com/example/myapplication/ui/AvatarEditFragment.java`

**问题:** `handleSelectedImage()` 中第 284-300 行读取一次 stream 检查大小，第 303 行再读一次 stream 处理图像。ContentResolver 的 stream 可能不可重复读取。

**修复方案:** 第一次读流时同时缓存到 ByteArrayOutputStream，然后用 ByteArrayInputStream 传给后续处理。

**Step 1: 合并两次读取**

```java
private void handleSelectedImage(Uri uri) {
    if (!isAdded()) return;

    ContentResolver resolver = requireContext().getContentResolver();
    String mimeType = resolver.getType(uri);
    if (mimeType == null ||
            (!mimeType.equals("image/jpeg") && !mimeType.equals("image/png")
                    && !mimeType.startsWith("image/"))) {
        Toast.makeText(requireContext(), "请选择 JPG 或 PNG 图片", Toast.LENGTH_SHORT).show();
        return;
    }

    // Read once into memory, validate size
    final int MAX_SIZE = 5 * 1024 * 1024; // 5MB
    byte[] imageBytes;
    try (InputStream inputStream = resolver.openInputStream(uri)) {
        if (inputStream == null) {
            Toast.makeText(requireContext(), "无法读取图片", Toast.LENGTH_SHORT).show();
            return;
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] tmp = new byte[4096];
        int bytesRead;
        int total = 0;
        while ((bytesRead = inputStream.read(tmp)) != -1) {
            total += bytesRead;
            if (total > MAX_SIZE) {
                Toast.makeText(requireContext(), "图片大小不能超过 5MB", Toast.LENGTH_SHORT).show();
                return;
            }
            buffer.write(tmp, 0, bytesRead);
        }
        imageBytes = buffer.toByteArray();
    } catch (Exception e) {
        Toast.makeText(requireContext(), "无法读取图片", Toast.LENGTH_SHORT).show();
        return;
    }

    // Process from in-memory bytes
    try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
        String savedPath = viewModel.saveCustomAvatarFromStream(bais);
        if (savedPath != null) {
            customAvatarPath = savedPath;
            viewModel.cleanupOldAvatars(savedPath);
            Toast.makeText(requireContext(), "头像已上传", Toast.LENGTH_SHORT).show();
            UserEntity user = viewModel.getCurrentUser().getValue();
            if (user != null) {
                updateCustomAvatarSection(user);
            }
        } else {
            Toast.makeText(requireContext(), "保存失败，请重试", Toast.LENGTH_SHORT).show();
        }
    } catch (Exception e) {
        Toast.makeText(requireContext(), "保存失败，请重试", Toast.LENGTH_SHORT).show();
    }
}
```

需要添加 import：`import java.io.ByteArrayOutputStream; import java.io.ByteArrayInputStream;`

**Step 2: 验证**

- `./gradlew assembleDebug`
- 选择图片上传 → 正常显示，不报错

---

### Task 7: 修复 FocusViewModel Timer 暂停恢复状态错误

**Files:**
- Modify: `app/src/main/java/com/example/myapplication/ui/FocusViewModel.java`

**问题:** `resumeTimer()` (第 145-169 行) 中，通过比较 `totalSeconds.getValue() == workDurationMinutes * 60` 来判断是 WORK 还是 BREAK 状态，但 break 时长也可能等于 work 时长（用户设置相同），导致状态判断错误。

**修复方案:** 使用 `wasWorkSession` 标志位 + `isPaused` 配合跟踪真实状态，而不是依赖总时长比较。

**Step 1: 修改 `pauseTimer()` 和 `resumeTimer()`**

```java
// 添加成员变量
private TimerState pausedState = null;

public void pauseTimer() {
    if (countDownTimer != null) {
        isPaused = true;
        pausedState = timerState.getValue(); // 保存暂停前的状态
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

    // 恢复正确状态
    timerState.setValue(pausedState != null ? pausedState : TimerState.RUNNING);
    isPaused = false;
    pausedState = null;
}
```

**Step 2: 验证**

- `./gradlew assembleDebug`
- 开始番茄钟 → 暂停 → 恢复 → 状态应为 RUNNING
- 开始休息 → 暂停 → 恢复 → 状态应为 BREAK

---

### Task 8: 修复 CalendarViewModel MutableLiveData 泄露

**Files:**
- Modify: `app/src/main/java/com/example/myapplication/ui/CalendarViewModel.java`

**问题:** (需要读文件确认) MutableLiveData 在 ViewModel 中可能被外部直接修改，违反封装原则。

**修复方案:** 将 MutableLiveData 改为 private，通过 LiveData getter 暴露。

**Step 1: 先读文件确认具体变量**

```bash
# 搜索 CalendarViewModel 中的 public MutableLiveData
grep -n "public.*MutableLiveData" CalendarViewModel.java
```

**Step 2: 改为 private + LiveData getter**

对每个 `public MutableLiveData` 改为：
```java
private final MutableLiveData<Type> _field = new MutableLiveData<>();
public LiveData<Type> getField() { return _field; }
```

**Step 3: 验证**

- `./gradlew assembleDebug`
- Calendar 页面正常显示

---

### Task 9: 修复已完成任务不可编辑/取消

**Files:**
- Modify: `app/src/main/java/com/example/myapplication/ui/HomeViewModel.java`
- Modify: `app/src/main/java/com/example/myapplication/ui/adapter/TaskAdapter.java`
- Modify: `app/src/main/java/com/example/myapplication/ui/HomeFragment.java`

**问题:** 已完成的任务无法取消完成（uncheck），用户误操作后无法撤销。

**修复方案:** 在 `completeTask()` 中增加 toggle 逻辑，已完成的可以取消。

**Step 1: 修改 `HomeViewModel.completeTask()`**

```java
public void completeTask(TaskEntity task) {
    AppDatabase.databaseWriteExecutor.execute(() -> {
        TaskEntity current = db.taskDao().getTaskById(task.getId());
        if (current == null) return;

        boolean wasCompleted = current.isCompleted();
        current.setCompleted(!wasCompleted);
        current.setUpdatedAt(System.currentTimeMillis());
        if (!wasCompleted) {
            current.setCompletedAt(System.currentTimeMillis());
            RewardCalculator.Reward reward = RewardCalculator.calculateTaskReward(current.getCategory());
            current.setCoinsEarned(reward.coins);
            current.setXpEarned(reward.xp);
            db.taskDao().update(current);

            RewardRepository rewardRepo = new RewardRepository(db);
            rewardRepo.awardTaskCompletionSync(userId, current.getId(), reward.coins, reward.xp,
                    "完成任务: " + current.getTitle());
        } else {
            // Uncompleting — remove rewards
            current.setCoinsEarned(0);
            current.setXpEarned(0);
            current.setCompletedAt(0);
            db.taskDao().update(current);
        }

        String today = DateUtils.getTodayString();
        int completed = db.taskDao().getCompletedCountForDateSync(userId, today);
        int total = db.taskDao().getTotalCountForDateSync(userId, today);

        if (completed == total && total > 0) {
            RewardCalculator.Reward bonus = RewardCalculator.getDayCompleteBonus();
            RewardRepository rewardRepo = new RewardRepository(db);
            rewardRepo.awardTaskCompletionSync(userId, -1, bonus.coins, bonus.xp, "全部完成奖励");
            updateStreakSync(rewardRepo);
        }
    });
}
```

**Step 2: 验证**

- `./gradlew assembleDebug`
- 点击已完成任务 → 取消完成 → 状态恢复为未完成

---

### Task 10: 修复 AppDatabase destructiveMigration 后 initLatch 状态混乱

**Files:**
- Modify: `app/src/main/java/com/example/myapplication/data/local/AppDatabase.java`

**问题:** `initLatch` 是 static volatile，如果数据库被 destructive migration 重建，`initLatch` 可能已是 count=0 状态（上一次初始化完成），导致 `awaitInitialization()` 立即返回而数据尚未填充。

**修复方案:** 在 `onDestructiveMigration` 回调中重置 `initLatch`。

**Step 1: 修改 AppDatabase**

```java
@Override
public void onDestructiveMigration(@NonNull SupportSQLiteDatabase db) {
    super.onDestructiveMigration(db);
    initLatch = new CountDownLatch(1); // 重置 latch
    initializeData(context);
}
```

**Step 2: 验证**

- `./gradlew assembleDebug`
- 升级数据库版本 → destructive migration 触发 → 数据正确初始化

---

## 验证检查清单

完成所有 Task 后执行：

```bash
cd /mnt/e/Vibe_work
./gradlew assembleDebug    # 编译通过
```

手动测试：
- [ ] App 首次启动 → 不崩溃，默认头像显示
- [ ] Focus 页面 → 进入/退出/再进入 → 无内存泄漏
- [ ] 连签 → 完成任务 streak+1 → 加新任务 streak 不变
- [ ] 番茄钟 → 开始/暂停/恢复 → 状态正确
- [ ] 头像上传 → 选择图片 → 正常显示
- [ ] 连续打卡页面 → 热力图正常
