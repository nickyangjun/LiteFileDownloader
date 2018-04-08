package com.nicky.litefiledownloader.engine;

/**
 * Created by nickyang on 2018/3/29.
 */

public interface Response {
    /**
     * request中的url有可能被302重定向了，所以这里可以返回重定向后的地址，用于取下载文件名
     */
    String reqUrl();

    Headers headers();
    int code();
    ResponseBody body();
}
