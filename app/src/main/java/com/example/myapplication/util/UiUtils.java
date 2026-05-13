package com.example.myapplication.util;

import android.content.Context;

public class UiUtils {

    public static float dpToPx(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }

    public static int dpToPxInt(Context context, float dp) {
        return Math.round(dpToPx(context, dp));
    }
}
