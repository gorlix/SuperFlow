package com.ratta.supernote.plugin.sticker.lib.view.editText.editor;

import android.text.TextUtils;

import java.util.HashMap;

public class LangUtils {

    public static final String text_copy = "text_copy";
    public static final String text_cut = "text_cut";
    public static final String text_paste = "text_paste";
    public static final String text_select_all = "text_select_all";
    private static final HashMap<String, String> enLangMap = new HashMap<>() {
        {
            put(text_copy, "Copy");
            put(text_cut, "Cut");
            put(text_paste, "Paste");
            put(text_select_all, "Select All");
        }
    };
    private static final HashMap<String, String> jaLangMap = new HashMap<>() {
        {
            put(text_copy, "コピー");
            put(text_cut, "カット");
            put(text_paste, "ペースト");
            put(text_select_all, "すべて選択");
        }
    };
    private static final HashMap<String, String> zhLangMap = new HashMap<>() {
        {
            put(text_copy, "复制");
            put(text_cut, "剪切");
            put(text_paste, "粘贴");
            put(text_select_all, "全选");
        }
    };
    private static final HashMap<String, String> zhTWLangMap = new HashMap<>() {
        {
            put(text_copy, "複製");
            put(text_cut, "剪切");
            put(text_paste, "貼上");
            put(text_select_all, "全選");
        }
    };

    public static String getString(String key, String lang) {
        HashMap<String, String> map = enLangMap;
        if (TextUtils.equals(lang, "en")) {
            map = enLangMap;

        } else if (TextUtils.equals(lang, "zh_CN")) {
            map = zhLangMap;

        } else if (TextUtils.equals(lang, "zh_TW")) {
            map = zhTWLangMap;

        } else if (TextUtils.equals(lang, "ja")) {
            map = jaLangMap;

        }
        return map.get(key);

    }

}
