package com.ratta.supernote.plugin.sticker.lib.view.groupList;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Type;

public class GroupItemView extends LinearLayout {

    private final String TAG = "GroupItemView";
    public TextView textView;
    public ImageView imageView;
    private int mOrientation;
    private boolean isItemSelected = false;

    Paint mPaint = new Paint();
    private RectF mRectF;
    private float mCornerRadius;

    public GroupItemView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(
                getResources().getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_4));
        mPaint.setColor(Color.BLACK);

        mRectF = new RectF();
        // Set corner radius (8dp)
        mCornerRadius = getResources().getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_5);

        mOrientation = getResources().getConfiguration().orientation;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        this.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                getContext().getResources()
                        .getDimensionPixelSize(
                                com.ratta.supernote.pluginlib.R.dimen.dp_px_112)));
        setPadding(
                getContext().getResources().getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_48),
                0,
                getContext().getResources().getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_48),
                0);

        initImageView();
        initText();

    }

    // Initialize icon view and add it to the parent
    private void initImageView() {
        imageView = new ImageView(getContext());
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                getContext().getResources().getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_48),
                getContext().getResources()
                        .getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_48)));
        addView(imageView);

    }

    // Initialize text view and add it to the parent
    private void initText() {
        textView = new TextView(getContext());
        float spValue = getContext().getResources().getDimension(com.ratta.supernote.pluginlib.R.dimen.sp_px_34)
                / getContext().getResources().getDisplayMetrics().scaledDensity;
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                spValue);
        textView.setLayoutParams(new LayoutParams(
                /*
                 * getContext().getResources().getDimensionPixelSize(mOrientation ==
                 * Configuration.ORIENTATION_PORTRAIT
                 * ? com.ratta.supernote.pluginlib.R.dimen.dp_px_270
                 * : com.ratta.supernote.pluginlib.R.dimen.dp_px_290)
                 */
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        textView.setMaxLines(1);
        textView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        textView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        textView.setPadding(
                getContext().getResources().getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_16),
                0, 0, 0);
        addView(textView);

    }

    public void setSelected(boolean isSelected) {
        Log.i(TAG, "setSelected isSelected:" + isSelected);
        super.setSelected(isSelected); // Keep this line
        if (isItemSelected != isSelected) {
            isItemSelected = isSelected;
            invalidate();
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.i(TAG, "onDraw isItemSelected:" + isItemSelected + "==" + getWidth() + "==" + getHeight());
        if (!isItemSelected) {
            return;
        }
        // Set drawing bounds
        mRectF.set(
                mPaint.getStrokeWidth() / 2, // Left
                mPaint.getStrokeWidth() / 2, // Top
                getWidth() - mPaint.getStrokeWidth() / 2, // Right
                getHeight() - mPaint.getStrokeWidth() / 2 // Bottom
        );
        canvas.drawRoundRect(mRectF, mCornerRadius, mCornerRadius, mPaint);

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        Log.i(TAG, "onLayout changed:" + changed + " l:" + l + " t:" + t + " r:" + r + " b:" + b);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.i(TAG, "onMeasure width:" + getMeasuredWidth() + " height:" + getMeasuredHeight());
    }
}
