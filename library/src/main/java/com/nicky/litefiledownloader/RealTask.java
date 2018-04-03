package com.nicky.litefiledownloader;

import android.text.TextUtils;

import com.nicky.litefiledownloader.dao.SnippetHelper;
import com.nicky.litefiledownloader.engine.Headers;
import com.nicky.litefiledownloader.engine.HttpEngine;
import com.nicky.litefiledownloader.engine.Response;
import com.nicky.litefiledownloader.internal.LogUtil;
import com.nicky.litefiledownloader.internal.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.internal.NamedRunnable;

/**
 * Created by nickyang on 2018/3/29.
 *
 */

final class RealTask implements Task {
    final static int START = 0;  //下载开始
    final static int CANCEL = 1;   //下载取消
    final static int PAUSE = 2;   //下载暂停
    final static int RESTART = 3;   //重新启动下载
    final static int SUCCESS = 4;  //下载成功
    final static int FAIL = 5;   //下载失败
    final static int PROGRESS = 6;   //下载进度更新

    final long MIN_PAGE_BYTES = 20 * 1024; //大于1M才开始断点分页下载
    final FileDownloader client;
    final HttpEngine engine;
    final SnippetHelper snippetHelper;
    final Request originalRequest;

    final File tempFileName;  //下载的临时文件
    private DownloadListener downloadListener; //用户设置的文件下载进度回调
    private AtomicInteger taskCounts = new AtomicInteger(0); //总的分段任务数
    private AtomicInteger pauseCounts = new AtomicInteger(0); //暂停的任务数
    private long startExecuteTime; //开始执行时间
    private List<AsyncTask> asyncTaskList;
    private List<SnippetHelper.Snippet> asyncSnippetList;
    private Exception exceptionResult;
    private long fileLength;

    // 是否被执行过，被执行过，再次执行会抛出异常
    private boolean executed = false;
    //是否正在执行
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private volatile boolean canceled;
    private volatile boolean paused;

    public RealTask(FileDownloader client, HttpEngine engine, SnippetHelper snippetHelper, Request originalRequest) {
        this.client = client;
        this.engine = engine;
        this.snippetHelper = snippetHelper;
        this.originalRequest = originalRequest;

        if (TextUtils.isEmpty(originalRequest.getStoragePath())) {
            originalRequest.storagePath = client.downloadFileDir;
        }

        if (originalRequest.maxDownloadThreads() <= 0) {
            originalRequest.maxThreads = client.maxThreadPerTask;
        }

        if(originalRequest.getProgressRate() == 0){
            originalRequest.progressRate = client.progressRate;
        }

        tempFileName = new File(originalRequest.getStoragePath() + originalRequest.getFileName() + ".tmp");
        asyncTaskList = new ArrayList<>();
        asyncSnippetList = Collections.synchronizedList(new ArrayList<SnippetHelper.Snippet>());
    }

    @Override
    public Request getRequest() {
        return originalRequest;
    }

    @Override
    public void execute() {
        synchronized (this) {
            if (executed) throw new IllegalStateException("Already Executed");
            executed = true;
        }

        try {
            client.dispatcher().executed(this);
        } catch (Exception e) {

        } finally {
            client.dispatcher().finished(this);
        }
    }

    @Override
    public void enqueue(DownloadListener listener) {
        downloadListener = listener;
        synchronized (this) {
            if (executed) throw new IllegalStateException("Already Executed");
            executed = true;
        }

        if (!Util.isFileExist(originalRequest.getStoragePath())) {
            if (!Util.createDir(originalRequest.getStoragePath())) {
                if (downloadListener != null) {
                    Exception exception = new SecurityException(" unable to create download storage dir: " +
                            originalRequest.getStoragePath());
                    downloadListener.onFailed(exception);
                    return;
                }
            }
        }
        restoreOrCreateTask();
    }

    private void restoreOrCreateTask(){
        List<SnippetHelper.Snippet> downloadSnippets = snippetHelper.getDownloadedSnippets(originalRequest);
        if (downloadSnippets != null && downloadSnippets.size() > 0) {
            for (SnippetHelper.Snippet snippet : downloadSnippets) {
                fileLength = snippet.getEndPoint();
                asyncSnippetList.add(snippet);
                if(snippet.getDownloadedPoint() < snippet.getEndPoint()){
                    LogUtil.e(" enqueue -----> snippet: " + snippet.getNum() + " req start: " + snippet.getStartPoint() + " end: " +
                            snippet.getEndPoint() + " downloaded: "+snippet.getDownloadedPoint());
                    realEnqueue(originalRequest, snippet);
                }else {
                    LogUtil.e(" enqueue ----> 分段下载完成: startPoint: " + snippet.getStartPoint()
                            + "  endPoint: " + snippet.getEndPoint() + " " + "curPoint: " + snippet.getDownloadedPoint());
                }
            }
        } else {
            realEnqueue(originalRequest, null);
        }
    }

