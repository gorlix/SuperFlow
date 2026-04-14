package com.ratta.supernote.plugin.sticker.lib.view.editText.editor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class EditorUtils {
    private static Bitmap selStartBitmap;
    private static Bitmap selEndBitmap;
    private static Bitmap cursorBitmap;

    private static final String res = "/plugins/51407189123aea95/res/drawable/";


    public static Bitmap getSelStartBitmap(Context context) {
        if (selStartBitmap == null || selStartBitmap.isRecycled()) {
            String resDir = context.getApplicationContext().getFilesDir().getAbsolutePath()
                    + res;
            selStartBitmap = BitmapFactory.decodeFile(resDir + "ic_sel_start.png");
        }
        return selStartBitmap;
    }

    public static Bitmap getSelEndBitmap(Context context) {
        if (selEndBitmap == null || selEndBitmap.isRecycled()) {
            String resDir = context.getApplicationContext().getFilesDir().getAbsolutePath()
                    + res;
            selEndBitmap = BitmapFactory.decodeFile(resDir + "ic_sel_end.png");
        }
        return selEndBitmap;
    }

    public static Bitmap getCursorBitmap(Context context) {
        if (cursorBitmap == null || cursorBitmap.isRecycled()) {
            String resDir = context.getApplicationContext().getFilesDir().getAbsolutePath()
                    + res;
            cursorBitmap = BitmapFactory.decodeFile(resDir + "ic_sel_handle.png");
        }
        return cursorBitmap;
    }
}
