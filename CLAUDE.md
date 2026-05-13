# Vibe_work Project Context (from Hermes conversation)

## Previous Conversation Summary

On May 11, the user asked Hermes to review their Android app code. The review was partially completed before the conversation was cut off. Key findings so far:

### Files Already Reviewed
- MainActivity.java - Single activity with BottomNavigation, ensureLocalUser() method
- SplashActivity.java - Disables back press
- HomeFragment.java - Main task list, header stats
- HomeViewModel.java - Task CRUD, streak logic
- TaskEntity.java - Room entity with category, priority, due date
- UserEntity.java - XP, coins, level system
- RewardCalculator.java - Base 10 coins + 25 XP per task, category bonuses
- FocusFragment.java - Pomodoro timer with forgetting curve review (PARTIALLY reviewed)
- FocusViewModel.java - Timer logic, observeForever() usage
- AppDatabase.java - Room DB with destructive migration fallback
- Various DAOs: TaskDao, UserDao, ShopItemDao, PomodoroSessionDao, ForgettingCurveItemDao, ReviewTaskDao, RewardTransactionDao

### Architecture
- MVVM + Room + Repository pattern
- Navigation Component with BottomNavigation
- ViewBinding
- No dependency injection (manual AppDatabase.getInstance())
- Single-activity design

### Features
- Task management with categories (Work/Study/Exercise/Personal)
- Pomodoro focus timer
- Forgetting curve review system
- XP, coins, level system
- Streak/连续打卡
- Avatar shop and theme store
- Custom avatar upload
- Calendar view
- Reward history
- Settings (dark mode toggle)
- Notifications and reminders

### Known Issues Already Identified
1. MainActivity.ensureLocalUser() blocks main thread with Thread.join()
2. Dark mode toggle not persisted
3. No tests
4. Hardcoded Chinese strings
5. Magic numbers in ThemeManager (themeId - 9)

## What Needs to Be Done

Continue the full code review. Read ALL Java files in:
app/src/main/java/com/example/myapplication/

Then provide:
1. Complete architecture review
2. All bugs (especially threading, memory leaks, logic errors)
3. Security issues
4. Performance problems
5. Code quality issues
6. Missing features
7. Overall rating out of 10

Be specific with file names and line numbers.
