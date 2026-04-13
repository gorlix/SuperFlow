package com.ratta.supernote.plugin.sticker.lib.view.groupList;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ratta.supernote.plugin.sticker.lib.bean.StickerGroupInfo;

import java.util.List;

public class StickerGroupAdapter  extends RecyclerView.Adapter<StickerGroupAdapter.ItemViewHolder> {

    List<StickerGroupInfo> list;

    ItemClickListener itemClickListener;

    String currentGroupId;
    Uri iconUri;

    public interface ItemClickListener {
        void onItemClick(StickerGroupInfo groupInfo);
        void onItemLongClick(StickerGroupInfo groupInfo);
    }

    public StickerGroupAdapter(List<StickerGroupInfo> list) {
        this.list = list;
    }

    public void setList(List<StickerGroupInfo> list) {
        this.list.clear();
        this.list.addAll(list);
        notifyDataSetChanged();
    }

    public void setCurrentGroupId(String currentGroupId) {
        this.currentGroupId = currentGroupId;
        notifyDataSetChanged();
    }

    public void setItemClickListener(ItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public void setItemDefaultIcon(Uri iconUri) {
        this.iconUri = iconUri;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        GroupItemView itemView = new GroupItemView(parent.getContext());
        return new ItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        StickerGroupInfo groupInfo = list.get(position);
        holder.textView.setText(groupInfo.isDefault ? groupInfo.langName : groupInfo.name);
        holder.imageView.setImageBitmap(null);
        if (!TextUtils.isEmpty(groupInfo.thumb_sticker)) {
            if (TextUtils.equals(groupInfo.id, "recent")) {
                android.net.Uri uri = android.net.Uri.parse(groupInfo.thumb_sticker);
                holder.imageView.setImageURI(uri);
            } else {
                Bitmap bitmap = BitmapFactory.decodeFile(groupInfo.thumb_sticker);
                holder.imageView.setImageBitmap(bitmap);
                holder.bitmap = bitmap;
            }
        } else {
            if(iconUri != null) {
                holder.imageView.setImageURI(iconUri);
            }
        }
        GroupItemView itemView = (GroupItemView) holder.itemView;
        itemView.setSelected(groupInfo.id.equals(currentGroupId));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCurrentGroupId(groupInfo.id);
                if(itemClickListener != null) {
                    itemClickListener.onItemClick(groupInfo);
                }
            }
        });


    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public void onViewRecycled(@NonNull ItemViewHolder holder) {
        super.onViewRecycled(holder);
        if(holder.bitmap != null) {
            holder.bitmap.recycle();
            holder.bitmap = null;
        }
    }

    class  ItemViewHolder extends RecyclerView.ViewHolder {
        private TextView textView;
        private ImageView imageView;
        private Bitmap bitmap = null;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            GroupItemView item = (GroupItemView) itemView;
            textView = item.textView;
            imageView = item.imageView;
        }
    }
}
