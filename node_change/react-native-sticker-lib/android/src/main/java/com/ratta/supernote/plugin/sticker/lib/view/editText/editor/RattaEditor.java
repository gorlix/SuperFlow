package com.ratta.supernote.plugin.sticker.lib.view.editText.editor;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.ratta.supernote.plugin.sticker.lib.R;
import com.ratta.supernote.plugin.sticker.lib.view.dashView.DashVerticalView;
import com.ratta.supernote.plugin.sticker.lib.view.editText.MenuItemView;
import com.ratta.supernote.plugin.sticker.lib.view.editText.SelectionView;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

@SuppressLint("NewApi")
public class RattaEditor {
    private final String TAG = "RattaEditor";

    private final TextView mTextView;

    private GestureDetector mGestureDetector;

    private int mSelStart = 0;
    private int mSelEnd = 0;
    //  cursorposition
    private int mCursorIndex;
    private Paint mHighlightPaint;

    SelHandleView mStartHandleView;
    SelHandleView mEndHandleView;
    //  cursor
    SelHandleView mCursorHandleView;
    private boolean isDownStartHandle;
    private boolean isDownEndHandle;
    private boolean isDownCursorHandle;
    private boolean isDownSelectionMenu;

    //  whetherdragstart
    public boolean isDraggingStart = false;

    private LinearLayout mSelectionView;
    private List<View> mBaseSelectMenuList = new ArrayList<>();

    private View selectAllView;
    private View pasteView;

    //  selectionmenu
    private PopupWindow mSelectionMenu;

    ClipboardManager mClipboard;
    final int[] mTempCoords = new int[2];
    private int mPositionX, mPositionY;
    private int mPositionXOnScreen, mPositionYOnScreen;

    private final int[] mTextViewLocationOffset = new int[2];

    RattaEditorListener mRattaEditorListener;

    RattaEditorActionListener mRattaEditorActionListener;

    //  previousscroll
    private int lastScrollY = 0;
    private int lastScrollX = 0;

    //  cursorx
    private CursorRect mCursorRect;

    //  cursordisposable
    Disposable cursorHandleDisposable = null;

    //  previousgravity
    private int lastGravity;

    private Drawable selStartBitmap;
    private Drawable selEndBitmap;
    private Drawable cursorBitmap;

    //  previousclickposition
    private PointF lastDownPoint;
    boolean isCursorTail = false;

    private Paint mPaint;

    private int action = MotionEvent.ACTION_UP;

    private String beforeTextStr = null;
    private boolean isTextSelected = false;

    boolean selectionMenuEnabled = true;
    boolean cursorMenuEnabled = true;
    boolean cursorHandleEnabled = true;

    //  line heightarea bottomline
    private ConcurrentHashMap<Integer, HighLightLineInfo> mHighLightMap = new ConcurrentHashMap<Integer, HighLightLineInfo>() {
    };

    private final PublishSubject<CharSequence> textChangedSubject = PublishSubject.create();
    private final CompositeDisposable disposables = new CompositeDisposable();

    private interface SelHandleListener {
        boolean onSelOffsetChange(int selOffset, boolean isBorder);

        void onDownHandle(boolean isDown);

        //  click
        void onClickHandle();

        //  touchrelease
        void onTouchUp();

        //  updatemenuposition
        void onUpdateHandleMenuPosition(boolean isBorder);

    }

    public interface RattaEditorListener {

        /**
         *  whetherselectionmenu
         * 
         * @param isShow
         *  @param menuLocation selectionmenurelative to screenposition
         *  @param width menuwidth
         *  @param height menuheight
         *
         */
        void onShowSelection(boolean isShow, int[] menuLocation, int width, int height);
    }

    //  listentext
    public interface RattaEditorActionListener {
        void onCopy(String value);

        void onCut(String value);

        void onPaste(CharSequence value);
    }

    public RattaEditor(TextView textView) {
        this.mTextView = textView;
        init();
    }

    private void init() {
        initBitmap();

        //  drawcursorPaint
        mPaint = new Paint();
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.FILL);

        // mTextView.getLocationInWindow(mTextViewLocation);
        lastGravity = mTextView.getGravity();

        mClipboard = (ClipboardManager) mTextView.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        initSelectionMenu();
        initHandleView();
        mTextView.setHighlightColor(Color.TRANSPARENT);
        mHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        //  TextViewselection draw
        mTextView.setTextIsSelectable(true);
        //  disabletext
        mTextView.setLongClickable(false);

