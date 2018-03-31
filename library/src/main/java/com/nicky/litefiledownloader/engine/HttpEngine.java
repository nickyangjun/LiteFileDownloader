package com.nicky.litefiledownloader.engine;


import com.nicky.litefiledownloader.Callback;

/**
 * Created by nickyang on 2018/3/29.
 */

public interface HttpEngine {

    void getHttpReq(String url, Callback callback);

    void getHttpReq(String url, long startPosition, long endPosition, Callback callback);
}
