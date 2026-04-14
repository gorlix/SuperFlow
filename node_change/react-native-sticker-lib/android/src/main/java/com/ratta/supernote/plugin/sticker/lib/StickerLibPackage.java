package com.ratta.supernote.plugin.sticker.lib;

import androidx.annotation.Nullable;

import com.facebook.react.TurboReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.module.model.ReactModuleInfo;
import com.facebook.react.module.model.ReactModuleInfoProvider;
import com.ratta.supernote.plugin.sticker.lib.moudles.StickerUtils;
import com.ratta.supernote.plugin.sticker.lib.utils.Constant;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class StickerLibPackage extends TurboReactPackage {

    @Nullable
    @Override
    public NativeModule getModule(String name, ReactApplicationContext reactContext) {
        Constant.PLUGIN_PATH = reactContext.getFilesDir()+ File.separator +"plugins/51407189123aea95";
        if (name.equals(StickerUtils.NAME)) {
            return new StickerUtils(reactContext);
        }
        return null;
    }

    @Override
    public ReactModuleInfoProvider getReactModuleInfoProvider() {
        return () -> {
            final Map<String, ReactModuleInfo> moduleInfoMap = new HashMap<>();
            moduleInfoMap.put(
                    StickerUtils.NAME,
                    new ReactModuleInfo(
                            StickerUtils.NAME,
                            StickerUtils.NAME,
                            false,
                            false,
                            false,
                            true
                    )
            );
            return moduleInfoMap;
        };
    }

}