    private void realEnqueue(Request request, SnippetHelper.Snippet snippet){
        AsyncTask asyncTask = new AsyncTask(request,snippet);
        asyncTaskList.add(asyncTask);
        taskCounts.incrementAndGet();
        client.dispatcher().enqueue(asyncTask);
    }

    void handleCallback(int code){
        switch (code){
            case PROGRESS:
                int progress = 0;
                long download = 0;
                for(SnippetHelper.Snippet snippet : asyncSnippetList){
                    download += (snippet.getDownloadedPoint() - snippet.getStartPoint());
                }
                if(fileLength > 0){
                    progress = (int) ((download*100 / fileLength));
                }
                downloadListener.onProgress(progress);
                if(originalRequest.code == START) {
                    postDelayCallback(originalRequest.getProgressRate(), PROGRESS);
                }else {
                    removeCallback(PROGRESS);
                }
                break;
            case START:
                downloadListener.onStart();
                break;
            case SUCCESS:
                downloadListener.onFinished();
                break;
            case PAUSE:
                downloadListener.onPause();
                break;
            case RESTART:
                downloadListener.onRestart();
                break;
            case CANCEL:
                Util.deleteFile(tempFileName);
                downloadListener.onCancel();
                break;
            case FAIL:
                downloadListener.onFailed(exceptionResult);
                break;
        }
    }

    @Override
    public boolean isExecuting() {
        return isRunning.get();
    }

