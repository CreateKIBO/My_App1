package com.example.myapplication.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.util.DateUtils;
import com.example.myapplication.util.RewardCalculator;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.text.SimpleDateFormat;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {

    private final List<CalendarDay> days = new ArrayList<>();
    private final Set<String> datesWithTasks = new HashSet<>();
    private String selectedDate;
    private String todayString;
    private final OnDateClickListener listener;
    private final int colorPrimary;
    private final int colorOnPrimary;
    private final int colorOnSurface;
    private final int colorOnSurfaceVariant;
    private final int colorStreakOrange;
    private final int colorCoinGold;

    public interface OnDateClickListener {
        void onDateClick(String date);
    }

    public CalendarAdapter(OnDateClickListener listener, Context context) {
        this.listener = listener;
        this.todayString = DateUtils.getTodayString();
        this.colorPrimary = context.getColor(R.color.md_primary);
        this.colorOnPrimary = context.getColor(R.color.md_on_primary);
        this.colorOnSurface = context.getColor(R.color.md_on_surface);
        this.colorOnSurfaceVariant = context.getColor(R.color.md_on_surface_variant);
        this.colorStreakOrange = context.getColor(R.color.streak_orange);
        this.colorCoinGold = context.getColor(R.color.coin_gold);
    }

    public void setDays(List<CalendarDay> days) {
        this.days.clear();
        this.days.addAll(days);
        notifyDataSetChanged();
    }

    public void setDatesWithTasks(Set<String> dates) {
        datesWithTasks.clear();
        if (dates != null) datesWithTasks.addAll(dates);
        notifyDataSetChanged();
    }

    public void setSelectedDate(String date) {
        this.selectedDate = date;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        CalendarDay day = days.get(position);
        holder.bind(day);
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    class DayViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvDay;
        private final MaterialCardView cardDay;
        private final LinearLayout dotsRow;
        private final View dotWork;
        private final View dotStudy;
        private final View dotExercise;
        private final View dotPersonal;
        private final View streakDot;
        private final TextView tvStar;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tv_day);
            cardDay = itemView.findViewById(R.id.card_day);
            dotsRow = itemView.findViewById(R.id.dots_row);
            dotWork = itemView.findViewById(R.id.dot_work);
            dotStudy = itemView.findViewById(R.id.dot_study);
            dotExercise = itemView.findViewById(R.id.dot_exercise);
            dotPersonal = itemView.findViewById(R.id.dot_personal);
            streakDot = itemView.findViewById(R.id.streak_dot);
            tvStar = itemView.findViewById(R.id.tv_star);
        }

        public void bind(CalendarDay day) {
            if (day.dayOfMonth == 0) {
                // Empty cell for padding
                tvDay.setText("");
                dotsRow.setVisibility(View.GONE);
                streakDot.setVisibility(View.GONE);
                tvStar.setVisibility(View.GONE);
                cardDay.setClickable(false);
                cardDay.setCardBackgroundColor(Color.TRANSPARENT);
                cardDay.setStrokeWidth(0);
                tvDay.setTextColor(colorOnSurface);
                tvDay.setAlpha(0.18f);
                return;
            }

            tvDay.setText(String.valueOf(day.dayOfMonth));
            tvDay.setAlpha(1f);

            boolean isSelected = day.dateStr.equals(selectedDate);
            boolean isToday = day.dateStr.equals(todayString);
            boolean hasTasks = datesWithTasks.contains(day.dateStr);

            // Show category dots if tasks exist
            if (hasTasks && day.taskCategories != null && !day.taskCategories.isEmpty()) {
                dotsRow.setVisibility(View.VISIBLE);
                boolean hasWork = day.taskCategories.contains(RewardCalculator.CAT_WORK);
                boolean hasStudy = day.taskCategories.contains(RewardCalculator.CAT_STUDY);
                boolean hasExercise = day.taskCategories.contains(RewardCalculator.CAT_EXERCISE);
                boolean hasPersonal = day.taskCategories.contains(RewardCalculator.CAT_PERSONAL);

                dotWork.setVisibility(hasWork ? View.VISIBLE : View.GONE);
                dotStudy.setVisibility(hasStudy ? View.VISIBLE : View.GONE);
                dotExercise.setVisibility(hasExercise ? View.VISIBLE : View.GONE);
                dotPersonal.setVisibility(hasPersonal ? View.VISIBLE : View.GONE);

                // Update dot colors based on selection state
                if (isSelected) {
                    int dotColor = Color.parseColor("#A6FFFFFF"); // semi-transparent white
                    setDotColor(dotWork, dotColor);
                    setDotColor(dotStudy, dotColor);
                    setDotColor(dotExercise, dotColor);
                    setDotColor(dotPersonal, dotColor);
                } else {
                    resetDotColor(dotWork, R.color.category_work);
                    resetDotColor(dotStudy, R.color.category_study);
                    resetDotColor(dotExercise, R.color.category_exercise);
                    resetDotColor(dotPersonal, R.color.category_personal);
                }
            } else if (hasTasks) {
                // Fallback: show a single primary dot if no category info
                dotsRow.setVisibility(View.VISIBLE);
                dotWork.setVisibility(View.VISIBLE);
                dotStudy.setVisibility(View.GONE);
                dotExercise.setVisibility(View.GONE);
                dotPersonal.setVisibility(View.GONE);
                if (isSelected) {
                    setDotColor(dotWork, Color.parseColor("#A6FFFFFF"));
                } else {
                    resetDotColor(dotWork, R.color.md_primary);
                }
            } else {
                dotsRow.setVisibility(View.GONE);
            }

            // Streak dot
            if (day.isStreak) {
                streakDot.setVisibility(View.VISIBLE);
                if (isSelected) {
                    GradientDrawable dotDrawable = (GradientDrawable) streakDot.getBackground();
                    dotDrawable.setColor(Color.parseColor("#99FFFFFF")); // semi-transparent white
                } else {
                    GradientDrawable dotDrawable = (GradientDrawable) streakDot.getBackground();
                    dotDrawable.setColor(colorStreakOrange);
                }
            } else {
                streakDot.setVisibility(View.GONE);
            }

            // All-done star
            if (day.isAllDone) {
                tvStar.setVisibility(View.VISIBLE);
                tvStar.setTextColor(isSelected ? Color.parseColor("#B3FFFFFF") : colorCoinGold);
            } else {
                tvStar.setVisibility(View.GONE);
            }

            // Selection and today styling
            if (isSelected) {
                // Selected date: filled accent circle with white text
                cardDay.setCardBackgroundColor(colorPrimary);
                cardDay.setStrokeWidth(0);
                tvDay.setTextColor(colorOnPrimary);
                tvDay.setTypeface(tvDay.getTypeface(), android.graphics.Typeface.BOLD);
            } else if (isToday) {
                // Today: accent ring outline with accent text
                cardDay.setCardBackgroundColor(Color.TRANSPARENT);
                cardDay.setStrokeColor(colorPrimary);
                cardDay.setStrokeWidth(2);
                tvDay.setTextColor(colorPrimary);
                tvDay.setTypeface(tvDay.getTypeface(), android.graphics.Typeface.BOLD);
            } else {
                // Normal day
                cardDay.setCardBackgroundColor(Color.TRANSPARENT);
                cardDay.setStrokeWidth(0);
                tvDay.setTextColor(colorOnSurface);
                tvDay.setTypeface(null, android.graphics.Typeface.NORMAL);
            }

            // Other-month dates: dim them
            if (day.isOtherMonth) {
                tvDay.setAlpha(0.18f);
                dotsRow.setVisibility(View.GONE);
                streakDot.setVisibility(View.GONE);
                tvStar.setVisibility(View.GONE);
                cardDay.setClickable(false);
            } else {
                cardDay.setClickable(true);
                cardDay.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDateClick(day.dateStr);
                    }
                });
            }
        }

        private void setDotColor(View dot, int color) {
            if (dot.getBackground() instanceof GradientDrawable) {
                ((GradientDrawable) dot.getBackground()).setColor(color);
            }
        }

        private void resetDotColor(View dot, int colorResId) {
            if (dot.getBackground() instanceof GradientDrawable) {
                ((GradientDrawable) dot.getBackground())
                        .setColor(itemView.getContext().getColor(colorResId));
            }
        }
    }

    public static class CalendarDay {
        public final int dayOfMonth;
        public final String dateStr;
        public final boolean isOtherMonth;
        public final boolean isStreak;
        public final boolean isAllDone;
        public final Set<String> taskCategories;

        public CalendarDay(int dayOfMonth, String dateStr, boolean isOtherMonth,
                          boolean isStreak, boolean isAllDone, Set<String> taskCategories) {
            this.dayOfMonth = dayOfMonth;
            this.dateStr = dateStr;
            this.isOtherMonth = isOtherMonth;
            this.isStreak = isStreak;
            this.isAllDone = isAllDone;
            this.taskCategories = taskCategories != null ? taskCategories : new HashSet<>();
        }

        public static CalendarDay empty() {
            return new CalendarDay(0, "", true, false, false, null);
        }

        public static CalendarDay otherMonth(int dayOfMonth, String dateStr) {
            return new CalendarDay(dayOfMonth, dateStr, true, false, false, null);
        }

        public static CalendarDay currentMonth(int dayOfMonth, String dateStr) {
            return new CalendarDay(dayOfMonth, dateStr, false, false, false, null);
        }

        public static CalendarDay currentMonth(int dayOfMonth, String dateStr,
                                                boolean isStreak, boolean isAllDone,
                                                Set<String> taskCategories) {
            return new CalendarDay(dayOfMonth, dateStr, false, isStreak, isAllDone, taskCategories);
        }
    }

    /**
     * Generate the list of CalendarDay objects for a given month,
     * including previous/next month padding days.
     */
    public static List<CalendarDay> generateDaysForMonth(int year, int month) {
        List<CalendarDay> result = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat(DateUtils.DATE_FORMAT_STR, Locale.getDefault());

        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0 = Sunday
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Previous month trailing days
        Calendar prevCal = (Calendar) cal.clone();
        prevCal.add(Calendar.MONTH, -1);
        int prevMonthDays = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = firstDayOfWeek - 1; i >= 0; i--) {
            int day = prevMonthDays - i;
            prevCal.set(Calendar.DAY_OF_MONTH, day);
            String dateStr = sdf.format(prevCal.getTime());
            result.add(CalendarDay.otherMonth(day, dateStr));
        }

        // Current month days
        for (int day = 1; day <= daysInMonth; day++) {
            cal.set(Calendar.DAY_OF_MONTH, day);
            String dateStr = sdf.format(cal.getTime());
            result.add(CalendarDay.currentMonth(day, dateStr));
        }

        // Next month leading days to fill the grid to a multiple of 7
        int totalCells = firstDayOfWeek + daysInMonth;
        int remaining = (7 - totalCells % 7) % 7;
        Calendar nextCal = (Calendar) cal.clone();
        nextCal.add(Calendar.MONTH, 1);
        for (int i = 1; i <= remaining; i++) {
            nextCal.set(Calendar.DAY_OF_MONTH, i);
            String dateStr = sdf.format(nextCal.getTime());
            result.add(CalendarDay.otherMonth(i, dateStr));
        }

        return result;
    }
}
