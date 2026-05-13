package com.example.myapplication.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.DateCount;
import com.example.myapplication.databinding.FragmentStreakBinding;
import com.example.myapplication.util.SessionManager;
import com.example.myapplication.util.UiUtils;
import com.example.myapplication.util.AnimUtils;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StreakFragment extends Fragment {

    private FragmentStreakBinding binding;
    private AppDatabase db;
    private SessionManager sessionManager;

    // Streak data
    private int currentStreak = 0;
    private int longestStreak = 0;
    private int monthlyCompleted = 0;
    private int freezeCount = 0;
    private Map<String, Integer> monthlyCountMap = new HashMap<>();

    // Milestone definitions (matching HTML: 3, 7, 14, 30, 60)
    private static final int[] MILESTONE_TARGETS = {3, 7, 14, 30, 60};
    private static final String[] MILESTONE_NAMES = {
            "3 天连续", "7 天连续", "14 天连续", "30 天连续", "60 天连续"
    };
    private static final String[] MILESTONE_DESCS = {
            "初露锋芒，连续完成 3 天",
            "一周不间断，习惯正在养成",
            "两周坚持，意志力初见成效",
            "一个月！习惯已深深扎根",
            "两个月，你已是自律的化身"
    };
    private static final int[] MILESTONE_COINS = {50, 150, 400, 1000, 2500};
    private static final int[] MILESTONE_XP = {30, 80, 200, 500, 1200};
    private static final String[] MILESTONE_SPECIAL = {
            null, null, null, "限定头像：凤凰", "限定主题：星河紫"
    };

    // Quotes
    private static final String[] QUOTES = {
            "每天至少完成 1 个任务即可维持连续天数。设置一个简单的晨间任务作为保底！",
            "坚持的力量不在于某一天的爆发，而在于每一天的积累。",
            "习惯的力量：21天可以养成一个新习惯，你已经走在路上了！",
            "不要追求完美，追求连续。哪怕只做一点点，也比什么都不做强。"
    };

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        db = AppDatabase.getInstance(requireContext());
        sessionManager = new SessionManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStreakBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadStreakData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStreakData();
    }

    private void loadStreakData() {
        long userId = sessionManager.getLocalUserId();
        if (userId == -1) return;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                var userDao = db.userDao();
                var user = userDao.getUserById(userId);
                if (user != null) {
                    currentStreak = user.getCurrentStreak();
                    longestStreak = user.getLongestStreak();
                    freezeCount = user.getFreezeCount();
                }

                // Calculate monthly completions: batch query instead of 31 individual queries
                Calendar cal = Calendar.getInstance();
                int thisMonth = cal.get(Calendar.MONTH);
                int thisYear = cal.get(Calendar.YEAR);

                String monthStart = String.format(Locale.getDefault(), "%04d-%02d-01", thisYear, thisMonth + 1);
                cal.set(thisYear, thisMonth, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                String monthEnd = String.format(Locale.getDefault(), "%04d-%02d-%02d", thisYear, thisMonth + 1, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                var taskDao = db.taskDao();
                List<DateCount> counts = taskDao.getCompletedCountsForMonth(userId, monthStart, monthEnd);

                Map<String, Integer> countMap = new HashMap<>();
                for (DateCount dc : counts) {
                    countMap.put(dc.date, dc.count);
                }
                monthlyCountMap = countMap;

                int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                int completedDays = 0;
                for (int d = 1; d <= daysInMonth; d++) {
                    String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", thisYear, thisMonth + 1, d);
                    Integer c = countMap.get(dateStr);
                    if (c != null && c > 0) completedDays++;
                }
                monthlyCompleted = completedDays;

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> updateUI());
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> updateUI());
                }
            }
        });
    }

    private void updateUI() {
        // Streak count
        binding.tvStreakCount.setText(String.valueOf(currentStreak));

        // Streak message
        updateStreakMessage();

        // Ring progress
        updateRingProgress();

        // Stats
        binding.tvStatCurrent.setText(String.valueOf(currentStreak));
        binding.tvStatLongest.setText(String.valueOf(longestStreak));
        binding.tvStatMonthly.setText(String.valueOf(monthlyCompleted));

        // Freeze card count
        binding.tvFreezeCount.setText(freezeCount + " 张冻结卡");
        binding.layoutFreezeCard.setVisibility(freezeCount > 0 ? View.VISIBLE : View.GONE);

        // Heatmap
        buildHeatmap();

        // Milestones
        buildMilestones();

        // Quote
        int quoteIndex = (int) (System.currentTimeMillis() / 86400000) % QUOTES.length;
        binding.tvQuoteText.setText(QUOTES[quoteIndex]);

        // Achievement entrance animations
        AnimUtils.flamePulse(binding.tvStreakCount);
        AnimUtils.progressiveReveal(binding.streakRingView, 100L);
        AnimUtils.progressiveReveal(binding.gridHeatmap, 250L);
        AnimUtils.progressiveReveal(binding.layoutMilestones, 400L);
    }

    private void updateStreakMessage() {
        String msg;
        if (currentStreak == 0) {
            msg = "今天开始你的连续打卡之旅吧！";
        } else {
            int nextMilestone = 0;
            for (int target : MILESTONE_TARGETS) {
                if (currentStreak < target) {
                    nextMilestone = target;
                    break;
                }
            }
            if (nextMilestone > 0) {
                int remaining = nextMilestone - currentStreak;
                msg = "再坚持 " + remaining + " 天即可解锁下一个里程碑！";
            } else {
                msg = "你已经达成所有里程碑，太厉害了！";
            }
        }
        binding.tvStreakMsg.setText(msg);
    }

    private void updateRingProgress() {
        // Progress = currentStreak / nextMilestone
        float progress;
        if (currentStreak == 0) {
            progress = 0f;
        } else {
            int nextMilestone = 0;
            for (int target : MILESTONE_TARGETS) {
                if (currentStreak < target) {
                    nextMilestone = target;
                    break;
                }
            }
            if (nextMilestone > 0) {
                progress = (float) currentStreak / nextMilestone;
            } else {
                progress = 1f; // all milestones achieved
            }
        }
        binding.streakRingView.setProgress(progress);
    }

    // ═══════════════════════════════════════════
    // Heatmap
    // ═══════════════════════════════════════════

    private void buildHeatmap() {
        // Set date range
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1; // 1-based
        binding.tvHeatmapRange.setText(year + " 年 " + month + " 月");

        // Build weekday labels
        buildWeekdayLabels();

        // Build heatmap grid
        buildHeatmapGrid(cal);
    }

    private void buildWeekdayLabels() {
        LinearLayout container = binding.layoutHeatmapWeekdays;
        container.removeAllViews();
        container.setWeightSum(7f);

        String[] dayLabels = {"一", "二", "三", "四", "五", "六", "日"};
        for (String label : dayLabels) {
            TextView tv = new TextView(requireContext());
            tv.setText(label);
            tv.setTextSize(10);
            tv.setTextColor(getColor(R.color.text_secondary));
            tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
            tv.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tv.setLayoutParams(lp);
            container.addView(tv);
        }
    }

    private void buildHeatmapGrid(Calendar cal) {
        GridLayout grid = binding.gridHeatmap;
        grid.removeAllViews();

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int today = cal.get(Calendar.DAY_OF_MONTH);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Calculate start offset: what day of week is the 1st?
        // 0=Mon, 1=Tue, ..., 6=Sun
        Calendar firstDayCal = Calendar.getInstance();
        firstDayCal.set(year, month, 1);
        int firstDayDow = firstDayCal.get(Calendar.DAY_OF_WEEK);
        // Convert Calendar.DAY_OF_WEEK (Sun=1, Mon=2, ..., Sat=7) to Mon=0 index
        int startOffset = (firstDayDow + 5) % 7;

        // Load activity data for the month (already fetched in loadStreakData)
        long userId = sessionManager.getLocalUserId();

        // Calculate cell size: (grid width - 6*gap) / 7
        // We'll use fixed dp sizes
        int cellSize = UiUtils.dpToPxInt(requireContext(),40);
        int gap = UiUtils.dpToPxInt(requireContext(),6);

        grid.setColumnCount(7);
        grid.setUseDefaultMargins(false);

        // Empty cells for offset
        for (int i = 0; i < startOffset; i++) {
            View empty = new View(requireContext());
            GridLayout.LayoutParams emptyLp = new GridLayout.LayoutParams();
            emptyLp.width = cellSize;
            emptyLp.height = cellSize;
            emptyLp.setMargins(gap / 2, gap / 2, gap / 2, gap / 2);
            empty.setLayoutParams(emptyLp);
            empty.setVisibility(View.INVISIBLE);
            grid.addView(empty);
        }

        // Day cells
        for (int d = 1; d <= daysInMonth; d++) {
            String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, d);
            int completedCount = 0;
            Integer c = monthlyCountMap.get(dateStr);
            if (c != null) completedCount = c;

            // Determine level
            int level = 0;
            if (completedCount == 1) level = 1;
            else if (completedCount == 2) level = 2;
            else if (completedCount == 3) level = 3;
            else if (completedCount >= 4) level = 4;

            boolean isToday = (d == today);
            boolean isFuture = (d > today);

            View cell = createHeatmapCell(d, completedCount, level, isToday, isFuture, cellSize);
            GridLayout.LayoutParams cellLp = new GridLayout.LayoutParams();
            cellLp.width = cellSize;
            cellLp.height = cellSize;
            cellLp.setMargins(gap / 2, gap / 2, gap / 2, gap / 2);
            cell.setLayoutParams(cellLp);
            grid.addView(cell);
        }
    }

    private View createHeatmapCell(int day, int count, int level, boolean isToday, boolean isFuture, int cellSize) {
        // Cell is a FrameLayout with rounded bg + centered text
        FrameLayout cell = new FrameLayout(requireContext());

        // Background
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(UiUtils.dpToPxInt(requireContext(),8));

        int bgColor;
        int textColor;
        switch (level) {
            case 1:
                bgColor = getColor(R.color.heatmap_lv1);
                textColor = getColor(R.color.heatmap_lv1_text);
                break;
            case 2:
                bgColor = getColor(R.color.heatmap_lv2);
                textColor = Color.WHITE;
                break;
            case 3:
                bgColor = getColor(R.color.heatmap_lv3);
                textColor = Color.WHITE;
                break;
            case 4:
                bgColor = getColor(R.color.heatmap_lv4);
                textColor = Color.WHITE;
                break;
            default:
                bgColor = getColor(R.color.heatmap_lv0);
                textColor = getColor(R.color.text_secondary);
                break;
        }

        bg.setColor(bgColor);

        // Today: accent inset border
        if (isToday) {
            int accentColor = getColor(R.color.md_primary);
            bg.setStroke(UiUtils.dpToPxInt(requireContext(),2), accentColor);
        }

        if (isFuture) {
            bg.setAlpha(77); // ~0.3 opacity
        }

        cell.setBackground(bg);

        // Inner layout: day number + optional count
        LinearLayout inner = new LinearLayout(requireContext());
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER);
        inner.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        // Day number
        TextView dayTv = new TextView(requireContext());
        dayTv.setText(String.valueOf(day));
        dayTv.setTextSize(10);
        dayTv.setTextColor(isFuture ? getColor(R.color.text_hint) : textColor);
        dayTv.setTypeface(dayTv.getTypeface(), android.graphics.Typeface.BOLD);
        dayTv.setGravity(Gravity.CENTER);
        inner.addView(dayTv);

        // Count label (only if > 0)
        if (count > 0 && !isFuture) {
            TextView countTv = new TextView(requireContext());
            countTv.setText(String.valueOf(count));
            countTv.setTextSize(9);
            countTv.setTextColor(textColor);
            countTv.setAlpha(0.7f);
            countTv.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams countLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            countLp.topMargin = UiUtils.dpToPxInt(requireContext(),1);
            countTv.setLayoutParams(countLp);
            inner.addView(countTv);
        }

        cell.addView(inner);
        return cell;
    }

    // ═══════════════════════════════════════════
    // Milestones (Timeline layout matching HTML)
    // ═══════════════════════════════════════════

    private void buildMilestones() {
        LinearLayout container = binding.layoutMilestones;
        container.removeAllViews();

        for (int i = 0; i < MILESTONE_TARGETS.length; i++) {
            int target = MILESTONE_TARGETS[i];
            boolean achieved = currentStreak >= target;
            boolean isCurrent = false;
            if (!achieved) {
                if (i == 0) {
                    isCurrent = currentStreak > 0;
                } else {
                    isCurrent = currentStreak >= MILESTONE_TARGETS[i - 1];
                }
            }
            boolean locked = !achieved && !isCurrent;

            View milestoneView = createMilestoneItem(i, target, achieved, isCurrent, locked);
            container.addView(milestoneView);
        }
    }

    private View createMilestoneItem(int index, int target, boolean achieved, boolean isCurrent, boolean locked) {
        // Outer layout: dot on left, card on right
        FrameLayout item = new FrameLayout(requireContext());
        item.setPadding(0, 0, 0, index < MILESTONE_TARGETS.length - 1 ? UiUtils.dpToPxInt(requireContext(),24) : 0);

        // ── Dot (positioned to align with timeline line) ──
        // The timeline line is at x=9dp (from layout). The dot is centered on that line.
        // Dot is 16dp wide, so left = 9dp - 8dp = 1dp from the container start,
        // but container has paddingStart=28dp, so dot is at 9dp from the FrameLayout left edge.
        int dotSize = UiUtils.dpToPxInt(requireContext(),16);
        View dot = new View(requireContext());
        FrameLayout.LayoutParams dotLp = new FrameLayout.LayoutParams(dotSize, dotSize);
        // Center dot on the timeline line at x=9dp
        dotLp.leftMargin = UiUtils.dpToPxInt(requireContext(),9) - dotSize / 2;
        dotLp.topMargin = UiUtils.dpToPxInt(requireContext(),4);
        dot.setLayoutParams(dotLp);

        GradientDrawable dotDrawable = new GradientDrawable();
        dotDrawable.setShape(GradientDrawable.OVAL);

        if (achieved) {
            dotDrawable.setColor(getColor(R.color.streak_orange));
            // Outer glow ring
            dotDrawable.setStroke(UiUtils.dpToPxInt(requireContext(),3), getColor(R.color.streak_light));
        } else if (isCurrent) {
            dotDrawable.setColor(getColor(R.color.md_primary));
            dotDrawable.setStroke(UiUtils.dpToPxInt(requireContext(),3), getColor(R.color.stat_blue_light));
        } else {
            dotDrawable.setColor(getColor(R.color.md_outline_variant));
        }
        dot.setBackground(dotDrawable);
        item.addView(dot);

        // ── Card ──
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        cardLp.leftMargin = UiUtils.dpToPxInt(requireContext(),28); // after the dot area
        card.setLayoutParams(cardLp);

        // Card background
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setShape(GradientDrawable.RECTANGLE);
        cardBg.setCornerRadius(UiUtils.dpToPxInt(requireContext(),16));
        cardBg.setColor(getColor(R.color.card_surface));

        // Left border (3dp)
        if (achieved) {
            cardBg.setStroke(UiUtils.dpToPxInt(requireContext(),3), getColor(R.color.streak_orange));
        } else if (isCurrent) {
            cardBg.setStroke(UiUtils.dpToPxInt(requireContext(),3), getColor(R.color.md_primary));
            cardBg.setColor(getColor(R.color.milestone_current_bg));
        }

        if (locked) {
            card.setAlpha(0.55f);
        }

        card.setBackground(cardBg);
        card.setPadding(UiUtils.dpToPxInt(requireContext(),14), UiUtils.dpToPxInt(requireContext(),14), UiUtils.dpToPxInt(requireContext(),16), UiUtils.dpToPxInt(requireContext(),14));

        // ── Top row: name + tag ──
        LinearLayout topRow = new LinearLayout(requireContext());
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView nameTv = new TextView(requireContext());
        nameTv.setText(MILESTONE_NAMES[index]);
        nameTv.setTextSize(15);
        nameTv.setTypeface(nameTv.getTypeface(), android.graphics.Typeface.BOLD);
        nameTv.setLetterSpacing(-0.01f);
        if (achieved) {
            nameTv.setTextColor(getColor(R.color.streak_orange));
        } else if (isCurrent) {
            nameTv.setTextColor(getColor(R.color.md_primary));
        } else {
            nameTv.setTextColor(getColor(R.color.md_on_surface));
        }

        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        nameTv.setLayoutParams(nameLp);
        topRow.addView(nameTv);

        // Tag
        TextView tagTv = new TextView(requireContext());
        tagTv.setTextSize(10);
        tagTv.setTypeface(tagTv.getTypeface(), android.graphics.Typeface.BOLD);
        tagTv.setGravity(Gravity.CENTER);
        int tagPadH = UiUtils.dpToPxInt(requireContext(),8);
        int tagPadV = UiUtils.dpToPxInt(requireContext(),3);
        tagTv.setPadding(tagPadH, tagPadV, tagPadH, tagPadV);

        GradientDrawable tagBg = new GradientDrawable();
        tagBg.setShape(GradientDrawable.RECTANGLE);
        tagBg.setCornerRadius(UiUtils.dpToPxInt(requireContext(),9999));

        if (achieved) {
            tagTv.setText("已达成");
            tagBg.setColor(getColor(R.color.tag_done_bg));
            tagTv.setTextColor(getColor(R.color.tag_done_text));
        } else if (isCurrent) {
            tagTv.setText("进行中");
            tagBg.setColor(getColor(R.color.tag_active_bg));
            tagTv.setTextColor(getColor(R.color.tag_active_text));
        } else {
            tagTv.setText("未解锁");
            tagBg.setColor(getColor(R.color.tag_locked_bg));
            tagTv.setTextColor(getColor(R.color.tag_locked_text));
        }
        tagTv.setBackground(tagBg);
        topRow.addView(tagTv);

        card.addView(topRow);

        // ── Description ──
        TextView descTv = new TextView(requireContext());
        descTv.setText(MILESTONE_DESCS[index]);
        descTv.setTextSize(12);
        descTv.setTextColor(getColor(R.color.text_secondary));
        descTv.setLineSpacing(0f, 1.3f);
        LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        descLp.topMargin = UiUtils.dpToPxInt(requireContext(),4);
        descLp.bottomMargin = UiUtils.dpToPxInt(requireContext(),6);
        descTv.setLayoutParams(descLp);
        card.addView(descTv);

        // ── Reward row ──
        LinearLayout rewardRow = new LinearLayout(requireContext());
        rewardRow.setOrientation(LinearLayout.HORIZONTAL);
        rewardRow.setGravity(Gravity.CENTER_VERTICAL);

        // Coin reward
        TextView coinReward = createRewardItem(
                "¢", String.valueOf(MILESTONE_COINS[index]),
                getColor(R.color.coin_gold_dark), getColor(R.color.coin_gold));
        rewardRow.addView(coinReward);

        // XP reward
        TextView xpReward = createRewardItem(
                "⚡", MILESTONE_XP[index] + " XP",
                getColor(R.color.xp_green), 0);
        LinearLayout.LayoutParams xpLp = (LinearLayout.LayoutParams) xpReward.getLayoutParams();
        xpLp.leftMargin = UiUtils.dpToPxInt(requireContext(),8);
        xpReward.setLayoutParams(xpLp);
        rewardRow.addView(xpReward);

        // Special reward
        if (MILESTONE_SPECIAL[index] != null) {
            TextView specialReward = createRewardItem(
                    "🎁", MILESTONE_SPECIAL[index],
                    getColor(R.color.md_primary), 0);
            LinearLayout.LayoutParams specialLp = (LinearLayout.LayoutParams) specialReward.getLayoutParams();
            specialLp.leftMargin = UiUtils.dpToPxInt(requireContext(),8);
            specialReward.setLayoutParams(specialLp);
            rewardRow.addView(specialReward);
        }

        card.addView(rewardRow);

        item.addView(card);
        return item;
    }

    private TextView createRewardItem(String icon, String text, int textColor, int coinBgColor) {
        TextView tv = new TextView(requireContext());
        tv.setText(icon + " " + text);
        tv.setTextSize(12);
        tv.setTextColor(textColor);
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return tv;
    }

    // ═══════════════════════════════════════════
    // Utilities
    // ═══════════════════════════════════════════

    private int getColor(int colorResId) {
        return requireContext().getColor(colorResId);
    }

    private void updateMotivationalMessage(int streak) {
        String msg;
        if (streak == 0) {
            msg = "完成今天的任务，开始你的连续打卡之旅！";
        } else if (streak < 3) {
            msg = "再坚持 " + (3 - streak) + " 天即可解锁第一个里程碑！";
        } else if (streak < 7) {
            msg = "再坚持 " + (7 - streak) + " 天即可解锁下一个里程碑！";
        } else if (streak < 14) {
            msg = "再坚持 " + (14 - streak) + " 天即可解锁下一个里程碑！";
        } else if (streak < 30) {
            msg = "再坚持 " + (30 - streak) + " 天即可解锁下一个里程碑！";
        } else {
            msg = "太厉害了！你已经连续 " + streak + " 天了！";
        }
        binding.tvStreakMsg.setText(msg);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
