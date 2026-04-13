package com.ratta.supernote.plugin.sticker.lib.view.editText;

import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ByteLengthFilter implements InputFilter {
    private final String TAG = "ByteLengthFilter";
    private final int mMaxBytes;

    public ByteLengthFilter(int maxBytes) {
        mMaxBytes = maxBytes;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned dest, int dstart, int dend) {
        String currentText = dest.toString();
        String inputStr = source.subSequence(start, end).toString();
        // Byte length of the text before the insertion point
        int currentStartLen = currentText.substring(0, dstart).getBytes().length;
        // Byte length of the text after the replacement range
        int currentEndLen = currentText.substring(dend).getBytes().length;
        // Byte length of the inserted text
        int inputLen = inputStr.getBytes().length;
        // Total byte length after insertion
        int textLen = currentEndLen + currentStartLen + inputLen;
        if (textLen > mMaxBytes) {
            int lenOffset = textLen - mMaxBytes;
            inputLen = inputLen - lenOffset;
            if (inputLen <= 0) {
                return "";
            }
            inputStr = getWholeText(inputStr, inputLen);
            return inputStr;

        }
        return null;

    }

    private String toHexString(String str) {
        StringBuilder hex = new StringBuilder();
        for (char c : str.toCharArray()) {
            hex.append(String.format("%04X ", (int) c));
        }
        return hex.toString();
    }

    private static int getCharWidth(byte[] bytes, int pos) {
        if ((bytes[pos] & 0x80) == 0) return 1;
        if ((bytes[pos] & 0xE0) == 0xC0) return 2;
        if ((bytes[pos] & 0xF0) == 0xE0) return 3;
        return 4;
    }

    private static int countBytes(String str, Charset charset) {
        return str.getBytes(charset).length;
    }

    private String getWholeText(String text, int byteCount) {
        if (text == null || text.isEmpty()) return text;

        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= byteCount) return text;

        StringBuilder suText = new StringBuilder();
        int len = text.length();
        for (int offset = 0; offset < len; ) {
            if (offset >= len) {
                break;
            }
            int codePoint = Character.codePointAt(text, offset);
            int charCount = Character.charCount(codePoint);
            // Skip BOM
            if (codePoint == 0xFEFF) {
                Log.i(TAG, "has BOM:");
                offset += charCount;
                continue;
            }
            // Ensure bounds safety
            int endOffset = Math.min(offset + charCount, len);
            if (endOffset > len) {
                endOffset = len;
            }
            String currentStr = text.substring(offset, endOffset);

            int subLen = currentStr.getBytes().length + suText.toString().getBytes().length;
            if (subLen > byteCount) {
                break;
            }

            suText.append(currentStr);
            if (subLen == byteCount) {
                break;
            }

            offset += charCount;
        }
        return suText.toString().toString();
    }
}
