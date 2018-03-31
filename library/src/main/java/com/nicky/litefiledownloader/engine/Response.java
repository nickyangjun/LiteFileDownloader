package com.nicky.litefiledownloader.engine;

/**
 * Created by nickyang on 2018/3/29.
 */

public interface Response {
    Headers headers();
    int code();
    ResponseBody body();

    /**
     * 请求取消
     */
    void cancel();
}
