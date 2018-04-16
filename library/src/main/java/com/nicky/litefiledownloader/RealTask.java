package com.nicky.litefiledownloader;

import android.text.TextUtils;

import com.nicky.litefiledownloader.dao.SnippetHelper;
import com.nicky.litefiledownloader.dao.StreamCodec;
import com.nicky.litefiledownloader.engine.Call;
import com.nicky.litefiledownloader.engine.Headers;
import com.nicky.litefiledownloader.engine.HttpEngine;
import com.nicky.litefiledownloader.engine.Response;
import com.nicky.litefiledownloader.internal.LogUtil;
import com.nicky.litefiledownloader.internal.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.internal.NamedRunnable;

/**
 * Created by nickyang on 2018/3/29.
 *
 */

final class RealTask implements Task {
    final static int START = 0;  //下载开始
    final static int SUCCESS = 1;  //下载成功
    final static int CANCEL = 2;   //下载取消
    final static int PAUSE = 3;   //下载暂停
    final static int RESTART = 4;   //重新启动下载
    final static int FAIL = 5;   //下载失败
    final static int PROGRESS = 6;   //下载进度更新
    private final static int RETRYING = 7;   //下载进度更新

    private static final long MIN_PAGE_BYTES = 32 * 1024; //大于32K才开始断点分页下载
    private final FileDownloader client;
    private final HttpEngine engine;
    private final StreamCodec codec;
    private final SnippetHelper snippetHelper;
    private final Request originalRequest;

    private DownloadListener downloadListener; //用户设置的文件下载进度回调
    private AtomicInteger taskCounts = new AtomicInteger(0); //总的任务数
    private List<AsyncTask> asyncTaskList;
    private Map<Integer,SnippetHelper.Snippet> asyncSnippetList;
    private Exception exceptionResult;
    private long fileLength;

    //回调执行线程
    private int callbackThreadMode = 0;
    // 是否被执行过，被执行过，再次执行会抛出异常
    private boolean executed = false;
    //是否正在执行
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private volatile boolean canceled;
    private volatile boolean paused;

    RealTask(FileDownloader client, HttpEngine engine, StreamCodec codec, SnippetHelper snippetHelper, Request originalRequest) {
        this.client = client;
        this.engine = engine;
        this.codec = codec;
        this.snippetHelper = snippetHelper;
        this.originalRequest = originalRequest;

        if (TextUtils.isEmpty(originalRequest.getStoragePath())) {
            originalRequest.storagePath = client.downloadFileDir;
        }

        if (originalRequest.maxDownloadThreads() <= 0) {
            originalRequest.maxThreads = client.maxThreadPerTask;
        }

        if (originalRequest.getProgressRate() == 0) {
            originalRequest.progressRate = client.progressRate;
        }

        if(originalRequest.getRetryTimes() == -1){
            originalRequest.retryTimes = client.retryTimes;
        }

        this.codec.setRequest(originalRequest);

        asyncTaskList = new ArrayList<>();
        asyncSnippetList = new ConcurrentHashMap<>();
    }

    @Override
    public Request getRequest() {
        return originalRequest;
    }

    private void realEnqueue(Request request,StreamCodec codec, SnippetHelper.Snippet snippet) {
        AsyncTask asyncTask = new AsyncTask(request, codec, snippet);
        asyncTaskList.add(asyncTask);
        taskCounts.incrementAndGet();
        client.dispatcher().enqueue(asyncTask);
    }

    private void realEnqueue(AsyncTask asyncTask) {
        if(!asyncTask.isDownloadOk()) {
            asyncTask.code = START;
            taskCounts.incrementAndGet();
            client.dispatcher().enqueue(asyncTask);
        }
    }

    @Override
    public void enqueue(DownloadListener listener) {
        downloadListener = listener;
        synchronized (this) {
            if (executed) throw new IllegalStateException("Already Executed");
            executed = true;
        }

        callbackThreadMode = MethodFinder.findListenerMethods(listener.getClass());

        if (!Util.isFileExist(originalRequest.getStoragePath())) {
            if (!Util.createDir(originalRequest.getStoragePath())) {
                exceptionResult= new SecurityException(" unable to create download storage dir: " + originalRequest.getStoragePath());
                postCallback(FAIL);
                return;
            }
        }
        realEnqueue(originalRequest,codec,null);
    }

    @Override
    public boolean isExecuted() {
        return executed;
    }

    @Override
    public boolean isExecuting() {
        return isRunning.get();
    }

