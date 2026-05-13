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

    /** Warm fade-in with gentle scale — for HomeFragment stats/header */
    public static void fadeInScale(View view, long delay) {
        view.setAlpha(0f);
        view.setScaleX(0.92f);
        view.setScaleY(0.92f);
        view.animate()
                .alpha(1f)
                .scaleX(1f).scaleY(1f)
                .setDuration(500)
                .setStartDelay(delay)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .start();
    }

    /** Staggered fade-in-scale for arrays — HomeFragment task cards */
    public static void staggeredFadeInScale(View[] views) {
        for (int i = 0; i < views.length; i++) {
            fadeInScale(views[i], i * 100L);
        }
    }

    /** Cascade reveal from center — CalendarFragment grid cells */
    public static void cascadeReveal(View view, long delay) {
        view.setAlpha(0f);
        view.setScaleX(0.85f);
        view.setScaleY(0.85f);
        view.animate()
                .alpha(1f)
                .scaleX(1f).scaleY(1f)
                .setDuration(350)
                .setStartDelay(delay)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    /** Gentle breathe-in — FocusFragment timer ring */
    public static void breatheIn(View view) {
        view.setAlpha(0f);
        view.setScaleX(0.9f);
        view.setScaleY(0.9f);
        view.animate()
                .alpha(1f)
                .scaleX(1f).scaleY(1f)
                .setDuration(700)
                .setInterpolator(new DecelerateInterpolator(2f))
                .start();
    }

    /** Soft fade for FocusFragment controls */
    public static void softFadeIn(View view, long delay) {
        view.setAlpha(0f);
        view.animate()
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(delay)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    /** Staggered bounce for ShopFragment item cards */
    public static void staggeredBounceIn(View[] views) {
        for (int i = 0; i < views.length; i++) {
            views[i].setScaleX(0.6f);
            views[i].setScaleY(0.6f);
            views[i].setAlpha(0f);
            views[i].animate()
                    .alpha(1f)
                    .scaleX(1f).scaleY(1f)
                    .setDuration(400)
                    .setStartDelay(i * 70L)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();
        }
    }

    /** Slide in from left — ProfileFragment info sections */
    public static void slideInFromLeft(View view, long delay) {
        view.setAlpha(0f);
        view.setTranslationX(-60f);
        view.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(450)
                .setStartDelay(delay)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .start();
    }

    /** Slide in from right — ProfileFragment paired sections */
    public static void slideInFromRight(View view, long delay) {
        view.setAlpha(0f);
        view.setTranslationX(60f);
        view.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(450)
                .setStartDelay(delay)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .start();
    }

    /** Celebratory pop — RewardFragment hero card */
    public static void celebratoryPop(View view) {
        view.setAlpha(0f);
        view.setScaleX(0.8f);
        view.setScaleY(0.8f);
        view.animate()
                .alpha(1f)
                .scaleX(1.05f).scaleY(1.05f)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator(2f))
                .withEndAction(() ->
                        view.animate().scaleX(1f).scaleY(1f).setDuration(200)
                                .setInterpolator(new DecelerateInterpolator()).start())
                .start();
    }

    /** Progressive reveal — StreakFragment milestones (slide + fade) */
    public static void progressiveReveal(View view, long delay) {
        view.setAlpha(0f);
        view.setTranslationY(30f);
        view.setScaleX(0.95f);
        view.setScaleY(0.95f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f).scaleY(1f)
                .setDuration(400)
                .setStartDelay(delay)
                .setInterpolator(new DecelerateInterpolator(1.2f))
                .start();
    }

    /** Flame pulse for streak count number */
    public static void flamePulse(View view) {
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.animate()
                .scaleX(1.15f).scaleY(1.15f)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() ->
                        view.animate().scaleX(1f).scaleY(1f).setDuration(300)
                                .setInterpolator(new OvershootInterpolator(1.5f)).start())
                .start();
    }
}
