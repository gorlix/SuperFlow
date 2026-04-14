package com.ratta.supernote.plugin.sticker.lib.view.dashView;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class DashView extends View {
    private Paint paint = new Paint();
    private Path path = new Path();
    private float dashWidth; // Dash segment width
    private float gapWidth; // Gap width
    private int lineColor; // Line color
    private boolean isVertical; // Whether vertical

    public DashView(Context context) {
        super(context);
        init();
    }

    public DashView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);

        // Default values
        dashWidth = 2f;
        gapWidth = 5f;
        lineColor = 0xFF000000;
        isVertical = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        paint.setColor(lineColor);
        // Convert dp to px
        paint.setStrokeWidth(1);

        // Create dashed effect
        float[] intervals = { 2, 5 };
        paint.setPathEffect(new DashPathEffect(intervals, 0));

        if (isVertical) {
            // Vertical dashed line
            path.moveTo(getWidth() / 2, 0);
            path.lineTo(getWidth() / 2, getHeight());
        } else {
            // Horizontal dashed line
            path.moveTo(0, getHeight() / 2);
            path.lineTo(getWidth(), getHeight() / 2);
        }

        canvas.drawPath(path, paint);
        path.reset();
    }

    // Set orientation (horizontal/vertical)
    public void setVertical(boolean vertical) {
        isVertical = vertical;
        requestLayout();
        invalidate();
    }

    // Set dash params
    public void setDashWidth(float dashWidth) {

        this.dashWidth = dashWidth;
        invalidate();
    }

    public void setGapWidth(float gapWidth) {

        this.gapWidth = gapWidth;
        invalidate();
    }

    // Set line color
    public void setLineColor(int color) {
        this.lineColor = color;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = isVertical ? (int) paint.getStrokeWidth() : MeasureSpec.getSize(widthMeasureSpec);
        int height = isVertical ? MeasureSpec.getSize(heightMeasureSpec) : (int) paint.getStrokeWidth();
        setMeasuredDimension(width, height);
    }
}
