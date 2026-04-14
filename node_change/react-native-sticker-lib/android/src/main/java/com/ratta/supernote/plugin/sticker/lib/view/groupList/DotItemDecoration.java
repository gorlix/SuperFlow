package com.ratta.supernote.plugin.sticker.lib.view.groupList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/// Dashed divider
public class DotItemDecoration extends RecyclerView.ItemDecoration {

    private Paint mPaint = new Paint();
    private Path path = new Path();
    private float dashWidth; // Dash segment width
    private float gapWidth; // Gap width
    private int lineColor;
    private int mDividerHeight;
    private int paddingH;

    public DotItemDecoration(Context context) {
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        // Default values
        dashWidth = context.getResources().getDimensionPixelOffset(com.ratta.supernote.pluginlib.R.dimen.dp_px_2);
        gapWidth = context.getResources().getDimensionPixelOffset(com.ratta.supernote.pluginlib.R.dimen.dp_px_4);
        lineColor = 0xFF000000;
        mDividerHeight = context.getResources().getDimensionPixelOffset(
                com.ratta.supernote.pluginlib.R.dimen.dp_px_2);
        paddingH = context.getResources().getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_48);

    }

    @Override
    public void onDraw(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();

        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount - 1; i++) {
            View child = parent.getChildAt(i);
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            int top = child.getBottom() + params.bottomMargin;

            mPaint.setColor(lineColor);
            // Convert dp to px
            mPaint.setStrokeWidth(1);

            // Create dashed effect
            float[] intervals = { 2, 5 };
            mPaint.setPathEffect(new DashPathEffect(intervals, 0));
            // Horizontal dashed line
            path.moveTo(left + paddingH, top + mDividerHeight / 2);
            path.lineTo(right - paddingH, top + mDividerHeight / 2);
            canvas.drawPath(path, mPaint);
            path.reset();

        }
    }

    // @Override
    // public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
    // @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
    // super.getItemOffsets(outRect, view, parent, state);
    // outRect.bottom = view.getContext().getResources().getDimensionPixelOffset(
    // com.ratta.supernote.pluginlib.R.dimen.dp_px_2
    // );
    // }
}
