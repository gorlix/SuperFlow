package com.plugin_sticker.view;

import androidx.annotation.NonNull;

import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;

public class TestStickerViewManager extends SimpleViewManager<TestStickerView> {
    private static final String NAME = "TestStickerView";
    @NonNull
    @Override
    public String getName() {
        return NAME;
    }

    @NonNull
    @Override
    protected TestStickerView createViewInstance(@NonNull ThemedReactContext themedReactContext) {
        return new TestStickerView(themedReactContext);
    }
}
