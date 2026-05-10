package com.example.myapplication.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.ShopItemEntity;
import com.example.myapplication.data.local.UserEntity;

/**
 * Maps theme shop item IDs to color values for dynamic theme switching.
 * Theme IDs are auto-generated: themes start at ID 9 (after 8 avatars).
 */
public class ThemeManager {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_PRIMARY = "theme_primary";
    private static final String KEY_PRIMARY_CONTAINER = "theme_primary_container";
    private static final String KEY_ON_PRIMARY_CONTAINER = "theme_on_primary_container";
    private static final String KEY_PRIMARY_DARK = "theme_primary_dark";

    // Theme color definitions (matching ShopItemEntity.getDefaultItems)
    private static final String[][] THEME_DATA = {
            // {name, primaryHex, primaryContainerHex, onPrimaryContainerHex, primaryDarkHex}
            {"默认蓝", "#3B82F6", "#DBE1FF", "#00105C", "#1A237E"},
            {"森林绿", "#16A34A", "#C8F7C5", "#00210B", "#1B5E20"},
            {"日落橙", "#EA580C", "#FFDBC8", "#3E0E00", "#BF360C"},
            {"樱花粉", "#DB2777", "#FFD8E7", "#3C0020", "#880E4F"},
            {"暗夜紫", "#7C3AED", "#E8DDFF", "#130073", "#4A148C"},
            {"海洋蓝", "#0369A1", "#CCE5FF", "#001B33", "#01579B"},
            {"沙漠金", "#B45309", "#FFDCBE", "#2B1500", "#E65100"},
            {"极光绿", "#0D9488", "#C5F8F5", "#00201E", "#004D40"},
    };

    public static String getPrimaryColor(long themeId) {
        int index = (int) (themeId - 9);
        if (index < 0 || index >= THEME_DATA.length) return THEME_DATA[0][1];
        return THEME_DATA[index][1];
    }

    public static String getPrimaryContainerColor(long themeId) {
        int index = (int) (themeId - 9);
        if (index < 0 || index >= THEME_DATA.length) return THEME_DATA[0][2];
        return THEME_DATA[index][2];
    }

    public static String getOnPrimaryContainerColor(long themeId) {
        int index = (int) (themeId - 9);
        if (index < 0 || index >= THEME_DATA.length) return THEME_DATA[0][3];
        return THEME_DATA[index][3];
    }

    public static String getPrimaryDarkColor(long themeId) {
        int index = (int) (themeId - 9);
        if (index < 0 || index >= THEME_DATA.length) return THEME_DATA[0][4];
        return THEME_DATA[index][4];
    }

    /**
     * Apply the user's equipped theme by updating gradient colors.
     * Called from MainActivity when user data changes.
     */
    public static void applyTheme(Context context, long themeId) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PRIMARY, getPrimaryColor(themeId))
                .putString(KEY_PRIMARY_CONTAINER, getPrimaryContainerColor(themeId))
                .putString(KEY_ON_PRIMARY_CONTAINER, getOnPrimaryContainerColor(themeId))
                .putString(KEY_PRIMARY_DARK, getPrimaryDarkColor(themeId))
                .apply();
    }

    public static String getThemePrimary(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_PRIMARY, "#3B82F6");
    }

    public static String getThemePrimaryContainer(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_PRIMARY_CONTAINER, "#DBE1FF");
    }

    public static String getThemeOnPrimaryContainer(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ON_PRIMARY_CONTAINER, "#00105C");
    }

    public static String getThemePrimaryDark(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_PRIMARY_DARK, "#1A237E");
    }

    public static int getThemePrimaryInt(Context context) {
        return Color.parseColor(getThemePrimary(context));
    }

    public static int getThemePrimaryContainerInt(Context context) {
        return Color.parseColor(getThemePrimaryContainer(context));
    }

    public static int getThemeOnPrimaryContainerInt(Context context) {
        return Color.parseColor(getThemeOnPrimaryContainer(context));
    }

    public static int getThemePrimaryDarkInt(Context context) {
        return Color.parseColor(getThemePrimaryDark(context));
    }
}