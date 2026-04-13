package com.ratta.supernote.plugin.sticker.lib.view.dashView;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// Vertical dashed divider
public class DashVerticalView extends View {
    private Paint paint = new Paint();
    private Path path = new Path();
    private float dashWidth; // Dash segment width
    private float gapWidth; // Gap width
    private int lineColor; // Line color

    public DashVerticalView(Context context) {
        super(context);
        init();
    }

    public DashVerticalView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DashVerticalView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
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
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        paint.setColor(lineColor);

        // Create dashed effect
        float[] intervals = { dashWidth, gapWidth };
        paint.setPathEffect(new DashPathEffect(intervals, 0));

        path.moveTo(getWidth() / 2, 0);
        path.lineTo(getWidth() / 2, getHeight());

        canvas.drawPath(path, paint);
        path.reset();

    }

    public void setDashWidth(float dashWidth) {
        this.dashWidth = dashWidth;
    }

    public void setGapWidth(float gapWidth) {
        this.gapWidth = gapWidth;
    }

    public void setStrokeWidth(float width) {
        paint.setStrokeWidth(width);
    }
}
