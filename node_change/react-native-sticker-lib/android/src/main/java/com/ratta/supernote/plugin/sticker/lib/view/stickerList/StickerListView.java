package com.ratta.supernote.plugin.sticker.lib.view.stickerList;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.ratta.supernote.pluginlib.utils.UiHandler;
import com.ratta.supernote.plugin.sticker.lib.bean.StickerInfo;
import com.ratta.supernote.plugin.sticker.lib.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Implement sticker list on Android to avoid white screen during fast
 * scrolling.
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public class StickerListView extends FrameLayout {
    private final String TAG = "StickerListView";
    StickerRecyclerView listView;

    // Custom scrollbar
    CustomScrollBar customScrollBar;

    List<StickerInfo> stickerList = new ArrayList<>();

    StickerAdapter mAdapter;

    public static final String EVENT_ITEM_CLICK = "itemClick";
    public static final String EVENT_ITEM_LONG_CLICK = "itemLongClick";
    private int mOrientation;

    // Whether scrolling is enabled
    private boolean scrollEnable = true;

    GridItemDecoration mItemDecoration;

    public StickerListView(@NonNull Context context) {
        super(context);
        init();
    }

    private void init() {
        mOrientation = getResources().getConfiguration().orientation;
        Log.i(TAG, "StickerListView init mOrientation:" + mOrientation);

        listView = new StickerRecyclerView(getContext());
        listView.setLayoutManager(new GridLayoutManager(getContext(),
                mOrientation == Configuration.ORIENTATION_PORTRAIT ? 4 : 6) {
            @Override
            public boolean canScrollVertically() {
                return scrollEnable;
            }
        });
        listView.setItemAnimator(null);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        params.setMargins(0,
                getContext().getResources().getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_48),
                0,
                getContext().getResources()
                        .getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_48));

        listView.setLayoutParams(params);
        mItemDecoration = new GridItemDecoration(mOrientation == Configuration.ORIENTATION_PORTRAIT ? 4 : 6,
                getContext().getResources().getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_24),
                0,
                false);
        listView.addItemDecoration(mItemDecoration);

        initAdapter();
        listView.setAdapter(mAdapter);
        addView(listView);

        customScrollBar = new CustomScrollBar(getContext());
        FrameLayout.LayoutParams scrollParams = new LayoutParams(
                getResources().getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_40),
                ViewGroup.LayoutParams.MATCH_PARENT);
        scrollParams.setMargins(0, 0, 0,
                getResources().getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_10));
        scrollParams.gravity = Gravity.END;
        customScrollBar.setLayoutParams(scrollParams);
        customScrollBar.setRecyclerView(listView);
        // customScrollBar.setRecyclerView(listView);
        updateScrollBar(listView, customScrollBar);

        listView.setOnScrollChangeListener(new OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                // customScrollBar.updateRecyclerViewBar(listView);
                updateScrollBar(listView, customScrollBar);
            }
        });

    }

    private void initAdapter() {
        Log.i(TAG, "initAdapter");
        mAdapter = new StickerAdapter(getContext(), stickerList);
        mAdapter.setItemClickListener(new StickerAdapter.ItemClickListener() {
            @Override
            public void onItemClick(StickerInfo sticker) {
                Log.i(TAG, "setItemClickListener onItemClick :" + sticker.id + "==" + sticker.name);
                WritableMap params = Arguments.createMap();
                params.putInt("id", sticker.id);
                sendData2RN((ReactContext) getContext(), EVENT_ITEM_CLICK, params);
            }

            @Override
            public void onItemLongClick(StickerInfo sticker) {
                WritableMap params = Arguments.createMap();
                params.putInt("id", sticker.id);
                sendData2RN((ReactContext) getContext(), EVENT_ITEM_LONG_CLICK, params);

            }
        });
    }

    private void updateScrollBar(RecyclerView recyclerView, CustomScrollBar bar) {
        Log.e(TAG, "updateScrollBar");
        if (recyclerView == null || bar == null) {
            return;
        }
        if (!scrollEnable) {
            bar.setVisibility(View.INVISIBLE);
            return;
        }
        int offset = recyclerView.computeVerticalScrollOffset();

        // Scroll height = content height - visible height
        int scrollHeight = recyclerView.computeVerticalScrollRange() - recyclerView.getMeasuredHeight();

        if (scrollHeight < 0) {
            bar.setVisibility(View.INVISIBLE);
            bar.updateScrollBar(0);
        } else {
            bar.setVisibility(VISIBLE);
            float percent = (float) offset / scrollHeight;
            bar.updateScrollBar(percent);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i(TAG, "onConfigurationChanged orientation:" + newConfig.orientation);
        setOrientation(newConfig.orientation);
        /*
         * if (mOrientation != newConfig.orientation) {
         * mOrientation = newConfig.orientation;
         * listView.setLayoutManager(new GridLayoutManager(getContext(),
         * mOrientation == Configuration.ORIENTATION_PORTRAIT ? 4 : 6) {
         * 
         * @Override
         * public boolean canScrollVertically() {
         * return scrollEnable;
         * }
         * });
         * mItemDecoration.setSpanCount(mOrientation ==
         * Configuration.ORIENTATION_PORTRAIT ? 4 : 6);
         * }
         */
    }

    public void setStickerList(List<StickerInfo> stickerList) {
        this.stickerList = stickerList;
        mAdapter.setList(stickerList);
        listView.scrollToPosition(0);
        listView.invalidate();

    }

    public void setEdit(boolean isEdit) {
        mAdapter.setEdit(isEdit);
        mAdapter.notifyDataSetChanged();
        invalidate();
    }

    public void setScrollEnable(boolean enable) {
        scrollEnable = enable;
        if (!scrollEnable) {
            customScrollBar.setVisibility(GONE);
        } else {
            updateScrollBar(listView, customScrollBar);
        }
    }

    public void setOrientation(int orientation) {
        UiHandler.getInstance().handler().post(new Runnable() {
            @Override
            public void run() {
                if (mOrientation != orientation) {
                    mOrientation = orientation;
                    listView.setLayoutManager(new GridLayoutManager(getContext(),
                            mOrientation == Configuration.ORIENTATION_PORTRAIT ? 4 : 6) {
                        @Override
                        public boolean canScrollVertically() {
                            return scrollEnable;
                        }
                    });
                    mItemDecoration.setSpanCount(mOrientation == Configuration.ORIENTATION_PORTRAIT ? 4 : 6);
                }

            }
        });

    }

    private void sendData2RN(ReactContext reactContext, String eventName, WritableMap params) {
        reactContext.getJSModule(RCTEventEmitter.class)
                .receiveEvent(getId(), eventName, params);

    }
}
