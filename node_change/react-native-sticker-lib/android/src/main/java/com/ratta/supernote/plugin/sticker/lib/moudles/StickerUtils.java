package com.ratta.supernote.plugin.sticker.lib.moudles;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.nfc.Tag;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.ratta.supernote.plugin.sticker.lib.NativeStickerUtilsSpec;
import com.ratta.supernote.plugin.sticker.lib.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

@SuppressLint("Range")
public class StickerUtils extends NativeStickerUtilsSpec {
    public StickerUtils(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public void encodeStickerZip(String path, Promise promise) {

    }

    @Override
    public void decodeStickerZip(String path, Promise promise) {

    }

    @Override
    public void checkStickerZip(String path, Promise promise) {
        if (TextUtils.isEmpty(path)) {
            promise.resolve(false);
            return;
        }
        File file = new File(path);
        if (file.exists()) {
            try {
                new ZipFile(file);
                promise.resolve(true);
                return;
            } catch (ZipException e) {
                e.printStackTrace();
                promise.resolve(false);
                return;
            } catch (IOException e) {
                e.printStackTrace();
                promise.resolve(false);
                return;
            }
        }
        promise.resolve(false);
    }
}
