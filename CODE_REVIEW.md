# VibeWork（日迹）代码审查报告

> 审查时间：2026-05-13 | 审查范围：全部源码 + 构建配置

---

## 🏗️ 一、架构问题

### 1. ❌ 无依赖注入，手动创建 Repository
- `HomeViewModel`、`ShopViewModel` 等直接 `new TaskRepository(db, executor)`
- 应该用 **Hilt/Dagger** 做依赖注入，方便测试和解耦
- 🔧 建议：引入 Hilt，至少用 `ViewModelProvider.Factory`

### 2. ❌ BaseViewModel 直接持有 AppDatabase 引用
- `BaseViewModel` 里 `db = AppDatabase.getInstance(application)` + `userId = sessionManager.getLocalUserId()`
- ViewModel 不应直接访问数据库，应该通过 Repository
- 🔧 建议：BaseViewModel 只提供 userId，db 由各 Repository 管理

### 3. ⚠️ Repository 层部分空壳
- `TaskRepository` 只是简单包装 DAO，没有业务逻辑
- `UserRepository` 方法太少（缺 addXp、spendCoins 等）
- 🔧 建议：把 ViewModel 中的数据库操作移到 Repository

---

## 🐛 二、Bug 和逻辑问题

### 4. 🔴 MainActivity.ensureLocalUser() 主线程阻塞
- 代码用 `Thread.join()` 在主线程等待数据库初始化，会 ANR
- 🔧 建议：改用 `LiveData` 观察 或 协程 `await`

### 5. 🔴 深色模式切换不持久化
- `SettingsFragment` 只调 `AppCompatDelegate.setDefaultNightMode()`，不保存到 SharedPreferences
- 重启 APP 后深色模式丢失
- 🔧 建议：切换时同步写入 SharedPreferences，启动时读取恢复

### 6. 🔴 ThemeManager 硬编码 themeId - 9
- `getPrimaryColor(long themeId)` 用 `themeId - 9` 做数组索引
- 如果商店数据顺序变了就全错，非常脆弱
- 🔧 建议：主题色存在 ShopItemEntity 里，或用 Map<Long, String[]> 映射

### 7. ⚠️ TaskRepository.insertTask() 在主线程
- `insertTask()` 直接调 `taskDao.insert(task)`，Room 默认不允许主线程操作
- 🔧 建议：所有写操作都用 `executor.execute()`

### 8. ⚠️ HomeViewModel.doComplete() 重复逻辑
- `doComplete()` 手动更新 coins/xp，但 `RewardRepository` 已有 `awardTaskCompletionSync()`
- 两套逻辑可能不一致
- 🔧 建议：统一走 RewardRepository

### 9. ⚠️ FocusViewModel 用 observeForever
- `observeForever` 不会随 Lifecycle 自动移除，**内存泄漏风险**
- 🔧 建议：在 `onCleared()` 中移除 observer（代码已有部分清理，确认覆盖所有）

### 10. ⚠️ ShopViewModel 用 `new Handler(Looper.getMainLooper())`
- 现代 Android 推荐用 `Executor` 或 `LiveData.postValue()`
- 🔧 建议：移除 Handler，用 `postValue` 代替

---

## 🔒 三、安全问题

### 11. 🔴 build.gradle 签名密钥明文暴露
```
storePassword 'kibo123'
keyPassword 'kibo123'
```
- 🔧 建议：从环境变量或 `local.properties` 读取，不要提交到 Git

### 12. ⚠️ allowBackup = true
- AndroidManifest 里 `android:allowBackup="true"`，用户数据可被 adb backup 提取
- 🔧 建议：设为 `false`

### 13. ⚠️ 无 ProGuard/R8 混淆
- `minifyEnabled false`，release 版代码完全可反编译
- 🔧 建议：开启 `minifyEnabled true` 并配置 ProGuard 规则

---

## 📱 四、UI/UX 问题

### 14. ⚠️ 缺少加载状态/空状态动画
- 列表加载时无 shimmer/skeleton 效果
- 🔧 建议：添加 shimmer 占位动画

### 15. ⚠️ 番茄钟计时器退出不恢复
- 用户切走 APP 再回来，计时器状态可能丢失
- 🔧 建议：用 `SavedStateHandle` 保存计时器状态，或用 Foreground Service

### 16. ⚠️ 没有任务拖拽排序
- 任务列表只按 startTime 排序，用户无法手动调整
- 🔧 建议：用 `ItemTouchHelper` 实现拖拽

### 17. ⚠️ 无数据导出功能
- 设置页有"数据导出"文字但没实现
- 🔧 建议：实现 JSON/CSV 导出，或 Room 数据库备份

---

## ⚡ 五、性能问题

### 18. 🔴 AppDatabase 版本9 + fallbackToDestructiveMigration
- 每次 schema 变更都**删库重建**，用户数据全丢！
- 🔧 建议：写 Migration 或至少用 `fallbackToDestructiveMigrationFrom()` 限定版本

### 19. ⚠️ 线程池用 Executors.newFixedThreadPool(4)
- 固定4线程，IO 密集型任务可能不够
- 🔧 建议：用 `Executors.newCachedThreadPool()` 或 Kotlin 协程 `Dispatchers.IO`

### 20. ⚠️ unlockedAvatars/unlockedThemes 用 CSV 字符串存储
- `"1,3,5"` 这种格式查询效率低，还容易出 bug
- 🔧 建议：用关联表（多对多）存储，或至少用 JSON

---

## 📦 六、依赖和配置

### 21. ⚠️ 没用 Kotlin 协程
- 所有异步操作用手动 `ExecutorService`，代码冗余
- 🔧 建议：迁移到 Kotlin + 协程，`suspend` 函数，代码量减半

### 22. ⚠️ 缺少关键依赖
- 没有 Glide/Coil（图片加载）
- 没有 Lottie（动画）
- 没有 Room-Paging（分页）
- 🔧 建议：按需引入

### 23. ⚠️ 没有单元测试
- `testImplementation` 只有 JUnit，没写任何测试
- 🔧 建议：至少给 RewardCalculator、DateUtils 写单元测试

---

## 🎯 七、优先级排序

| 优先级 | # | 问题 | 原因 |
|--------|---|------|------|
| 🔴 P0 | 4 | 主线程阻塞 | 会 ANR 崩溃 |
| 🔴 P0 | 11 | 密钥明文 | 安全隐患 |
| 🔴 P0 | 18 | 破坏性迁移 | 用户丢数据 |
| 🔴 P1 | 5 | 深色模式丢失 | 基本功能 bug |
| 🔴 P1 | 7 | 主线程写DB | 可能崩溃 |
| 🔴 P1 | 12 | allowBackup | 数据可被提取 |
| 🟡 P2 | 6 | ThemeManager脆弱 | 维护隐患 |
| 🟡 P2 | 9 | observeForever泄漏 | 长期运行内存泄漏 |
| 🟡 P2 | 8 | 奖励逻辑重复 | 代码一致性 |
| 🟢 P3 | - | 其余 | 优化和体验提升 |

---

## ⭐ 亮点

- ✅ MVVM + Repository 分层方向正确
- ✅ ViewBinding 用了，没用 deprecated 的 `findViewById`
- ✅ Room + LiveData 响应式数据流
- ✅ 遗忘曲线功能很有创意
- ✅ 积分/等级/商店游戏化系统完整
- ✅ EdgeToEdge 适配做了
- ✅ strings.xml 国际化基础有了

---

*审查完毕，建议优先修复 P0 的三个问题：主线程阻塞、密钥明文、破坏性数据库迁移。*