    @Override
    public void pause() {
        paused = true;
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    @Override
    public void resume() {
        if(originalRequest.code == PAUSE) {
            paused = false;
            if (asyncTaskList.size() > 0) {
                for (AsyncTask task : asyncTaskList) {
                    client.dispatcher().enqueue(task);
                }
            } else {
                restoreOrCreateTask();
            }
        }
    }

    @Override
    public void cancel() {
        if(originalRequest.code != CANCEL) {
            canceled = true;
            if (originalRequest.code != START) {
                originalRequest.code = CANCEL;
                snippetHelper.downloadDone(CANCEL, originalRequest);
                postCallback(CANCEL);
            }
        }
    }

    @Override
    public boolean isCancel() {
        return canceled;
    }

    private void postCallback(int code){
        if(downloadListener != null){
            client.dispatcher().postCallback(code,this);
        }
    }

    private void postDelayCallback(int delayMillis, int code){
        if(downloadListener != null){
            client.dispatcher().postDelayCallback(delayMillis, code,this);
        }
    }

    private void removeCallback(int code){
        if(downloadListener != null){
            client.dispatcher().removeCallback(code,this);
        }
    }

    final class AsyncTask extends NamedRunnable {
        private Request request;
        private SnippetHelper.Snippet snippet;

        AsyncTask(Request request, SnippetHelper.Snippet snippet) {
            super("FileDownloader %s", request.getReqUrl());
            this.request = request;
            this.snippet = snippet;
        }

        @Override
        protected void execute() {
            if(isRunning.compareAndSet(false,true)){
                if(request.code == PAUSE) {
                    pauseCounts.set(0);
                    postCallback(RESTART);
                }else {
                    postCallback(START);
                }
                request.code = START;
                postCallback(PROGRESS);
            }
            try {
                if(snippet != null){
                    //下载部分文件
                    LogUtil.e(" ----------> parts req threadId: "+ Thread.currentThread().getId());
                    engine.getHttpReq(request.getReqUrl(), snippet.getDownloadedPoint(),
                            snippet.getEndPoint(), new HttpCallback(request,snippet));
                } else {
                    LogUtil.e(" ----------> first req threadId: "+ Thread.currentThread().getId());
                    snippet = snippetHelper.getSnippet(request,0,0, 0, 0);
                    asyncSnippetList.add(snippet);
                    engine.getHttpReq(request.getReqUrl(), new HttpCallback(request,snippet));
                }
            } catch (Exception e) {

            } finally {
                // TODO: 2018/4/1  这里执行有问题，因为上面是异步的
                client.dispatcher().finished(this);
            }
        }

    }

    private class HttpCallback implements Callback {
        Request request;
        SnippetHelper.Snippet snippet;

        HttpCallback(Request request, SnippetHelper.Snippet snippet) {
            this.request = request;
            this.snippet = snippet;
        }

        @Override
        public void onSuccess(Response response) {
            if (response.code() != 206 && response.code() != 200) {// 206：请求部分资源成功码，表示服务器支持断点续传
                return;
            }

            if (snippet.getEndPoint() == 0) { //代表第一个请求,并且此时还没有取到文件长度，则处理http头部
                handleHttpHeader(snippet, response.headers()); //处理是否分段下载
            }

            LogUtil.e(" ----->  thread " + snippet.getNum() + " req start: " + snippet.getStartPoint() + " end: " +
                    snippet.getEndPoint()  + " downloadedPoint: " + snippet.getDownloadedPoint() + " threadId: "+ Thread.currentThread().getId());

            try {
                InputStream is = response.body().byteStream();// 获取流

                RandomAccessFile tmpAccessFile = new RandomAccessFile(tempFileName, "rw");
                tmpAccessFile.seek(snippet.getDownloadedPoint());// 文件写入的开始位置.

                /*  将网络流中的文件写入本地*/
                byte[] buffer = new byte[2048];
                int length;

                int total = 0;// 记录本次下载文件的大小
                long curReadIndex = snippet.getDownloadedPoint();
                snippet.startDownload();
                int result = SUCCESS;

                while ((length = is.read(buffer, 0, getReadCounts(buffer, curReadIndex))) > 0) {//读取流
                    if (isCancel()) { //取消了下载
                        result = CANCEL;
                        break;
                    }

                    if (isPaused()) {
                        result = PAUSE;
                        break;
                    }

                    tmpAccessFile.write(buffer, 0, length);
                    total += length;
                    curReadIndex = snippet.getDownloadedPoint() + length;

//                    LogUtil.e(" ----->  thread " + snippet.getNum() + " curReadIndex " + curReadIndex + " endPoint: "+snippet.getEndPoint()  + " threadId: "+ Thread.currentThread().getId());
                    
                    //将该线程最新完成下载的位置记录并保存到缓存数据文件中
                    snippet.updateCurDownloadedPoint(curReadIndex);
                }

                LogUtil.e(" ----->  thread " + snippet.getNum() + " done  total " + total + " threadId: "+ Thread.currentThread().getId());

                Util.close(tmpAccessFile);//关闭资源
                snippet.stopDownload();
                response.cancel();

                handleDownloadDone(result, snippet);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        int getReadCounts(byte[] buffer, long curReadIndex) {
            int readCounts = buffer.length;
            int remaining = (int) (snippet.getEndPoint() - curReadIndex + 1);
            if (remaining < buffer.length) {
                readCounts = remaining;
            }
            return readCounts;
        }
    }

    private void handleHttpHeader(SnippetHelper.Snippet firstSnippet, Headers headers){
        int threads = 1; //默认一个下载线程
        if (headers != null) {
            fileLength = Long.valueOf(headers.header("Content-Length"));
            String ranges = headers.header("Accept-Ranges");
            if (!TextUtils.isEmpty(ranges) && ranges.equalsIgnoreCase("bytes")) { //代表支持断点续传
                threads = getTasksCount(fileLength);
            }
            LogUtil.e(" ----->  fileLength " + fileLength + " all thread size: " + threads);
        }
        try {
            //每个个分段大小
            long pageLength = fileLength / threads;

            //第一个分段
            firstSnippet.setEndPoint(pageLength - 1);
            firstSnippet.serializeToLocal();

            if (threads > 1) {
                //设置第一个请求，下载的最大长度
                long startIndex;
                long endIndex;
                for (int thread = 1; thread < threads; thread++) {
                    startIndex = pageLength * thread;
                    endIndex = startIndex + pageLength - 1;
                    if (thread == threads - 1) {  //最后一个下载分段
                        endIndex = fileLength - 1;
                    }

                    SnippetHelper.Snippet snippet = snippetHelper.getSnippet(originalRequest, thread, startIndex, endIndex, startIndex);
                    asyncSnippetList.add(snippet);
                    snippet.serializeToLocal();
                    realEnqueue(originalRequest, snippet);
                }
            }
        }catch (Exception e){
            //出现异常，直接用一个线程下载文件
            firstSnippet.setEndPoint(fileLength);
        }
    }

    private int getTasksCount(long contentLength) {
        if (contentLength <= MIN_PAGE_BYTES) {
            return 1;
        }
        int size = (int) (contentLength / MIN_PAGE_BYTES);
        return size > originalRequest.maxDownloadThreads() ? originalRequest.maxDownloadThreads() : size;
    }

    // TODO: 2018/4/1  判断所以下再成功，不对
    private void handleDownloadDone(int code, SnippetHelper.Snippet snippet) {
        if(code == SUCCESS || code == CANCEL) {
            if (taskCounts.compareAndSet(1, 0)) { //所有分段都下载完
                originalRequest.code = code;
                if (code == SUCCESS) {
                    tempFileName.renameTo(new File(originalRequest.getStoragePath() + originalRequest.getFileName()));
                    LogUtil.e(" ----->  download finished. elapsed time: " + (System.currentTimeMillis() - startExecuteTime) + " ms");
                    postCallback(PROGRESS);
                }
                postCallback(code);
                snippetHelper.downloadDone(code, originalRequest);
                isRunning.set(false);
            } else {
                taskCounts.decrementAndGet();
            }
        }else if(code == PAUSE){
            if(pauseCounts.incrementAndGet() == taskCounts.get()){
                originalRequest.code = PAUSE;
                postCallback(PAUSE);
                isRunning.set(false);
            }
        }
    }

}
