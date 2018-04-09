package com.nicky.litefiledownloader.engine;


import java.io.IOException;

/**
 * Created by nickyang on 2018/3/29.
 *
 * 此接口用于自定义http请求逻辑， 默认实现了OkHttp请求逻辑，详情见 OkHttpEngine
 */

public interface HttpEngine {

    Call getHttpReq(String url) throws IOException;

    /**
     * 同步http请求
     * @param url
     * @param startPosition     请求内容的起始位置
     * @param endPosition       请求内容的结束位置
     * @return
     * @throws IOException
     */
    Call getHttpReq(String url, long startPosition, long endPosition) throws IOException;
}
