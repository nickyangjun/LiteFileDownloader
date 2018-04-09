package com.nicky.litefiledownloader;

/**
 * Created by nickyang on 2018/3/30.
 */

public interface DownloadListener {

    void onStart(Request request);

    /**
     *
     * @param request
     * @param curBytes   0 <= curBytes <= totalBytes
     * @param totalBytes
     */
    void onProgress(Request request, long curBytes, long totalBytes);

    void onPause(Request request);

    void onRestart(Request request);

    void onFinished(Request request);

    void onCancel(Request request);

    void onFailed(Request request, Exception e);
}
