package com.nicky.litefiledownloader;

import android.text.TextUtils;

/**
 * Created by nickyang on 2018/3/29.
 */

public final class Request {
    String reqUrl;
    int maxThreads;
    String storagePath;    //文件保存的路径
    String fileName;    //文件名称
    Object tag;

    Request() {
    }

    Request(Builder builder) {
        this.reqUrl = builder.reqUrl;
        this.maxThreads = builder.maxThreads;
        this.storagePath = builder.storagePath;
        this.fileName = builder.fileName;
        this.tag = builder.tag;

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
}
