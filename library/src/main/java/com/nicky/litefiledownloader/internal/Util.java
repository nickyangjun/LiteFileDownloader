package com.nicky.litefiledownloader.internal;

import android.text.TextUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.concurrent.ThreadFactory;

/**
 * Created by nickyang on 2018/3/29.
 */

public class Util {
    public static final Charset UTF_8 = Charset.forName("UTF-8");
    public static final Charset US_ASCII = Charset.forName("US-ASCII");

    public static String newStringUtf8(final byte[] bytes) {
        return newString(bytes, UTF_8);
    }

    private static String newString(final byte[] bytes, final Charset charset) {
        return bytes == null ? null : new String(bytes, charset);
    }

    public static byte[] getBytesUtf8(final String string) {
        return getBytes(string, UTF_8);
    }

    private static byte[] getBytes(final String string, final Charset charset) {
        if (string == null) {
            return null;
        }
        return string.getBytes(charset);
    }

    public static String newStringUsAscii(final byte[] bytes) {
        return newString(bytes, US_ASCII);
    }


    /** Returns a {@link Locale#US} formatted {@link String}. */
    public static String format(String format, Object... args) {
        return String.format(Locale.CHINA, format, args);
    }

    public static ThreadFactory threadFactory(final String name, final boolean daemon) {
        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread result = new Thread(runnable, name);
                result.setDaemon(daemon);
                return result;
            }
        };
    }

    public static void close(Closeable closeable){
        if(closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //删除文件
    public static boolean deleteFile(String path){
        if(TextUtils.isEmpty(path)) return false;
        boolean deleteSuccess = false;
        File file = new File(path);
        if(file.exists()){
            deleteSuccess = file.delete();
        }
        return deleteSuccess;
    }

    //删除文件
    public static boolean deleteFile(File file){
        boolean deleteSuccess = false;
        if(file.exists()){
            deleteSuccess = file.delete();
        }
        return deleteSuccess;
    }

    public static boolean isFileExist(String filePath){
        if(TextUtils.isEmpty(filePath)){
            return false;
        }
        File file = new File(filePath);
        return file.exists();
    }

    //创建文件夹
    public static boolean createDir(String path){
        try {
            File file = new File(path);
            return file.mkdirs();
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }
}
