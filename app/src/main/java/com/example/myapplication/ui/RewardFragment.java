package com.example.myapplication.ui;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.RewardTransactionEntity;
import com.example.myapplication.data.local.UserEntity;
import com.example.myapplication.databinding.FragmentRewardBinding;
import com.example.myapplication.util.RewardCalculator;
import com.example.myapplication.util.SessionManager;
import com.example.myapplication.util.UiUtils;
import com.example.myapplication.util.ThemeManager;
import com.example.myapplication.util.AnimUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RewardFragment extends Fragment {

    private FragmentRewardBinding binding;
    private long userId;
    private String currentFilter = "all";

    // Level name table
    private static final String[] LEVEL_NAMES = {
            "初心者", "初心者", "初心者", "初心者", "初心者",
            "初心者", "初心者", "初心者", "初心者", "初心者",
            "坚持者", "专注行者", "专注大师", "自律王者",
            "传奇勇者", "不朽之星", "至高守护"
    };

    // Source category label map
    private static final String getCategoryLabel(String cat) {
        if (cat == null) return "其他";
        switch (cat) {
            case "Work": return "工作";
            case "Study": return "学习";
            case "Exercise": return "运动";
            case "Personal": return "个人";
            default: return "其他";
        }
    }

    // Source category color resource map
    private static int getCategoryColorRes(String cat) {
        if (cat == null) return R.color.md_on_surface_variant;
        switch (cat) {
            case "Work": return R.color.source_work;
            case "Study": return R.color.source_study;
            case "Exercise": return R.color.source_exercise;
            case "Personal": return R.color.source_personal;
            default: return R.color.md_on_surface_variant;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRewardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userId = new SessionManager(requireContext()).getLocalUserId();

        applyThemeColors();
        setupObservers();
        setupCategoryTabs();

        // Celebratory entrance: pop for hero card, slide for list
        AnimUtils.celebratoryPop(binding.heroCard);
        AnimUtils.slideUpFadeIn(binding.rvRewards, 200L);
    }

    private void applyThemeColors() {
        if (getContext() == null) return;
        int primary = ThemeManager.getThemePrimaryInt(requireContext());
        int primaryDark = ThemeManager.getThemePrimaryDarkInt(requireContext());

        // Hero card: dark gradient matching HTML oklch(25%/20%)
        GradientDrawable heroGradient = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{primaryDark, darkenColor(primaryDark, 0.85f)}
        );
        heroGradient.setCornerRadius(UiUtils.dpToPx(requireContext(), 20));
        binding.heroCard.setBackground(heroGradient);

        // Level badge: accent color circle
        GradientDrawable badgeDrawable = new GradientDrawable();
        badgeDrawable.setShape(GradientDrawable.OVAL);
        badgeDrawable.setColor(primary);
        badgeDrawable.setSize(UiUtils.dpToPxInt(requireContext(), 52), UiUtils.dpToPxInt(requireContext(), 52));
        binding.tvHeroLevel.setBackground(badgeDrawable);
    }

    private int darkenColor(int color, float factor) {
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private void setupObservers() {
        AppDatabase db = AppDatabase.getInstance(requireContext());
        db.userDao().observeUser(userId).observe(getViewLifecycleOwner(), user -> {
            if (user == null) return;
            updateHeroCard(user);
            updateStats(user);
            applyThemeColors();
            renderMilestones(user);
        });

        db.rewardTransactionDao().getRecentForUser(userId, 50).observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null) {
                renderRewardList(transactions);
            }
        });
    }

    // ═══════════════════════════════════════════════
    // Hero card
    // ═══════════════════════════════════════════════

    private void updateHeroCard(UserEntity user) {
        int level = RewardCalculator.calculateLevel(user.getTotalXp());
        binding.tvHeroLevel.setText(String.valueOf(level));
        binding.tvHeroTitle.setText(getLevelName(level));
        binding.tvHeroSub.setText(String.format(Locale.CHINESE, "Lv.%d · 累计 %,d XP", level, user.getTotalXp()));

        int xpProgress = RewardCalculator.getXpProgressInLevel(user.getTotalXp());
        int xpNeeded = RewardCalculator.getXpForNextLevel(level);
        binding.tvXpRange.setText(String.format(Locale.CHINESE, "%,d / %,d XP", user.getTotalXp(), xpNeeded));

        int pct = xpNeeded > 0 ? (xpProgress * 100 / xpNeeded) : 0;
        binding.tvXpPercent.setText(pct + "%");

        binding.tvXpNext.setText(String.format(Locale.CHINESE, "距离「%s」还需 %d XP",
                getLevelName(level + 1), xpNeeded - xpProgress));

        // Update XP fill bar using weight system
        if (xpNeeded > 0) {
            LinearLayout.LayoutParams fillParams =
                    (LinearLayout.LayoutParams) binding.viewXpFill.getLayoutParams();
            fillParams.weight = xpProgress;
            binding.viewXpFill.setLayoutParams(fillParams);
        }
    }

    // ═══════════════════════════════════════════════
    // Stats row with change indicators
    // ═══════════════════════════════════════════════

    private void updateStats(UserEntity user) {
        binding.tvStatCoins.setText(String.format(Locale.CHINESE, "%,d", user.getCurrentCoins()));
        binding.tvStatXp.setText(String.format(Locale.CHINESE, "%,d", user.getTotalXp()));
        binding.tvStatStreak.setText(String.valueOf(user.getCurrentStreak()));

        // Streak change: show longest streak
        binding.tvStatStreakChange.setText(String.format(Locale.CHINESE, "最长 %d 天", user.getLongestStreak()));

        // Weekly coins & daily XP change indicators (computed from DB)
        AppDatabase db = AppDatabase.getInstance(requireContext());
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (getContext() == null) return;

            long oneWeekAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000;
            long todayStart = getTodayStartMillis();

            int coinsThisWeek = db.rewardTransactionDao()
                    .getSumByTypeSince(userId, RewardCalculator.TX_COIN, oneWeekAgo);
            int xpToday = db.rewardTransactionDao()
                    .getSumByTypeSince(userId, RewardCalculator.TX_XP, todayStart);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding == null) return;
                    binding.tvStatCoinsChange.setText(String.format(Locale.CHINESE, "↑ +%,d 本周", coinsThisWeek));
                    binding.tvStatXpChange.setText(String.format(Locale.CHINESE, "↑ +%,d 今日", xpToday));
                });
            }
        });
    }

    private long getTodayStartMillis() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    // ═══════════════════════════════════════════════
    // Category tabs (pill-style TextViews)
    // ═══════════════════════════════════════════════

    private void setupCategoryTabs() {
        binding.chipAll.setOnClickListener(v -> setFilter("all"));
        binding.chipXp.setOnClickListener(v -> setFilter("xp"));
        binding.chipCoin.setOnClickListener(v -> setFilter("coin"));
        binding.chipLevel.setOnClickListener(v -> setFilter("level"));
        binding.chipBonus.setOnClickListener(v -> setFilter("bonus"));
        setFilter("all");
    }

    private void setFilter(String type) {
        currentFilter = type;
        TextView[] tabs = {binding.chipAll, binding.chipXp, binding.chipCoin, binding.chipLevel, binding.chipBonus};
        String[] types = {"all", "xp", "coin", "level", "bonus"};

        for (int i = 0; i < tabs.length; i++) {
            TextView tab = tabs[i];
            if (types[i].equals(type)) {
                // Active: dark bg + white text + rounded pill
                tab.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.pill_active_bg));
                tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                GradientDrawable activeBg = new GradientDrawable();
                activeBg.setCornerRadius(dpToPx(9999));
                activeBg.setColor(ContextCompat.getColor(requireContext(), R.color.pill_active_bg));
                tab.setBackground(activeBg);
            } else {
                // Inactive: white bg + border + muted text
                GradientDrawable inactiveBg = new GradientDrawable();
                inactiveBg.setCornerRadius(dpToPx(9999));
                inactiveBg.setColor(ContextCompat.getColor(requireContext(), R.color.pill_inactive_bg));
                inactiveBg.setStroke((int) dpToPx(1.5f),
                        ContextCompat.getColor(requireContext(), R.color.pill_inactive_border));
                tab.setBackground(inactiveBg);
                tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_on_surface_variant));
            }
        }

        // Re-render reward list with new filter
        AppDatabase db = AppDatabase.getInstance(requireContext());
        db.rewardTransactionDao().getRecentForUser(userId, 50).observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null) renderRewardList(transactions);
        });
    }

    // ═══════════════════════════════════════════════
    // Reward list (dynamically built LinearLayout)
    // ═══════════════════════════════════════════════

    private void renderRewardList(List<RewardTransactionEntity> transactions) {
        LinearLayout container = binding.rvRewards;
        container.removeAllViews();

        SimpleDateFormat sdf = new SimpleDateFormat("M月d日", Locale.CHINESE);
        SimpleDateFormat timeSdf = new SimpleDateFormat("今天 HH:mm", Locale.CHINESE);

        AppDatabase db = AppDatabase.getInstance(requireContext());

        for (RewardTransactionEntity tx : transactions) {
            if (!currentFilter.equals("all") && !currentFilter.equals(getFilterType(tx.getType()))) continue;

            View itemView = LayoutInflater.from(requireContext()).inflate(R.layout.item_reward, container, false);

            TextView tvName = itemView.findViewById(R.id.tv_reward_name);
            TextView tvDesc = itemView.findViewById(R.id.tv_reward_desc);
            TextView tvAmount = itemView.findViewById(R.id.tv_reward_amount);
            TextView tvTime = itemView.findViewById(R.id.tv_reward_time);
            View typeBar = itemView.findViewById(R.id.view_type_bar);
            TextView iconBg = itemView.findViewById(R.id.tv_reward_icon);
            View sourceDot = itemView.findViewById(R.id.view_source_dot);
            TextView tvSource = itemView.findViewById(R.id.tv_reward_source);

            tvName.setText(getRewardName(tx));
            tvDesc.setText(tx.getReason());

            // Determine type-specific styling
            String amountStr;
            String iconStr;
            int barColor;
            int iconBgDrawable;
            int amountColor;

            switch (tx.getType()) {
                case RewardCalculator.TX_XP:
                    amountStr = "+" + tx.getAmount() + " XP";
                    iconStr = "⚡";
                    barColor = R.color.xp_green;
                    iconBgDrawable = R.drawable.bg_reward_icon_xp;
                    amountColor = R.color.xp_green;
                    break;
                case RewardCalculator.TX_COIN:
                    amountStr = "+" + tx.getAmount() + " ¢";
                    iconStr = "¢";
                    barColor = R.color.coin_gold;
                    iconBgDrawable = R.drawable.bg_reward_icon_coin;
                    amountColor = R.color.coin_gold_dark;
                    break;
                case RewardCalculator.TX_LEVEL_UP:
                    amountStr = "+" + tx.getAmount() + " ¢";
                    iconStr = "↑";
                    barColor = R.color.reward_level_blue;
                    iconBgDrawable = R.drawable.bg_reward_icon_level;
                    amountColor = R.color.coin_gold_dark;
                    break;
                case RewardCalculator.TX_SPEND:
                    amountStr = "-" + tx.getAmount() + " ¢";
                    iconStr = "↓";
                    barColor = R.color.md_error;
                    iconBgDrawable = R.drawable.bg_reward_icon_coin;
                    amountColor = R.color.md_error;
                    break;
                default: // bonus / streak
                    amountStr = "+" + tx.getAmount() + " ¢";
                    iconStr = "★";
                    barColor = R.color.streak_orange;
                    iconBgDrawable = R.drawable.bg_reward_icon_bonus;
                    amountColor = R.color.coin_gold_dark;
                    break;
            }

            tvAmount.setText(amountStr);
            tvAmount.setTextColor(ContextCompat.getColor(requireContext(), amountColor));
            iconBg.setText(iconStr);
            iconBg.setBackgroundResource(iconBgDrawable);
            typeBar.setBackgroundColor(ContextCompat.getColor(requireContext(), barColor));

            // Source dot: resolve category from linked task
            resolveSourceCategory(tx, db, sourceDot, tvSource);

            // Time formatting
            long now = System.currentTimeMillis();
            long diff = now - tx.getTimestamp();
            if (diff < 86400000L) {
                tvTime.setText(timeSdf.format(new Date(tx.getTimestamp())));
            } else {
                tvTime.setText(sdf.format(new Date(tx.getTimestamp())));
            }

            container.addView(itemView);
        }

        if (container.getChildCount() == 0) {
            binding.tvNoRewards.setVisibility(View.VISIBLE);
        } else {
            binding.tvNoRewards.setVisibility(View.GONE);
        }
    }

    /**
     * Resolve the source category for a reward transaction.
     * Since RewardTransactionEntity does not have a taskId field, we infer
     * the source from the transaction type and reason text.
     */
    private void resolveSourceCategory(RewardTransactionEntity tx, AppDatabase db,
                                        View sourceDot, TextView tvSource) {
        // For level-up rewards
        if (RewardCalculator.TX_LEVEL_UP.equals(tx.getType())) {
            GradientDrawable dotDrawable = new GradientDrawable();
            dotDrawable.setShape(GradientDrawable.OVAL);
            dotDrawable.setColor(ThemeManager.getThemePrimaryInt(requireContext()));
            sourceDot.setBackground(dotDrawable);
            tvSource.setText("升级");
            return;
        }

        // For spending
        if (RewardCalculator.TX_SPEND.equals(tx.getType())) {
            GradientDrawable dotDrawable = new GradientDrawable();
            dotDrawable.setShape(GradientDrawable.OVAL);
            dotDrawable.setColor(ContextCompat.getColor(requireContext(), R.color.md_error));
            sourceDot.setBackground(dotDrawable);
            tvSource.setText("消费");
            return;
        }

        // For XP and Coin rewards: try to infer category from reason text
        String reason = tx.getReason();
        String category = inferCategoryFromReason(reason);
        GradientDrawable dotDrawable = new GradientDrawable();
        dotDrawable.setShape(GradientDrawable.OVAL);
        dotDrawable.setColor(ContextCompat.getColor(requireContext(), getCategoryColorRes(category)));
        sourceDot.setBackground(dotDrawable);
        tvSource.setText(getCategoryLabel(category));
    }

    /**
     * Infer a task category from the reward reason text.
     * Looks for keywords that map to Work/Study/Exercise/Personal categories.
     */
    private String inferCategoryFromReason(String reason) {
        if (reason == null) return "Other";
        String lower = reason.toLowerCase(Locale.CHINESE);
        if (lower.contains("工作") || lower.contains("work") || lower.contains("项目") || lower.contains("周报")) {
            return "Work";
        }
        if (lower.contains("学习") || lower.contains("study") || lower.contains("阅读") || lower.contains("复习")) {
            return "Study";
        }
        if (lower.contains("运动") || lower.contains("exercise") || lower.contains("健身") || lower.contains("跑步")) {
            return "Exercise";
        }
        if (lower.contains("个人") || lower.contains("personal") || lower.contains("生活") || lower.contains("早起")) {
            return "Personal";
        }
        // Streak / bonus keywords
        if (lower.contains("连续") || lower.contains("streak") || lower.contains("打卡")) {
            return "Streak";
        }
        return "Other";
    }

    // ═══════════════════════════════════════════════
    // Level milestones grid (4-col GridLayout)
    // ═══════════════════════════════════════════════

    private void renderMilestones(UserEntity user) {
        GridLayout grid = binding.gridMilestones;
        grid.removeAllViews();

        int currentLevel = RewardCalculator.calculateLevel(user.getTotalXp());
        int startLevel = Math.max(1, currentLevel - 2);
        int endLevel = Math.min(LEVEL_NAMES.length - 1, currentLevel + 5);

        int accentColor = ThemeManager.getThemePrimaryInt(requireContext());
        int mutedColor = ContextCompat.getColor(requireContext(), R.color.md_on_surface_variant);
        int cellMargin = (int) dpToPx(4);

        for (int lv = startLevel; lv <= endLevel; lv++) {
            boolean reached = lv <= currentLevel;
            boolean isCurrent = lv == currentLevel;

            // Cell container
            LinearLayout cell = new LinearLayout(requireContext());
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);

            GridLayout.LayoutParams cellParams = new GridLayout.LayoutParams();
            cellParams.width = 0;
            cellParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            cellParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            cellParams.setMargins(cellMargin, cellMargin, cellMargin, cellMargin);
            cell.setLayoutParams(cellParams);

            int cellPaddingH = (int) dpToPx(6);
            int cellPaddingV = (int) dpToPx(10);
            cell.setPadding(cellPaddingH, cellPaddingV, cellPaddingH, cellPaddingV);

            // Background: reached/current/future
            GradientDrawable cellBg = new GradientDrawable();
            cellBg.setCornerRadius(dpToPx(12));

            if (isCurrent) {
                cellBg.setColor(ContextCompat.getColor(requireContext(), R.color.milestone_current_bg));
                cellBg.setStroke((int) dpToPx(2), accentColor);
            } else if (reached) {
                cellBg.setColor(ContextCompat.getColor(requireContext(), R.color.card_surface));
                cellBg.setStroke((int) dpToPx(2), accentColor);
            } else {
                cellBg.setColor(ContextCompat.getColor(requireContext(), R.color.card_surface));
                // Future: no border, dimmed
            }
            cell.setBackground(cellBg);

            // Future cells are dimmed
            if (!reached) {
                cell.setAlpha(0.5f);
            }

            // Level number
            TextView tvLv = new TextView(requireContext());
            tvLv.setText(String.valueOf(lv));
            tvLv.setTextSize(16);
            tvLv.setTypeface(null, android.graphics.Typeface.BOLD);
            tvLv.setGravity(Gravity.CENTER);
            tvLv.setTextColor(reached ? accentColor : mutedColor);
            cell.addView(tvLv);

            // Level name
            TextView tvName = new TextView(requireContext());
            tvName.setText(getLevelName(lv));
            tvName.setTextSize(10);
            tvName.setGravity(Gravity.CENTER);
            tvName.setTextColor(mutedColor);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            nameParams.topMargin = (int) dpToPx(2);
            tvName.setLayoutParams(nameParams);
            cell.addView(tvName);

            // Coin reward row: mini coin + amount
            LinearLayout coinRow = new LinearLayout(requireContext());
            coinRow.setOrientation(LinearLayout.HORIZONTAL);
            coinRow.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams coinRowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            coinRowParams.topMargin = (int) dpToPx(4);
            coinRow.setLayoutParams(coinRowParams);

            // Mini coin circle (10dp)
            View miniCoin = new View(requireContext());
            LinearLayout.LayoutParams coinParams = new LinearLayout.LayoutParams(
                    (int) dpToPx(10), (int) dpToPx(10));
            coinParams.setMarginEnd((int) dpToPx(2));
            miniCoin.setLayoutParams(coinParams);
            GradientDrawable coinDrawable = new GradientDrawable();
            coinDrawable.setShape(GradientDrawable.OVAL);
            coinDrawable.setColors(new int[]{
                    ContextCompat.getColor(requireContext(), R.color.coin_gold),
                    ContextCompat.getColor(requireContext(), R.color.coin_gold_dark)
            });
            coinDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
            miniCoin.setBackground(coinDrawable);
            coinRow.addView(miniCoin);

            // Coin amount text
            int coinReward = RewardCalculator.getLevelUpBonusCoins(lv);
            TextView tvCoin = new TextView(requireContext());
            tvCoin.setText(String.valueOf(coinReward));
            tvCoin.setTextSize(10);
            tvCoin.setTypeface(null, android.graphics.Typeface.BOLD);
            tvCoin.setTextColor(ContextCompat.getColor(requireContext(), R.color.coin_gold_dark));
            coinRow.addView(tvCoin);

            cell.addView(coinRow);

            // Check mark for reached levels
            if (reached) {
                TextView tvCheck = new TextView(requireContext());
                tvCheck.setText("✓");
                tvCheck.setTextSize(14);
                tvCheck.setGravity(Gravity.CENTER);
                tvCheck.setTextColor(accentColor);
                LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                checkParams.topMargin = (int) dpToPx(2);
                tvCheck.setLayoutParams(checkParams);
                cell.addView(tvCheck);
            }

            grid.addView(cell);
        }
    }

    // ═══════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════

    private String getLevelName(int level) {
        if (level >= 0 && level < LEVEL_NAMES.length) return LEVEL_NAMES[level];
        return "传奇勇者";
    }

    private String getFilterType(String txType) {
        if (RewardCalculator.TX_XP.equals(txType)) return "xp";
        if (RewardCalculator.TX_COIN.equals(txType)) return "coin";
        if (RewardCalculator.TX_LEVEL_UP.equals(txType)) return "level";
        if (RewardCalculator.TX_SPEND.equals(txType)) return "coin";
        return "bonus";
    }

    private String getRewardName(RewardTransactionEntity tx) {
        switch (tx.getType()) {
            case RewardCalculator.TX_XP: return "完成任务 XP";
            case RewardCalculator.TX_COIN: return "任务金币";
            case RewardCalculator.TX_LEVEL_UP: return "升级奖励";
            case RewardCalculator.TX_SPEND: return "金币消费";
            default: return tx.getReason();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
