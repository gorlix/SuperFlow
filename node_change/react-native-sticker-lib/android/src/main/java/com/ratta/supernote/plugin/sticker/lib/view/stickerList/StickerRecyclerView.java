package com.ratta.supernote.plugin.sticker.lib.view.stickerList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

public class StickerRecyclerView extends RecyclerView {
    private boolean mRequestedLayout = false;

    /**
     * Constructor: initialize StickerRecyclerView
     */
    public StickerRecyclerView(@NonNull Context context) {
        super(context);
        disableAccessibility();
    }

    /**
     * Constructor: initialize StickerRecyclerView
     */
    public StickerRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        disableAccessibility();
    }

    /**
     * Constructor: initialize StickerRecyclerView
     */
    public StickerRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        disableAccessibility();
    }

    /**
     * Disable RecyclerView accessibility logic to avoid internal NPEs on some ROMs
     */
    private void disableAccessibility() {
        ViewCompat.setAccessibilityDelegate(this, null);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
    }

    @Override
    public void requestLayout() {
        super.requestLayout();
        if (!isAttachedToWindow()) {
            return;
        }
        // Force-post a layout request to ensure re-layout
        if (!mRequestedLayout) {
            mRequestedLayout = true;
            post(() -> {
                mRequestedLayout = false;
                measure(MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY));
                layout(getLeft(), getTop(), getRight(), getBottom());
            });
        }
    }

    /**
     * Set adapter and guard against crashes triggered by system/ROM accessibility
     * paths
     */
    @Override
    public void setAdapter(@Nullable Adapter adapter) {
        try {
            super.setAdapter(adapter);
        } catch (NullPointerException ignored) {
            try {
                super.swapAdapter(adapter, false);
            } catch (Throwable ignored2) {
            }
        }
    }

    /**
     * Avoid exceptions caused by accessibility-triggered layout computations early
     * in lifecycle
     */
    @Override
    public boolean isComputingLayout() {
        return false;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        return false;
    }

    @Override
    public void sendAccessibilityEventUnchecked(AccessibilityEvent event) {
    }

}
