package com.ratta.supernote.plugin.sticker.lib.view.editText;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

// Selection menu layout
public class SelectionView extends LinearLayout {
    private Paint mPaint;
    private RectF mRectF;
    private float mCornerRadius;

    public SelectionView(Context context) {
        super(context);
        init();
    }

    public SelectionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @SuppressLint("NewApi")
    public SelectionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        this.mPaint = new Paint();
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(
                getResources().getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_2));
        mPaint.setAntiAlias(true);
        mRectF = new RectF();
        // Set corner radius (8dp)
        mCornerRadius = getResources().getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_4);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mRectF.set(
                mPaint.getStrokeWidth() / 2, // Left
                mPaint.getStrokeWidth() / 2, // Top
                getWidth() - mPaint.getStrokeWidth() / 2, // Right
                getHeight() - mPaint.getStrokeWidth() / 2 // Bottom
        );
        canvas.drawRoundRect(mRectF, mCornerRadius, mCornerRadius, mPaint);

    }
}
