package com.ratta.supernote.plugin.sticker.lib.view.stickerList;

import android.content.res.Configuration;
import android.os.Build;
import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewProps;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.ratta.supernote.plugin.sticker.lib.bean.StickerInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@RequiresApi(api = Build.VERSION_CODES.M)
public class StickerListManager extends SimpleViewManager<StickerListView> {

    private static final String NAME = "StickerList";

    @NonNull
    @Override
    public String getName() {
        return NAME;
    }

    @NonNull
    @Override
    protected StickerListView createViewInstance(@NonNull ThemedReactContext themedReactContext) {
        return new StickerListView(themedReactContext);
    }

    @Nullable
    @Override
    public Map getExportedCustomBubblingEventTypeConstants() {
        return MapBuilder.builder().put(
                        StickerListView.EVENT_ITEM_CLICK,
                        MapBuilder.of("phasedRegistrationNames",
                                MapBuilder.of("bubbled",  StickerListView.EVENT_ITEM_CLICK))
                )
                .put( StickerListView.EVENT_ITEM_LONG_CLICK,
                        MapBuilder.of("phasedRegistrationNames",
                                MapBuilder.of("bubbled",  StickerListView.EVENT_ITEM_LONG_CLICK)))
                .build();
    }

    @ReactProp(name = ViewProps.WIDTH)
    public void setWidth(StickerListView view, @Nullable int value) {
        Log.i(NAME,"setWidth :"+value);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null) {
            params.width = value;
            view.setLayoutParams(params);
        }
    }

    @ReactProp(name = ViewProps.HEIGHT)
    public void setHeight(StickerListView view, @Nullable int value) {
        Log.i(NAME,"setHeight :"+value);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null) {
            params.height = value;
            view.setLayoutParams(params);
        }
    }

    @ReactProp(name = "dataList")
    public void setDataList(StickerListView view, @Nullable ReadableArray dataList) {
        if (dataList == null) {
            return;
        }
        List<StickerInfo> sticikerList = new ArrayList<>();
        Log.i(NAME, "setDataList size:"+dataList.size());


        for (int i = 0; i < dataList.size(); i++) {
            ReadableMap readableMap = dataList.getMap(i);
            StickerInfo sticker = new StickerInfo();
            sticker.id = readableMap.hasKey("id") ? readableMap.getInt("id") : 0;
            sticker.name = readableMap.hasKey("name") ? readableMap.getString("name") : "";
            sticker.langName = readableMap.hasKey("langName") ? readableMap.getString("langName") : "";
            sticker.path = readableMap.hasKey("path") ? readableMap.getString("path") : "";
            sticker.thumb_sticker = readableMap.hasKey("thumb_sticker") ? readableMap.getString("thumb_sticker") : "";
            sticker.sticker_group_id = readableMap.hasKey("sticker_group_id") ? readableMap.getString("sticker_group_id") : "";
            sticker.create_time = readableMap.hasKey("create_time") ? (long) readableMap.getDouble("create_time") : 0;
            sticker.update_time = readableMap.hasKey("update_time") ? (long) readableMap.getDouble("update_time") : 0;
            sticker.use_time = readableMap.hasKey("use_time") ? (long) readableMap.getDouble("use_time") : 0;
            sticker.num = readableMap.hasKey("num") ? readableMap.getInt("num") : 0;
            sticker.md5 = readableMap.hasKey("md5") ? readableMap.getString("md5") : "";
            sticker.status = readableMap.hasKey("status") ? readableMap.getBoolean("status") : false;
            sticker.isDefault = readableMap.hasKey("isDefault") ? readableMap.getBoolean("isDefault") : false;
            sticikerList.add(sticker);
        }
        view.setStickerList(sticikerList);

    }

    @ReactProp(name = "isEdit")
    public void setEdit(StickerListView view, @Nullable boolean isEdit) {
        Log.i(NAME,"setEdit isEdit:"+isEdit);
        view.setEdit(isEdit);

    }

    @ReactProp(name = "scrollEnable")
    public void setScrollEnable(StickerListView view, @Nullable boolean scrollEnable) {
        Log.i(NAME,"setScrollEnable scrollEnable:"+scrollEnable);
        view.setScrollEnable(scrollEnable);

    }

    @ReactProp(name = "rotation")
    public void setRotation(StickerListView view, @Nullable int rotation) {
        Log.i(NAME,"setScrollEnable setRotation:"+rotation);
        view.setOrientation(rotation == 1 || rotation == 3
                ? Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_PORTRAIT);

    }


}
