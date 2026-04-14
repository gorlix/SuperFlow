package com.ratta.supernote.plugin.sticker.lib.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

public class Utils {

    public static int dpToPx(Context context, float dp) {
        Resources r = context.getResources();
        return (int) Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }
}
