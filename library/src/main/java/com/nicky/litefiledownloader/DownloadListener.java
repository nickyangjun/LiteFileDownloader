package com.nicky.litefiledownloader;

/**
 * Created by nickyang on 2018/3/30.
 */

public interface DownloadListener {

    void onStart();

    void onProgress(float progress);

    void onPause();

    void onFinished();

    void onCancel();

    void onFailed(Exception e);
}
