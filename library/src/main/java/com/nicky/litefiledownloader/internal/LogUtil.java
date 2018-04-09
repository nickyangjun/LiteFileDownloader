package com.nicky.litefiledownloader.internal;

import android.util.Log;

import com.nicky.litefiledownloader.BuildConfig;

/**
 * Created by nickyang on 2018/3/31.
 */

public class LogUtil {
    private static final String TAG = "LiteFileDownloader";
    private static String className;//类名
    private static String methodName;//方法名
    private static int lineNumber;//行数
    private static boolean isLoggable = Log.isLoggable(TAG, Log.DEBUG);

    private LogUtil() {
    }

    public void onLoggable(){
        isLoggable = true;
    }

    private static boolean isLoggable() {
        return BuildConfig.DEBUG || isLoggable;
    }

    private static void getMethodNames(StackTraceElement[] sElements){
        className = sElements[2].getFileName();
        methodName = sElements[2].getMethodName();
        lineNumber = sElements[2].getLineNumber();
    }

    private static String createLog(String log) {
        getMethodNames(new Throwable().getStackTrace());
        return methodName + "(" + className + ":" + lineNumber + ") --> " + log;
    }

    public static String e(final String message){
        if (!isLoggable()) {
            return "";
        }

        String info = createLog(message);
        String tag = className;
        Log.e(tag,info);
        return info;
    }

    public static String e(String tag, final String message) {
        if (!isLoggable()) {
            return message;
        }
        String info = message;
        if(tag == null){
            info = createLog(message);
            tag = className;
        }
        Log.e(tag,info);
        return info;
    }

    public static String e(final String tag, final Throwable exception) {
        String info = Log.getStackTraceString(exception);
        if (!isLoggable()) {
            return info;
        }

        Log.e(tag,info);
        return info;
    }

    public static void e(String tag, final String message, final Throwable exception) {
        if (!isLoggable()) {
            return;
        }
        String info = message;
        if(tag == null){
            info = createLog(message);
            tag = className;
        }

        Log.e(tag, info, exception);
    }

    public static String d(final String message) {
        if (!isLoggable()) {
            return "";
        }
        String info = createLog(message);
        String tag = className;

        Log.d(tag,info);
        return info;
    }

    public static String d(String tag, final String message) {
        if (!isLoggable()) {
            return message;
        }
        String info = message;
        if(tag == null){
            info = createLog(message);
            tag = className;
        }

        Log.d(tag, info);
        return info;
    }

    public static void d(String tag, final String message, final Throwable exception) {
        if (!isLoggable()) {
            return;
        }
        String info = message;
        if(tag == null){
            info = createLog(message);
            tag = className;
        }

        Log.d(tag, info, exception);
    }

    public static void i(final String message) {
        if (!isLoggable()) {
            return;
        }
        String info = createLog(message);
        String tag = className;

        Log.i(tag, info);
    }

    public static String i(String tag, final String message) {
        if (!isLoggable()) {
            return message;
        }
        String info = message;
        if(tag == null){
            info = createLog(message);
            tag = className;
        }

        Log.i(tag, info);
        return info;
    }

    public static void i(String tag, final String message, final Throwable exception) {
        if (!isLoggable()) {
            return;
        }
        String info = message;
        if(tag == null){
            info = createLog(message);
            tag = className;
        }

        Log.i(tag, info, exception);
    }

    public static void w(final String message) {
        if (!isLoggable()) {
            return;
        }
        String info = createLog(message);
        String tag = className;

        Log.w(tag, info);
    }

    public static String w(String tag, final String message) {
        if (!isLoggable()) {
            return message;
        }
        String info = message;
        if(tag == null){
            info = createLog(message);
            tag = className;
        }

        Log.w(tag, info);
        return info;
    }

    public static void w(final String tag, final Throwable e) {
        if (!isLoggable()) {
            return;
        }

        Log.w(tag, Log.getStackTraceString(e));
    }
}

