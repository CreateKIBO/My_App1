package com.example.myapplication.util;

import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

public class AnimUtils {

    public static void scaleOnClick(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.93f).scaleY(0.93f).setDuration(100)
                            .setInterpolator(new DecelerateInterpolator()).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(150)
                            .setInterpolator(new OvershootInterpolator(2f)).start();
                    break;
            }
            return false;
        });
    }

    public static void slideUpFadeIn(View view, long delay) {
        view.setAlpha(0f);
        view.setTranslationY(40f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(delay)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    public static void staggeredSlideUp(View[] views) {
        for (int i = 0; i < views.length; i++) {
            slideUpFadeIn(views[i], i * 80L);
        }
    }

    public static void crossFade(View outView, View inView) {
        outView.animate().alpha(0f).setDuration(200).withEndAction(() -> {
            outView.setVisibility(View.GONE);
            inView.setAlpha(0f);
            inView.setVisibility(View.VISIBLE);
            inView.animate().alpha(1f).setDuration(200).start();
        }).start();
    }

    public static void animateCounter(TextView textView, int from, int to) {
        if (from == to) return;
        ValueAnimator animator = ValueAnimator.ofInt(from, to);
        animator.setDuration(500);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation ->
                textView.setText(String.valueOf((int) animation.getAnimatedValue())));
        animator.start();
    }

    public static void animateCounter(TextView textView, int from, int to, long delay) {
        if (from == to) return;
        ValueAnimator animator = ValueAnimator.ofInt(from, to);
        animator.setDuration(500);
        animator.setStartDelay(delay);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation ->
                textView.setText(String.valueOf((int) animation.getAnimatedValue())));
        animator.start();
    }

    public static void pulse(View view) {
        view.animate()
                .scaleX(1.05f).scaleY(1.05f)
                .setDuration(150)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() ->
                        view.animate().scaleX(1f).scaleY(1f).setDuration(150)
                                .setInterpolator(new OvershootInterpolator(2f)).start())
                .start();
    }

    public static void bounceIn(View view) {
        view.setScaleX(0.7f);
        view.setScaleY(0.7f);
        view.setAlpha(0f);
        view.animate()
                .alpha(1f)
                .scaleX(1f).scaleY(1f)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator(1.5f))
                .start();
    }
}
