package com.nicky.litefiledownloader.demo;

import com.nicky.litefiledownloader.DownloadListener;
import com.nicky.litefiledownloader.FileDownloader;
import com.nicky.litefiledownloader.Task;
import com.nicky.litefiledownloader.dao.BufferRandomAccessFileCodec;
import com.nicky.litefiledownloader.dao.SnippetHelper;
import com.nicky.litefiledownloader.engine.OkHttpEngine;
import com.nicky.litefiledownloader.internal.LogUtil;
import com.nicky.litefiledownloader.internal.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by nickyang on 2018/4/11.
 */

public class DownloadFactory {
    private static final AtomicReference<DownloadFactory> INSTANCE = new AtomicReference<>();
    //用来存放各个下载的请求
    private Map<String, Call> downCalls;
    //用来存放下载完成的请求
    private Map<String, Boolean> isDownloadDone;
    private OkHttpClient mClient;
    FileDownloader downloader;

    static class Holder {
        static final DownloadFactory INSTANCE = new DownloadFactory();
    }

    private DownloadFactory() {
        downCalls = new ConcurrentHashMap<>();
        isDownloadDone = new ConcurrentHashMap<>();
        mClient = new OkHttpClient.Builder().build();

        downloader = FileDownloader.createBuilder()
                .httpEngine(new HttpEngine())
                .build();
    }

    public static DownloadFactory getInstance() {
        return Holder.INSTANCE;
    }

    class HttpEngine extends OkHttpEngine {

        @Override
        protected OkHttpClient getOkHttpClient() {
            return mClient;
        }
    }

    //防止反复调用下载线程 下载之前应先调用该方法判断是否已经下载完成过
    public boolean isDownload(String url, String savePath) {
        if (isDownloadDone.get(url) != null) {
            if (!isDownloadDone.get(url)) { //正在下载
                return true;
            } else if (isDownloadDone.get(url) && !Util.isFileExist(savePath)) { //下载了，但是文件被删除了
                return false;
            }
        }
        return false;
    }

    //取消下载请求
    public void cancel(String url) {
        Call call = downCalls.get(url);
        if (call != null) {
            call.cancel();
            downCalls.remove(url);
            LogUtil.e("download cancel ==== ", url);
        }
        if (null != isDownloadDone.get(url) && !isDownloadDone.get(url)) {
            isDownloadDone.remove(url);
        }
    }

