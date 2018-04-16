package com.nicky.litefiledownloader.demo;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * 下载订阅
 * Created by seaky on 2017/4/24.
 */

public abstract class DownLoadObserver implements Observer<DownloadInfo> {

    protected Disposable d;//可以用于取消注册的监听者
    protected DownloadInfo downloadInfo;
    @Override
    public void onSubscribe(Disposable d) {
        this.d = d;
        onStart();
    }

    @Override
    public void onNext(DownloadInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
    }

    @Override
    public void onError(Throwable e) {
        e.printStackTrace();
    }

    public void onStart(){}
}
