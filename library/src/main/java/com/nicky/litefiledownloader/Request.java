package com.nicky.litefiledownloader;

import java.io.File;

/**
 * Created by nickyang on 2018/3/29.
 *
 */

public final class Request {
    String reqUrl;
    int maxThreads;
    String storagePath;    //文件保存的路径
    String fileName;    //文件名称
    int progressRate;  //进度更新频率，毫秒
    int retryTimes = -1;
    volatile int code = -1;  //当前状态
    String contentMd5; //下载内容的MD5码验证
    Object tag;

    private Request() {
    }

    Request(Builder builder) {
        this.reqUrl = builder.reqUrl;
        this.maxThreads = builder.maxThreads;
        this.storagePath = builder.storagePath;
        this.fileName = builder.fileName;
        this.tag = builder.tag;
        this.progressRate = builder.progressRate;
        this.retryTimes = builder.retryTimes;
    }

    public String getReqUrl() {
        return reqUrl;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getFileName() {
        return fileName;
    }

    void setFileName(String name){
        fileName = name;
    }

    public int maxDownloadThreads() {
        return maxThreads;
    }

    public int getProgressRate() {
        return progressRate;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public Object getTag(){
        return tag;
    }

    Request newRequest() {
        Request request = new Request();
        request.reqUrl = reqUrl;
        request.maxThreads = maxThreads;
        request.storagePath = storagePath;
        request.fileName = fileName;
        request.progressRate = progressRate;
        request.retryTimes = retryTimes;
        return request;
    }

    public static class Builder {
        String reqUrl;
        int maxThreads;
        String storagePath; //文件保存的路径
        String fileName;    //文件名称
        int progressRate;
        int retryTimes = -1;
        Object tag;

        Builder() {
        }

        public Builder url(String url) {
            reqUrl = url;
            return this;
        }

        public Builder maxDownloadThreads(int threads) {
            maxThreads = threads;
            return this;
        }

        public Builder storagePathDir(String storageDirPath) {
            storagePath = storageDirPath;
            if(!storageDirPath.endsWith(File.separator)){
                storagePath = storagePath+File.separator;
            }
            return this;
        }

        public Builder downloadFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder progressRate(int progressRate){
            this.progressRate = progressRate;
            return this;
        }

        public Builder retryTimes(int retryTimes){
            this.retryTimes = retryTimes;
            return this;
        }

        public Builder tag(Object tag){
            this.tag = tag;
            return this;
        }

        public Request build() {
            return new Request(this);
        }
    }

    public static Builder createBuilder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "Request[ reqUrl: "
                + reqUrl + " maxThreads: "
                + maxThreads+" storagePath: "
                + storagePath + " fileName: "+
                fileName+
                "]";
    }
}
