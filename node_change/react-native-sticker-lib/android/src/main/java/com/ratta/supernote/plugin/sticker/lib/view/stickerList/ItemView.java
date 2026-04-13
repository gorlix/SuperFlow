package com.ratta.supernote.plugin.sticker.lib.view.stickerList;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.ratta.supernote.plugin.sticker.lib.R;
import com.ratta.supernote.plugin.sticker.lib.utils.ImageResUtils;

public class ItemView extends LinearLayout {
    private final String TAG = "ItemView";
    public TextView textView;

    public ItemImageView itemImageView;

    public ItemView(@NonNull Context context) {
        super(context);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        // float dimen =
        // getContext().getResources().getDimension(com.ratta.supernote.pluginlib.R.dimen.dp_px_3);
        // Log.i("", "recycle item dimen:" + dimen);

        this.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        itemImageView = new ItemImageView(getContext());
        addView(itemImageView);

        Log.i(TAG, "ItemView init 1");
        textView = new TextView(getContext());
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getContext().getResources()
                        .getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_26));
        textView.setTextScaleX(1.0f);
        textView.setLayoutParams(new LayoutParams(
                getResources().getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_180),
                ViewGroup.LayoutParams.WRAP_CONTENT));
        Log.i(TAG, "ItemView init width:" + textView.getWidth());
        textView.setMaxLines(1);
        textView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        textView.setGravity(Gravity.CENTER);

        textView.setPadding(0,
                getContext().getResources().getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_8),
                0, 0);
        addView(textView);
    }

    public static class ItemImageView extends RelativeLayout {
        private Context mContext;

        private ImageView imageView;

        private ImageView selectImageView;

        public ItemImageView(Context context) {
            super(context);
            mContext = context;
            init();
        }

        private void init() {
            setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                    getContext().getResources()
                            .getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_180),
                    getContext().getResources()
                            .getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_180)));
            addView(imageView);

            selectImageView = new ImageView(mContext);
            addView(selectImageView);
            LayoutParams params = (LayoutParams) selectImageView.getLayoutParams();
            params.height = getContext().getResources()
                    .getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_48);
            params.width = getContext().getResources()
                    .getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_48);
            params.setMargins(
                    mContext.getResources().getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_8),
                    mContext.getResources().getDimensionPixelSize(com.ratta.supernote.pluginlib.R.dimen.dp_px_8),
                    0,
                    0);

            selectImageView.setLayoutParams(params);
//            selectImageView.setImageBitmap(ImageResUtils.getItemNoSelected(mContext));
            selectImageView.setImageResource(R.drawable.ic_select_no);

        }

        public void setImageBitmap(Bitmap bitmap) {
            if (imageView != null) {
                if (bitmap == null) {
                    this.imageView.setImageDrawable(null);
                    return;
                }
                this.imageView.setImageBitmap(bitmap);
            }
        }

        public void setSelected(boolean isSelected) {
            /*selectImageView.setImageBitmap(isSelected ? ImageResUtils.getItemSelected(mContext)
                    : ImageResUtils.getItemNoSelected(mContext));*/
            selectImageView.setImageResource(isSelected? R.drawable.ic_select_black: R.drawable.ic_select_no);
        }

        public void setEdit(boolean isEdit) {
            selectImageView.setVisibility(isEdit ? VISIBLE : GONE);
        }
    }
}
