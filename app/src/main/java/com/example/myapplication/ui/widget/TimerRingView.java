package com.example.myapplication.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.example.myapplication.util.ThemeManager;

public class TimerRingView extends View {

    private Paint bgPaint;
    private Paint progressPaint;
    private RectF rect = new RectF();
    float progress = 0f; // 0..1

    public TimerRingView(Context context) {
        super(context);
        init();
    }

    public TimerRingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TimerRingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setStrokeWidth(dpToPx(10));
        bgPaint.setColor(0xFFE8E8F0);
        bgPaint.setStrokeCap(Paint.Cap.ROUND);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(dpToPx(10));
        progressPaint.setColor(ContextCompat.getColor(getContext(), R.color.md_primary));
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setProgress(float p) {
        progress = Math.max(0f, Math.min(1f, p));
        int color = ThemeManager.getThemePrimaryInt(getContext());
        progressPaint.setColor(color);
        invalidate();
    }

    public float getProgress() {
        return progress;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float stroke = dpToPx(10);
        float pad = stroke / 2f + dpToPx(4);
        rect.set(pad, pad, getWidth() - pad, getHeight() - pad);

        canvas.drawArc(rect, 0f, 360f, false, bgPaint);

        float sweepAngle = progress * 360f;
        if (sweepAngle > 0.5f) {
            canvas.drawArc(rect, -90f, sweepAngle, false, progressPaint);
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
