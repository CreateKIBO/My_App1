package com.example.myapplication.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DateUtils {

    public static final String DATE_FORMAT_STR = "yyyy-MM-dd";

    public static String getTodayString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
    }

    public static String formatDate(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(millis);
    }

    public static String formatDateDisplay(String isoDate) {
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat output = new SimpleDateFormat("M月d日", Locale.getDefault());
            return output.format(input.parse(isoDate));
        } catch (Exception e) {
            return isoDate;
        }
    }

    public static String getWeekday(String isoDate) {
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat output = new SimpleDateFormat("EEEE", Locale.getDefault());
            return output.format(input.parse(isoDate));
        } catch (Exception e) {
            return "";
        }
    }

    public static String getYesterdayString() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        return new SimpleDateFormat(DATE_FORMAT_STR, Locale.getDefault()).format(cal.getTime());
    }

    public static String minutesToTime(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", h, m);
    }

    public static int timeToMinutes(int hour, int minute) {
        return hour * 60 + minute;
    }

    public static String getDisplayDate(String isoDate) {
        return formatDateDisplay(isoDate);
    }
}