    @Override
    public void pause() {
        if(originalRequest.code == START) {
            originalRequest.code = PAUSE;
            paused = true;
            if (asyncTaskList.size() > 0) {
                for (AsyncTask task : asyncTaskList) {
                    task.pause();
                }
            }
        }
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    @Override
    public void resume() {
        if (originalRequest.code == PAUSE) {
            paused = false;
            if (asyncTaskList.size() > 0) {
                for (AsyncTask task : asyncTaskList) {
                    realEnqueue(task);
                }
            } else {
                realEnqueue(originalRequest,codec, null);
            }
        }
    }

    @Override
    public void cancel() {
        if (originalRequest.code != CANCEL) {
            canceled = true;
            if(originalRequest.code != START){ //不在下载状态
                originalRequest.code = CANCEL;
                postCallback(CANCEL);
            }else {
                originalRequest.code = CANCEL;
                if (asyncTaskList.size() > 0) {
                    for (AsyncTask task : asyncTaskList) {
                        task.cancel();
                    }
                }
            }
        }
    }

    @Override
    public boolean isCancel() {
        return canceled;
    }

    final class AsyncTask extends NamedRunnable {
        private Request request;
        private StreamCodec codec;
        private SnippetHelper.Snippet snippet;
        private Call call;
        int retryTimes;
        volatile int code; //当前状态

        AsyncTask(Request request, StreamCodec codec, SnippetHelper.Snippet snippet) {
            super("FileDownloader %s", request.getReqUrl());
            this.request = request;
            this.codec = codec;
            this.snippet = snippet;
        }

        @Override
        protected void execute() {
            try {
                if(code == CANCEL || code == PAUSE){
                    return;
                }

                if(this.snippet == null){  //检测是否有已经下载过的分段记录
                    checkDownloadSnippets(codec);
                }

                if (isRunning.compareAndSet(false, true)) {
                    if (request.code == PAUSE) {
                        postCallback(RESTART);
                    } else {
                        postCallback(START);
                    }
                    request.code = START;
                    if(fileLength > 0) { //还没有文件长度，不回调
                        postCallback(PROGRESS);
                    }
                }

                if (snippet != null) {  //下载部分文件
                    LogUtil.e(" NO:" + snippet.getNum() + " req start: " + snippet.getStartPoint()
                            + " end: " + snippet.getEndPoint() + " curIndex: " + snippet.getDownloadedPoint());
                    call = engine.getHttpReq(request.getReqUrl(), snippet.getDownloadedPoint(), snippet.getEndPoint());
                } else {
                    snippet = snippetHelper.getSnippet(request, 0, 0, 0, 0);
                    asyncSnippetList.put(snippet.getNum(), snippet);
                    call = engine.getHttpReq(request.getReqUrl());
                }

                Response response = call.execute();
                if(response == null){
                    throw new IllegalStateException("request failed, Response is null");
                }

                LogUtil.e(" NO:" + snippet.getNum() + " req resp code: "+response.code());

                if (response.code() != 206 && response.code() != 200) {// 206：请求部分资源成功码，表示服务器支持断点续传
                    throw new IllegalStateException("request " + response.code() + " failed");
                }

                handleHttpHeader(this, response); //处理是否分段下载

                InputStream is = response.body().byteStream();

                long start = snippet.getDownloadedPoint();
                long size = snippet.getEndPoint() - snippet.getDownloadedPoint()+1;
                codec.startSeek(snippet.getNum(),start, size); //定位文件写入的开始位置.

                byte[] buffer = new byte[2048];
                int length;
                long curReadIndex = snippet.getDownloadedPoint(); //当前读服务端文件的Index
                long curWriteIndex = snippet.getDownloadedPoint(); //当前写入本地文件的Index
                int updateSnippet;
                snippet.startDownload();

                while ((length = is.read(buffer, 0, getReadCounts(buffer, curReadIndex))) > 0) {//读取流
                    if(code == CANCEL || code == PAUSE){
                        LogUtil.e(" NO:" + snippet.getNum() + " 停止read, code: "+code);
                        return;
                    }

                    updateSnippet = codec.write(snippet.getNum(), buffer, 0, length);
                    curReadIndex += length;
                    if(updateSnippet > 0) {//将该线程最新完成下载的位置记录并保存到缓存数据文件中
                        curWriteIndex = snippet.getDownloadedPoint() + updateSnippet;
                        snippet.updateCurDownloadedPoint(curWriteIndex);
                    }
                }

                updateSnippet = codec.flush();
                if(updateSnippet > 0) {//将该线程最新完成下载的位置记录并保存到缓存数据文件中
                    curWriteIndex = snippet.getDownloadedPoint() + updateSnippet;
                    snippet.updateCurDownloadedPoint(curWriteIndex);
                }

                LogUtil.e(" NO:" + snippet.getNum() + " done.  curIndex: " + curWriteIndex
                            + " startPoint: "+ snippet.getStartPoint() + " endPoint: "+snippet.getEndPoint());

                if(snippet.getEndPoint() > snippet.getDownloadedPoint()){
                    throw new IllegalStateException("Response content not read complete, retry");
                }

                code = SUCCESS;
                codec.onSnippetDone(snippet.getNum());
                snippet.stopDownload();
                cancelCall();
            } catch (Exception e) {
                codec.onSnippetDownloadError(snippet.getNum(),e);
                snippet.stopDownload();
                cancelCall();
                if(code != CANCEL && code != PAUSE) {
                    checkRetry(e, this);
                }else {
                    LogUtil.e(" NO:" + snippet.getNum() +" "+ e.getMessage() + " 不用重试，code: "+code);
                }
            } finally {
                if(code != RETRYING) { //重试中，当做请求未完成
                    handleDownloadDone();
                }
                client.dispatcher().finished(this);
            }
        }

        void checkDownloadSnippets(StreamCodec codec){
            List<SnippetHelper.Snippet> downloadSnippets = snippetHelper.getDownloadedSnippets(request);
            if (downloadSnippets != null && downloadSnippets.size() > 0) {
                for (SnippetHelper.Snippet snippet : downloadSnippets) {
                    fileLength = snippet.getEndPoint()+1;
                    asyncSnippetList.put(snippet.getNum(), snippet);
                    LogUtil.i(snippet.toString());
                    if (snippet.getDownloadedPoint() < snippet.getEndPoint()) {
                        if(this.snippet == null){
                            this.snippet = snippet;
                        }else {
                            realEnqueue(originalRequest, codec, snippet);
                        }
                    }
                }
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

        void cancel(){
            code = CANCEL;
            cancelCall();
        }

        void pause(){
            code = PAUSE;
            cancelCall();
        }

        void cancelCall(){
            if(call != null){
                if(!call.isCancel()) {
                    call.cancel();
                }
                call = null;
            }
        }

        boolean isDownloadOk(){
            return snippet.getDownloadedPoint() >= snippet.getEndPoint();
        }
    }

    private String getNameFormUrl(String url){
        int index = url.lastIndexOf("/");
        if (index > -1) {
            return url.substring(index + 1);
        } else {
            return url;
        }
    }

    private void handleHttpHeader(AsyncTask asyncTask, Response response) {
        if(response.headers() == null){
            asyncTask.snippet.setEndPoint(Long.MAX_VALUE);
            asyncTask.request.setFileName(getNameFormUrl(asyncTask.request.getReqUrl()));
            return;
        }

        Headers headers = response.headers();
        if (TextUtils.isEmpty(asyncTask.request.getFileName())) { //因为没有保存下载文件名，重新下载时必须再获取一次文件名
            synchronized (RealTask.this) {
                if (TextUtils.isEmpty(asyncTask.request.getFileName())) {
                    //获取下载文件名
                    String contentDisposition = headers.header("content-disposition");
                    if (!TextUtils.isEmpty(contentDisposition)) {
                        Pattern pattern = Pattern.compile("(filename=)\"?(\\w+(\\.)?)+\\w+\"?");
                        Matcher matcher = pattern.matcher(contentDisposition);
                        if (matcher.find()) {
                            String fileName = matcher.group();
                            String name = fileName.split("=")[1];
                            name = name.replaceAll("\"", "");
                            try {
                                name = URLDecoder.decode(name, "utf-8");
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                            asyncTask.request.setFileName(name.trim());
                        }
                    } else {
                        //请求的下载地址中有可能302重定向了，所以优先取response中的url
                        String reqUrl = response.reqUrl();
                        if (TextUtils.isEmpty(reqUrl)) {
                            reqUrl = asyncTask.request.getReqUrl();
                        }

                        int index = reqUrl.lastIndexOf("/");
                        if (index > -1) {
                            asyncTask.request.setFileName(reqUrl.substring(index + 1));
                        } else {
                            asyncTask.request.setFileName(reqUrl);
                        }
                    }
                }
            }
        }

        if (asyncTask.snippet.getEndPoint() == 0) { //代表第一个请求,并且此时还没有取到文件长度，则处理http头部
            int threads = 1; //默认一个下载线程
            //获取到文件长度后，开始回调进度
            fileLength = Long.valueOf(headers.header("Content-Length"));
            postCallback(PROGRESS);

            String ranges = headers.header("Accept-Ranges");
            if (!TextUtils.isEmpty(ranges) && ranges.equalsIgnoreCase("bytes")) { //代表支持断点续传
                threads = getTasksCount(fileLength);
            }

            String contentMd5 = headers.header("content-md5");
            if (!TextUtils.isEmpty(contentMd5)) {
                asyncTask.request.contentMd5 = contentMd5;
            }

            try {
                //每个个分段大小
                long pageLength = fileLength / threads;

                //第一个分段
                asyncTask.snippet.setEndPoint(pageLength - 1);
                asyncTask.snippet.serializeToLocal();

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
                        asyncSnippetList.put(snippet.getNum(), snippet);
                        snippet.serializeToLocal();
                        if (!isCancel() && !isPaused()) {
                            realEnqueue(originalRequest, asyncTask.codec, snippet);
                        }
                    }
                }
            } catch (Exception e) {
                //出现异常，直接用一个线程下载文件
                asyncTask.snippet.setEndPoint(fileLength);
            }
        }
    }

    private int getTasksCount(long contentLength) {
        if (contentLength <= MIN_PAGE_BYTES) {
            return 1;
        }
        int size = (int) (contentLength / MIN_PAGE_BYTES);
        return size > originalRequest.maxDownloadThreads() ? originalRequest.maxDownloadThreads() : size;
    }

    private void checkRetry(Exception e, AsyncTask asyncTask){
        LogUtil.e(e.getMessage() + " " + asyncTask.snippet.toString() + " retryTimes:"+asyncTask.retryTimes + " ");
        if(asyncTask.retryTimes < originalRequest.getRetryTimes()){ //重试
            asyncTask.retryTimes++;
            asyncTask.code = RETRYING;
            client.dispatcher().enqueue(asyncTask);
        }else {
            asyncTask.code = FAIL;
            originalRequest.code = FAIL;
            exceptionResult = e;
            if (asyncTaskList.size() > 0) {
                for (AsyncTask task : asyncTaskList) {
                    task.cancel();
                }
            }
        }
    }

    /**
     * 确保所有task都处理完后，post callback
     */
    private void handleDownloadDone() {
        if(taskCounts.getAndDecrement() == 1){
            isRunning.set(false);
            if(originalRequest.code == START || originalRequest.code == SUCCESS) {
                try {
                    codec.allSnippetDone(originalRequest.contentMd5, SUCCESS);
                    originalRequest.code = SUCCESS;
                } catch (IOException e) {
                    originalRequest.code = FAIL; //校验不通过，下载失败
                    exceptionResult = e;
                    snippetHelper.downloadDone(originalRequest.code, originalRequest);
                }
            }
            postCallback(PROGRESS);
            postCallback(originalRequest.code);
        }
    }

    private boolean isAsyncThreadMode(int code){
        return (callbackThreadMode & (1<<code))==0;
    }

    private void postCallback(int code) {
        if (downloadListener != null) {
            if(isAsyncThreadMode(code)){
                client.dispatcher().postCallback(code, this);       //回调在异步线程
            }else {
                client.dispatcher().postMainCallback(code, this);   //回调在主线程
            }
        }
    }

    private void postDelayCallback(int delayMillis, int code) {
        if (downloadListener != null) {
            if(isAsyncThreadMode(code)) {
                client.dispatcher().postDelayCallback(delayMillis, code, this);  //回调在异步线程
            }else {
                client.dispatcher().postMainDelayCallback(delayMillis, code, this);  //回调在主线程
            }
        }
    }

    private void removeCallback(int code) {
        if (downloadListener != null) {
            if(isAsyncThreadMode(code)) {
                client.dispatcher().removeCallback(code, this);
            }else {
                client.dispatcher().removeMainCallback(code,this);
            }
        }
    }

    void handleCallback(int code) {
        switch (code) {
            case PROGRESS:
                if(fileLength == 0) return;
                long download = 0;
                Collection<SnippetHelper.Snippet> collection = asyncSnippetList.values();
                for (SnippetHelper.Snippet snippet : collection) {
                    download += (snippet.getDownloadedPoint() - snippet.getStartPoint());
                }
                downloadListener.onProgress(originalRequest, download, fileLength);
                if (isExecuting()) {
                    //间隔调度更新进度
                    postDelayCallback(originalRequest.getProgressRate(), PROGRESS);
                } else {
                    //已经执行完毕，清除可能还在等待执行的回调
                    removeCallback(PROGRESS);
                    if(originalRequest.code == SUCCESS){
                        snippetHelper.downloadDone(originalRequest.code, originalRequest);
                        asyncTaskList.clear();
                        asyncSnippetList.clear();
                    }
                }
                break;
            case START:
                downloadListener.onStart(originalRequest);
                break;
            case SUCCESS:
                asyncTaskList.clear();
                asyncSnippetList.clear();
                downloadListener.onFinished(originalRequest);
                break;
            case PAUSE:
                downloadListener.onPause(originalRequest);
                break;
            case RESTART:
                downloadListener.onRestart(originalRequest);
                break;
            case CANCEL:
                asyncTaskList.clear();
                asyncSnippetList.clear();
                snippetHelper.downloadDone(CANCEL, originalRequest);
                try {
                    codec.allSnippetDone(originalRequest.contentMd5, CANCEL);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                downloadListener.onCancel(originalRequest);
                break;
            case FAIL:
                asyncTaskList.clear();
                asyncSnippetList.clear();
                downloadListener.onFailed(originalRequest, exceptionResult);
                break;
        }
    }

}
