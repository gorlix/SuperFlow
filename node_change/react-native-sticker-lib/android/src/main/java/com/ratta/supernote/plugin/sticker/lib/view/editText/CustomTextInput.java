package com.ratta.supernote.plugin.sticker.lib.view.editText;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Layout;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

import androidx.annotation.RequiresApi;

import com.facebook.react.views.textinput.ReactEditText;
import com.ratta.supernote.plugin.sticker.lib.view.editText.editor.CursorRect;
import com.ratta.supernote.plugin.sticker.lib.view.editText.editor.RattaEditor;
import com.ratta.supernote.plugin.sticker.lib.view.editText.editor.RattaTextAPI;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@RequiresApi(api = Build.VERSION_CODES.GINGERBREAD)
public class CustomTextInput extends ReactEditText {
    private final String TAG = "CustomTextInput";
    private int action = 1;
    private static final int MAX_TEXT_LENGTH = 150;

    private RattaEditor mRattaEditor;

    public CustomTextInput(Context context) {
        super(context);
        init();

    }

    private void init() {
        Log.i(TAG, "CustomTextInput new");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            disableDragHandles();
        }
        mRattaEditor = new RattaEditor(this);
        InputFilter[] filters = getFilters();
        InputFilter[] filtersNew = Arrays.copyOf(filters, filters.length + 1);
        filtersNew[filters.length] = new ByteLengthFilter(MAX_TEXT_LENGTH);
        setFilters(filtersNew);

        // Listen for text changes inside the input
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        /*
         * Editable editable = getText();
         * int len = editable.toString().getBytes().length;
         * if (len > MAX_TEXT_LENGTH) {
         * int selEndIndex = Selection.getSelectionEnd(editable);
         * String str = editable.toString();
         * // Truncate new string
         * String newStr = getWholeText(str, MAX_TEXT_LENGTH);
         * text = newStr;
         * setText(newStr);
         * editable = getText();
         * // Length of the new string
         * int newLen = editable.length();
         * // Old cursor position exceeds string length
         * if (selEndIndex > newLen) {
         * selEndIndex = editable.length();
         * }
         * // Set new cursor position
         * Selection.setSelection(editable, selEndIndex);
         * }
         */
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    private void disableDragHandles() {
        Log.i(TAG, "CustomTextInput disableDragHandles");
        // Disable long-press text selection
        setLongClickable(false);
        setTextIsSelectable(false);
        setCursorVisible(false);
        setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {

            }
        });
    }

    @Override
    public boolean onTextContextMenuItem(int id) {
        if (id == android.R.id.cut || id == android.R.id.copy) {
            return false; // Disable cut/copy
        }
        return super.onTextContextMenuItem(id);
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onPreDraw() {
        Log.i(TAG, "RattaEditText onPreDraw");
        // When dragging the start handle, scroll the dragged position into view
        if (mRattaEditor.isDraggingStart) {
            int curs = getSelectionStart();
            bringPointIntoView(curs);
            return true;
        }
        // No need to call onPreDraw at the end; otherwise it may scroll and cause
        // issues
        if (mRattaEditor.bringTextIntoView()) {
            return true;
        }
        return super.onPreDraw();
    }

    public void onDraw(Canvas canvas) {
        mRattaEditor.drawHighlight(canvas);
        super.onDraw(canvas);
        mRattaEditor.drawSelectedText(canvas);
        mRattaEditor.drawCursor(canvas);
    }

    public boolean onTouchEvent(MotionEvent event) {
        action = event.getAction();
        if (mRattaEditor.onTouchEvent(event)) {
            return true;
        }
        if (event.getAction() == 1 && this.getCompoundDrawables()[2] != null) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            boolean isInnerWidth = x > this.getWidth() - this.getTotalPaddingRight() + 24 && x < this.getWidth();
            boolean isInnerHeight = y >= 0 && y < this.getHeight();
            if (isInnerWidth && isInnerHeight) {
                this.requestFocus();
                this.setText("");
            }
        }

        return super.onTouchEvent(event);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        // Intercept the physical keyboard Enter key
        if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
            return true; // Stop event propagation
        }
        return super.onKeyPreIme(keyCode, event);
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (mRattaEditor != null) {
            mRattaEditor.onSelectionChanged(selStart, selEnd);
        }

    }

    public void cancelSelection() {
        if (mRattaEditor != null) {
            mRattaEditor.cancelSelection();
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mRattaEditor != null) {
            mRattaEditor.destroy();
        }
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (!hasFocus()) {
            cancelSelection();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        Log.i(TAG, "onWindowFocusChanged hasWindowFocus:" + hasWindowFocus);
        clearFocus();
        if (!hasWindowFocus) {
            cancelSelection();
        }
    }

    @SuppressLint("NewApi")
    private static String getWholeText(String text, int byteCount) {
        if (text != null && text.getBytes(StandardCharsets.UTF_8).length > byteCount) {
            char[] tempChars = text.toCharArray();
            int sumByte = 0;
            int charIndex = 0;
            for (int i = 0, len = tempChars.length; i < len; i++) {
                char itemChar = tempChars[i];
                // Determine byte length based on Unicode value
                if (itemChar <= 0x007F) {
                    sumByte += 1;
                } else if (itemChar <= 0x07FF) {
                    sumByte += 2;
                } else {
                    sumByte += 3;
                }
                if (sumByte > byteCount) {
                    charIndex = i;
                    break;
                }
            }
            return String.valueOf(tempChars, 0, charIndex);
        }
        return text;
    }

}
