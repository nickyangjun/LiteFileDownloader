package com.nicky.litefiledownloader;

import android.text.TextUtils;

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
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.internal.NamedRunnable;

/**
 * Created by nickyang on 2018/3/29.
 */

final class RealTask implements Task {
    final long MIN_PAGE_BYTES = 20*1024; //大于1M才开始断点分页下载
    final FileDownloader client;
    final Request originalRequest;
    final HttpEngine engine;

    final File tempFileName;  //下载的临时文件
    private DownloadListener downloadListener; //用户设置的文件下载进度回调
    private AtomicInteger taskCounts = new AtomicInteger(0);
    private long startExecuteTime; //开始执行时间

    // Guarded by this.
    private boolean executed;
    private volatile boolean canceled;
    private volatile boolean paused;

    public RealTask(FileDownloader client, Request originalRequest, HttpEngine engine) {
        this.client = client;
        this.originalRequest = originalRequest;
        this.engine = engine;

        if(TextUtils.isEmpty(originalRequest.getStoragePath())){
            originalRequest.storagePath = client.downloadFileDir;
        }

        if(originalRequest.maxDownloadThreads() == 0){
            originalRequest.maxThreads = client.maxThreadPerTask;
        }

        tempFileName = new File(originalRequest.getStoragePath()+originalRequest.getFileName()+".tmp");
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
        }catch (Exception e){

        }finally {
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

        if(!Util.isFileExist(originalRequest.getStoragePath())){
            if(!Util.createDir(originalRequest.getStoragePath())){
                executed = true;
                if(downloadListener != null){
                    Exception exception = new SecurityException(" unable to create download storage dir: "
                                                        +originalRequest.getStoragePath());
                    downloadListener.onFailed(exception);
                    return;
                }
            }
        }
        AsyncTask asyncTask = new AsyncTask(originalRequest, new HttpCallback(originalRequest));
        taskCounts.incrementAndGet();
        client.dispatcher().enqueue(asyncTask);
    }

    @Override
    public boolean isExecuteDone() {
        return false;
    }

    @Override
    public boolean isExecuting() {
        return false;
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
        paused = false;
    }

    @Override
    public void cancel() {
        canceled = true;
    }

    @Override
    public boolean isCancel() {
        return false;
    }

    final class AsyncTask extends NamedRunnable {
        private final Callback responseCallback;
        private Request request;

        AsyncTask(Request request, Callback responseCallback) {
            super("FileDownloader %s", request.getReqUrl());
            this.request = request;
            this.responseCallback = responseCallback;
        }

