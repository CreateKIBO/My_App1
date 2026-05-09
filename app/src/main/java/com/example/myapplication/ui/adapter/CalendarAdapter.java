package com.example.myapplication.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.util.DateUtils;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {

    private final List<CalendarDay> days = new ArrayList<>();
    private final Set<String> datesWithTasks = new HashSet<>();
    private String selectedDate;
    private String todayString;
    private final OnDateClickListener listener;
    private final int colorPrimary;
    private final int colorOnPrimary;
    private final int colorPrimaryContainer;
    private final int colorOnPrimaryContainer;
    private final int colorOnSurface;

    public interface OnDateClickListener {
        void onDateClick(String date);
    }

    public CalendarAdapter(OnDateClickListener listener, Context context) {
        this.listener = listener;
        this.todayString = DateUtils.getTodayString();
        this.colorPrimary = context.getColor(R.color.md_primary);
        this.colorOnPrimary = context.getColor(R.color.md_on_primary);
        this.colorPrimaryContainer = context.getColor(R.color.md_primary_container);
        this.colorOnPrimaryContainer = context.getColor(R.color.md_on_primary_container);
        this.colorOnSurface = context.getColor(R.color.md_on_surface);
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
        private final View dotIndicator;
        private final MaterialCardView cardDay;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tv_day);
            dotIndicator = itemView.findViewById(R.id.dot_indicator);
            cardDay = itemView.findViewById(R.id.card_day);
        }

        public void bind(CalendarDay day) {
            if (day.dayOfMonth == 0) {
                tvDay.setText("");
                dotIndicator.setVisibility(View.GONE);
                cardDay.setClickable(false);
                cardDay.setCardBackgroundColor(Color.TRANSPARENT);
                return;
            }

            tvDay.setText(String.valueOf(day.dayOfMonth));

            boolean hasTasks = datesWithTasks.contains(day.dateStr);
            dotIndicator.setVisibility(hasTasks ? View.VISIBLE : View.GONE);

            boolean isSelected = day.dateStr.equals(selectedDate);
            boolean isToday = day.dateStr.equals(todayString);

            if (isSelected) {
                cardDay.setCardBackgroundColor(colorPrimary);
                tvDay.setTextColor(colorOnPrimary);
            } else if (isToday) {
                cardDay.setCardBackgroundColor(colorPrimaryContainer);
                tvDay.setTextColor(colorOnPrimaryContainer);
            } else {
                cardDay.setCardBackgroundColor(Color.TRANSPARENT);
                tvDay.setTextColor(colorOnSurface);
            }

            cardDay.setClickable(true);
            cardDay.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDateClick(day.dateStr);
                }
            });
        }
    }

    public static class CalendarDay {
        public final int dayOfMonth;
        public final String dateStr;

        public CalendarDay(int dayOfMonth, String dateStr) {
            this.dayOfMonth = dayOfMonth;
            this.dateStr = dateStr;
        }

        public static CalendarDay empty() {
            return new CalendarDay(0, "");
        }
    }

    public static List<CalendarDay> generateDaysForMonth(int year, int month) {
        List<CalendarDay> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1);

        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0 = Sunday
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        SimpleDateFormat sdf = new SimpleDateFormat(DateUtils.DATE_FORMAT_STR, Locale.getDefault());

        // Empty cells for days before the 1st
        for (int i = 0; i < firstDayOfWeek; i++) {
            result.add(CalendarDay.empty());
        }

        // Days of the month
        for (int day = 1; day <= daysInMonth; day++) {
            cal.set(year, month, day);
            String dateStr = sdf.format(cal.getTime());
            result.add(new CalendarDay(day, dateStr));
        }

        return result;
    }
}