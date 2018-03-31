package com.nicky.litefiledownloader;

/**
 * Created by nickyang on 2018/3/28.
 */

public interface Task {

    Request getRequest();

    /**
     * 同步执行
     */
    void execute();

    /**
     * 异步回调执行
     * @param listener
     */
    void enqueue(DownloadListener listener);

    /**
     * 是否已经执行完毕
     * @return
     */
    boolean isExecuteDone();

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
