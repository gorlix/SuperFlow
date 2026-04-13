package com.ratta.supernote.plugin.sticker.lib.view.groupList;

import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewProps;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.ratta.supernote.plugin.sticker.lib.bean.StickerGroupInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StickerGroupListManager extends SimpleViewManager<StickerGroupListView> {
    private static final String NAME = "StickerGroupList";

    @NonNull
    @Override
    public String getName() {
        return NAME;
    }

    @NonNull
    @Override
    protected StickerGroupListView createViewInstance(@NonNull ThemedReactContext themedReactContext) {
        return new StickerGroupListView(themedReactContext);
    }

    @Nullable
    @Override
    public Map getExportedCustomBubblingEventTypeConstants() {
        return MapBuilder.builder().put(
                        StickerGroupListView.EVENT_ITEM_CLICK,
                        MapBuilder.of("phasedRegistrationNames",
                                MapBuilder.of("bubbled", StickerGroupListView.EVENT_ITEM_CLICK))
                )
                .put(StickerGroupListView.EVENT_ITEM_LONG_CLICK,
                        MapBuilder.of("phasedRegistrationNames",
                                MapBuilder.of("bubbled", StickerGroupListView.EVENT_ITEM_LONG_CLICK)))
                .put(StickerGroupListView.EVENT_ON_SCROLL,
                        MapBuilder.of("phasedRegistrationNames",
                                MapBuilder.of("bubbled", StickerGroupListView.EVENT_ON_SCROLL)))
                .build();
    }

    @ReactProp(name = ViewProps.WIDTH)
    public void setWidth(StickerGroupListView view, @Nullable int value) {
        Log.i(NAME, "setWidth :" + value);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null) {
            params.width = value;
            view.setLayoutParams(params);
        }
    }

    @ReactProp(name = ViewProps.HEIGHT)
    public void setHeight(StickerGroupListView view, @Nullable int value) {
        Log.i(NAME, "setHeight :" + value);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null) {
            params.height = value;
            view.setLayoutParams(params);
        }
    }
    @ReactProp(name = "currentStickerGroupId")
    public void setCurrentStickerGroupId(StickerGroupListView view, @Nullable String id) {
        view.setCurrentGroupId(id);
    }
    @ReactProp(name = "scrollPosition")
    public void scrollPosition(StickerGroupListView view, @Nullable int scrollPosition) {
        Log.i(NAME, "scrollPosition :" + scrollPosition);
        view.scrollPosition(scrollPosition);
    }

    @ReactProp(name = "dataList")
    public void setDataList(StickerGroupListView view, @Nullable ReadableArray dataList) {
        if (dataList == null) {
            return;
        }
        Log.i(NAME, "setDataList dataList:"+dataList.toString());

        List<StickerGroupInfo> groupInfoList = new ArrayList<>();
        for (int i = 0; i < dataList.size(); i++) {
            ReadableMap readableMap = dataList.getMap(i);
            StickerGroupInfo groupInfo = new StickerGroupInfo();
            groupInfo.isDefault = readableMap.hasKey("isDefault") ? readableMap.getBoolean("isDefault") : false;
            if (readableMap.hasKey("id")) {
                switch (readableMap.getType("id")) {
                    case Number:
                        int idInt = readableMap.getInt("id");
                        idInt = groupInfo.isDefault ? -idInt : idInt;
                        groupInfo.id = String.valueOf(idInt);
                        break;
                    case String:
                        groupInfo.id = readableMap.getString("id");
                        break;
                    default:
                        groupInfo.id = "";
                        break;
                }
            } else {
                groupInfo.id = "";
            }
            groupInfo.name = readableMap.hasKey("name") ? readableMap.getString("name")
                    : "";
            groupInfo.langName = readableMap.hasKey("langName") ? readableMap.getString("langName") : "";
            groupInfo.num = readableMap.hasKey("num") ? readableMap.getInt("num")
                    : 0;
            groupInfo.path = readableMap.hasKey("path") ? readableMap.getString("path")
                    : "";
            groupInfo.thumb_sticker = readableMap.hasKey("thumb_sticker") ? readableMap.getString("thumb_sticker")
                    : "";
            groupInfo.dir_name = readableMap.hasKey("dir_name") ? readableMap.getString("dir_name")
                    : "";
            groupInfo.create_time = readableMap.hasKey("create_time")
                    ? (long) readableMap.getDouble("create_time")
                    : 0;
            groupInfo.update_time = readableMap.hasKey("update_time")
                    ? (long) readableMap.getDouble("update_time") : 0;
            groupInfo.status = readableMap.hasKey("status") ? readableMap.getBoolean("status") : false;


            groupInfoList.add(groupInfo);

        }
        view.setGroupList(groupInfoList);
    }
    @ReactProp(name = "searchStr")
    public void setSearchStr(StickerGroupListView view, @Nullable String searchStr) {
        view.setSearchStr(searchStr);
    }

    @ReactProp(name = "searchIcon")
    public void setSearchIcon(StickerGroupListView view, ReadableMap icon) {
        if (icon != null && icon.hasKey("uri")) {
            String uriString = icon.getString("uri");
            android.net.Uri uri = android.net.Uri.parse(uriString);
            view.setSearchIcon(uri);
        }
    }

    @ReactProp(name = "itemDefaultIocn")
    public void setItemDefaultIcon(StickerGroupListView view, ReadableMap icon) {
        if (icon != null && icon.hasKey("uri")) {
            String uriString = icon.getString("uri");
            android.net.Uri uri = android.net.Uri.parse(uriString);
            view.setItemDefaultIcon(uri);
        }
    }

}
