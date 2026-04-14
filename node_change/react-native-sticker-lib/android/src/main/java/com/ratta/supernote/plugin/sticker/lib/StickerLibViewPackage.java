package com.ratta.supernote.plugin.sticker.lib;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewManager;
import com.facebook.react.views.textinput.ReactEditText;
import com.facebook.react.views.textinput.ReactTextInputManager;
import com.ratta.supernote.plugin.sticker.lib.view.editText.CustomTextInput;
import com.ratta.supernote.plugin.sticker.lib.view.dashView.DashViewManager;
import com.ratta.supernote.plugin.sticker.lib.view.groupList.StickerGroupListManager;
import com.ratta.supernote.plugin.sticker.lib.view.stickerList.StickerListManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StickerLibViewPackage implements ReactPackage {
    @NonNull
    @Override
    public List<NativeModule> createNativeModules(@NonNull ReactApplicationContext reactApplicationContext) {
        return Collections.emptyList();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @NonNull
    @Override
    public List<ViewManager> createViewManagers(@NonNull ReactApplicationContext reactApplicationContext) {
        List<ViewManager> list = new ArrayList<>();
        list.add(new ReactTextInputManager(){
            @Override
            public ReactEditText createViewInstance(ThemedReactContext context) {
                return new CustomTextInput(context);
            }
        });
        list.add(new StickerListManager());
        list.add(new DashViewManager());
        list.add(new StickerGroupListManager());
        return list;
    }
}