    /**
     * 外部调用下载入口
     * 下载大文件时调用
     * 如视频 原图 apk等
     * @param url              文件的url
     * @param savePath         文件的本地绝对路径
     * @param downLoadObserver 下载动作的订阅者
     */
    public void download(final int count, final String url, final String savePath, DownLoadObserver downLoadObserver) {
        Observable.just(url)
                .filter(new Predicate<String>() {
                    @Override
                    public boolean test(@NonNull String url) throws Exception {
                        synchronized (DownloadFactory.class) {
                            if (isDownload(url, savePath)) {
                                return false;
                            } else {
                                isDownloadDone.put(url, false);
                                return true;
                            }
                        }
                    }
                })
                .concatMap(new Function<String, ObservableSource<DownloadInfo>>() {
                    @Override
                    public ObservableSource<DownloadInfo> apply(String s) throws Exception {
                        return Observable.create(new ObservableOnSubscribe<DownloadInfo>() {
                            @Override
                            public void subscribe(final ObservableEmitter<DownloadInfo> emitter) throws Exception {
                                final DownloadInfo downloadInfo = new DownloadInfo(url, savePath);
                                String path = savePath;
                                String name = "";
                                int indexOf = savePath.lastIndexOf(File.separator);
                                if(indexOf > -1 && indexOf < savePath.length()-1){
                                    name = savePath.substring(indexOf+1);
                                    path = savePath.substring(0,indexOf+1);
                                }
                                com.nicky.litefiledownloader.Request request = com.nicky.litefiledownloader.Request.createBuilder()
                                        .url(url)
                                        .storagePathDir(path)
                                        .downloadFileName(name)
                                        .maxDownloadThreads(count)
                                        .build();
                                Task task = downloader.newTask(request);
                                task.enqueue(new DownloadListener() {
                                    @Override
                                    public void onStart(com.nicky.litefiledownloader.Request request) {
                                    }

                                    @Override
                                    public void onProgress(com.nicky.litefiledownloader.Request request, long l, long l1) {
                                        downloadInfo.setTotal(l1);
                                        downloadInfo.setProgress(l);
                                        emitter.onNext(downloadInfo);
                                    }

                                    @Override
                                    public void onPause(com.nicky.litefiledownloader.Request request) {

                                    }

                                    @Override
                                    public void onRestart(com.nicky.litefiledownloader.Request request) {

                                    }

                                    @Override
                                    public void onFinished(com.nicky.litefiledownloader.Request request) {
                                        isDownloadDone.put(url, true);
                                        emitter.onComplete();
                                    }

                                    @Override
                                    public void onCancel(com.nicky.litefiledownloader.Request request) {

                                    }

                                    @Override
                                    public void onFailed(com.nicky.litefiledownloader.Request request, Exception e) {
                                        isDownloadDone.remove(url);
                                        emitter.onError(e);
                                    }
                                });
                            }
                        });
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(downLoadObserver);
    }

    /**
     * 外部调用下载入口
     * 下载大文件时调用
     * 如视频 原图 apk等
     * @param url              文件的url
     * @param savePath         文件的本地绝对路径
     * @param downLoadObserver 下载动作的订阅者
     */
    public void download(final String url, final String savePath, DownLoadObserver downLoadObserver) {
        download(1,url,savePath,downLoadObserver);
    }


    /**
     * 外部调用下载入口
     * 下载大文件时调用
     * 如视频 原图 apk等
     * @param url              文件的url
     * @param savePath         文件的本地绝对路径
     * @param downLoadObserver 下载动作的订阅者
     */
    public void download2(String url, final String savePath, DownLoadObserver downLoadObserver) {
        Observable.just(url)
                .filter(new Predicate<String>() {
                    @Override
                    public boolean test(@NonNull String url) throws Exception {
                        synchronized (DownloadFactory.class) {
                            if (isDownload(url, savePath)) {
                                return false;
                            } else {
                                isDownloadDone.put(url, false);
                                return true;
                            }
                        }
                    }
                })
                .flatMap(new Function<String, ObservableSource<DownloadInfo>>() {
                    @Override
                    public ObservableSource<DownloadInfo> apply(@NonNull String url) throws Exception {
                        return Observable.just(createDownInfo(url, savePath));
                    }
                }).map(new Function<DownloadInfo, DownloadInfo>() {
            @Override
            public DownloadInfo apply(@NonNull DownloadInfo downloadInfo) throws Exception {
                return getRealFileName(downloadInfo);
            }
        }).flatMap(new Function<DownloadInfo, ObservableSource<DownloadInfo>>() {
            @Override
            public ObservableSource<DownloadInfo> apply(@NonNull DownloadInfo downloadInfo) throws Exception {
                return Observable.create(new DownloadSubscribe(downloadInfo));
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(downLoadObserver);

    }


    private class DownloadSubscribe implements ObservableOnSubscribe<DownloadInfo> {
        private DownloadInfo downloadInfo;

        public DownloadSubscribe(DownloadInfo downloadInfo) {
            this.downloadInfo = downloadInfo;
        }

        @Override
        public void subscribe(ObservableEmitter<DownloadInfo> e) throws Exception {
            String url = downloadInfo.getUrl();
            if (downCalls.get(url) != null) {
                LogUtil.e("download files 被并发执行 ==== ", url);
                return;
            } else {
                LogUtil.e("download start ==== ", url);
            }

            //如果已经下载完成
            if (downloadInfo.isFileDownloadFinish()) {
                isDownloadDone.put(url, true);
                e.onComplete();
                return;
            }

            e.onNext(downloadInfo);

            Request.Builder request = new Request.Builder()
                    .url(url);

            long downloadLength = downloadInfo.getProgress();//已经下载好的长度
            long contentLength = downloadInfo.getTotal();//文件的总长度
            if(downloadInfo.isGetFileTotalLengthOk()){
                //确定下载的范围,添加此头,则服务器就可以跳过已经下载好的部分
                request.addHeader("RANGE", "bytes=" + downloadLength + "-" + contentLength);
            }

            Call call = mClient.newCall(request.build());
            //把这个添加到call里,方便取消
            downCalls.put(url, call);
            InputStream is = null;
            FileOutputStream fileOutputStream = null;
            try {
                Response response = call.execute();
                File file = new File(downloadInfo.getFilePath());
                is = response.body().byteStream();
                fileOutputStream = new FileOutputStream(file, true);
                byte[] buffer = new byte[2048];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, len);
                    downloadLength += len;
                    downloadInfo.setProgress(downloadLength);
                    e.onNext(downloadInfo);
                }
                fileOutputStream.flush();
                downCalls.remove(url);
                isDownloadDone.put(url, true);
                LogUtil.e("download end ==== ", url + "  size: " + file.length());
                e.onComplete();
            } catch (Exception ex1) {
                LogUtil.e("download error ==== ", url);
                downCalls.remove(url);
                isDownloadDone.remove(url);
                e.onError(ex1);
            } finally {
                try {
                    if (null != is) {
                        is.close();
                    }
                    if (null != fileOutputStream) {
                        fileOutputStream.close();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            }
        }
    }


    private DownloadInfo getRealFileName(DownloadInfo downloadInfo) {
        long downloadLength = 0;
//        File file = new File(downloadInfo.getFilePath());
//        if (file.exists()) {
//            //有这个文件的话，表示已经下载过，拿到长度
//            downloadLength = file.length();
//        }
//        downloadInfo.setProgress(downloadLength);
        return downloadInfo;
    }


    private DownloadInfo createDownInfo(String url, String filePath) {
        DownloadInfo downloadInfo = new DownloadInfo(url, filePath);
        long contentLength = getContentLength(url);
        downloadInfo.setTotal(contentLength);
        return downloadInfo;
    }


    /**
     * 获取下载长度
     */
    private long getContentLength(String downloadUrl) {
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        try {
            Response response = mClient.newCall(request).execute();
            if (response != null && response.isSuccessful()) {
                long contentLength = response.body().contentLength();
                response.close();
                return contentLength <= 0 ? DownloadInfo.TOTAL_ERROR : contentLength;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return DownloadInfo.TOTAL_ERROR;
    }
}
