package com.ratta.supernote.plugin.sticker.lib.view.groupList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.ratta.supernote.plugin.sticker.lib.bean.StickerGroupInfo;
import com.ratta.supernote.plugin.sticker.lib.view.stickerList.CustomScrollBar;
import com.ratta.supernote.plugin.sticker.lib.view.stickerList.StickerRecyclerView;

import java.util.ArrayList;
import java.util.List;

public class StickerGroupListView extends FrameLayout {
    private final String TAG = "StickerGroupListView";

    public static final String EVENT_ITEM_CLICK = "itemClick";
    public static final String EVENT_ITEM_LONG_CLICK = "itemLongClick";

    public static final String EVENT_ON_SCROLL = "onScroll";

    List<StickerGroupInfo> groupList = new ArrayList<>();
    // Custom scrollbar
    CustomScrollBar customScrollBar;

    StickerRecyclerView listView;

    StickerGroupAdapter mAdapter = null;
    private int mOrientation;

    private String searchStr;
    GroupItemView searchView;

    // Whether scrolling is enabled
    private boolean scrollEnable = true;

    public StickerGroupListView(@NonNull Context context) {
        super(context);
        init();
    }

    private void init() {
        Log.i(TAG, "StickerGroupListView init");
        mOrientation = getResources().getConfiguration().orientation;
        initSearchView();
        initListView();
        initScrollbar();

    }

    private void initSearchView() {

        searchView = new GroupItemView(getContext());
        searchView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setCurrentGroupId("search");
                searchView.setSelected(true);
                WritableMap params = Arguments.createMap();
                params.putString("id", "search");
                sendData2RN((ReactContext) getContext(), EVENT_ITEM_CLICK, params);
            }
        });
        addView(searchView);

        // Divider line
        View view = new View(getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                getContext().getResources()
                        .getDimensionPixelSize(
                                com.ratta.supernote.pluginlib.R.dimen.dp_px_2));
        params.topMargin = getContext().getResources()
                .getDimensionPixelSize(
                        com.ratta.supernote.pluginlib.R.dimen.dp_px_112);
        view.setLayoutParams(params);
        view.setBackgroundColor(Color.BLACK);
        addView(view);
    }

    private void initListView() {
        listView = new StickerRecyclerView(getContext());

        listView.setLayoutManager(new LinearLayoutManager(getContext()));
        listView.setItemAnimator(null);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        params.topMargin = getContext().getResources()
                .getDimensionPixelSize(
                        com.ratta.supernote.pluginlib.R.dimen.dp_px_112);
        listView.setLayoutParams(params);
        listView.addItemDecoration(new DotItemDecoration(getContext()));
        initAdapter();
        listView.setAdapter(mAdapter);
        addView(listView);

    }

    private void initAdapter() {
        mAdapter = new StickerGroupAdapter(groupList);
        Log.i(TAG, "initAdapter");
        mAdapter.setItemClickListener(new StickerGroupAdapter.ItemClickListener() {
            @Override
            public void onItemClick(StickerGroupInfo groupInfo) {
                searchView.setSelected(false);
                WritableMap params = Arguments.createMap();
                params.putString("id", groupInfo.id);
                sendData2RN((ReactContext) getContext(), EVENT_ITEM_CLICK, params);
            }

            @Override
            public void onItemLongClick(StickerGroupInfo groupInfo) {

            }
        });
    }

    // Initialize scrollbar
    @SuppressLint("NewApi")
    private void initScrollbar() {

        customScrollBar = new CustomScrollBar(getContext());
        FrameLayout.LayoutParams scrollParams = new LayoutParams(
                getResources().getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_48),
                ViewGroup.LayoutParams.MATCH_PARENT);
        scrollParams.setMargins(0,
                getContext().getResources()
                        .getDimensionPixelSize(
                                com.ratta.supernote.pluginlib.R.dimen.dp_px_112),
                0,
                0);
        scrollParams.gravity = Gravity.END;
        customScrollBar.setLayoutParams(scrollParams);
        customScrollBar.setRecyclerView(listView);
        addView(customScrollBar);
        updateScrollBar(listView, customScrollBar);

        listView.setOnScrollChangeListener(new OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                // customScrollBar.updateRecyclerViewBar(listView);
                updateScrollBar(listView, customScrollBar);
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
        if (mOrientation != newConfig.orientation) {
            updateScrollBar(listView, customScrollBar);
        }
        mOrientation = newConfig.orientation;
    }

    public void setGroupList(List<StickerGroupInfo> groupList) {
        this.groupList = groupList;
        mAdapter.setList(groupList);
        listView.invalidate();
    }

    public void setCurrentGroupId(String id) {
        searchView.setSelected("search".equals(id));
        mAdapter.setCurrentGroupId(id);

    }

    public void scrollPosition(int position) {

        listView.scrollToPosition(position);

    }

    public void setSearchStr(String searchStr) {
        this.searchStr = searchStr;
        searchView.textView.setText(searchStr);
    }

    public void setSearchIcon(Uri iconUri) {
        // searchView.imageView.setImageBitmap();
        searchView.imageView.setImageURI(iconUri);

    }

    public void setItemDefaultIcon(Uri iconUri) {
        mAdapter.setItemDefaultIcon(iconUri);
    }

    private void sendData2RN(ReactContext reactContext, String eventName, WritableMap params) {
        reactContext.getJSModule(RCTEventEmitter.class)
                .receiveEvent(getId(), eventName, params);

    }

}
