package com.example.myapplication.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.myapplication.R;

/**
 * Custom View that draws a circular progress ring matching the HTML SVG ring.
 * Background track + foreground arc with stroke-dasharray animation.
 */
public class StreakRingView extends View {

    private static final float RING_RADIUS_DP = 58f;
    private static final float STROKE_WIDTH_DP = 8f;
    private static final float START_ANGLE = -90f; // top

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF ringRect = new RectF();

    private float progress = 0f; // 0..1
    private int streakColor;
    private int trackColor;

    public StreakRingView(Context context) {
        super(context);
        init(context);
    }

    public StreakRingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public StreakRingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        streakColor = context.getColor(R.color.streak_orange);
        trackColor = context.getColor(R.color.md_outline_variant);

        bgPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setStrokeCap(Paint.Cap.ROUND);
        bgPaint.setColor(trackColor);

        fgPaint.setStyle(Paint.Style.STROKE);
        fgPaint.setStrokeCap(Paint.Cap.ROUND);
        fgPaint.setColor(streakColor);
    }

    /**
     * Set the progress of the ring.
     * @param progress value from 0f to 1f
     */
    public void setProgress(float progress) {
        this.progress = Math.max(0f, Math.min(1f, progress));
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        float density = getResources().getDisplayMetrics().density;
        float strokeWidth = STROKE_WIDTH_DP * density;
        float ringRadius = RING_RADIUS_DP * density;

        bgPaint.setStrokeWidth(strokeWidth);
        fgPaint.setStrokeWidth(strokeWidth);

        float cx = w / 2f;
        float cy = h / 2f;
        ringRect.set(cx - ringRadius, cy - ringRadius, cx + ringRadius, cy + ringRadius);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Background track (full circle)
        canvas.drawArc(ringRect, 0f, 360f, false, bgPaint);

        // Foreground arc (progress)
        if (progress > 0f) {
            float sweepAngle = 360f * progress;
            canvas.drawArc(ringRect, START_ANGLE, sweepAngle, false, fgPaint);
        }
    }
}
