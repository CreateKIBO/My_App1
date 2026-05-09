package com.example.myapplication.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DateUtils {

    public static final String DATE_FORMAT_STR = "yyyy-MM-dd";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_STR, Locale.getDefault());
    private static final SimpleDateFormat DATE_DISPLAY_FORMAT = new SimpleDateFormat("M月d日 EEEE", Locale.CHINESE);

    public static String getTodayString() {
        return DATE_FORMAT.format(new Date());
    }

    public static String getYesterdayString() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        return DATE_FORMAT.format(cal.getTime());
    }

    public static String formatDate(Date date) {
        return DATE_FORMAT.format(date);
    }

    public static String formatDateDisplay(String dateStr) {
        try {
            Date date = DATE_FORMAT.parse(dateStr);
            return date != null ? DATE_DISPLAY_FORMAT.format(date) : dateStr;
        } catch (Exception e) {
            return dateStr;
        }
    }

    public static String getDisplayDate(String dateStr) {
        return formatDateDisplay(dateStr);
    }

    public static String minutesToTime(int minutes) {
        int hour = minutes / 60;
        int minute = minutes % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
    }

    public static int timeToMinutes(int hour, int minute) {
        return hour * 60 + minute;
    }

    public static String getRelativeTimeSpan(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < TimeUnit.MINUTES.toMillis(1)) return "刚刚";
        if (diff < TimeUnit.HOURS.toMillis(1)) return TimeUnit.MILLISECONDS.toMinutes(diff) + "分钟前";
        if (diff < TimeUnit.DAYS.toMillis(1)) return TimeUnit.MILLISECONDS.toHours(diff) + "小时前";
        if (diff < TimeUnit.DAYS.toMillis(7)) return TimeUnit.MILLISECONDS.toDays(diff) + "天前";
        return DATE_FORMAT.format(new Date(timestamp));
    }
}
