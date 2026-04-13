package com.ratta.supernote.plugin.sticker.lib.view.stickerList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.react.bridge.ReactContext;
import com.ratta.supernote.plugin.sticker.lib.bean.StickerInfo;

import java.util.List;
import java.util.Locale;

public class StickerAdapter extends RecyclerView.Adapter<StickerAdapter.ItemViewHolder> {
    private final String TAG = "StickerAdapter";
    private ReactContext mReactContext;

    List<StickerInfo> list;

    // Whether edit mode is enabled
    boolean isEdit = false;

    ItemClickListener itemClickListener;

    public interface ItemClickListener {
        void onItemClick(StickerInfo sticker);

        void onItemLongClick(StickerInfo sticker);
    }

    public StickerAdapter(Context context, List<StickerInfo> list) {
        this.mReactContext = (ReactContext) context;
        this.list = list;
    }

    public void setItemClickListener(ItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public void setList(List<StickerInfo> list) {
        Log.i(TAG, "setList list:" + list.size());
        this.list.clear();
        this.list.addAll(list);
        notifyDataSetChanged();
    }

    public void setEdit(boolean edit) {
        isEdit = edit;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemView itemView = new ItemView(parent.getContext());
        return new ItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        StickerInfo sticker = list.get(position);
        holder.textView.setText(sticker.isDefault ? sticker.langName : sticker.name);
        holder.itemImageView.setImageBitmap(null);
        if (!TextUtils.isEmpty(sticker.thumb_sticker)) {
            Bitmap bitmap = BitmapFactory.decodeFile(sticker.thumb_sticker);
            holder.itemImageView.setImageBitmap(bitmap);
            holder.bitmap = bitmap;
        }
        holder.itemImageView.setEdit(isEdit);
        if (isEdit) {
            holder.itemImageView.setSelected(sticker.status);
        }
        holder.itemImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemClickListener != null) {
                    if (isEdit) {
                        sticker.status = !sticker.status;
                        holder.itemImageView.setSelected(sticker.status);
                    }
                    Log.i(TAG, "itemImageView click:" + sticker.id);
                    itemClickListener.onItemClick(sticker);
                }

            }
        });

        holder.itemImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (itemClickListener != null && !isEdit) {
                    sticker.status = true;
                    notifyDataSetChanged();
                    itemClickListener.onItemLongClick(sticker);
                }
                return false;
            }
        });

    }

    @Override
    public void onViewRecycled(@NonNull ItemViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.bitmap != null) {
            holder.bitmap.recycle();
            holder.bitmap = null;
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {

        private TextView textView;
        private ItemView.ItemImageView itemImageView;
        private Bitmap bitmap = null;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            ItemView item = (ItemView) itemView;
            textView = item.textView;
            itemImageView = item.itemImageView;
        }

    }
}
