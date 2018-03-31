package com.nicky.litefiledownloader;

import android.os.Environment;

import com.nicky.litefiledownloader.engine.HttpEngine;
import com.nicky.litefiledownloader.engine.OkHttpEngine;

import java.io.File;

/**
 * Created by nickyang on 2018/2/4.
 */

public class FileDownloader {
    private static final int DEFAULT_MAX_THREADS = 4;
    private static final String DEFAULT_FILE_DIR;  //默认下载目录

    static {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) || !Environment.isExternalStorageRemovable()) {
            DEFAULT_FILE_DIR = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + File.separator + "fileDownload" + File.separator;
        }else {
            DEFAULT_FILE_DIR = "";
        }
    }

    final Dispatcher dispatcher;
    final String downloadFileDir;
    final int maxThreadPerTask;
    final HttpEngine engine;

    /**
     * 获取默认下载目录
     *
     * @return
     */
    private String getDefaultDirectory() {
        return DEFAULT_FILE_DIR;
    }

    Dispatcher dispatcher() {
        return dispatcher;
    }

    public Task newTask(Request request){
       return new RealTask(this, request, engine);
    }

    FileDownloader(Builder builder){
        dispatcher = builder.dispatcher;
        downloadFileDir = builder.downloadFileDir;
        maxThreadPerTask = builder.maxThreadPerTask;
        engine = builder.engine;
    }

    public static class Builder{
        Dispatcher dispatcher;
        String downloadFileDir;
        int maxThreadPerTask;
        HttpEngine engine;

        public Builder(){
            dispatcher = new Dispatcher();
            maxThreadPerTask = DEFAULT_MAX_THREADS;
            downloadFileDir = DEFAULT_FILE_DIR;
            engine = new OkHttpEngine();
        }

        public Builder downloadFileDirectory(String pathDir){
            downloadFileDir = pathDir;
            return this;
        }

        public Builder maxThreadPerTask(int threads){
            maxThreadPerTask = threads;
            return this;
        }

        public Builder httpEngine(HttpEngine engine){
            this.engine = engine;
            return this;
        }

        public FileDownloader build(){
            return new FileDownloader(this);
        }
    }

    public static Builder createBuilder(){
        return new Builder();
    }
}

