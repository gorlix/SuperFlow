package com.ratta.supernote.plugin.sticker.lib.view.editText.editor;

import androidx.annotation.Nullable;

/**
 * Cursor region info
 */
public class CursorRect {
    // Relative X coordinate inside the text input
    public float cursorX;
    // Relative Y coordinate inside the text input; top is the cursor top
    public float top;
    // Relative Y coordinate inside the text input; bottom is the cursor bottom
    public float bottom;

    public CursorRect(float cursorX, float top, float bottom) {
        this.cursorX = cursorX;
        this.top = top;
        this.bottom = bottom;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof CursorRect)) {
            return false;
        }
        CursorRect cursorRect = (CursorRect) obj;

        return Math.abs(cursorX - cursorRect.cursorX) <= 0.01f
                && Math.abs(top - cursorRect.top) <= 0.01f
                && Math.abs(bottom - cursorRect.bottom) <= 0.01f;
    }

    @Override
    public String toString() {
        return "CursorRect{" +
                "cursorX=" + cursorX +
                ", top=" + top +
                ", bottom=" + bottom +
                '}';
    }
}
