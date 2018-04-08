package com.nicky.litefiledownloader.engine;

import java.io.IOException;

/**
 * Created by nickyang on 2018/4/5.
 * 代表一个请求
 */

public interface Call {
    /**
     * 取消请求
     */
    void cancel();

    boolean isCancel();

    /**
     * 同步执行请求
     * @return
     * @throws IOException
     */
    Response execute() throws IOException;
}
