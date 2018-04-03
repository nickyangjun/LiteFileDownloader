package com.nicky.litefiledownloader;

/**
 * Created by nickyang on 2018/3/30.
 */

public interface DownloadListener {

    void onStart();

    void onProgress(int progress);

    void onPause();

    void onRestart();

    void onFinished();

    void onCancel();

    void onFailed(Exception e);
}
