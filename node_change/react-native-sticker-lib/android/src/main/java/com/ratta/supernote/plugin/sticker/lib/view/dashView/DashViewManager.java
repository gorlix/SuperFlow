package com.ratta.supernote.plugin.sticker.lib.view.dashView;

import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewProps;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.ratta.supernote.plugin.sticker.lib.utils.Utils;

public class DashViewManager extends SimpleViewManager<DashView> {
    private static final String NAME = "DashView";

    private ThemedReactContext themedReactContext;

    @NonNull
    @Override
    public String getName() {
        return NAME;
    }

    @NonNull
    @Override
    protected DashView createViewInstance(@NonNull ThemedReactContext themedReactContext) {
        this.themedReactContext = themedReactContext;
        return new DashView(themedReactContext);
    }

    @ReactProp(name = ViewProps.WIDTH)
    public void setWidth(DashView view, @Nullable int value) {
        Log.i(NAME, "setWidth :" + value);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null) {
            params.width = value;
            view.setLayoutParams(params);
        }
    }

    @ReactProp(name = ViewProps.HEIGHT)
    public void setHeight(DashView view, @Nullable int value) {
        Log.i(NAME, "setHeight :" + value);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null) {
            params.height = value;
            view.setLayoutParams(params);
        }
    }

    @ReactProp(name = "dashWidth")
    public void setDashWidth(DashView view, @Nullable float dashWidth) {
        view.setDashWidth(Utils.dpToPx(themedReactContext, dashWidth));

    }

    @ReactProp(name = "gapWidth")
    public void setGapWidth(DashView view, @Nullable float gapWidth) {
        view.setGapWidth(Utils.dpToPx(themedReactContext, gapWidth));
    }


}