        initTextViewListener();
        Disposable disposable = textChangedSubject.throttleLast(500, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.io())
                .subscribe(s -> {
                    textChanged(s);
                });
        disposables.add(disposable);

    }

    //  initializeTextViewlisten
    private void initTextViewListener() {
        //  TextViewmenulisten
        mTextView.setCustomInsertionActionModeCallback(new ActionMode.Callback() {
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

        //  TextViewselectionmenulisten
        mTextView.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
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

        //  listenscroll
        mTextView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                int scrollY = mTextView.getScrollY();
                Log.i(TAG, "TextView addOnScrollChangedListener onScrollChanged scrollX:" + mTextView.getScrollX()
                        + ", scrollY:" + scrollY);
                //  position needupdate
                if ((lastScrollY != scrollY || lastScrollX != mTextView.getScrollX())
                        && mSelectionMenu != null && mSelectionMenu.isShowing()) {
                    updateSelectionMenuPosition();

                }
            }
        });
        //  listen listenselection
        mGestureDetector = new GestureDetector(mTextView.getContext(), new GestureDetector.SimpleOnGestureListener() {
            // @Override
            // public void onLongPress(@NonNull MotionEvent e) {
            // handleLongPress(e.getX(), e.getY());
            // }

        });
        listenTextViewLayoutChange();

        //  listenchange
        mTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //  needclearlastDownPoint preventcursorchange
                String str = s.toString();
                if (!TextUtils.equals(beforeTextStr, str)) {
                    lastDownPoint = null;
                }
                beforeTextStr = str;

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textChangedSubject.onNext(s);

            }

            @Override
            public void afterTextChanged(Editable s) {
                hideCursorHandle();
                dismissPopupView();

            }
        });
    }

    private void initBitmap() {
        selStartBitmap = mTextView.getContext().getResources().getDrawable(R.drawable.ic_sel_start);
        selEndBitmap = mTextView.getContext().getResources().getDrawable(R.drawable.ic_sel_end);
        cursorBitmap = mTextView.getContext().getResources().getDrawable(R.drawable.ic_sel_handle);

    }

    private void listenTextViewLayoutChange() {
        mTextView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop,
                    int oldRight, int oldBottom) {

                /*
                 * int[] tempLocation = new int[2];
                 * mTextView.getLocationInWindow(tempLocation);
                 * Log.i(TAG, "TextView onLayoutChange tempLocation:" + tempLocation[0] + "==" +
                 * tempLocation[1] + ", TextViewLocation："
                 * + mTextViewLocation[0] + "==" + mTextViewLocation[1]);
                 * if (tempLocation[0] != mTextViewLocation[0] || tempLocation[1] !=
                 * mTextViewLocation[1]) {
                 * mTextViewLocation = tempLocation;
                 * refreshSelectionView();
                 * }
                 */
                textChangedSubject.onNext(mTextView.getText());
            }
        });

        mTextView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mTextView.post(() -> {
                    int[] tempLocationScreen = new int[2];

                    mTextView.getLocationInWindow(tempLocationScreen);
                    Rect visibleRect = new Rect();
                    mTextView.getGlobalVisibleRect(visibleRect);
                    if (visibleRect.width() <= 0 || visibleRect.height() <= 0) {
                        return;
                    }
                    mTextView.getLocationOnScreen(mTempCoords);
                    mPositionXOnScreen = mTempCoords[0];
                    mPositionYOnScreen = mTempCoords[1];
                    mTextViewLocationOffset[0] = mPositionXOnScreen - mPositionX;
                    mTextViewLocationOffset[1] = mPositionYOnScreen - mPositionY;

                    if (tempLocationScreen[0] != mPositionX || tempLocationScreen[1] != mPositionY) {

                        Log.i(TAG, "TextView onPreDraw mTextViewLocationScreen:" + Arrays.toString(mTempCoords));
                        mPositionX = tempLocationScreen[0];
                        mPositionY = tempLocationScreen[1];
                        Log.i(TAG, "TextView onPreDraw mPositionX:" + mPositionX + "==" + mPositionY);
                        refreshSelectionView();

                    }
                });

                return true;
            }
        });
    }

    private MenuItemView getMenuItem(String text) {
        float fountSize = mTextView.getContext().getResources()
                .getDimension(com.ratta.supernote.pluginlib.R.dimen.dp_px_34)
                / mTextView.getResources().getDisplayMetrics().scaledDensity;
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        // layoutParams.leftMargin =
        // mTextView.getContext().getResources().getDimensionPixelSize(R.dimen.dp_px_24);
        // layoutParams.rightMargin =
        // mTextView.getContext().getResources().getDimensionPixelSize(R.dimen.dp_px_24);

        MenuItemView textView = new MenuItemView(mTextView.getContext());

        textView.setLayoutParams(layoutParams);
        textView.setText(text);
        textView.setGravity(Gravity.CENTER);
        textView.setEnabled(true);
        textView.setClickable(true);
        textView.setTextColor(Color.BLACK);
        textView.setPadding(
                mTextView.getContext().getResources()
                        .getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_24),
                0,
                mTextView.getContext().getResources()
                        .getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_24),
                0);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fountSize);
        return textView;
    }

    //  initialize
    private View getMenuDivideView() {
        DashVerticalView dashView = new DashVerticalView(mTextView.getContext());
        dashView.setStrokeWidth(mTextView.getContext().getResources()
                .getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_1));
        dashView.setDashWidth(mTextView.getContext().getResources()
                .getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_2));
        dashView.setGapWidth(mTextView.getContext().getResources()
                .getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_2));
        // dashView.setVertical(true);
        dashView.setLayoutParams(new LinearLayout.LayoutParams(
                mTextView.getContext().getResources()
                        .getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_1),
                mTextView.getContext().getResources()
                        .getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_26)));
        // dashView.setBackgroundColor(Color.BLACK);
        return dashView;
    }

    private void initSelectionMenu() {
        View menuDivideView = getMenuDivideView();
        initSelectMenuList();
        //  initializePopupWindow
        mSelectionView = new SelectionView(mTextView.getContext());
        for (int i = 0; i < mBaseSelectMenuList.size(); i++) {
            mSelectionView.addView(mBaseSelectMenuList.get(i));
            if (i < mBaseSelectMenuList.size() - 1) {
                mSelectionView.addView(menuDivideView);
            }
        }
        mSelectionView.setGravity(Gravity.CENTER);
        mSelectionView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                mTextView.getContext().getResources()
                        .getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_64)));
        mSelectionView.setOrientation(LinearLayout.HORIZONTAL);
        mSelectionView.setBackgroundColor(Color.WHITE);
        mSelectionMenu = new PopupWindow(mSelectionView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                mTextView.getContext().getResources()
                        .getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_64));
        mSelectionMenu.setWindowLayoutType(WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL);
        mSelectionMenu.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mSelectionMenu.setOutsideTouchable(false);
        mSelectionMenu.setFocusable(false); //  get
        mSelectionMenu.setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.i(TAG, "mSelectionMenu onInterceptTouchEvent touch:" + event.getAction());
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_OUTSIDE:
                        isDownSelectionMenu = isDownSelectionMenu(event);
                        // if (!isDownStartHandle && !isDownEndHandle && !isDownSelectionMenu) {
                        //// Selection.setSelection((Spannable) mTextView.getText(), mSelStart);
                        // dismissPopupView();
                        // }
                        break;

                }
                return false;
            }
        });

        //  PopupWindow
        mSelectionMenu.setAnimationStyle(0); //  disable
        mSelectionMenu.setClippingEnabled(false); //  disable
    }

    private void initSelectMenuList() {
        String lang = mTextView.getResources().getConfiguration().getLocales().get(0).toString();
        View copyView = getMenuItem(LangUtils.getString(LangUtils.text_copy, lang));
        copyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int selStart = Math.min(mTextView.getSelectionStart(), mTextView.getSelectionEnd());
                int selEnd = Math.max(mTextView.getSelectionStart(), mTextView.getSelectionEnd());

                String textContent = mTextView.getText().toString();

                String subStr = textContent.substring(selStart, selEnd);

                //  issue need
                ClipData clip = ClipData.newPlainText("text", subStr);
                mClipboard.setPrimaryClip(clip);
                Selection.setSelection((Spannable) mTextView.getText(), selEnd);
                dismissPopupView();
                if (mRattaEditorActionListener != null) {
                    mRattaEditorActionListener.onCopy(subStr);
                }
            }
        });
        mBaseSelectMenuList.add(copyView);

        if (mTextView instanceof EditText) {

            View cutView = getMenuItem(LangUtils.getString(LangUtils.text_cut, lang));
            cutView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int selStart = Math.min(mTextView.getSelectionStart(), mTextView.getSelectionEnd());
                    int selEnd = Math.max(mTextView.getSelectionStart(), mTextView.getSelectionEnd());
                    String subStr = "";
                    if (mTextView.getText() instanceof Editable) {
                        Editable editable = (Editable) mTextView.getText();
                        subStr = editable.toString().substring(mSelStart, mSelEnd);
                        editable.delete(selStart, selEnd);
                    } else {
                        String textContent = mTextView.getText().toString();
                        subStr = textContent.substring(mSelStart, mSelEnd);
                        StringBuilder newContent = new StringBuilder(textContent);
                        newContent.delete(mSelStart, mSelEnd);
                        mTextView.setText(newContent.toString());
                        if (mTextView instanceof EditText) {
                            ((EditText) mTextView).setSelection(selStart);
                        } else if (mTextView.getText() instanceof Spannable) {
                            Selection.setSelection((Spannable) mTextView.getText(),
                                    selStart);
                        }
                    }
                    //  issue need
                    ClipData clip = ClipData.newPlainText("text", subStr);
                    mClipboard.setPrimaryClip(clip);
                    dismissPopupView();
                    if (mRattaEditorActionListener != null) {
                        mRattaEditorActionListener.onCut(subStr);
                    }
                }
            });
            mBaseSelectMenuList.add(cutView);

            //  needwhethercontent
            pasteView = getMenuItem(LangUtils.getString(LangUtils.text_paste, lang));
            pasteView.setId(View.generateViewId());
            pasteView.setOnClickListener(new View.OnClickListener() {
                @SuppressLint("NewApi")
                @Override
                public void onClick(View v) {

                    String clipText = getClipboardText().toString();
                    if (TextUtils.isEmpty(clipText)) {
                        return;
                    }
                    boolean isMultiLine = (mTextView.getInputType() & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0;
                    if (mTextView.getMaxLines() <= 1 || !isMultiLine) {
                        clipText = clipText.replace("\n", " ");
                        clipText = clipText.replace("\r", " ");

                    }
                    int selStart = Math.min(mTextView.getSelectionStart(), mTextView.getSelectionEnd());
                    int selEnd = Math.max(mTextView.getSelectionStart(), mTextView.getSelectionEnd());
                    //  whether
                    if (mTextView.getText() instanceof Editable) {
                        Editable editable = (Editable) mTextView.getText();
                        editable.replace(selStart, selEnd, clipText);
                        textChangedSubject.onNext(editable);
                        int cursorOffset = selStart + clipText.length();
                        if (cursorOffset > editable.length()) {
                            cursorOffset = editable.length();
                        }
                        ((EditText) mTextView).setSelection(cursorOffset);

                    } else {
                        StringBuilder textContent = new StringBuilder(mTextView.getText());
                        textContent.delete(selStart, selEnd);
                        textContent.insert(selStart, clipText);
                        mTextView.setText(textContent.toString());
                        int cursorOffset = selStart + clipText.length();
                        if (cursorOffset > mTextView.getText().length()) {
                            cursorOffset = mTextView.getText().length();
                        }
                        //  cursorpositiontext bug
                        if (mTextView instanceof EditText) {
                            ((EditText) mTextView).setSelection(cursorOffset);
                        } else if (mTextView.getText() instanceof Spannable) {
                            Selection.setSelection((Spannable) mTextView.getText(),
                                    cursorOffset);
                        }
                    }
                    hideCursorHandle();
                    dismissPopupView();
                    if (mRattaEditorActionListener != null) {
                        mRattaEditorActionListener.onPaste(clipText);
                    }

                }
            });

        }
        selectAllView = getMenuItem(LangUtils.getString(LangUtils.text_select_all, lang));
        selectAllView.setId(View.generateViewId());
        selectAllView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelStart = 0;
                mSelEnd = mTextView.getText().length();

                hideCursorHandle();
                showSelHandleView();
                showSelectionMenu(false);
                mTextView.postInvalidate();

            }
        });
    }

    //  whethercontent
    private boolean checkClipboard() {
        if (mClipboard.hasPrimaryClip()
                && mClipboard.getPrimaryClip() != null
                && mClipboard.getPrimaryClip().getItemCount() > 0) {
            ClipData.Item item = mClipboard.getPrimaryClip().getItemAt(0);
            CharSequence text = item.getText();
            if (!TextUtils.isEmpty(text)) {
                return true;
            }

        }

        return false;
    }

    //  getcontent
    private CharSequence getClipboardText() {
        if (mClipboard.hasPrimaryClip()
                && mClipboard.getPrimaryClip() != null
                && mClipboard.getPrimaryClip().getItemCount() > 0) {
            ClipData.Item item = mClipboard.getPrimaryClip().getItemAt(0);
            CharSequence text = item.getText();
            if (!TextUtils.isEmpty(text)) {
                return text;
            }

        }
        return null;
    }

    private void initHandleView() {
        mCursorHandleView = new SelHandleView(mTextView.getContext(), cursorBitmap);
        mCursorHandleView.setHandleWidth(mTextView.getContext().getResources().getDimensionPixelSize(
                com.ratta.supernote.pluginlib.R.dimen.dp_px_32));
        mCursorHandleView.setHandleHeight(mTextView.getContext().getResources().getDimensionPixelSize(
                com.ratta.supernote.pluginlib.R.dimen.dp_px_56));
        mCursorHandleView.setCursorHandle(true);
        mCursorHandleView.setSelListener(new SelHandleListener() {
            @Override
            public boolean onSelOffsetChange(int selOffset, boolean isBorder) {
                if (selOffset == mCursorIndex) {
                    return false;
                }
                mCursorIndex = mSelStart = mSelEnd = selOffset;
                Selection.setSelection((Spannable) mTextView.getText(), mCursorIndex);
                mTextView.postInvalidate();
                return true;
            }

            @Override
            public void onDownHandle(boolean isDown) {
                isDownCursorHandle = true;
                disposeCursorHandle();
            }

            @Override
            public void onClickHandle() {
                if (cursorMenuEnabled) {
                    showSelectionMenu(true);
                    disposeCursorHandle();
                }

            }

            @Override
            public void onTouchUp() {
                isDownCursorHandle = false;
                if (!mSelectionMenu.isShowing()) {
                    cursorHandleTimeout();
                }
            }

            @Override
            public void onUpdateHandleMenuPosition(boolean isBorder) {
                if (mSelectionMenu.isShowing()) {
                    updateSelectionMenuPosition(isBorder);
                }

            }

        });

        mStartHandleView = new SelHandleView(mTextView.getContext(),
                selStartBitmap);
        mStartHandleView.setHandleWidth(mTextView.getContext().getResources().getDimensionPixelSize(
                com.ratta.supernote.pluginlib.R.dimen.dp_px_32));
        mStartHandleView.setHandleHeight(mTextView.getContext().getResources().getDimensionPixelSize(
                com.ratta.supernote.pluginlib.R.dimen.dp_px_56));
        mStartHandleView.setStart(true);
        mStartHandleView.setSelListener(new SelHandleListener() {
            @Override
            public boolean onSelOffsetChange(int selOffset, boolean isBorder) {
                //  startpositionendposition update
                if (selOffset == mSelEnd) {
                    return false;
                }
                Log.i(TAG, "StartHandleView onSelOffsetChange selOffset:" + selOffset);

                mSelStart = selOffset;
                isDraggingStart = mSelStart < mSelEnd;
                updateSystemSelOffset();
                updateSelHandleDrawable();
                mTextView.postInvalidate();

                return true;
            }

            @Override
            public void onDownHandle(boolean isDown) {
                isDownStartHandle = isDown;

            }

            @Override
            public void onClickHandle() {

            }

            @Override
            public void onTouchUp() {
                isDownStartHandle = false;
                isDraggingStart = false;

            }

            @Override
            public void onUpdateHandleMenuPosition(boolean isBorder) {
                updateSelectionMenuPosition(isBorder);

            }
        });

        mEndHandleView = new SelHandleView(mTextView.getContext(),
                selEndBitmap);
        mEndHandleView.setHandleWidth(mTextView.getContext().getResources().getDimensionPixelSize(
                com.ratta.supernote.pluginlib.R.dimen.dp_px_32));
        mEndHandleView.setHandleHeight(mTextView.getContext().getResources().getDimensionPixelSize(
                com.ratta.supernote.pluginlib.R.dimen.dp_px_56));
        mEndHandleView.setStart(false);
        mEndHandleView.setSelListener(new SelHandleListener() {
            @Override
            public boolean onSelOffsetChange(int selOffset, boolean isBorder) {
                //  startpositionendposition update
                if (selOffset == mSelStart) {
                    return false;
                }
                Log.i(TAG, "EndHandleView onSelOffsetChange:" + selOffset);
                mSelEnd = selOffset;
                isDraggingStart = mSelStart > mSelEnd;
                updateSystemSelOffset();
                updateSelHandleDrawable();
                mTextView.postInvalidate();

                return true;
            }

            @Override
            public void onDownHandle(boolean isDown) {
                isDownEndHandle = isDown;

            }

            @Override
            public void onClickHandle() {

            }

            @Override
            public void onTouchUp() {
                isDownEndHandle = false;
                isDraggingStart = false;
            }

            @Override
            public void onUpdateHandleMenuPosition(boolean isBorder) {
                updateSelectionMenuPosition(isBorder);

            }

        });
    }

    private void updateSelHandleDrawable() {
        if (mSelEnd > mSelStart) {
            if (!mStartHandleView.isStart) {
                mStartHandleView.setDrawable(
                        selStartBitmap);
                mStartHandleView.setStart(true);
                mStartHandleView.invalidate();
                mStartHandleView.update(false);
            }
            if (mEndHandleView.isStart) {
                mEndHandleView.setDrawable(selEndBitmap);
                mEndHandleView.setStart(false);
                mEndHandleView.invalidate();
                mEndHandleView.update(false);
            }

        } else {
            if (mStartHandleView.isStart) {
                mStartHandleView.setDrawable(
                        selEndBitmap);

                mStartHandleView.setStart(false);
                mStartHandleView.invalidate();
                mStartHandleView.update(false);
            }
            if (!mEndHandleView.isStart) {
                mEndHandleView.setDrawable(selStartBitmap);

                mEndHandleView.setStart(true);
                mEndHandleView.invalidate();
                mEndHandleView.update(false);
            }
        }
    }

    public void destroy() {
        hideCursorHandle();
        dismissPopupView();
        disposables.clear();
    }

    //  selection
    public void cancelSelection() {
        if (mTextView.isFocused()) {
            int selStart = Selection.getSelectionStart(mTextView.getText());
            Selection.setSelection((Spannable) mTextView.getText(), selStart);
        }
        Log.i(TAG, "cancelSelection");
        hideCursorHandle();
        dismissPopupView();

    }

    //  positionchange need
    public void refreshSelectionView() {
        if (mSelectionMenu.isShowing()) {
            showSelHandleView();
            updateSelectionMenuPosition();
        }
        //  cursor
        if (mCursorRect != null && mCursorHandleView.isShow()) {
            updateCursorPosition(new PointF(mCursorRect.cursorX, (mCursorRect.top + mCursorRect.bottom) / 2));
        }

    }

    //  shezhiEditorlisten
    public void setRattaEditorListener(RattaEditorListener rattaEditorListener) {
        this.mRattaEditorListener = rattaEditorListener;
    }

    public void setRattaEditorActionListener(RattaEditorActionListener mRattaEditorOpListener) {
        this.mRattaEditorActionListener = mRattaEditorOpListener;
    }

    //  update
    private void updateSystemSelOffset() {
        int selStart = Math.min(mSelStart, mSelEnd);
        int selEnd = Math.max(mSelStart, mSelEnd);
        int textLength = mTextView.getText().length();
        selStart = Math.max(0, Math.min(selStart, textLength));
        selEnd = Math.max(0, Math.min(selEnd, textLength));
        //  ensurestartendposition cursor
        if (selStart == selEnd) {
            dismissPopupView();
            return;
        }
        mTextView.requestFocus();
        Selection.setSelection((Spannable) mTextView.getText(), selStart, selEnd);
        isShowingSelectionMenu = false;
    }

    //  cursor 3s
    private void cursorHandleTimeout() {
        disposeCursorHandle();
        cursorHandleDisposable = Observable
                .just(1).delay(3, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Throwable {
                        isDownCursorHandle = false;
                        mCursorHandleView.dismiss();
                    }
                });
    }

    private void disposeCursorHandle() {
        if (cursorHandleDisposable != null && !cursorHandleDisposable.isDisposed()) {
            cursorHandleDisposable.dispose();
            cursorHandleDisposable = null;
        }
    }

    private void hideCursorHandle() {
        mCursorHandleView.dismiss();
        isDownCursorHandle = false;
        disposeCursorHandle();
    }

    private void showCursorHandleView(PointF pointF) {
        Log.i(TAG, "showCursorHandleView");
        mCursorHandleView.setSelOffset(mCursorIndex);
        boolean isBr = mCursorHandleView.isCursorHandleTail(pointF);
        mCursorHandleView.show(false, isBr);
        cursorHandleTimeout();

    }

    private void showSelHandleView() {
        //  comment
        if (mSelEnd == mSelStart) {
            dismissPopupView();
            return;
        }

        mEndHandleView.setSelOffset(mSelEnd);
        if (mEndHandleView.isStart) {
            mEndHandleView.setDrawable(selEndBitmap);
            mEndHandleView.invalidate();
            mEndHandleView.setStart(false);
        }
        mEndHandleView.show(false);
        mStartHandleView.setSelOffset(mSelStart);
        if (!mStartHandleView.isStart) {
            mStartHandleView.setDrawable(
                    selStartBitmap);
            mStartHandleView.invalidate();
            mStartHandleView.setStart(true);
        }
        mStartHandleView.show(false);
        mTextView.post(() -> {
            updateSystemSelOffset();

        });
        isDownEndHandle = isDownStartHandle = true;

    }

    //  getcursorpositionmenu
    private int[] getCursorMenuPosition(int menuWidth, int menuHeight, boolean isShow) {

        //  getcursorareaposition
        Rect cursorRect = mCursorHandleView.getHandleRect();

        int menuBottomMargin = mTextView.getContext().getResources()
                .getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_24);
        //  menu
        int menuHMargin = mTextView.getContext().getResources()
                .getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_40);
        //  screenwidth
        int screenWidth = getScreenWidth(mTextView.getContext());

        //  menuright
        int locationX = cursorRect.right + menuHMargin;

        //  menuleftscreenwidth,needleft
        if (locationX + menuWidth >= screenWidth - 10) {
            locationX = cursorRect.left - menuHMargin - menuWidth;
        }

        //  menulinebottom
        int locationY = cursorRect.top + menuBottomMargin;

        //  getTextViewscroll
        lastScrollY = mTextView.getScrollY();

        lastScrollX = mTextView.getScrollX();

        return new int[] {
                (int) (locationX),
                locationY
        };

    }

    //  getselectionmenuposition
    private int[] getSelectionPosition(int menuWidth, int menuHeight, boolean isShow) {
        //  cursor needcursormenu needcalculate
        if (mCursorHandleView.isShow()) {
            return getCursorMenuPosition(menuWidth, menuHeight, isShow);
        }
        lastScrollX = mTextView.getScrollX();
        //  getTextViewscroll
        lastScrollY = mTextView.getScrollY();
        int selStart = Math.min(mTextView.getSelectionStart(), mTextView.getSelectionEnd());
        int selEnd = Math.max(mTextView.getSelectionStart(), mTextView.getSelectionEnd());
        Layout layout = mTextView.getLayout();

        int startLine = getSelStartLine();
        int endLine = getSelEndLine();
        boolean isEndBorder = isSelectionBr();
        //  showgetposition needboundsissue
        /*
         * if (isShow) {
         * //  gettopline
         * int endPreLine = layout.getLineForOffset(selEnd - 1);
         * //  linetopline bounds
         * isEndBorder = endLine != endPreLine;
         * Log.i(TAG,"getSelectionPosition endLine:"+endLine+"=="+endPreLine);
         * endLine = endPreLine;
         * }
         */
        int lineEndBottom = layout.getLineBottom(endLine);
        int lineEndTop = layout.getLineTop(endLine);

        Log.i(TAG, "getSelectionPosition startLine:" + startLine + ", endLine:" + endLine);
        //
        int selLeft = (int) layout.getPrimaryHorizontal(selStart);
        Log.i(TAG, "getSelectionPosition selLeft:" + selLeft + "==" + layout.getLineLeft(startLine));
        int selRight = (int) (isEndBorder ? layout.getLineRight(endLine) : layout.getPrimaryHorizontal(selEnd));

        for (int line = startLine; line <= endLine; line++) {
            if (line != startLine) {
                //  getleftbounds leftbounds
                selLeft = (int) Math.min(selLeft, layout.getLineLeft(line));
            }
            if (line == endLine && !isEndBorder) {
                selRight = (int) Math.max(selRight, layout.getPrimaryHorizontal(selEnd));
            } else {
                selRight = (int) Math.max(selRight, layout.getLineRight(line));
            }
        }
        Log.i(TAG, "getSelectionPosition startLine:" + startLine + ", selLeft:" + selLeft);
        //  needleftrightscroll leftrightscroll menu
        selLeft -= lastScrollX;
        selRight -= lastScrollX;

        if (selLeft < 0) {
            selLeft = 0;
        }
        if (selRight > mTextView.getWidth() - mTextView.getCompoundPaddingLeft()
                - mTextView.getCompoundPaddingRight()) {
            selRight = mTextView.getWidth() - mTextView.getCompoundPaddingLeft() - mTextView.getCompoundPaddingRight();
        }

        float selectionWidth = selRight - selLeft;

        int selectionWidthOffset = (int) ((selectionWidth - menuWidth) / 2);

        int[] location = new int[] { mPositionX, mPositionY };
        // mTextView.getLocationInWindow(location);

        //  getendpositionx
        int xEnd = isEndBorder ? (int) layout.getLineRight(endLine) : (int) (layout.getPrimaryHorizontal(selEnd));
        //  relative to screenX
        int locationEndX = location[0] + xEnd + mTextView.getPaddingLeft() - lastScrollX;
        int locationEndY = location[1] + layout.getLineBottom(endLine) + getTextViewPaddingTop() - lastScrollY;

        int locationStartX = (int) (location[0] + layout.getPrimaryHorizontal(selStart) + mTextView.getPaddingLeft()
                - lastScrollX);
        int locationStartY = location[1] + layout.getLineBottom(startLine) + getTextViewPaddingTop() - lastScrollY;

        int menuBottomMargin = mTextView.getContext().getResources()
                .getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_24);

        int locationX = location[0] + selectionWidthOffset + mTextView.getPaddingLeft() + selLeft;
        if (locationX < 10) {
            locationX = 10;
        }
        int screenWidth = getScreenWidth(mTextView.getContext());

        if (locationX + menuWidth > screenWidth - 10) {
            locationX = screenWidth - 10 - menuWidth;
        }
        int locationY = 0;

        /*
         * //  visible needcalculate
         * if (isEndHandleVisible()) {
         * 
         * locationY = location[1] + lineBottom + menuBottomMargin;
         * 
         * // / menubottom
         * // / 1.highlightareawidthmenuwidth needtopheight,width need
         * // / 2.xpositionmenu
         * // / 3.topwidthmenux line
         * if (Math.abs(selectionWidth) < menuWidth
         * || (locationEndX > locationX && locationEndX < locationX + menuWidth)
         * || (locationEndX + mEndHandleView.getHandleWidth() > locationX &&
         * locationEndX + mEndHandleView.getHandleWidth() < locationX + menuWidth)) {
         * locationY += mStartHandleView.getHandleHeight();
         * }
         * if (locationY < 10) {
         * //  get
         * int endLineBottom = layout.getLineBottom(endLine);
         * locationY = location[1] + endLineBottom +
         * mTextView.getContext().getResources().getDimensionPixelSize(com.ratta.
         * supernote.note.plugincore.R.dimen.dp_px_80);
         * }
         * 
         * 
         * locationY = locationY - lastScrollY;
         * } else {
         * //  visible text
         * locationY = mTextView.getHeight() + location[1] + menuBottomMargin;
         * }
         */

        /*
         *  bottomcalculateselectionmenurelative to screeny bottom
         *  1.menuline
         *  2.linerelative to screenYTextView linevisible
         *  menuTextViewboundsbottom
         *  3.linetop ybottom
         *  4.needmenuwhether menuneedbottomheight
         *  5.top menuscreenvisible screentop topposition
         */
        //  getlineY
        int lineEndBottomWindowY = location[1] + lineEndBottom + getTextViewPaddingTop() - lastScrollY;
        //  top windows screen
        int lineEndTopWindowY = location[1] + lineEndTop + getTextViewPaddingTop() - lastScrollY;
        Log.i(TAG,
                "getSelectionPosition location[1]:" + location[1] + "=lineEndBottom=" + lineEndBottom
                        + "=getTextViewPaddingTop=" + getTextViewPaddingTop()
                        + "=lastScrollY=" + lastScrollY + "=getHeight=" + mTextView.getHeight());
        //  lineYTextView menuTexView
        if (lineEndBottomWindowY > location[1] + mTextView.getHeight()) {
            locationY = mTextView.getHeight() + location[1] + menuBottomMargin;
        } else if (lineEndBottomWindowY < location[1]) {
            locationY = location[1] + menuBottomMargin;
        } else {
            locationY = location[1] + lineEndBottom + menuBottomMargin + getTextViewPaddingTop() - lastScrollY;
        }

        Rect startRect = getStartHandleRect();
        Rect endRect = getEndHandleRect();
        Rect selectionRect = new Rect(locationX, locationY, locationX + menuWidth, locationY + menuHeight);
        Log.i(TAG, "getSelectionPosition startRect:" + startRect + "==" + endRect + "==" + selectionRect);

        //  comment
        if ((startRect != null && Rect.intersects(startRect, selectionRect))
                || (endRect != null && Rect.intersects(endRect, selectionRect))) {
            locationY += mEndHandleView.getHandleHeight();
        }
        //  positionwhetherscreenheight
        int screenHeight = getScreenHeight(mTextView.getContext());
        int yWindowsScreenOffset = mPositionYOnScreen - location[1];
        int locationYOnScreen = yWindowsScreenOffset + locationY;
        if (locationYOnScreen + menuHeight > screenHeight) {
            locationY = lineEndTopWindowY - menuHeight - menuBottomMargin;
        }

        return new int[] {
                locationX,
                locationY
        };
    }

    //  checkselectionwhethervisible
    private boolean isEndHandleVisible() {
        if (!mStartHandleView.isStart) {
            return mStartHandleView.getVisibility() == View.VISIBLE
                    && mStartHandleView.isShow();
        }
        if (!mEndHandleView.isStart) {
            return mEndHandleView.getVisibility() == View.VISIBLE
                    && mEndHandleView.isShow();
        }
        return false;
    }

    private boolean isStartHandleVisible() {
        if (mStartHandleView.isStart) {
            return mStartHandleView.getVisibility() == View.VISIBLE
                    && mStartHandleView.isShow();
        }
        if (mEndHandleView.isStart) {
            return mEndHandleView.getVisibility() == View.VISIBLE
                    && mEndHandleView.isShow();
        }
        return false;
    }

    private Rect getStartHandleRect() {
        if (mStartHandleView.isStart && mStartHandleView.getVisibility() == View.VISIBLE
                && mStartHandleView.isShow()) {
            return mStartHandleView.getHandleRect();
        }
        if (mEndHandleView.isStart && mEndHandleView.getVisibility() == View.VISIBLE
                && mEndHandleView.isShow()) {
            return mEndHandleView.getHandleRect();
        }
        return null;
    }

    private Rect getEndHandleRect() {
        if (!mStartHandleView.isStart && mStartHandleView.getVisibility() == View.VISIBLE
                && mStartHandleView.isShow()) {
            return mStartHandleView.getHandleRect();
        }
        if (!mEndHandleView.isStart && mEndHandleView.getVisibility() == View.VISIBLE
                && mEndHandleView.isShow()) {
            return mEndHandleView.getHandleRect();
        }
        return null;
    }

    public int getScreenWidth(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }

    public int getScreenHeight(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels;
    }

    //  comment
    private void addPasteView() {

        if (mTextView instanceof EditText) {

            //  whethercontent
            boolean isHasClip = checkClipboard();

            //  whether
            boolean isExistPaste = false;
            int pasteIndex = -1;
            for (int i = 0; i < mSelectionView.getChildCount(); i++) {
                if (mSelectionView.getChildAt(i).getId() == pasteView.getId()) {
                    isExistPaste = true;
                    pasteIndex = i;
                    break;
                }
            }
            if (isExistPaste) {
                //  content
                if (!isHasClip) {
                    mSelectionView.removeViewAt(pasteIndex);
                    //  comment
                    mSelectionView.removeViewAt(pasteIndex);
                }
                return;
            }
            //  content need
            if (!isHasClip) {
                return;
            }
            pasteIndex = 4;
            if (pasteIndex >= mSelectionView.getChildCount()) {
                pasteIndex = mSelectionView.getChildCount();
            }

            mSelectionView.addView(pasteView, pasteIndex);
            mSelectionView.addView(getMenuDivideView(), pasteIndex);
        }
    }

    private boolean isDownSelectionMenu(MotionEvent event) {
        return (event.getRawX() - lastSelectionMenuLocation[0]) < mSelectionView.getMeasuredWidth()
                && (event.getRawX() - lastSelectionMenuLocation[0]) > 0
                && (event.getRawY() - lastSelectionMenuLocation[1]) < mSelectionView.getMeasuredHeight()
                && (event.getRawY() - lastSelectionMenuLocation[1]) > 0;

    }

    int[] lastSelectionMenuLocation;

    //  selectionmenu
    private void showSelectionMenu(boolean isPateMenu) {

        mSelectionView.removeAllViews();
        if (isPateMenu) {
            //  menu ,needwhethercontent contentneed
            if (checkClipboard()) {
                mSelectionView.addView(pasteView);
            } else {
                //  textcontent content needmenu
                if (TextUtils.isEmpty(mTextView.getText())) {
                    return;
                }
            }
        } else {
            //  startend needmenu issue
            if (mSelStart == mSelEnd || (mSelStart > mTextView.length() && mSelEnd > mTextView.length())) {
                return;
            }
            //  selectionmenu
            for (int i = 0; i < mBaseSelectMenuList.size(); i++) {
                mSelectionView.addView(mBaseSelectMenuList.get(i));
                if (i < mBaseSelectMenuList.size() - 1) {
                    View menuDivideView = getMenuDivideView();
                    mSelectionView.addView(menuDivideView);
                }
            }
            addPasteView();
        }

        updateSelectionMenuPosition(true);
        if (mRattaEditorListener != null) {
            mRattaEditorListener.onShowSelection(true, lastSelectionMenuLocation, mSelectionView.getMeasuredWidth(),
                    mSelectionView.getMeasuredHeight());
        }
    }

    private void updateSelectionMenuPosition() {
        updateSelectionMenuPosition(false);
    }

    private void updateSelectionMenuPosition(boolean isShow) {
        isDownSelectionMenu = true;
        int textLength = mTextView.getText().length();

        //  whether
        boolean isSelectedAll = mSelStart == 0 && mSelEnd == textLength
                && textLength > 0;
        View endView = mSelectionView.getChildAt(mSelectionView.getChildCount() - 1);
        if (isSelectedAll) {
            if (endView == null || endView.getId() == selectAllView.getId()) {
                mSelectionView.removeView(endView);
                mSelectionView.removeViewAt(mSelectionView.getChildCount() - 1);
            }
        } else if (textLength > 0) {
            if (endView == null || endView.getId() != selectAllView.getId()) {
                mSelectionView.addView(getMenuDivideView());
                mSelectionView.addView(selectAllView);
            }
        }
        mSelectionView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        //  get
        int[] pos = getSelectionPosition(mSelectionView.getMeasuredWidth(),
                mTextView.getContext().getResources()
                        .getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_64),
                isShow);
        lastSelectionMenuLocation = pos;
        //  menu
        if (!mSelectionMenu.isShowing()) {
            mSelectionMenu.showAtLocation(mTextView, Gravity.START | Gravity.TOP, pos[0], pos[1]);
        } else {
            mSelectionMenu.update(pos[0], pos[1], -1, -1);
        }
    }

    private void dismissPopupView() {
        Log.i(TAG, "dismissPopupView");

        // mSelEnd = mSelStart = 0;
        mStartHandleView.dismiss();
        mEndHandleView.dismiss();
        if (!mCursorHandleView.isShow()) {
            dismissSelectionMenu();
        }

    }

    private void dismissSelectionMenu() {
        Log.i(TAG, "dismissSelectionMenu");
        if (mSelectionMenu.isShowing()) {
            mSelectionMenu.dismiss();
            if (mRattaEditorListener != null) {
                mRattaEditorListener.onShowSelection(false, lastSelectionMenuLocation,
                        mSelectionView.getMeasuredWidth(), mSelectionView.getMeasuredHeight());
            }
        }
    }

    public void setCursorRect(CursorRect cursorRect) {
        this.mCursorRect = cursorRect;
    }

    //  cursorwhether
    boolean isChangeCursor;

    //  getcursorposition
    private int getOffsetForPosition(PointF pointF) {
        Layout layout = mTextView.getLayout();
        int position = mTextView.getOffsetForPosition(pointF.x, pointF.y);
        if (position == -1)
            return position;
        if (TextUtils.isEmpty(mTextView.getText())) {
            return position;
        }
        //  line1,bottompositionneed
        //  cursor top need
        if (layout.getLineCount() > 1 && position + 1 <= mTextView.getText().length()) {
            int nextPositionLine = layout.getLineForOffset(position + 1);
            int positionLine = layout.getLineForOffset(position);
            //  bottompositionlinetopline needwhetherdrawcursor
            if (nextPositionLine != positionLine && !isPositionContainsTab(position)) {
                float positionX = (layout.getPrimaryHorizontal(position) + mTextView.getCompoundPaddingLeft());
                float lineRight = layout.getLineRight(positionLine) + mTextView.getCompoundPaddingLeft();
                if (pointF.x - positionX > lineRight - pointF.x) {
                    position += 1;
                }
            }
        }
        return position;

    }

    //  positionwhetherline
    public boolean isPositionContainsTab(int position) {
        String contentStr = mTextView.getText().toString();
        if (TextUtils.isEmpty(contentStr)) {
            return false;
        }
        String subStr;
        if (position >= contentStr.length()) {
            subStr = contentStr.substring(contentStr.length() - 1);

        } else {
            int start = Math.max(0, position - 1);
            subStr = contentStr.substring(start, position + 1);
        }
        return subStr.contains("\n") || subStr.contains("\r");
    }

    private void updateCursorPosition(PointF pointF) {
        if (mCursorHandleView.isShow()) {
            showCursorHandleView(pointF);
            isDownStartHandle = isDownEndHandle = false;
        }
    }

    //  cursor
    private void handleCursorClickEvent(MotionEvent event) {
        Log.i(TAG, "handleCursorClickEvent event:" + event.toString());
        //  selection
        /*
         * if(mSelEnd != mSelStart) {
         * return;
         * }
         */
        //  gettouch
        int offset = getOffsetForPosition(new PointF(event.getX(), event.getY()));
        Log.i(TAG, "handleCursorClickEvent offset:" + offset + "==" + mCursorRect);

        if (offset == -1)
            return;
        if (mCursorRect == null) {
            return;
        }

        //  EditTextcursorclick
        if (mTextView instanceof EditText) {
            if (TextUtils.isEmpty(mTextView.getText())) {
                if (mSelectionMenu.isShowing()) {
                    mSelectionMenu.dismiss();
                } else {
                    if (cursorMenuEnabled) {
                        showSelectionMenu(true);
                    }
                }
            } else {
                dismissPopupView();
                int cursorPosition = Selection.getSelectionStart(mTextView.getText());

                mSelStart = mSelEnd = mCursorIndex = offset;
                // showSelectionMenu(true);

                dismissSelectionMenu();
                isChangeCursor = false;
                if (offset != cursorPosition) {
                    Selection.setSelection(((EditText) mTextView).getText(), offset);
                }
                //  comment

                showCursorHandleView(new PointF(event.getX(), event.getY()));
                isDownStartHandle = isDownEndHandle = false;
                mTextView.postInvalidate();
            }

            // Log.i(TAG, "handleLongPress text
            // length"+content.length()+"=="+cursorPosition);
            // Log.i(TAG,"handleLongPress cursorX:"+ mCursorRect.cursorX+"=="+
            // mCursorRect.top+"=="+
            // mCursorRect.bottom+"=="+event.getX()+"=="+event.getY()+"=="+Math.abs(mCursorRect.cursorX
            // - event.getX()) );
            //
            //
            //  // needscroll leftxscroll
            // if (TextUtils.isEmpty(content)
            // || (((content.length() == cursorPosition && event.getX() >=
            // mCursorRect.cursorX)
            // || Math.abs(mCursorRect.cursorX - mTextView.getScrollX() - event.getX()) <=
            // 10)
            // && event.getY() > mCursorRect.top - mTextView.getScrollY()
            // && event.getY() < mCursorRect.bottom - mTextView.getScrollY())
            // ) {
            // mSelStart = mSelEnd = mCursorIndex = cursorPosition;
            //// showSelectionMenu(true);
            //  // need
            // showCursorHandleView();
            // isDownStartHandle = isDownEndHandle = false;
            // mTextView.invalidate();
            // isShowingSelectionMenu = false;
            // return;
            // } else {
            // hideCursorHandle();
            // }
        }
        isShowingSelectionMenu = false;

    }

    //  whetherselection
    boolean isShowingSelectionMenu = false;

    //  selectionclick
    private void handleSelectClickEvent(MotionEvent event) {

        //  gettouch
        int offset = mTextView.getOffsetForPosition(event.getX(), event.getY());
        if (offset == -1)
            return;
        //  content needmenu
        if (TextUtils.isEmpty(mTextView.getText())) {
            return;
        }
        isShowingSelectionMenu = true;
        //  comment
        // if (mTextView instanceof EditText) {
        // Layout layout = mTextView.getLayout();
        // String content = mTextView.getText().toString();
        // int cursorPosition = Selection.getSelectionStart(mTextView.getText());
        //
        // int cursorLine = layout.getLineForOffset(cursorPosition);
        // float lineTop = layout.getLineTop(cursorLine) + getTextViewPaddingTop();
        // float lineBottom = layout.getLineBottom(cursorLine) +
        // getTextViewPaddingTop();
        // Log.i(TAG, "handleLongPress text
        // length"+content.length()+"=="+cursorPosition);
        // Log.i(TAG,"handleLongPress cursorX:"+cursorX+"=="+
        // lineTop+"=="+lineBottom+"=="+event.getX()+"=="+event.getY()+"=="+Math.abs(cursorX
        // - event.getX()) );
        //
        // if (TextUtils.isEmpty(content)
        // || (content.length() == cursorPosition && event.getX()> cursorX ||
        // event.getY() > lineBottom)
        // || (Math.abs(cursorX - event.getX()) <= 10 && event.getY() > lineTop
        // && event.getY() < lineBottom)) {
        // mSelStart = mSelEnd = cursorPosition;
        // showSelectionMenu(true);
        // isDownStartHandle = isDownEndHandle = false;
        // mTextView.invalidate();
        // isShowingSelectionMenu = false;
        // return;
        // }
        // }

        hideCursorHandle();
        //  bounds
        int[] wordRange = getWordBoundaries(offset);
        mSelStart = wordRange[0];
        mSelEnd = wordRange[1];
        Log.i(TAG, "handleLongPress mSelStart:" + mSelStart + ", mSelEnd" + mSelEnd);
        //  comment
        showSelHandleView();
        showSelectionMenu(false);
        //  needupdate issue
        mTextView.postInvalidate();

    }

    private void updateSelectionPopWindows() {
        hideCursorHandle();
        mStartHandleView.setSelOffset(mSelStart);
        mEndHandleView.setSelOffset(mSelEnd);
        updateSelHandleDrawable();
        mStartHandleView.update(false);
        mEndHandleView.update(false);
        showSelectionMenu(false);

    }

    //  getendposition
    private int[] getWordBoundaries(int offset) {
        String text = mTextView.getText().toString();
        BreakIterator iterator = BreakIterator.getWordInstance(Locale.getDefault());
        iterator.setText(text);

        int start = iterator.preceding(offset);
        int end = iterator.following(offset);

        //  bounds
        if (start == BreakIterator.DONE)
            start = 0;
        if (end == BreakIterator.DONE)
            end = text.length();

        return new int[] { start, end };
    }

    public int getSelStart() {
        return mSelStart;
    }

    public int getSelEnd() {
        return mSelEnd;
    }

    private Path getUpdatedHighlightPath() {
        Path highlight = null;
        Layout layout = mTextView.getLayout();
        int selStart = Math.min(Selection.getSelectionStart(mTextView.getText()),
                Selection.getSelectionEnd(mTextView.getText()));
        int selEnd = Math.max(Selection.getSelectionStart(mTextView.getText()),
                Selection.getSelectionEnd(mTextView.getText()));
        if (selStart >= 0 && selEnd >= 0) {
            if (selStart == selEnd) {
                return null;
            } else {

                highlight = new Path();

                float spacingExtra = mTextView.getLineSpacingExtra(); //  line
                int startLine = layout.getLineForOffset(selStart);
                int endLine = layout.getLineForOffset(selEnd);
                //  gettopline
                int endPreLine = layout.getLineForOffset(selEnd - 1);
                //  linetopline bounds
                boolean isEndBorder = endLine != endPreLine;
                if (isEndBorder) {
                    //  boundsline need-1
                    endLine -= 1;
                }
                //  getline
                int finalLine = layout.getLineForOffset(mTextView.getText().length());
                //  getEditText
                int paddingLeft = mTextView.getPaddingLeft();
                int paddingTop = getTextViewPaddingTop();
                //  comment
                float skewOffset = getSkewOffset();

                TextPaint paint = layout.getPaint();

                String contentStr = mTextView.getText().toString();
                for (int line = startLine; line <= endLine; line++) {
                    int lineStart = layout.getLineStart(line);
                    int lineEnd = layout.getLineEnd(line);

                    if (selStart <= lineStart && selEnd >= lineEnd) {
                        HighLightLineInfo textLineInfo = mHighLightMap.get(line);
                        if (textLineInfo == null) {
                            textLineInfo = getHighLightInfo(contentStr, layout,
                                    line, finalLine, paddingLeft, paddingTop, spacingExtra);
                            mHighLightMap.put(line, textLineInfo);
                        }
                        highlight.addPath(textLineInfo.linePath);
                        continue;
                    }

                    boolean isRtlParagraph = layout.getParagraphDirection(line) == -1;
                    float lineTop = layout.getLineTop(line) + paddingTop;

                    float lineBottom = layout.getLineBottom(line) + paddingTop;

                    //  calculatelineselection
                    int lineSelStart = Math.max(selStart, lineStart);
                    int lineSelEnd = Math.min(selEnd, lineEnd);

                    //  ,need
                    float adjustedTop = lineTop;
                    //  textlineneedline
                    float adjustedBottom = (line == finalLine ? lineBottom : lineBottom - spacingExtra);

                    //  checklinewhetherline
                    boolean isLF = false;
                    if (lineEnd - lineStart == 1
                            && (mTextView.getText().charAt(lineStart) == '\n'
                                    || mTextView.getText().charAt(lineStart) == '\r')) {
                        isLF = true;
                    }
                    RectF lastRectF = null;
                    if (lineSelStart < lineSelEnd) {

                        for (int offset = lineSelStart; offset < lineSelEnd;) {
                            //  bounds prevent
                            if (offset >= contentStr.length()) {
                                break;
                            }

                            int codePoint = contentStr.codePointAt(offset);
                            int charCount = Character.charCount(codePoint);

                            //  ensure
                            int endOffset = Math.min(offset + charCount, lineEnd);
                            if (endOffset > contentStr.length()) {
                                endOffset = contentStr.length();
                            }

                            //  get
                            String currentChar = contentStr.substring(offset, endOffset);
                            //  getbottompreviousbottomwidth
                            float textWidth = 0;
                            if (TextUtils.equals(currentChar, "\t")) {
                                //  Layoutgetwidth
                                float startPos = layout.getPrimaryHorizontal(offset);
                                float endPos;
                                if (offset + 1 < contentStr.length()) {
                                    endPos = layout.getPrimaryHorizontal(offset + 1);
                                } else {
                                    //  lineposition
                                    endPos = layout.getLineRight(layout.getLineForOffset(offset));
                                }
                                textWidth = endPos - startPos;
                            } else {
                                textWidth = paint.measureText(currentChar);
                            }

                            boolean isRtlChar = layout.isRtlCharAt(offset);
                            float startX = 0;
                            if (isRtlParagraph) {
                                if (lineEnd - lineStart > 1
                                        && (TextUtils.equals(currentChar, "\n")
                                                || TextUtils.equals(currentChar, "\r"))) {
                                    break;
                                }
                                startX = isRtlChar ? layout.getPrimaryHorizontal(offset) - textWidth
                                        : layout.getSecondaryHorizontal(offset);
                            } else {
                                startX = !isRtlChar ? layout.getPrimaryHorizontal(offset)
                                        : layout.getSecondaryHorizontal(offset) - textWidth;
                                // / needtopline
                                // / top topgetpositiontop
                                // / Androidbug needgetSecondaryHorizontalgetposition position
                                if (offset > 0 && !isRtlChar) {
                                    boolean isRtlNextChar = layout.isRtlCharAt(offset - 1);
                                    if (isRtlNextChar) {
                                        float preCharWidth = paint
                                                .measureText(String.valueOf(contentStr.charAt(offset - 1)));
                                        float preX = layout.getSecondaryHorizontal(offset - 1) - preCharWidth;
                                        //  comment
                                        if (Math.abs(preX - startX) < textWidth / 2) {
                                            startX = layout.getSecondaryHorizontal(offset);
                                        }

                                    }
                                }
                            }
                            startX += mTextView.getCompoundPaddingLeft();

                            float endX = startX + textWidth + skewOffset;
                            if (TextUtils.equals(currentChar, "\n") || TextUtils.equals(currentChar, "\r")) {
                                endX = startX + getLFWidth();
                            }

                            //  getbounds
                            Rect bounds = new Rect();
                            mTextView.getPaint().getTextBounds(currentChar, 0, currentChar.length(), bounds);
                            if (bounds.left < 0) {
                                //  needtop topselection
                                startX += bounds.left;
                            }
                            RectF rectF = new RectF(
                                    startX,
                                    adjustedTop,
                                    endX,
                                    adjustedBottom);
                            //  needchecktoprectrightrectleftwhetherneed2px
                            if (lastRectF != null && Math.abs(rectF.left - lastRectF.right) <= 2) {
                                rectF.left = lastRectF.right;

                            }
                            lastRectF = rectF;

                            highlight.addRect(rectF,
                                    Path.Direction.CW);
                            //  bottom
                            offset += charCount;

                        }
                    }

                }

                mHighlightPaint.setColor(Color.BLACK);
                mHighlightPaint.setStyle(Paint.Style.FILL);
            }
        }
        return highlight;
    }

    /**
     *  getlinehighlightarea
     * 
     * @param layout
     * @param selStart
     * @param selEnd
     * @param line
     * @param startLine
     * @param endLine
     * @param finalLine
     * @param isEndBorder
     * @return
     */
    private RectF getLineHighlightRect(Layout layout, int selStart, int selEnd, int line, int startLine, int endLine,
            int finalLine, boolean isEndBorder) {
        int paddingLeft = mTextView.getPaddingLeft();
        int paddingTop = getTextViewPaddingTop();
        float spacingExtra = mTextView.getLineSpacingExtra(); //  line

        float lineTop = layout.getLineTop(line) + paddingTop;

        float lineBottom = layout.getLineBottom(line) + paddingTop;
        int lineStart = layout.getLineStart(line);
        int lineEnd = layout.getLineEnd(line);

        //  calculatelineselection
        int lineSelStart = Math.max(selStart, lineStart);
        int lineSelEnd = Math.min(selEnd, lineEnd);

        //  ,need
        float adjustedTop = lineTop;
        //  textlineneedline
        float adjustedBottom = line == finalLine ? lineBottom : lineBottom - spacingExtra;

        //  getlineleftrightbounds
        float left = line == startLine ? layout.getPrimaryHorizontal(selStart) : layout.getLineLeft(line);
        left = left + paddingLeft;

        //  scrollposition
        int scrollX = mTextView.getScrollX();

        //  ensureleftboundspaddingLeft scroll
        // left = Math.max(left, paddingLeft + scrollX);

        float right;
        if (line == endLine && !isEndBorder) {
            right = layout.getPrimaryHorizontal(selEnd) + paddingLeft;
        } else {
            right = layout.getLineRight(line) + paddingLeft;
        }
        right += getSkewOffset();
        //  ensurerightboundsTextViewwidthrightpadding
        // right = Math.min(right, mTextView.getWidth() -
        // mTextView.getCompoundPaddingRight() + scrollX);
        //  checklinewhetherline
        if (lineSelEnd - lineSelStart == 1
                && (mTextView.getText().charAt(lineSelStart) == '\n'
                        || mTextView.getText().charAt(lineSelStart) == '\r')) {
            right = left + getLFWidth();
        }

        //  comment
        Log.i(TAG, "Highlight area calculation - line:" + line +
                " paddingLeft:" + paddingLeft +
                " paddingTop:" + paddingTop +
                " left:" + left +
                " right:" + right);

        RectF rect = new RectF(
                left,
                adjustedTop,
                right,
                adjustedBottom);
        return rect;
    }

    /**
     *  getlinehighlightarea,linerightleft
     * 
     * @param layout
     * @param selStart
     * @param selEnd
     * @param line
     * @param startLine
     * @param endLine
     * @param finalLine
     * @param isEndBorder
     * @return
     */
    private void getLineRtlHighlightRect(Layout layout, int selStart, int selEnd, int line, int startLine, int endLine,
            int finalLine, Path highlight, boolean isEndBorder) {
        TextPaint paint = mTextView.getPaint();
        int paddingLeft = mTextView.getCompoundPaddingLeft();
        int paddingTop = getTextViewPaddingTop();
        float spacingExtra = mTextView.getLineSpacingExtra(); //  line

        //  comment
        float skewOffset = getSkewOffset();

        float lineTop = layout.getLineTop(line) + paddingTop;

        float lineBottom = layout.getLineBottom(line) + paddingTop;

        //  ,need
        float adjustedTop = lineTop;
        //  textlineneedline
        float adjustedBottom = line == finalLine ? lineBottom : lineBottom - spacingExtra;

        int lineStart = selStart;
        if (line != startLine) {
            lineStart = layout.getLineStart(line);
        }
        int lineEnd = selEnd;
        if (line != endLine) {
            lineEnd = layout.getLineEnd(line);
        }

        //  checklinewhetherline
        boolean isLF = false;
        if (lineEnd - lineStart == 1
                && (mTextView.getText().charAt(lineStart) == '\n'
                        || mTextView.getText().charAt(lineStart) == '\r')) {
            isLF = true;
        }
        if (lineStart < lineEnd) {
            String contentStr = mTextView.getText().toString();

            for (int offset = lineStart; offset < lineEnd; offset++) {
                float startX = (layout.isRtlCharAt(offset) ? layout.getPrimaryHorizontal(offset + 1)
                        : layout.getSecondaryHorizontal(offset)) + paddingLeft;
                String str = contentStr.substring(offset, offset + 1);
                //  getbottompreviousbottomwidth
                float textWidth = paint.measureText(str);
                float endX = startX + textWidth + skewOffset;
                if ((TextUtils.equals(str, "\n") || TextUtils.equals(str, "\r")) && !isLF) {
                    continue;
                }
                if (isLF) {
                    endX = startX + getLFWidth();
                }
                highlight.addRect(new RectF(
                        startX,
                        adjustedTop,
                        endX,
                        adjustedBottom),
                        Path.Direction.CW);

            }
        }
    }

    //  get needtop
    public float getSkewOffset() {
        /*
         * boolean isItalic = mTextView.getTypeface() != null &&
         * (mTextView.getTypeface().getStyle() & Typeface.ITALIC) != 0;
         * if (isItalic) {
         * float textSize = mTextView.getTextSize();
         * float italicAngle = -0.25f;
         * float skewOffset = textSize * Math.abs(italicAngle) * 0.5f;
         * return skewOffset;
         * }
         */
        float textSize = mTextView.getTextSize();
        float italicAngle = mTextView.getPaint().getTextSkewX();
        float skewOffset = textSize * Math.abs(italicAngle) * 0.5f;
        return skewOffset;
    }

    //  getlinewidth
    public float getLFWidth() {
        float textSize = mTextView.getTextSize();
        return textSize * 0.25f * 0.5f;
    }

    /**
     *  cursorposition,position
     */
    private void adjustCursorPosition(PointF pointF) {
        Layout layout = mTextView.getLayout();
        if (layout == null) {
            return;
        }
        Log.i(TAG, "adjustCursorPosition pointF:" + pointF.toString());
        //  getcursorposition,returnpositionposition
        int cursorPosition = mTextView.getSelectionStart();
        //  return textx
        float cursorX = (int) (layout.getPrimaryHorizontal(cursorPosition) + mTextView.getCompoundPaddingLeft());
        //  getline
        int cursorLine = layout.getLineForOffset(cursorPosition);
        lastDownPoint = pointF;
        //  line1,bottompositionneed
        //  cursor top need
        if (pointF != null && layout.getLineCount() > 1 && cursorPosition + 1 <= mTextView.getText().length()) {
            int nextPositionLine = layout.getLineForOffset(cursorPosition + 1);
            //  bottompositionlinetopline needwhetherdrawcursor
            if (nextPositionLine != cursorLine && !isPositionContainsTab(cursorPosition)) {
                float lineRight = layout.getLineRight(cursorLine);
                //  clickpositionright cursor
                if (pointF.x - cursorX > lineRight - pointF.x) {
                    isCursorTail = true;
                    //  clicktopcursor +1
                    if (mTextView instanceof EditText) {
                        ((EditText) mTextView).setSelection(cursorPosition + 1);
                    } else {
                        Selection.setSelection(((EditText) mTextView).getText(), cursorPosition + 1);
                    }

                }
            } else if (isCursorTail) {
                //  previouscursor needlinecheckwhether EditText
                float lineTop = layout.getLineTop(cursorLine) + getTextViewPaddingTop() - mTextView.getScrollY();
                float lineBottom = layout.getLineBottom(cursorLine) + getTextViewPaddingTop() - mTextView.getScrollY();
                if (pointF.y > lineTop && pointF.y < lineBottom) {
                    mTextView.postInvalidate();
                }
            }

        }

    }

    public boolean onTouchEvent(MotionEvent event) {

        action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            lastDownPoint = new PointF(event.getX(), event.getY());
            adjustCursorPosition(lastDownPoint);
        }
        if (onDoubleTapEvent(event)) {
            return true;
        }

        mGestureDetector.onTouchEvent(event);

        return false;
    }

    private long lastTapTime = 0;
    private float lastTapX = 0;
    private float lastTapY = 0;
    private static final int DOUBLE_TAP_TIMEOUT = 300;
    boolean isDoubleTap = false;

    //  comment
    public boolean onDoubleTapEvent(MotionEvent event) {
        //  comment
        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            long currentTime = System.currentTimeMillis();
            float x = event.getX();
            float y = event.getY();

            //  whether 300msposition
            if (selectionMenuEnabled && currentTime - lastTapTime < DOUBLE_TAP_TIMEOUT &&
                    Math.abs(x - lastTapX) < 30 &&
                    Math.abs(y - lastTapY) < 30) {

                //  default
                lastTapTime = 0;

                //  needline
                //  comment
                // int offset = getOffsetForPosition(x, y);
                // selectWordAt(offset);
                isDoubleTap = true;
                //  returntrue
                handleSelectClickEvent(event);
                return true;
            }

            //  click
            lastTapTime = currentTime;
            lastTapX = x;
            lastTapY = y;
        } else if (event.getAction() == MotionEvent.ACTION_CANCEL
                || event.getAction() == MotionEvent.ACTION_UP) {
            float currentX = event.getX();
            float currentY = event.getY();
            if (mCursorHandleView.isShow() && mSelectionMenu.isShowing()) {
                dismissSelectionMenu();
            }
            //  prevent
            hideCursorHandle();

            //  checkwhether
            if (!isDoubleTap && System.currentTimeMillis() - lastTapTime < 300
                    && Math.abs(currentX - lastTapX) < 30
                    && Math.abs(currentY - lastTapY) < 30) {
                //  clickcursor need
                handleCursorClickEvent(event);
            }
            isDoubleTap = false;
        }
        return false;
    }

    public void drawSelectedText(Canvas canvas) {
        if (!mTextView.isFocused()) {
            // cancelSelection();
            return;
        }
        int selStart = Math.min(Selection.getSelectionStart(mTextView.getText()),
                Selection.getSelectionEnd(mTextView.getText()));
        int selEnd = Math.max(Selection.getSelectionStart(mTextView.getText()),
                Selection.getSelectionEnd(mTextView.getText()));
        TextPaint paint = mTextView.getPaint();

        //  selectiontext drawselection
        if (selStart != selEnd) {
            paint.setColor(Color.WHITE);
            Layout layout = mTextView.getLayout();
            if (layout != null) {
                int startLine = layout.getLineForOffset(selStart);
                int endLine = layout.getLineForOffset(selEnd);
                //  gettopline
                int endPreLine = layout.getLineForOffset(selEnd - 1);
                //  linetopline bounds
                boolean isEndBorder = endLine != endPreLine;
                if (isEndBorder) {
                    //  boundsline need-1
                    endLine -= 1;
                }
                //  comment
                float skewOffset = getSkewOffset();
                float spacingExtra = mTextView.getLineSpacingExtra(); //  line
                int finalLine = layout.getLineForOffset(mTextView.getText().length());

                String contentStr = mTextView.getText().toString();
                char[] contentChars = contentStr.toCharArray();
                canvas.save();
                //  area paddingarea
                int paddingLeft = mTextView.getCompoundPaddingLeft();
                int paddingTop = getTextViewPaddingTop();
                int paddingRight = mTextView.getCompoundPaddingRight();
                int paddingBottom = mTextView.getCompoundPaddingBottom();

                //  area contentareadraw
                canvas.clipRect(
                        paddingLeft + mTextView.getScrollX(),
                        paddingTop + mTextView.getScrollY(),
                        mTextView.getWidth() - paddingRight + mTextView.getScrollX(),
                        mTextView.getHeight() - paddingBottom + mTextView.getScrollY());

                canvas.translate(mTextView.getCompoundPaddingLeft(), getTextViewPaddingTop());

                //  linetext
                for (int line = startLine; line <= endLine; line++) {
                    //  getline leftrightrightleft
                    int lineStart = layout.getLineStart(line);
                    int lineEnd = layout.getLineEnd(line);
                    String lineStr = contentStr.substring(lineStart, lineEnd);

                    if (selStart <= lineStart && lineEnd <= selEnd && !lineStr.contains("\t")) {

                        HighLightLineInfo textLineInfo = mHighLightMap.get(line);
                        if (textLineInfo == null || textLineInfo.start != lineStart || textLineInfo.end != lineEnd) {
                            textLineInfo = getHighLightInfo(contentStr, layout,
                                    line, finalLine, paddingLeft, paddingTop, spacingExtra);
                            mHighLightMap.put(line, textLineInfo);
                        }

                        canvas.drawPath(textLineInfo.linePath, mHighlightPaint);

                        canvas.drawText(contentStr, textLineInfo.start, textLineInfo.end, textLineInfo.x,
                                textLineInfo.y, paint);
                        continue;
                    }

                    //  getline leftrightrightleft
                    boolean isRtlParagraph = layout.getParagraphDirection(line) == -1;

                    //  calculatelineselection
                    int lineSelStart = Math.max(selStart, lineStart);
                    int lineSelEnd = Math.min(selEnd, lineEnd);

                    RectF lastRectF = null;
                    //  lineselectiontext
                    if (lineSelStart < lineSelEnd) {
                        float lineTop = layout.getLineTop(line);

                        float lineBottom = layout.getLineBottom(line);
                        //  ,need
                        float adjustedTop = lineTop;
                        //  textlineneedline
                        float adjustedBottom = (line == finalLine ? lineBottom : lineBottom - spacingExtra);

                        float startX;
                        float baseline = layout.getLineBaseline(line);

                        for (int offset = lineSelStart; offset < lineSelEnd;) {
                            //  bounds prevent
                            if (offset >= contentStr.length()) {
                                break;
                            }
                            int codePoint = contentStr.codePointAt(offset);
                            int charCount = Character.charCount(codePoint);
                            //  ensure
                            int endOffset = Math.min(offset + charCount, lineEnd);
                            if (endOffset > contentStr.length()) {
                                endOffset = contentStr.length();
                            }

                            //  get
                            String currentChar = contentStr.substring(offset, endOffset);
                            //  get
                            float charWidth = paint.measureText(currentChar);
                            if (TextUtils.equals(currentChar, "\t")) {
                                //  Layoutgetwidth
                                float startPos = layout.getPrimaryHorizontal(offset);
                                float endPos;
                                if (offset + 1 < contentStr.length()) {
                                    endPos = layout.getPrimaryHorizontal(offset + 1);
                                } else {
                                    //  lineposition
                                    endPos = layout.getLineRight(layout.getLineForOffset(offset));
                                }
                                charWidth = endPos - startPos;
                            }
                            //  needchecklinewhether getPrimaryHorizontalget getSecondaryHorizontalget
                            boolean isRtlChar = layout.isRtlCharAt(offset);
                            if (isRtlParagraph) {
                                if (TextUtils.equals(currentChar, "\n") || TextUtils.equals(currentChar, "\r")) {
                                    offset += charCount;
                                    continue;
                                }
                                startX = isRtlChar ? layout.getPrimaryHorizontal(offset) - charWidth
                                        : layout.getSecondaryHorizontal(offset);
                            } else {
                                startX = !isRtlChar ? layout.getPrimaryHorizontal(offset)
                                        : layout.getSecondaryHorizontal(offset) - charWidth;
                                // / needtopline
                                // / top topgetpositiontop
                                // / Androidbug needgetSecondaryHorizontalgetposition position
                                if (offset > 0 && !isRtlChar) {
                                    boolean isRtlNextChar = layout.isRtlCharAt(offset - 1);
                                    if (isRtlNextChar) {
                                        float preCharWidth = paint
                                                .measureText(String.valueOf(contentStr.charAt(offset - 1)));
                                        float preX = layout.getSecondaryHorizontal(offset - 1) - preCharWidth;
                                        //  comment
                                        if (Math.abs(preX - startX) < charWidth / 2) {
                                            startX = layout.getSecondaryHorizontal(offset);
                                        }

                                    }
                                }
                            }

                            //  gethighlightareastartposition
                            float highLightStartX = startX;
                            float highLightEndX = highLightStartX + charWidth + skewOffset;
                            if (TextUtils.equals(currentChar, "\n") || TextUtils.equals(currentChar, "\r")) {
                                highLightEndX = highLightStartX + getLFWidth();
                            }
                            //  getbounds
                            Rect bounds = new Rect();
                            mTextView.getPaint().getTextBounds(currentChar, 0, currentChar.length(), bounds);
                            if (bounds.left < 0) {
                                //  needtop topselection
                                highLightStartX += bounds.left;
                            }
                            RectF rectF = new RectF(
                                    highLightStartX,
                                    adjustedTop,
                                    highLightEndX,
                                    adjustedBottom);
                            //  needchecktoprectrightrectleftwhetherneed2px
                            if (lastRectF != null && Math.abs(rectF.left - lastRectF.right) <= 2) {
                                rectF.left = lastRectF.right;

                            }
                            lastRectF = rectF;
                            Path highlight = new Path();
                            highlight.addRect(rectF, Path.Direction.CW);
                            canvas.drawPath(highlight, mHighlightPaint);

                            canvas.drawText(currentChar, startX, baseline, paint);
                            //  bottom
                            offset += charCount;
                        }
                        /*
                         * //  rightleftneeddraw
                         * if (true) {
                         * 
                         * 
                         * } else {
                         * //  drawlineselectiontext
                         * canvas.drawText(mTextView.getText().toString().toCharArray(), lineSelStart,
                         * lineSelEnd - lineSelStart, startX, baseline, paint);
                         * }
                         */

                    }
                }
                canvas.restore();
            }
        }
    }

    public CursorRect getCursorLocation(PointF pointF) {

        Layout layout = mTextView.getLayout();
        //  getcursorposition,returnpositionposition
        int cursorPosition = mTextView.getSelectionStart();
        //  return textx
        float cursorX = (int) (layout.getPrimaryHorizontal(cursorPosition) + mTextView.getCompoundPaddingLeft());
        Log.i(TAG, "getCursorLocation cursorX:" + cursorX);
        //  getline
        int cursorLine = layout.getLineForOffset(cursorPosition);
        int pointLine = cursorLine;
        isCursorTail = false;
        lastDownPoint = pointF;
        if (pointF != null) {
            pointLine = layout.getLineForVertical((int) pointF.y);
            Log.i(TAG,
                    "getCursorLocation pointF:" + pointF.toString() + "=getLineCount=" + layout.getLineCount()
                            + "=cursorPosition=" + cursorPosition
                            + "=length=" + mTextView.getText().length() + "=cursorX=" + cursorX + "=getLineTop="
                            + (layout.getLineTop(cursorLine) + getTextViewPaddingTop()));

        }
        Log.i(TAG, "getCursorLocation cursorLine:" + cursorLine + "==" + pointLine);
        if (pointF != null && layout.getLineCount() > 1) {
            //  linecursorline line needcalculate,drag
            if (pointLine < cursorLine) {
                cursorLine = pointLine;
                float lineRight = layout.getLineRight(cursorLine) + mTextView.getCompoundPaddingLeft();
                cursorX = lineRight;
                isCursorTail = true;

            } else if (pointLine == cursorLine && cursorPosition + 1 <= mTextView.getText().length()) {
                //  bottompositionlinetopline needwhetherdrawcursor click
                int nextPositionLine = layout.getLineForOffset(cursorPosition + 1);
                if (nextPositionLine != cursorLine) {
                    float lineRight = layout.getLineRight(cursorLine) + mTextView.getPaddingLeft();
                    //  clickpositionright cursor
                    if (pointF.x - cursorX > lineRight - pointF.x) {
                        cursorX = lineRight;
                        isCursorTail = true;
                        cursorPosition += 1;
                        Selection.setSelection((Spannable) mTextView.getText(), cursorPosition);
                    }

                }
            }
        }

        //  line1,bottompositionneed
        //  cursor top need
        /*
         * if(pointF != null && layout.getLineCount() >1 && cursorPosition + 1 <=
         * mTextView.getText().length()
         * && !isPositionContainsTab(cursorPosition)) {
         * int nextPositionLine = layout.getLineForOffset(cursorPosition + 1);
         * Log.i(TAG,"getCursorLocation nextPositionLine:"+nextPositionLine+"=="+
         * cursorLine);
         * //  bottompositionlinetopline needwhetherdrawcursor
         * if(nextPositionLine != cursorLine) {
         * float lineRight = layout.getLineRight(cursorLine) +
         * mTextView.getCompoundPaddingLeft();
         * Log.i(TAG,"getCursorLocation lineRight:"+lineRight);
         * //  clickpositionright cursor
         * if(pointF.x - cursorX > lineRight - pointF.x) {
         * cursorX = lineRight;
         * isCursorTail = true;
         * cursorPosition += 1;
         * if(mTextView instanceof EditText) {
         * ((EditText)mTextView).setSelection(cursorPosition);
         * } else {
         * Selection.setSelection(((EditText) mTextView).getText(), cursorPosition);
         * }
         * }
         * }
         * //  bottomline positionneed
         * else if (pointF.x > cursorX && pointF.y < layout.getLineTop(cursorLine) +
         * getTextViewPaddingTop() - mTextView.getScrollY()) {
         * cursorX = layout.getLineRight(cursorLine - 1) +
         * mTextView.getCompoundPaddingLeft();
         * //  needtopline
         * cursorLine = cursorLine - 1;
         * isCursorTail = true;
         * }
         * 
         * }
         */

        float textSize = mTextView.getTextSize();
        //  getcursor
        int cursorWidth = (int) (textSize / 15);
        mPaint.setStrokeWidth(cursorWidth); //  comment

        //  cursor
        /*
         * if (isAtStart) {
         * if (isGravityRight) {
         * cursorX -= cursorWidth / 2;
         * //  content bottom cursor
         * if (TextUtils.isEmpty(mTextView.getText().toString()) && isItalic) {
         * cursorX -= skewOffset;
         * }
         * } else {
         * //  rightcursorwidth
         * cursorX += cursorWidth / 2;
         * }
         * } else if (isAtEnd) {
         * if (isGravityRight) {
         * cursorX += cursorWidth / 2;
         * 
         * } else {
         * //  leftcursorwidth
         * cursorX -= cursorWidth / 2;
         * }
         * }
         */

        float assentLine = layout.getLineAscent(cursorLine);
        float baseLine = layout.getLineBaseline(cursorLine);

        CursorRect cursorRect = new CursorRect(cursorX, baseLine + assentLine + getTextViewPaddingTop(),
                baseLine + layout.getLineDescent(cursorLine) + getTextViewPaddingTop());
        setCursorRect(cursorRect);
        return cursorRect;
    }

    /**
     *  drawcursor
     * 
     * @param canvas
     */
    public void drawCursor(Canvas canvas) {
        if (!mTextView.hasFocus()) {
            return;
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (!isTextSelected) {
                    CursorRect cursorPoint = getCursorLocation(lastDownPoint);

                    Log.i(TAG, "Edit onDraw cursorX:" + cursorPoint.cursorX);

                    canvas.drawLine(cursorPoint.cursorX, cursorPoint.top, cursorPoint.cursorX, cursorPoint.bottom,
                            mPaint);
                }
            }
                break;
        }
    }

    /**
     *  drawhighlightarea
     * 
     * @param canvas
     */
    public void drawHighlight(Canvas canvas) {
        int[] tempLocation = new int[] { mPositionX, mPositionY };
        // mTextView.getLocationInWindow(tempLocation);
        // drawTextViewIcon(canvas);
        if (!mTextView.isFocused()) {
            cancelSelection();
            return;
        }
        if (lastGravity != mTextView.getGravity()) {
            lastGravity = mTextView.getGravity();
            if (mCursorHandleView.isShow()) {
                hideCursorHandle();
                dismissSelectionMenu();
            }
            /*
             * if (mSelStart != mSelEnd) {
             * updateSelectionPopWindows();
             * }
             */

        }
        /*
         * //  gethighlight
         * Path highlight = getUpdatedHighlightPath();
         * if (highlight != null) {
         * canvas.save();
         * //  area paddingarea
         * int paddingLeft = mTextView.getCompoundPaddingLeft();
         * int paddingTop = getTextViewPaddingTop();
         * int paddingRight = mTextView.getCompoundPaddingRight();
         * int paddingBottom = mTextView.getCompoundPaddingBottom();
         * 
         * //  area contentareadraw
         * canvas.clipRect(
         * paddingLeft + mTextView.getScrollX(),
         * paddingTop + mTextView.getScrollY(),
         * mTextView.getWidth() - paddingRight + mTextView.getScrollX(),
         * mTextView.getHeight() - paddingBottom + mTextView.getScrollY());
         * 
         * //  drawhighlight draw
         * canvas.drawPath(highlight, mHighlightPaint);
         * canvas.restore();
         * }
         * TextPaint paint = mTextView.getPaint();
         * int originalColor = mTextView.getCurrentTextColor();
         * 
         * //  drawselectiontext
         * paint.setColor(originalColor);
         */

    }

    static final int LEFT = 0;
    static final int TOP = 1;
    static final int RIGHT = 2;
    static final int BOTTOM = 3;

    public void onSelectionChanged(int selStart, int selEnd) {
        isTextSelected = selStart != selEnd;
        boolean isTextVisible = mTextView.getVisibility() == View.VISIBLE;
        if (!isTextVisible || selStart != selEnd) {
            hideCursorHandle();
        }
        //  TextViewvisible selection selectionarea needupdateselection updateselectionarea
        if (isTextVisible && selStart != selEnd) {

            int tempSelStart = Math.min(mSelEnd, mSelStart);
            int tempSelEnd = Math.max(mSelEnd, mSelStart);
            if (tempSelStart != selStart || selEnd != tempSelEnd) {
                mSelStart = selStart;
                mSelEnd = selEnd;

            }
            if (mSelectionMenu.isShowing()) {
                updateSelectionPopWindows();
            }

        }

        if ((selStart == selEnd && !isShowingSelectionMenu) || !isTextVisible) {
            mSelEnd = mSelStart = selStart;
            if (mCursorIndex != mSelEnd) {
                hideCursorHandle();
            }
            mCursorIndex = mSelStart;
            dismissPopupView();
        }
        if (isTextVisible && selStart == selEnd && !isDownCursorHandle) {
            mTextView.postInvalidate();
        }
    }

    //  cursor TextViewscroll
    public boolean bringTextIntoView() {

        int selStart = Selection.getSelectionStart(mTextView.getText());
        int selEnd = Selection.getSelectionEnd(mTextView.getText());
        boolean isTail = false;
        //  cursor
        if (selEnd == selStart && mCursorRect != null) {
            isTail = mCursorHandleView.isCursorHandleTail();
            if (isTail) {
                mTextView.bringPointIntoView(selEnd - 1);
            }
            return isTail;
        }
        if (isDownStartHandle) {
            isTail = mStartHandleView.isSelHandleTail();
            if (isTail) {
                mTextView.bringPointIntoView(mSelStart - 1);
            }
            return isTail;
        }
        if (isDownEndHandle) {
            isTail = mEndHandleView.isSelHandleTail();
            if (isTail) {
                mTextView.bringPointIntoView(mSelEnd - 1);
            }
            return isTail;
        }
        return false;
    }

    //  getpaddingTop 0 getBaselinegetpaddingTop
    //  layout.getLineBaseline 0 getpaddingTop
    public int getTextViewPaddingTop() {
        if (mTextView == null) {
            return 0;
        }

        return mTextView.getBaseline() - mTextView.getLayout().getLineBaseline(0);
    }

    /**
     *  selectionmenuwhether
     * 
     * @param enabled
     */
    public void setSelectionMenuEnabled(boolean enabled) {
        selectionMenuEnabled = enabled;

    }

    /**
     *  cursormenuwhether
     * 
     * @param enabled
     */
    public void setCursorMenuEnabled(boolean enabled) {
        cursorMenuEnabled = enabled;

    }

    /**
     *  cursordragwhether
     * 
     * @param enabled
     */
    public void setCursorHandleEnabled(boolean enabled) {
        cursorHandleEnabled = enabled;
    }

    private int getSelStartLine() {
        if (mStartHandleView.isStart) {
            return mStartHandleView.getCurrentLine();
        } else {
            return mEndHandleView.getCurrentLine();
        }
    }

    private int getSelEndLine() {
        if (!mStartHandleView.isStart) {
            return mStartHandleView.getCurrentLine();
        } else {
            return mEndHandleView.getCurrentLine();
        }
    }

    //  whetherselectionbounds
    private boolean isSelectionBr() {
        if (!mStartHandleView.isStart) {
            return mStartHandleView.isBr;
        } else {
            return mEndHandleView.isBr;
        }
    }

    /**
     *  changelisten
     * 
     *  @param s changecontent
     *  @param start changestartposition topchangeposition position
     *  @param before
     *  @param count
     */
    @SuppressLint("CheckResult")
    private void textChanged(CharSequence s) {
        Log.i(TAG, "textChanged");
        updateDrawTextValue(s);

    }

    /**
     *  updateneedpositioncontent
     * 
     * @param s
     * @param start
     * @param before
     * @param count
     */
    private void updateDrawTextValue(CharSequence s) {
        Log.i(TAG, "updateDrawTextValue");

        if (mHighLightMap == null) {
            mHighLightMap = new ConcurrentHashMap<>();
        }
        mHighLightMap.clear();

        if (TextUtils.isEmpty(mTextView.getText())) {
            return;
        }
        Layout layout = mTextView.getLayout();
        if (layout == null) {
            return;
        }
        try {
            int lineCount = layout.getLineCount();

            int paddingLeft = mTextView.getCompoundPaddingLeft();
            float spacingExtra = mTextView.getLineSpacingExtra(); //  line
            int paddingTop = getTextViewPaddingTop();
            //  getline
            int finalLine = layout.getLineForOffset(mTextView.getText().length());

            String contentStr = mTextView.getText().toString();

            for (int line = 0; line < lineCount; line++) {
                HighLightLineInfo highLightLineInfo = getHighLightInfo(contentStr, layout, line, finalLine, paddingLeft,
                        paddingTop, spacingExtra);
                mHighLightMap.put(line, highLightLineInfo);

            }
            //  linechange needclearmap drawget
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            mHighLightMap.clear();
        }

    }

    private HighLightLineInfo getHighLightInfo(String contentStr, Layout layout, int line, int finalLine,
            int paddingLeft, int paddingTop, float spacingExtra) {
        Path linePath = new Path();
        HighLightLineInfo highLightLineInfo = new HighLightLineInfo();
        Paint paint = mTextView.getPaint();

        float lineLeft = layout.getLineLeft(line);
        float startX = lineLeft;
        float endX = layout.getLineRight(line) + getSkewOffset();
        float lineTop = layout.getLineTop(line);
        float lineBottom = layout.getLineBottom(line);
        float baseline = layout.getLineBaseline(line);
        int start = layout.getLineStart(line);
        int end = layout.getLineEnd(line);
        float LFWidth = getLFWidth();
        //  ,need
        float adjustedTop = lineTop;
        //  textlineneedline
        float adjustedBottom = (line == finalLine ? lineBottom : lineBottom - spacingExtra);

        String endChar = String.valueOf(contentStr.charAt(end - 1));
        //  toplinewidth
        if (TextUtils.equals(endChar, "\n")
                || TextUtils.equals(endChar, "\r")) {
            endX += LFWidth;
        }

        RectF rectF = new RectF(
                startX,
                adjustedTop,
                endX,
                adjustedBottom);
        linePath.addRect(rectF, Path.Direction.CW);

        highLightLineInfo.linePath = linePath;

        float drawTextX = lineLeft;
        if (layout.getParagraphDirection(line) == -1) {
            //  toplinewidth
            if (TextUtils.equals(endChar, "\n")
                    || TextUtils.equals(endChar, "\r")) {
                end = end - 1;
            }
        }
        highLightLineInfo.x = drawTextX;
        highLightLineInfo.y = baseline;
        highLightLineInfo.start = start;
        highLightLineInfo.end = end;
        return highLightLineInfo;
    }

    public int findEmojiStart(String text, int offset) {
        //  whetherposition
        if (offset > 0 && offset < text.length() &&
                Character.isLowSurrogate(text.charAt(offset)) &&
                Character.isHighSurrogate(text.charAt(offset - 1))) {
            return offset - 1; //  returnposition
        }
        return offset;
    }

    public void onSetFontSize(int unit, float size) {
        if (!TextUtils.isEmpty(mTextView.getText())) {
            textChanged(mTextView.getText());
        }

    }

    /**
     *  View
     */
    public class SelHandleView extends View {

        private final String TAG = "SelHandleView";

        protected Drawable mBitmap;
        private PopupWindow mContainer;
        int handleWidth;
        int handleHeight;

        private int selOffset;

        boolean isStart;
        private int lastScrollY = 0;
        private int lastScrollX = 0;

        SelHandleListener mSelListener;

        //  whethercursordrag selection
        boolean isCursorHandle = false;

        //  TextView
        private float xOnTextView = 0;
        private float yOnTextView = 0;

        //  bounds
        private float boundsLeft = 0;

        Paint mPaint = new Paint();

        //  whetherbounds
        private boolean isBr = false;

        private float padding;

        //  area
        private int width;
        private int height;

        public SelHandleView(Context context, Drawable bitmap) {
            super(context);
            mBitmap = bitmap;
            init();
        }

        private void init() {
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setFilterBitmap(true);
            mPaint.setDither(true);

            setWillNotDraw(false);
            handleWidth = mBitmap.getIntrinsicWidth();
            handleHeight = mBitmap.getIntrinsicHeight();
            padding = mTextView.getResources().getDimension(com.ratta.supernote.pluginlib.R.dimen.dp_px_10);
            //  areaneedtoppadding
            width = (int) (handleWidth + padding);
            height = (int) (handleHeight + padding);
            mContainer = new PopupWindow(this, ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            mContainer.setContentView(this);
            mContainer.setWindowLayoutType(WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL);
            mContainer.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            mContainer.setOutsideTouchable(true);
            mContainer.setFocusable(false);
            //  PopupWindow
            mContainer.setAnimationStyle(0); //  disable
            mContainer.setClippingEnabled(false); //  disable
            mContainer.setTouchInterceptor(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    boolean isDown = isDownHandle(event);
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_POINTER_DOWN:
                            if (event.getPointerCount() > 1) {
                                return false;
                            }
                            /*
                             * case MotionEvent.ACTION_DOWN:
                             * if (isDown) {
                             * return false;
                             * }
                             * 
                             * break;
                             */

                        case MotionEvent.ACTION_OUTSIDE:
                            if (mSelListener != null) {
                                mSelListener.onDownHandle(false);
                                mSelListener.onTouchUp();
                            }
                            //  click returntrue
                            return true;

                    }
                    return false;
                }
            });

            mTextView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
                @Override
                public void onScrollChanged() {
                    int scrollY = mTextView.getScrollY();
                    //  checkpreviousscrollwhether
                    if (mContainer.isShowing() && (lastScrollY != scrollY || lastScrollX != mTextView.getScrollX())) {
                        //  needcursorcursor
                        if (isCursorHandle) {
                            show(false,
                                    isCursorHandleTail(new PointF(mCursorRect.cursorX,
                                            (mCursorRect.top + mCursorRect.bottom) / 2)));
                        } else {
                            show(false);
                        }

                    }
                }
            });
        }

        public void setDrawable(Drawable bitmap) {
            this.mBitmap = bitmap;

        }

        public void setStart(boolean start) {
            if (start != isStart) {
                isStart = start;
            }

        }

        public boolean isStart() {
            return isStart;
        }

        public void setCursorHandle(boolean cursorHandle) {
            isCursorHandle = cursorHandle;
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            Log.i(TAG, "handleView onDraw");

            mBitmap.setBounds(0, 0, handleWidth, handleHeight);
            mBitmap.draw(canvas);
        }

        //  getarea
        public Rect getHandleRect() {
            return new Rect(lastLocationX, lastLocationY,
                    lastLocationX + handleWidth, lastLocationY + handleHeight);
        }

        private int lastLocationX;
        private int lastLocationY;

        public void show(boolean isTop) {

            boolean isBr = false;
            //  cursor selectionposition positionlinetoppositionlinebounds
            if (!isStart && selOffset > 0 /* && !isPositionContainsTab(selOffset) */) {
                Layout layout = mTextView.getLayout();
                int selLine = layout.getLineForOffset(selOffset);
                if (layout.getParagraphDirection(selLine) == Layout.DIR_LEFT_TO_RIGHT) {
                    int selPreLine = layout.getLineForOffset(selOffset - 1);
                    isBr = selLine != selPreLine;
                }
            }
            update(true, isTop, isBr);

        }

        public void show(boolean isTop, boolean isBr) {
            update(true, isTop, isBr);
        }

        public boolean isCursorHandleTail() {
            if (!isCursorHandle) {
                return false;
            }
            if (selOffset >= mTextView.getText().length()) {
                return false;
            }
            if (selOffset > 0 && !isPositionContainsTab(selOffset)) {
                Layout layout = mTextView.getLayout();
                if (layout == null) {
                    return false;
                }
                int selLine = layout.getLineForOffset(selOffset);
                int selPreLine = layout.getLineForOffset(selOffset - 1);

                if (selLine != selPreLine) {
                    //  needscroll calculateissue
                    float lineTop = layout.getLineTop(selLine);
                    float lineBottom = layout.getLineBottom(selLine);
                    if (yOnTextView > lineTop && yOnTextView < lineBottom) {
                        return false;
                    } else {
                        return true;
                    }
                }
            }
            return false;
        }

        //  cursorwhether
        public boolean isCursorHandleTail(PointF pointF) {
            if (!isCursorHandle) {
                return false;
            }
            if (selOffset >= mTextView.getText().length()) {
                return false;
            }
            //  cursor selectionposition positionlinetoppositionlinebounds
            if (selOffset > 0 && !isPositionContainsTab(selOffset)) {
                Layout layout = mTextView.getLayout();
                int selLine = layout.getLineForOffset(selOffset);
                int selPreLine = layout.getLineForOffset(selOffset - 1);
                if (selLine != selPreLine) {
                    //  needscroll calculateissue
                    float lineTop = layout.getLineTop(selLine) + getTextViewPaddingTop() - mTextView.getScrollY();
                    float lineBottom = layout.getLineBottom(selLine) + getTextViewPaddingTop() - mTextView.getScrollY();

                    if (pointF.y > lineTop && lineBottom > pointF.y) {
                        return false;
                    } else {
                        return true;
                    }
                }
            }
            return false;
        }

        private void update(boolean isShow, boolean isTop, boolean isBr) {

            int[] selLocation = getSelHandlePosition(selOffset, isTop, isBr, true);

            int xOffset = 0;
            if (isCursorHandle) {
                xOffset = handleWidth / 2;
            } else if (mBitmap == selStartBitmap) {
                xOffset = handleWidth;
            }
            lastLocationX = selLocation[0] - xOffset;

            lastLocationY = selLocation[1] + (isTop ? -handleHeight : 0);
            //  cursorneed
            if (!isCursorHandle) {
                boolean isHandleInTextView = isHandleInTextView(new float[] { lastLocationX, lastLocationY });
                setVisibility(isHandleInTextView ? VISIBLE : GONE);
            }

            Log.i(TAG, "update lastLocationX:" + lastLocationX + ", lastLocationY" + lastLocationY);

            if (mContainer.isShowing()) {
                mContainer.update(lastLocationX, lastLocationY, -1, -1);
            } else if (isShow) {
                mContainer.showAtLocation(mTextView, Gravity.START | Gravity.TOP, lastLocationX, lastLocationY);
            }
        }

        public void update(boolean isTop) {
            update(false, isTop, false);
        }

        private void updatePosition(int x, int y) {

            lastLocationX = x;
            lastLocationY = y;
            Log.i(TAG, "update lastLocationX:" + lastLocationX + ", lastLocationY" + lastLocationY);
            //  cursorneed
            if (!isCursorHandle) {
                boolean isHandleInTextView = isHandleInTextView(new float[] { x, y });
                setVisibility(isHandleInTextView ? VISIBLE : GONE);
            }
            if (mContainer.isShowing()) {
                mContainer.update(x, y, -1, -1);
            } /*
               * else {
               * mContainer.showAtLocation(mTextView, Gravity.START | Gravity.TOP, x, y);
               * }
               */
        }

        public void dismiss() {
            if (mContainer != null && mContainer.isShowing()) {
                mContainer.dismiss();
                lastLine = -1;
            }
        }

        public boolean isShow() {
            if (mContainer == null) {
                return false;
            }
            return mContainer.isShowing();
        }

        //  whetherTextView
        private boolean isHandleInTextView(float[] position) {
            if (position == null || position.length < 2) {
                return false;
            }
            Layout layout = mTextView.getLayout();
            if (isStart && layout.getParagraphDirection(lastLine) != -1) {
                position[0] += handleWidth;
            }
            int[] textViewPosition = new int[] { mPositionX, mPositionY };
            // mTextView.getLocationInWindow(textViewPosition);
            Log.i(TAG, "isHandleInTextView position:" + Arrays.toString(position) + "=="
                    + Arrays.toString(textViewPosition) + "==" + mTextView.getCompoundPaddingLeft());
            return position[0] >= textViewPosition[0] + mTextView.getCompoundPaddingLeft() + boundsLeft
                    && position[0] <= textViewPosition[0] + mTextView.getWidth() - mTextView.getCompoundPaddingRight()
                    && position[1] >= textViewPosition[1] + getTextViewPaddingTop()
                    && position[1] <= textViewPosition[1] + mTextView.getHeight() - mTextView.getPaddingBottom();
        }

        public int getHandleWidth() {
            return handleWidth;
        }

        public int getHandleHeight() {
            return handleHeight;
        }

        public void setHandleWidth(int handleWidth) {
            this.handleWidth = handleWidth;
            //  areaneedtoppadding
            width = (int) (handleWidth + padding);
        }

        public void setHandleHeight(int handleHeight) {
            this.handleHeight = handleHeight;
            //  areaneedtoppadding
            height = (int) (handleHeight + padding);
        }

        public void setSelOffset(int selOffset) {
            this.selOffset = selOffset;
        }

        public void setSelListener(SelHandleListener mSelListener) {
            this.mSelListener = mSelListener;
        }

        private int lastTouchX;
        private int lastTouchY;

        private long lastTouchTime = 0;

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int[] windowLocation = convertRawToWindow(event);
            int rawX = windowLocation[0];
            int rawY = windowLocation[1];
            switch (event.getAction()) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    if (event.getPointerCount() > 1) {
                        return false;
                    }
                case MotionEvent.ACTION_DOWN:
                    boolean isDown = isDownHandle(event);
                    if (mSelListener != null) {
                        mSelListener.onDownHandle(true);
                    }
                    /*
                     * if (!isDown) {
                     * return false;
                     * }
                     */
                    lastTouchX = rawX;
                    lastTouchY = rawY;
                    lastTouchTime = System.currentTimeMillis();
                    return true;

                case MotionEvent.ACTION_MOVE:
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:

                    int deltaX = rawX - lastTouchX;
                    int deltaY = rawY - lastTouchY;

                    moveHandle(deltaX, deltaY);

                    if (mSelListener != null) {
                        if (event.getAction() == MotionEvent.ACTION_UP
                                || MotionEvent.ACTION_CANCEL == event.getAction()) {
                            if (System.currentTimeMillis() - lastTouchTime <= 200
                                    && deltaX < 10 && deltaY < 10) {
                                mSelListener.onClickHandle();
                            }
                            mSelListener.onTouchUp();

                        }
                    }

                    return true;
            }

            return super.onTouchEvent(event);
        }

        private int[] convertRawToWindow(MotionEvent event) {
            int[] windowLocation = new int[2];

            //  calculate
            windowLocation[0] = (int) event.getRawX() - (mPositionXOnScreen - mPositionX);
            windowLocation[1] = (int) event.getRawY() - (mPositionYOnScreen - mPositionY);

            return windowLocation;
        }

        //  whetherclicktop
        private boolean isDownHandle(MotionEvent event) {
            return event.getRawX() - lastLocationX < handleWidth
                    && event.getRawX() - lastLocationX > 0
                    && event.getRawY() - lastLocationY < handleHeight
                    && event.getRawY() - lastLocationY > 0;
        }

        int lastLine = -1;

        private void moveHandle(int deltaX, int deltaY) {
            float moveOffset = mTextView.getTextSize() / 3;
            if (Math.abs(deltaX) <= moveOffset && Math.abs(deltaY) <= moveOffset) {
                return;
            }
            Layout layout = mTextView.getLayout();
            int[] textViewLocation = new int[] { mPositionX, mPositionY };
            // mTextView.getLocationInWindow(textViewLocation);

            int textViewX = textViewLocation[0] + mTextView.getCompoundPaddingLeft();
            int textViewY = textViewLocation[1] + getTextViewPaddingTop();

            //  clicktextposition
            int xInTextView = lastTouchX - textViewX + deltaX + mTextView.getScrollX();
            //  bounds ensureline
            int lineCount = layout.getLineCount();
            lastLine = lastLine < 0 || lastLine >= lineCount ? layout.getLineForOffset(selOffset) : lastLine;
            int lineTop = layout.getLineTop(lastLine);
            //  need lineTop + textViewY - lastLocationY calculatehandlelefttopline heightposition
            int yInTextView = lastTouchY + (lineTop + textViewY - lastLocationY) - textViewY + deltaY;
            Log.i(TAG, "moveHandle lastLine:" + lastLine + "=lintTop=" + lineTop + "=textViewY=" + textViewY
                    + "=lastLocationY=" + lastLocationY + "=lastTouchY=" + lastTouchY);
            //  getline
            int line = layout.getLineForVertical(yInTextView);
            Log.i(TAG, "moveHandle line:" + line + "=yInTextView=" + yInTextView
                    + "=getLineTop=" + layout.getLineTop(lastLine)
                    + "=getLineBottom=" + layout.getLineBottom(lastLine));

            yOnTextView = (float) (layout.getLineTop(lastLine) + layout.getLineBottom(lastLine)) / 2;

            //  getline
            int offset = layout.getOffsetForHorizontal(line, xInTextView);

            //  whetherline
            float lineRight = layout.getLineRight(line);
            Log.i(TAG, "moveHandle lineRight:" + lineRight + "==" + xInTextView);
            boolean isBr = false;
            if (layout.getParagraphDirection(line) == Layout.DIR_LEFT_TO_RIGHT) {
                //  1.dragpositionlinebounds
                //  2.linetopline line
                if (xInTextView > lineRight && line == lastLine) {
                    int lineEndOffset = layout.getLineEnd(line);
                    offset = lineEndOffset;
                    isBr = true;

                }
            }
            this.isBr = isBr;

            //  whether
            boolean isChange = false;
            int tempX = lastTouchX;
            int tempY = lastTouchY;
            if (selOffset != offset) {
                tempX = lastTouchX + deltaX;
                isChange = true;
            }
            if (lastLine != line) {
                lastLine = line;
                tempY = lastTouchY + deltaY;
                isChange = true;
            }

            if (!isChange) {
                return;
            }

            boolean isSelUpdate = true;
            if (isCursorHandle) {
                lastDownPoint = new PointF(xInTextView, yOnTextView);
            }
            if (mSelListener != null) {
                isSelUpdate = mSelListener.onSelOffsetChange(offset, isBr);
            }
            //  need
            //  1.cursor isSelUpdateneedupdate selection need
            //  2.cursor line needupdate
            //  cursorbottomneedupdate
            if (!isSelUpdate && (lastLine == line || !isCursorHandle)) {
                return;
            }

            selOffset = offset;
            lastTouchX = tempX;
            lastTouchY = tempY;

            boolean isTop = false;
            //  getcursorposition
            int[] handlePosition = getSelHandlePosition(offset, isTop, isBr, false);
            //  line position needTextView, isSelUpdate=falseTextView
            //  cursorposition
            if (!isSelUpdate) {
                mTextView.postInvalidate();
            }
            int xOffset = 0;
            if (isCursorHandle) {
                xOffset = handleWidth / 2;
            } else if (mBitmap == selStartBitmap) {
                xOffset = handleWidth;
            }

            updatePosition(handlePosition[0] - xOffset, handlePosition[1] + (isTop ? -handleHeight : 0));
            if (mSelListener != null) {
                mSelListener.onUpdateHandleMenuPosition(isBr);
            }
            // invalidate();
            postInvalidate();

        }

        //  getcursorposition
        private int[] getCursorHandlePosition(int selOffset, boolean isTop, boolean isBr, boolean isShow) {
            Layout layout = mTextView.getLayout();
            if (layout == null || selOffset < 0 || selOffset > mTextView.getText().length()) {
                return new int[] { 0, 0 };
            }
            lastScrollY = mTextView.getScrollY();

            PointF pointF = null;
            CursorRect cursorRect = null;
            //  getline line selOffsetneed-1 position
            int line = layout.getLineForOffset(isBr ? selOffset - 1 : selOffset);
            lastLine = line;
            if (isBr) {
                //  getX needpadding ,line needgetrightposition
                float xHorizontal = (isBr ? layout.getLineRight(line) : layout.getPrimaryHorizontal(selOffset))
                        + mTextView.getCompoundPaddingLeft();

                float lineBottom = layout.getLineBottom(line);
                float linTop = layout.getLineTop(line);
                pointF = new PointF(xHorizontal, (linTop + lineBottom) / 2);
            }
            cursorRect = getCursorLocation(pointF);

            if (cursorRect == null) {
                return null;
            }

            yOnTextView = (cursorRect.bottom + cursorRect.top) / 2;

            //  screen
            int[] location = new int[] { mPositionX, mPositionY };
            // mTextView.getLocationInWindow(location);

            //  getTextViewscroll

            lastScrollX = mTextView.getScrollX();
            Log.i(TAG, "getCursorHandlePosition location:" + Arrays.toString(location) +
                    ", cursorX:" + cursorRect.cursorX + ", lastScrollX:" + lastScrollX);

            //  getcursorcursorXtoppadding needcalculatepadding
            return new int[] {
                    (int) (location[0] + cursorRect.cursorX - lastScrollX),
                    (int) (location[1] + cursorRect.bottom - lastScrollY)
            };
        }

        //  needrightleftleftright
        private void checkDrawable() {
            if (isCursorHandle) {
                return;
            }
            if (TextUtils.isEmpty(mTextView.getText())) {
                return;
            }
            int charOffset = selOffset;
            if (selOffset == mTextView.length()) {
                charOffset = selOffset - 1;
            }
            if (charOffset < 0) {
                charOffset = 0;
            }
            Layout layout = mTextView.getLayout();

            boolean isCharRtl = layout.isRtlCharAt(charOffset);
            if (isStart) {
                if (isCharRtl) {
                    if (mBitmap != selEndBitmap) {
                        mBitmap = selEndBitmap;
                        postInvalidate();
                    }
                } else {
                    if (mBitmap != selStartBitmap) {
                        mBitmap = selStartBitmap;
                        postInvalidate();
                    }
                }
            } else {
                if (isCharRtl) {
                    if (mBitmap != selStartBitmap) {
                        mBitmap = selStartBitmap;
                        postInvalidate();
                    }
                } else {
                    if (mBitmap != selEndBitmap) {
                        mBitmap = selEndBitmap;
                        postInvalidate();
                    }
                }
            }
        }

        /**
         *  get
         *
         *  @param selOffset cursorposition
         *  @param isBr whetherline selOffset line positiontop position
         *  @param isShow whetherstart
         *  @return returncursorlefttop
         */
        private int[] getSelHandlePosition(int selOffset, boolean isTop, boolean isBr, boolean isShow) {
            Log.i(TAG, "getSelHandlePosition isBr:" + isBr);
            this.isBr = isBr;

            if (isCursorHandle) {
                int[] handleLocation = getCursorHandlePosition(selOffset, isTop, isBr, isShow);
                if (handleLocation != null) {
                    return handleLocation;
                }
            }
            checkDrawable();

            Layout layout = mTextView.getLayout();
            if (layout == null || selOffset < 0 || selOffset > mTextView.getText().length()) {
                return new int[] { 0, 0 };
            }
            float spacingExtra = mTextView.getLineSpacingExtra(); //  line

            //  getline line selOffsetneed-1 position
            int line = layout.getLineForOffset(isBr ? selOffset - 1 : selOffset);
            lastLine = line;
            float lineBottom = layout.getLineBottom(line);
            //  getline
            int finalLine = layout.getLineForOffset(mTextView.getText().length());
            lineBottom = finalLine == line ? lineBottom : lineBottom - spacingExtra;
            //  getliney
            float yLine = isTop ? layout.getLineTop(line) : lineBottom;
            yOnTextView = (layout.getLineTop(line) + lineBottom) / 2;
            final boolean isRtlChar = layout.isRtlCharAt(selOffset);
            final boolean isRtlParagraph = layout.getParagraphDirection(line) == -1;

            float xHorizontal = 0;
            //  needchecklinewhether leftright needgetSecondaryHorizontal
            if (isRtlChar == isRtlParagraph) {
                //  getX padding ,line needgetrightposition
                xHorizontal = (isBr ? layout.getLineRight(line) : layout.getPrimaryHorizontal(selOffset));
            } else {
                xHorizontal = layout.getSecondaryHorizontal(selOffset);
            }

            xOnTextView = xHorizontal;

            //  screen
            int[] location = new int[] { mPositionX, mPositionY };
            // mTextView.getLocationInWindow(location);
            //  getTextViewscroll
            lastScrollY = mTextView.getScrollY();
            int lineStart = layout.getLineStart(line);
            int lineEnd = layout.getLineEnd(line);
            boundsLeft = 0;
            boolean isStart = mBitmap == selStartBitmap;
            if (!isStart) {

                //  needchecklinewhetherline needtoplinewidth
                if (lineEnd - lineStart == 1
                        && (mTextView.getText().charAt(lineStart) == '\n'
                                || mTextView.getText().charAt(lineStart) == '\r')) {
                    xHorizontal += getLFWidth();
                } else {
                    //  needwhether needtop
                    xHorizontal += getSkewOffset();
                    //  toplinewidth
                    if (!isRtlParagraph && selOffset > 0 && ((mTextView.getText().charAt(selOffset - 1) == '\n'
                            || mTextView.getText().charAt(selOffset - 1) == '\r'))) {
                        xHorizontal += getLFWidth();
                    }
                }
            } else {
                //  whetherline
                boolean isSelLineEnd = isRtlParagraph ? selOffset != lineStart : selOffset != lineEnd;

                if (isSelLineEnd) {
                    String lineStr = mTextView.getText().toString().substring(selOffset, lineEnd);
                    //  getbounds
                    Rect bounds = new Rect();
                    mTextView.getPaint().getTextBounds(lineStr, 0, lineStr.length(), bounds);
                    if (bounds.left < 0) {
                        boundsLeft = bounds.left;
                        xHorizontal += bounds.left;
                    }
                }
            }

            lastScrollX = mTextView.getScrollX();

            return new int[] {
                    (int) (location[0] + xHorizontal - lastScrollX) + mTextView.getPaddingLeft(),
                    (int) (location[1] + yLine - lastScrollY) + getTextViewPaddingTop()
            };
        }

        public int getCurrentLine() {
            if (lastLine < 0) {
                return 0;
            }
            return lastLine;
        }

        //  whetherbounds
        public boolean isBr() {
            return isBr;
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {

        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(width, height);
        }

        //  selectionwhether
        public boolean isSelHandleTail() {
            if (isCursorHandle) {
                return false;
            }
            if (selOffset > 0) {
                Layout layout = mTextView.getLayout();
                int selLine = layout.getLineForOffset(selOffset);
                if (layout.getParagraphDirection(selLine) == Layout.DIR_LEFT_TO_RIGHT) {
                    int selPreLine = layout.getLineForOffset(selOffset - 1);
                    if (selLine != selPreLine) {
                        //  needscroll calculateissue
                        float lineTop = layout.getLineTop(selLine);
                        float lineBottom = layout.getLineBottom(selLine);
                        if (yOnTextView > lineTop && yOnTextView < lineBottom) {
                            return false;
                        } else {
                            return true;
                        }
                    }
                }

            }
            return false;
        }

    }

    /**
     *  line
     */
    private class HighLightLineInfo {
        //  drawpositionX
        float x;
        //  drawpositiony
        float y;
        int start;
        int end;
        Path linePath;

        public HighLightLineInfo() {
        }
    }

}
