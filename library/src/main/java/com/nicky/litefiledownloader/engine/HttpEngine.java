package com.nicky.litefiledownloader.engine;


import java.io.IOException;

/**
 * Created by nickyang on 2018/3/29.
 */

public interface HttpEngine {

    Call getHttpReq(String url) throws IOException;

    Call getHttpReq(String url, long startPosition, long endPosition) throws IOException;

}
