package com.ratta.supernote.plugin.sticker.lib.view.stickerList;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class GridItemDecoration extends RecyclerView.ItemDecoration {
    private final int spacingV; // Vertical spacing
    private final int spacingH; // Horizontal spacing
    private int spanCount; // Span count
    private final boolean includeEdge; // Whether to include edge spacing

    public GridItemDecoration(int spanCount, int spacingV, int spacingH, boolean includeEdge) {
        this.spanCount = spanCount;
        this.spacingV = spacingV;
        this.spacingH = spacingH;
        this.includeEdge = includeEdge;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
            @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view); // Current item position
        int column = position % spanCount; // Current item column

        if (includeEdge) {
            // Left/right spacing
            outRect.left = spacingH - column * spacingH / spanCount;
            outRect.right = (column + 1) * spacingH / spanCount;
        } else {
            outRect.left = column * spacingH / spanCount;
            outRect.right = spacingH - (column + 1) * spacingH / spanCount;
        }

        // Top/bottom spacing
        if (position >= spanCount) {
            outRect.top = spacingV; // Add top spacing from the second row onward
        }
        outRect.bottom = spacingV; // All items have bottom spacing
    }

    public void setSpanCount(int spanCount) {
        this.spanCount = spanCount;
    }
}
