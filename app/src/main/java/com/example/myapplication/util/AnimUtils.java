package com.example.myapplication.util;

import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

public final class AnimUtils {

    private AnimUtils() {}

    public static void scaleOnClick(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.92f).scaleY(0.92f)
                            .setDuration(100)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f)
                            .setDuration(200)
                            .setInterpolator(new OvershootInterpolator(2f))
                            .start();
                    break;
            }
            return false;
        });
    }

    public static void bounceIn(View view) {
        view.setScaleX(0f);
        view.setScaleY(0f);
        view.setAlpha(0f);
        view.animate()
                .scaleX(1f).scaleY(1f)
                .alpha(1f)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();
    }

    public static void fadeIn(View view) {
        view.setAlpha(0f);
        view.animate()
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    public static void slideUpFadeIn(View view, float dpOffset) {
        view.setTranslationY(dpOffset);
        view.setAlpha(0f);
        view.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(350)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }
}
