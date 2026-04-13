package com.ratta.supernote.plugin.sticker.lib.bean;

public class StickerInfo {
    public int id;
    public String name;
    public String path;
    public String thumb_sticker;
    public String sticker_group_id;

    public long create_time;
    public long update_time;
    public long use_time;
    public int num;
    public String md5;
    public boolean status = false;

    public boolean isDefault = false;

    // Localized name
    public String langName = "";

}
