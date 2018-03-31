package com.nicky.litefiledownloader.engine;


import android.support.annotation.Nullable;

/**
 * Created by nickyang on 2018/3/29.
 */

public interface Headers {
    @Nullable
    String header(String name);
}