        @Override
        protected void execute() {
            try{
                if(request.startIndex > 0){
                    final File cacheFile = new File(request.getCacheFilePath());
                    final RandomAccessFile cacheAccessFile = new RandomAccessFile(cacheFile, "rwd");
                    if (cacheFile.exists()) {// 如果文件存在
                        String storageLengthStr = cacheAccessFile.readLine();
                        if(!TextUtils.isEmpty(storageLengthStr)) {
                            try {
                                request.startIndex = Integer.parseInt(storageLengthStr);//重新设置下载起点
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                        Util.close(cacheAccessFile);
                    }
                    //下载部分文件
                    engine.getHttpReq(request.getReqUrl(), request.startIndex,
                            request.endIndex, new HttpCallback(request));
                }else {
                    startExecuteTime = System.currentTimeMillis();
                    //下载所有文件
                    engine.getHttpReq(request.getReqUrl(), new HttpCallback(request));
                }
            }catch (Exception e){

            } finally {
                client.dispatcher().finished(this);
            }
        }
    }

    private class HttpCallback implements Callback{
        Request request;
        String cacheFile;

        HttpCallback(Request request){
            this.request = request;
            cacheFile = request.getCacheFilePath();
        }

        @Override
        public void onSuccess(Response response) {
            if (response.code() != 206 && response.code() != 200) {// 206：请求部分资源成功码，表示服务器支持断点续传
                return;
            }

            if(request.startIndex == 0) { //代表第一个请求，拿到整个文件
                handleHttpHeader(request, response.headers()); //处理是否分段下载
            }

            LogUtil.e(" ----->  thread " + request.threadNum + " req start: " + request.startIndex + " end: "+ request.endIndex);

            try {
                final RandomAccessFile cacheAccessFile = new RandomAccessFile(cacheFile, "rwd");
                InputStream is = response.body().byteStream();// 获取流

                RandomAccessFile tmpAccessFile = new RandomAccessFile(tempFileName, "rw");
                tmpAccessFile.seek(request.startIndex);// 文件写入的开始位置.

                /*  将网络流中的文件写入本地*/
                byte[] buffer = new byte[2048];
                int length;

                int total = 0;// 记录本次下载文件的大小
                int curReadIndex = request.startIndex;

                while ((length = is.read(buffer,0, getReadCounts(buffer,curReadIndex))) > 0) {//读取流
                    if (isCancel()) { //取消了下载
                        Util.close(tmpAccessFile);//关闭资源
                        Util.close(cacheAccessFile);//关闭资源
                        response.cancel();
                        Util.deleteFile(cacheFile);//删除对应缓存文件
                        return;
                    }

                    if (isPaused()) {
                        Util.close(tmpAccessFile);//关闭资源
                        Util.close(cacheAccessFile);//关闭资源
                        response.cancel();
                        return;
                    }

                    tmpAccessFile.write(buffer, 0, length);
                    total += length;
                    curReadIndex = request.startIndex + total;

                    LogUtil.e(" ----->  thread " + request.threadNum + " curReadIndex " + curReadIndex);

                    //将该线程最新完成下载的位置记录并保存到缓存数据文件中
                    cacheAccessFile.seek(0);
                    cacheAccessFile.write((curReadIndex + "").getBytes("UTF-8"));
                }

                LogUtil.e(" ----->  thread " + request.threadNum + " done  total " + total);

                Util.close(tmpAccessFile);//关闭资源
                Util.close(cacheAccessFile);//关闭资源
                response.cancel();
                Util.deleteFile(cacheFile);//删除对应缓存文件

                handleDownloadDone(request);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        int getReadCounts(byte[] buffer, int curReadIndex){
            int readCounts = buffer.length;
            int remaining = request.endIndex - curReadIndex + 1;
            if(remaining < buffer.length){
                readCounts = remaining;
            }
            return readCounts;
        }
    }

    private void handleHttpHeader(Request request, Headers headers){
        int threads = 1; //默认一个下载线程
        int fileLength = 0;
        if(headers != null){
            String ranges = headers.header("Accept-Ranges");
            if(!TextUtils.isEmpty(ranges) && ranges.equalsIgnoreCase("bytes")){ //代表支持断点续传
                fileLength = Integer.valueOf(headers.header("Content-Length"));
                threads = getTasksCount(fileLength);

                LogUtil.e(" ----->  fileLength " + fileLength + " all thread size: " + threads);
            }
        }
        if(threads > 1){
            int pageLength = fileLength / threads;
            request.endIndex = pageLength -1; //设置第一个请求，下载的最大长度
            for(int thread = 1; thread<threads; thread++){
                Request newReq = request.newRequest();
                newReq.threadNum = thread;
                newReq.startIndex = pageLength*thread;
                newReq.endIndex = newReq.startIndex + pageLength - 1;
                if(thread == threads-1){
                    newReq.endIndex = fileLength-1;
                }

                AsyncTask asyncTask = new AsyncTask(newReq, new HttpCallback(newReq));
                taskCounts.incrementAndGet();
                client.dispatcher().enqueue(asyncTask);
            }
        }else {
            request.endIndex = fileLength-1;
        }
    }

    private int getTasksCount(long contentLength){
        if(contentLength <= MIN_PAGE_BYTES){
            return 1;
        }
        int size = (int)(contentLength / MIN_PAGE_BYTES);
        return size > originalRequest.maxDownloadThreads()?originalRequest.maxDownloadThreads():size;
    }

    private void handleDownloadDone(Request request){
        if(taskCounts.compareAndSet(1,0)){ //所有分段都下载完
            if(tempFileName.exists()){
                tempFileName.renameTo(new File(originalRequest.getStoragePath()+originalRequest.getFileName()));
                LogUtil.e(" ----->  download finished. elapsed time: " + (System.currentTimeMillis() - startExecuteTime) + " ms");
            }
        }else {
            taskCounts.decrementAndGet();
        }
    }

}
