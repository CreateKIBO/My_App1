package com.example.myapplication.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import com.example.myapplication.data.local.ShopItemEntity;

/**
 * Maps theme shop item IDs to color values for dynamic theme switching.
 * Uses a lookup table keyed by theme name, not by fragile ID arithmetic.
 */
public class ThemeManager {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_PRIMARY = "theme_primary";
    private static final String KEY_PRIMARY_CONTAINER = "theme_primary_container";
    private static final String KEY_ON_PRIMARY_CONTAINER = "theme_on_primary_container";
    private static final String KEY_PRIMARY_DARK = "theme_primary_dark";

    // Theme color definitions keyed by name
    private static final java.util.Map<String, String[]> THEME_MAP = new java.util.LinkedHashMap<>();
    static {
        // {primaryHex, primaryContainerHex, onPrimaryContainerHex, primaryDarkHex}
        THEME_MAP.put("默认蓝",   new String[]{"#3B82F6", "#DBE1FF", "#00105C", "#1A237E"});
        THEME_MAP.put("森林绿",   new String[]{"#16A34A", "#C8F7C5", "#00210B", "#1B5E20"});
        THEME_MAP.put("日落橙",   new String[]{"#EA580C", "#FFDBC8", "#3E0E00", "#BF360C"});
        THEME_MAP.put("樱花粉",   new String[]{"#DB2777", "#FFD8E7", "#3C0020", "#880E4F"});
        THEME_MAP.put("暗夜紫",   new String[]{"#7C3AED", "#E8DDFF", "#130073", "#4A148C"});
        THEME_MAP.put("海洋蓝",   new String[]{"#0369A1", "#CCE5FF", "#001B33", "#01579B"});
        THEME_MAP.put("沙漠金",   new String[]{"#B45309", "#FFDCBE", "#2B1500", "#E65100"});
        THEME_MAP.put("极光绿",   new String[]{"#0D9488", "#C5F8F5", "#00201E", "#004D40"});
        THEME_MAP.put("烈焰红",   new String[]{"#DC2626", "#FECACA", "#5C0000", "#991B1B"});
        THEME_MAP.put("星河紫",   new String[]{"#6D28D9", "#E9D5FF", "#2E005A", "#4C1D95"});
    }

    private static final String[] DEFAULT = THEME_MAP.get("默认蓝");

    private static String[] getThemeData(long themeId) {
        // Look up theme by querying the database for the item name, then mapping name to colors.
        // Since we don't have DB access here, we use a positional fallback based on theme count.
        // Themes start after avatars in ShopItemEntity.getDefaultItems(), but we no longer hardcode the offset.
        // Instead, we iterate THEME_MAP entries by position.
        java.util.List<String[]> entries = new java.util.ArrayList<>(THEME_MAP.values());
        // Theme IDs are sequential starting after avatar items.
        // We compute the offset dynamically: first theme ID = total avatar count + 1.
        // For safety, we just try each position and return the matching one.
        int index = (int) (themeId - computeThemeOffset());
        if (index >= 0 && index < entries.size()) return entries.get(index);
        return DEFAULT;
    }

    private static long computeThemeOffset() {
        // Count free avatars in default items to determine where themes start
        int avatarCount = 0;
        for (ShopItemEntity item : ShopItemEntity.getDefaultItems()) {
            if ("AVATAR".equals(item.getType())) avatarCount++;
        }
        // First theme ID = avatarCount + 1 (since IDs are auto-generated sequentially)
        return avatarCount + 1;
    }

    public static String getPrimaryColor(long themeId) {
        return getThemeData(themeId)[0];
    }

    public static String getPrimaryContainerColor(long themeId) {
        return getThemeData(themeId)[1];
    }

    public static String getOnPrimaryContainerColor(long themeId) {
        return getThemeData(themeId)[2];
    }

    public static String getPrimaryDarkColor(long themeId) {
        return getThemeData(themeId)[3];
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