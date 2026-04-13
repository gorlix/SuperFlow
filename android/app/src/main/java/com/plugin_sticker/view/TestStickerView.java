package com.plugin_sticker.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.plugin_sticker.R;

@SuppressLint("MissingInflatedId")
public class TestStickerView extends FrameLayout {
    private final String TAG = "TestStickerView";

    public TestStickerView(@NonNull Context context) {
        super(context);
        init();
    }

    private void init() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_test, null);
        TextView textView = view.findViewById(R.id.test_text);
        textView.setTextSize(getResources().getDimensionPixelSize(
                com.ratta.supernote.pluginlib.R.dimen.dp_px_20));

        removeAllViews();
        addView(view);
        android.util.Log.d(TAG, "Layout inflated successfully, child count: " + getChildCount());

        // Check child views
        for (int i = 0; i < getChildCount(); i++) {
            android.view.View child = getChildAt(i);
            android.util.Log.d(TAG, "Child " + i + ": " + child.getClass().getName() +
                    ", visibility: " + child.getVisibility() +
                    ", width: " + child.getLayoutParams().width +
                    ", height: " + child.getLayoutParams().height);
        }

    }
}
