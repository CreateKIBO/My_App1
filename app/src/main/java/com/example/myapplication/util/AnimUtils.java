package com.example.myapplication.util;

import com.example.myapplication.R;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

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

    /** Warm fade-in with gentle scale — for HomeFragment stats/header */
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

    /** Cascade reveal from center — CalendarFragment grid cells */
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

    /**
     * Show a full-screen "item acquired" animation overlay.
     * Sequence: overlay fades in → card pops up with overshoot → pause → card shrinks & fades out.
     *
     * @param activity  The hosting activity
     * @param emoji     Item emoji (e.g. "🔥")
     * @param name      Item name (e.g. "凤凰")
     * @param desc      One-line description (e.g. "限定头像已解锁")
     * @param colorHex  Accent color hex (e.g. "#DC2626") for the glow ring
     */
    public static void showItemAcquired(Activity activity, String emoji, String name,
                                         String desc, String colorHex) {
        ViewGroup root = activity.findViewById(android.R.id.content);
        FrameLayout overlay = (FrameLayout) LayoutInflater.from(activity)
                .inflate(R.layout.overlay_item_acquire, root, false);

        TextView tvEmoji = overlay.findViewById(R.id.acquire_emoji);
        TextView tvName = overlay.findViewById(R.id.acquire_name);
        TextView tvDesc = overlay.findViewById(R.id.acquire_desc);
        View glow = overlay.findViewById(R.id.acquire_glow);
        View card = overlay.findViewById(R.id.acquire_card);

        tvEmoji.setText(emoji);
        tvName.setText(name);
        tvDesc.setText(desc);

        // Tint glow ring with accent color
        try {
            int color = android.graphics.Color.parseColor(colorHex);
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(color);
            gd.setAlpha(51); // 20% opacity
            glow.setBackground(gd);
        } catch (Exception ignored) {}

        // Initial state: overlay transparent, card scaled down
        overlay.setAlpha(0f);
        card.setScaleX(0.4f);
        card.setScaleY(0.4f);
        card.setAlpha(0f);

        root.addView(overlay);

        // Phase 1: Fade in overlay (200ms)
        overlay.animate()
                .alpha(1f)
                .setDuration(200)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    // Phase 2: Card pops in with overshoot (400ms)
                    card.animate()
                            .alpha(1f)
                            .scaleX(1f).scaleY(1f)
                            .setDuration(400)
                            .setInterpolator(new OvershootInterpolator(1.8f))
                            .withEndAction(() -> {
                                // Phase 3: Glow pulse while card is visible
                                glow.animate()
                                        .scaleX(1.4f).scaleY(1.4f)
                                        .alpha(0f)
                                        .setDuration(600)
                                        .setInterpolator(new AccelerateInterpolator())
                                        .start();

                                // Phase 4: After pause, shrink & fade out card (500ms)
                                card.animate()
                                        .scaleX(0.3f).scaleY(0.3f)
                                        .alpha(0f)
                                        .setDuration(500)
                                        .setStartDelay(800)
                                        .setInterpolator(new AccelerateInterpolator())
                                        .withEndAction(() -> {
                                            // Phase 5: Fade out overlay & remove
                                            overlay.animate()
                                                    .alpha(0f)
                                                    .setDuration(200)
                                                    .withEndAction(() -> root.removeView(overlay))
                                                    .start();
                                        })
                                        .start();
                            })
                            .start();
                })
                .start();

        // Allow tap to dismiss early
        overlay.setOnClickListener(v -> {
            overlay.animate().cancel();
            card.animate().cancel();
            overlay.animate().alpha(0f).setDuration(150)
                    .withEndAction(() -> root.removeView(overlay)).start();
        });
    }
}
