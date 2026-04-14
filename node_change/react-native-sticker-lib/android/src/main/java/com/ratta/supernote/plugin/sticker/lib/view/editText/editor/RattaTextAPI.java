package com.ratta.supernote.plugin.sticker.lib.view.editText.editor;

import android.graphics.PointF;

public interface RattaTextAPI {

    /**
     * Cursor location near the given point
     * 
     * @param pointF
     * @return
     */
    CursorRect getCursorLocation(PointF pointF);
}
