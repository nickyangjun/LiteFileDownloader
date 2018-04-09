package com.nicky.litefiledownloader;

/**
 * Created by nickyang on 2018/3/28.
 */

public interface Task {

    Request getRequest();

    /**
     * 异步回调执行
     * @param listener
     */
    void enqueue(DownloadListener listener);

    /**
     * 是否执行过，包括正在执行和取消执行的请求
     * @return
     */
    boolean isExecuted();

    /**
     * 是否正在执行
     * @return
     */
    boolean isExecuting();

    void pause();

    boolean isPaused();

    void resume();

    void cancel();

    boolean isCancel();
}
