package com.ratta.supernote.plugin.sticker.lib.view.stickerList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

public class CustomScrollBar extends View {

    private final int STROKE_WIDTH = 4;// Border stroke width
    private final int LINE_STROKE_WIDTH = 4;// Center line stroke width
    private final int CENTER_LINE_MARGIN_LEFTORRIGHT = 9;// Center line horizontal margin
    private final int CENTER_LINE_MARGIN_TOPORBOTTOM = 4;// Center line vertical margin
    private Paint paint;
    private Paint backgroundPaint;
    private Paint linePaint;

    private RectF oval3;

    private RectF firstLine;
    private RectF secondLine;
    private RectF threeLine;
    private float barWidth;
    private float barHeight;
    private Context context;
    private float top = 0;
    private static final String TAG = "CustomScrollBar";

    private OnScrollYChange onScrollPercent;

    private RecyclerView recyclerView;

    public CustomScrollBar(Context context) {
        this(context, null);
    }

    public CustomScrollBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomScrollBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;

        init();
    }

    public void setOnScrollPercent(OnScrollYChange onScrollPercent) {
        this.onScrollPercent = onScrollPercent;
    }

    public void updateScrollBar(float percent) {
        top = percent * (getMeasuredHeight() - barHeight);
        float offset = (getMeasuredWidth() - barWidth) / 2.0f;
        // Offset by half stroke width to avoid clipping
        oval3 = new RectF(offset + STROKE_WIDTH / 2.0f, top + STROKE_WIDTH / 2.0f,
                offset + barWidth - (STROKE_WIDTH >> 1), top + barHeight - (STROKE_WIDTH >> 1));// Create a new rect
        firstLine = new RectF(offset + CENTER_LINE_MARGIN_LEFTORRIGHT,
                top + (barHeight / 2) - (LINE_STROKE_WIDTH >> 1) - CENTER_LINE_MARGIN_TOPORBOTTOM - LINE_STROKE_WIDTH,
                offset + barWidth - CENTER_LINE_MARGIN_LEFTORRIGHT,
                top + (barHeight / 2) - (LINE_STROKE_WIDTH >> 1) - CENTER_LINE_MARGIN_TOPORBOTTOM);
        secondLine = new RectF(offset + CENTER_LINE_MARGIN_LEFTORRIGHT,
                top + (barHeight / 2) - (LINE_STROKE_WIDTH >> 1), offset + barWidth - CENTER_LINE_MARGIN_LEFTORRIGHT,
                top + (barHeight / 2) + (LINE_STROKE_WIDTH >> 1));
        threeLine = new RectF(offset + CENTER_LINE_MARGIN_LEFTORRIGHT,
                top + (barHeight / 2) + (LINE_STROKE_WIDTH >> 1) + CENTER_LINE_MARGIN_TOPORBOTTOM,
                offset + barWidth - CENTER_LINE_MARGIN_LEFTORRIGHT,
                top + (barHeight / 2) + (LINE_STROKE_WIDTH >> 1) + CENTER_LINE_MARGIN_TOPORBOTTOM + LINE_STROKE_WIDTH);
        invalidate();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(STROKE_WIDTH);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.WHITE);
        backgroundPaint.setStyle(Paint.Style.FILL);

        linePaint = new Paint();
        linePaint.setColor(Color.BLACK);
        linePaint.setStrokeWidth(LINE_STROKE_WIDTH);
        linePaint.setAntiAlias(true);
        linePaint.setStyle(Paint.Style.FILL);

        barWidth = context.getResources().getDimension(com.ratta.supernote.pluginlib.R.dimen.dp_px_34);
        barHeight = context.getResources().getDimension(com.ratta.supernote.pluginlib.R.dimen.dp_px_90);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float offset = (getMeasuredWidth() - barWidth) / 2.0f;
        // Offset by half stroke width to avoid clipping

        oval3 = new RectF(offset + STROKE_WIDTH / 2.0f, top + STROKE_WIDTH / 2.0f,
                offset + barWidth - (STROKE_WIDTH >> 1), top + barHeight - (STROKE_WIDTH >> 1));// Create a new rect
        firstLine = new RectF(offset + CENTER_LINE_MARGIN_LEFTORRIGHT,
                top + (barHeight / 2) - (LINE_STROKE_WIDTH >> 1) - CENTER_LINE_MARGIN_TOPORBOTTOM - LINE_STROKE_WIDTH,
                offset + barWidth - CENTER_LINE_MARGIN_LEFTORRIGHT,
                top + (barHeight / 2) - (LINE_STROKE_WIDTH >> 1) - CENTER_LINE_MARGIN_TOPORBOTTOM);
        secondLine = new RectF(offset + CENTER_LINE_MARGIN_LEFTORRIGHT,
                top + (barHeight / 2) - (LINE_STROKE_WIDTH >> 1), offset + barWidth - CENTER_LINE_MARGIN_LEFTORRIGHT,
                top + (barHeight / 2) + (LINE_STROKE_WIDTH >> 1));
        threeLine = new RectF(offset + CENTER_LINE_MARGIN_LEFTORRIGHT,
                top + (barHeight / 2) + (LINE_STROKE_WIDTH >> 1) + CENTER_LINE_MARGIN_TOPORBOTTOM,
                offset + barWidth - CENTER_LINE_MARGIN_LEFTORRIGHT,
                top + (barHeight / 2) + (LINE_STROKE_WIDTH >> 1) + CENTER_LINE_MARGIN_TOPORBOTTOM + LINE_STROKE_WIDTH);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.i(TAG, "onDraw firstLine:" + firstLine.toString());

        // Draw background (mainly to set background color)
        canvas.drawRoundRect(oval3, 150f, 150f, backgroundPaint);

        // Rounded rectangle
        canvas.drawRoundRect(oval3, 150f, 150f, paint);

        // Center line 1
        canvas.drawRoundRect(firstLine, 150f, 150f, linePaint);

        // Center line 2
        canvas.drawRoundRect(secondLine, 150f, 150f, linePaint);

        // Center line 3
        canvas.drawRoundRect(threeLine, 150f, 150f, linePaint);
    }

    public boolean isInvalid = false;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (recyclerView == null) {
            return super.onTouchEvent(event);
        }

        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                float scrollRatio = y / (getMeasuredHeight() - barHeight);
                int totalScrollRange = recyclerView.computeVerticalScrollRange() - recyclerView.getMeasuredHeight();
                int scrollTo = (int) (scrollRatio * totalScrollRange);
                recyclerView.scrollBy(0, scrollTo - recyclerView.computeVerticalScrollOffset());
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    public interface OnScrollYChange {

        void onYChange(int dy);
    }

    /**
     * Add a scrollbar for RecyclerView
     *
     * @param recyclerView
     */
    public void updateRecyclerViewBar(RecyclerView recyclerView) {
        Log.i(TAG, "updateRecyclerViewBar:");
        int offset = recyclerView.computeVerticalScrollOffset();
        // Scroll height = content height - visible height
        float scrollHeight = recyclerView.computeVerticalScrollRange() - recyclerView.getMeasuredHeight();
        if (scrollHeight < 0) {
            setVisibility(View.GONE);
            updateScrollBar(0);
        } else {
            setVisibility(View.VISIBLE);
            float percent = offset / scrollHeight;
            updateScrollBar(percent);
        }
    }

    /**
     * Add a scrollbar for ScrollView
     *
     * @param scrollView
     * @param textView
     */
    public void updateScrollViewBar(ScrollView scrollView, TextView textView) {
        Log.i(TAG, "updateScrollViewBar");
        int offset = scrollView.getScrollY();
        // Scroll height = content height - visible height
        float scrollHeight = textView.getMeasuredHeight() - scrollView.getMeasuredHeight();
        if (scrollHeight < 0) {
            int contentHeight = textView.getLayout().getHeight();
            if (contentHeight > scrollView.getMeasuredHeight()) {
                setVisibility(View.VISIBLE);
            } else {
                setVisibility(View.GONE);
                updateScrollBar(0);
            }
        } else {
            setVisibility(View.VISIBLE);
            float percent = offset / scrollHeight;
            updateScrollBar(percent);
        }
    }

    /**
     * Custom scrollbar for NestedScrollView
     *
     * @param scrollView
     * @param recyclerView1
     * @param recyclerView2
     */
    public void updateNestedScrollViewBar(NestedScrollView scrollView, RecyclerView recyclerView1,
            RecyclerView recyclerView2) {
        int totalRecyclerViewHeight = recyclerView1.getMeasuredHeight() + recyclerView2.getMeasuredHeight();
        Log.d(TAG, "totalRecyclerViewHeight:" + totalRecyclerViewHeight);
        int offset = scrollView.getScrollY();
        // Scroll height = content height - visible height
        float scrollHeight = totalRecyclerViewHeight - scrollView.getMeasuredHeight();
        Log.d(TAG, "scrollHeight:" + scrollHeight);
        if (scrollHeight < 0) {
            int contentHeight = totalRecyclerViewHeight;
            if (contentHeight > scrollView.getMeasuredHeight()) {
                setVisibility(View.VISIBLE);
            } else {
                setVisibility(View.GONE);
                updateScrollBar(0);
            }
        } else {
            setVisibility(View.VISIBLE);
            float percent = offset / scrollHeight;
            Log.d(TAG, "percent:" + percent);
            if (percent > 1f) {
                percent = 1.0f;
            }
            Log.d(TAG, "lastpercent:" + percent);
            updateScrollBar(percent);
        }
    }
}
