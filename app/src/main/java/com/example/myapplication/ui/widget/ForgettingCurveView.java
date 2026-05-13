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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ForgettingCurveView extends View {

    private Paint curvePaint;
    private Paint axisPaint;
    private Paint dotPaint;
    private Paint dotDonePaint;
    private Paint labelPaint;
    private Paint datePaint;
    private Paint fillPaint;

    private int completedSteps = 0;
    private String createdDate = null; // yyyy-MM-dd

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
        labelPaint.setTextSize(dpToPx(9));
        labelPaint.setColor(0xFF999999);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        datePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        datePaint.setTextSize(dpToPx(8));
        datePaint.setColor(0xFFBBBBBB);
        datePaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setCompletedSteps(int steps) {
        completedSteps = steps;
        invalidate();
    }

    public void setCreatedDate(String date) {
        createdDate = date;
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
        float padBottom = dpToPx(40);

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

        // Draw forgetting curve: R = e^(-t/S) where S=5
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

        // Draw review interval dots with labels
        int[] intervals = RewardCalculator.REVIEW_INTERVALS;
        labelPaint.setTextAlign(Paint.Align.CENTER);

        // Pre-calculate x positions and review dates
        float[] dotXs = new float[intervals.length];
        float[] dotYs = new float[intervals.length];
        String[] dateLabels = new String[intervals.length];

        SimpleDateFormat sdf = new SimpleDateFormat("M/d", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        if (createdDate != null) {
            try {
                SimpleDateFormat inputSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                cal.setTime(inputSdf.parse(createdDate));
            } catch (Exception e) {
                cal = Calendar.getInstance();
            }
        }

        for (int i = 0; i < intervals.length; i++) {
            float t = intervals[i];
            float retention = (float) Math.exp(-t / 5.0);
            dotXs[i] = padLeft + (t / 30f) * chartW;
            dotYs[i] = padTop + (1f - retention) * chartH;

            // Calculate review date
            Calendar reviewCal = (Calendar) cal.clone();
            reviewCal.add(Calendar.DAY_OF_YEAR, intervals[i]);
            dateLabels[i] = sdf.format(reviewCal.getTime());
        }

        for (int i = 0; i < intervals.length; i++) {
            boolean done = i < completedSteps;
            float dotR = dpToPx(5);

            canvas.drawCircle(dotXs[i], dotYs[i], dotR, done ? dotDonePaint : dotPaint);

            if (done) {
                Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                ringPaint.setStyle(Paint.Style.STROKE);
                ringPaint.setStrokeWidth(dpToPx(2));
                ringPaint.setColor(ContextCompat.getColor(getContext(), R.color.xp_green));
                canvas.drawCircle(dotXs[i], dotYs[i], dotR + dpToPx(3), ringPaint);
            }

            // Day label — alternate above/below to avoid overlap
            String dayLabel = intervals[i] + "天";
            float labelY;
            if (i % 2 == 0) {
                labelY = padTop + chartH + dpToPx(14);
            } else {
                labelY = padTop + chartH + dpToPx(26);
            }
            canvas.drawText(dayLabel, dotXs[i], labelY, labelPaint);

            // Date label below day label
            if (createdDate != null) {
                canvas.drawText(dateLabels[i], dotXs[i], labelY + dpToPx(10), datePaint);
            }
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
