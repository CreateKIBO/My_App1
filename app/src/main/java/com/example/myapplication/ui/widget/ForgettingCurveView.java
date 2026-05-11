package com.example.myapplication.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.example.myapplication.util.RewardCalculator;
import com.example.myapplication.util.ThemeManager;

public class ForgettingCurveView extends View {

    private Paint curvePaint;
    private Paint axisPaint;
    private Paint dotPaint;
    private Paint dotDonePaint;
    private Paint labelPaint;
    private Paint fillPaint;

    private int completedSteps = 0;

    public ForgettingCurveView(Context context) {
        super(context);
        init();
    }

    public ForgettingCurveView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ForgettingCurveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        curvePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        curvePaint.setStyle(Paint.Style.STROKE);
        curvePaint.setStrokeWidth(dpToPx(2.5f));
        curvePaint.setColor(ThemeManager.getThemePrimaryInt(getContext()));

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(ThemeManager.getThemePrimaryInt(getContext()));
        fillPaint.setAlpha(20);

        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setStyle(Paint.Style.STROKE);
        axisPaint.setStrokeWidth(dpToPx(1));
        axisPaint.setColor(0xFFD0D0D0);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(0xFFD0D0D0);

        dotDonePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotDonePaint.setStyle(Paint.Style.FILL);
        dotDonePaint.setColor(ContextCompat.getColor(getContext(), R.color.xp_green));

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTextSize(dpToPx(10));
        labelPaint.setColor(0xFF999999);
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setCompletedSteps(int steps) {
        completedSteps = steps;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        float padLeft = dpToPx(36);
        float padRight = dpToPx(16);
        float padTop = dpToPx(16);
        float padBottom = dpToPx(28);

        float chartW = w - padLeft - padRight;
        float chartH = h - padTop - padBottom;

        // Axes
        canvas.drawLine(padLeft, padTop, padLeft, padTop + chartH, axisPaint);
        canvas.drawLine(padLeft, padTop + chartH, padLeft + chartW, padTop + chartH, axisPaint);

        // Y-axis labels
        labelPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("100%", padLeft - dpToPx(4), padTop + dpToPx(4), labelPaint);
        canvas.drawText("50%", padLeft - dpToPx(4), padTop + chartH / 2f + dpToPx(3), labelPaint);
        canvas.drawText("0%", padLeft - dpToPx(4), padTop + chartH + dpToPx(3), labelPaint);

        // Draw forgetting curve: R = e^(-t/S) where S=5 (stability)
        // Map t from 0 to 30 days across chartW
        Path curvePath = new Path();
        Path fillPath = new Path();
        float startX = padLeft;
        float startY = padTop;

        curvePath.moveTo(startX, startY);
        fillPath.moveTo(startX, padTop + chartH);
        fillPath.lineTo(startX, startY);

        int steps = 60;
        for (int i = 1; i <= steps; i++) {
            float t = (i / (float) steps) * 30f;
            float retention = (float) Math.exp(-t / 5.0);
            float x = padLeft + (t / 30f) * chartW;
            float y = padTop + (1f - retention) * chartH;
            curvePath.lineTo(x, y);
            fillPath.lineTo(x, y);
        }

        fillPath.lineTo(padLeft + chartW, padTop + chartH);
        fillPath.close();

        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(curvePath, curvePaint);

        // Draw review interval dots
        int[] intervals = RewardCalculator.REVIEW_INTERVALS;
        labelPaint.setTextAlign(Paint.Align.CENTER);

        for (int i = 0; i < intervals.length; i++) {
            float t = intervals[i];
            float retention = (float) Math.exp(-t / 5.0);
            float x = padLeft + (t / 30f) * chartW;
            float y = padTop + (1f - retention) * chartH;

            boolean done = i < completedSteps;
            float dotR = dpToPx(5);

            canvas.drawCircle(x, y, dotR, done ? dotDonePaint : dotPaint);

            if (done) {
                // Green ring
                Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                ringPaint.setStyle(Paint.Style.STROKE);
                ringPaint.setStrokeWidth(dpToPx(2));
                ringPaint.setColor(ContextCompat.getColor(getContext(), R.color.xp_green));
                canvas.drawCircle(x, y, dotR + dpToPx(3), ringPaint);
            }

            // Label below
            String label = intervals[i] + "天";
            canvas.drawText(label, x, padTop + chartH + dpToPx(16), labelPaint);
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
