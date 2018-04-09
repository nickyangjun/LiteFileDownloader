package com.nicky.litefiledownloader.annotation;

/**
 * Created by nickyang on 2018/4/8.
 */

public enum ThreadMode {
    /**
     * Subscriber will be called in Android's main thread (sometimes referred to as UI thread).
     */
    MAIN,

    /**
     * Subscriber will be called in an independent thread
     */
    ASYNC
}
