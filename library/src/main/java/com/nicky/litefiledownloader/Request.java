package com.nicky.litefiledownloader;

import android.text.TextUtils;

/**
 * Created by nickyang on 2018/3/29.
 */

public final class Request {
    String reqUrl;
    int maxThreads;
    int threadNum = 0; //下载线程编号，默认0号
    int startIndex;   //下载文件的起点
    int endIndex;      //下载文件的终点
    String storagePath;    //文件保存的路径
    String fileName;    //文件名称

    Request() {
    }

    Request(Builder builder) {
        this.reqUrl = builder.reqUrl;
        this.maxThreads = builder.maxThreads;
        this.storagePath = builder.storagePath;
        this.fileName = builder.fileName;

        if (TextUtils.isEmpty(this.fileName)) {
            int index = reqUrl.lastIndexOf("/");
            if (index > -1) {
                fileName = reqUrl.substring(index+1);
            }else {
                fileName = reqUrl;
            }
        }
    }

    public void setReqUrl(String url) {
        reqUrl = url;
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

    public int maxDownloadThreads() {
        return maxThreads;
    }

    String getCacheFilePath(){
//        return getStoragePath()+getFileName() + threadNum +".cache";
        return getStoragePath()+getFileName() +".cache";
    }

    Request newRequest() {
        Request request = new Request();
        request.reqUrl = reqUrl;
        request.maxThreads = maxThreads;
        request.storagePath = storagePath;
        request.fileName = fileName;
        return request;
    }

    public static class Builder {
        String reqUrl;
        int maxThreads;
        String storagePath; //文件保存的路径
        String fileName;    //文件名称
        Object tag;

        public Builder() {
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
            return this;
        }

        public Builder downloadFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Request build() {
            return new Request(this);
        }
    }

    public static Builder createBuilder() {
        return new Builder();
    }
}
