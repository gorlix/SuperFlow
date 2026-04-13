package com.ratta.supernote.plugin.sticker.lib.utils;

import android.util.Log;

import com.facebook.react.bridge.Promise;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtils {
    private final static String TAG = "FileUtils";

    private static final String ENCODE_KEY = "sticker";

    public static void decodeStickerZip(String zipFile) {
        Log.i(TAG, "decodeZipFile zipFile:" + zipFile);
        String tempZipFile = zipFile + "temp";
        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(zipFile);
            outputStream = new FileOutputStream(tempZipFile);
            String encodeStr = ENCODE_KEY;
            byte[] encodeBytes = encodeStr.getBytes();
            byte[] buffer = new byte[1024 + encodeBytes.length];
            byte[] subBuffer = new byte[encodeBytes.length];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                if (length > encodeBytes.length) {
                    System.arraycopy(buffer, 0, subBuffer, 0, encodeBytes.length);
                    String subStr = new String(subBuffer);
                    Log.i(TAG, "decodeZipFile subStr:" + subStr);
                    if (encodeStr.equals(subStr)) {
                        outputStream.write(buffer, encodeBytes.length, length - encodeBytes.length);
                    } else {
                        outputStream.write(buffer, 0, length);
                    }
                } else {
                    outputStream.write(buffer, 0, length);
                }
            }
            inputStream.close();
            inputStream = null;
            outputStream.close();
            outputStream = null;
            File zip = new File(zipFile);
            zip.delete();
            zip = new File(zipFile);
            File source = new File(tempZipFile);
            source.renameTo(zip);

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static void encodeStickerZip(String zipFile) {
        String tempZipFile = zipFile + "temp";
        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(zipFile);
            outputStream = new FileOutputStream(tempZipFile);
            String encodeStr = ENCODE_KEY;
            byte[] encodeBytes = encodeStr.getBytes();
            Log.i(TAG, "encodeZipFile encodeBytes:" + encodeBytes.length);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(encodeBytes);
                outputStream.write(buffer, 0, length);

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
           return file.delete();
        } else {
            return true;
        }
    }

    public static boolean renameToFile(String sourceFile, String destFile) {

        File source = new File(sourceFile);
        if (!source.exists()) {
            return false;
        }
        File dest = new File(destFile);
        return source.renameTo(dest);

    }



}
