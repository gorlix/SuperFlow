package com.ratta.supernote.plugin.sticker.lib.bean;

public class StickerGroupInfo {
    public String id;
    public String name;
    public int num;
    public String path;
    public String thumb_sticker;
    public String dir_name;
    public long create_time;
    public long update_time;

    public boolean status = false;

    public boolean isDefault = false;

    // Localized name
    public String langName = "";
    public boolean isCurrent = false;
}
