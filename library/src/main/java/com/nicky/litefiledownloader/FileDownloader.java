package com.nicky.litefiledownloader;

import android.os.Environment;

import com.nicky.litefiledownloader.dao.CodecFactory;
import com.nicky.litefiledownloader.dao.DefaultCodecFactory;
import com.nicky.litefiledownloader.dao.FileSnippetHelper;
import com.nicky.litefiledownloader.dao.SnippetHelper;
import com.nicky.litefiledownloader.engine.HttpEngine;
import com.nicky.litefiledownloader.engine.OkHttpEngine;

import java.io.File;

/**
 * Created by nickyang on 2018/2/4.
 */

public class FileDownloader {
    // TODO: 2018/4/16 目前默认单线程下载，由于网速低于1M时，下载速度取决于网速，当网速快时，下载速度取决于IO性能，多线程写文件还没有单线程快
    private static final int DEFAULT_TASK_MAX_THREADS = 1; //单个下载任务默认单线程
    private static final String DEFAULT_FILE_DIR;  //默认下载目录
    private static final int DEFAULT_PROGRESS_RATE = 200; //默认300MS更新一次进度
    private static final int DEFAULT_RETRY_TIMES = 2; //默认重试2次

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
    final int progressRate;
    final HttpEngine engine;
    final SnippetHelper snippetHelper;
    final int retryTimes;
    final CodecFactory codecFactory;

    /**
     * 获取默认下载目录
     *
     * @return
     */
    public String getDefaultDirectory() {
        return DEFAULT_FILE_DIR;
    }

    Dispatcher dispatcher() {
        return dispatcher;
    }

    public Task newTask(Request request){
       return new RealTask(this, engine, codecFactory.createCodec(), snippetHelper, request);
    }

    FileDownloader(Builder builder){
        dispatcher = builder.dispatcher;
        downloadFileDir = builder.downloadFileDir;
        maxThreadPerTask = builder.maxThreadPerTask;
        engine = builder.engine;
        snippetHelper = builder.snippetHelper;
        progressRate = builder.progressRate;
        retryTimes = builder.retryTimes;
        codecFactory = builder.codecFactory;
    }

    public static class Builder{
        Dispatcher dispatcher;
        String downloadFileDir;
        int maxThreadPerTask;
        int progressRate;
        HttpEngine engine;
        SnippetHelper snippetHelper;
        int retryTimes;
        CodecFactory codecFactory;

        public Builder(){
            dispatcher = new Dispatcher();
            maxThreadPerTask = DEFAULT_TASK_MAX_THREADS;
            downloadFileDir = DEFAULT_FILE_DIR;
            progressRate = DEFAULT_PROGRESS_RATE;
            engine = new OkHttpEngine();
            snippetHelper = new FileSnippetHelper();
            retryTimes = DEFAULT_RETRY_TIMES;
            codecFactory = new DefaultCodecFactory();
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

        public Builder snippetHelper(SnippetHelper snippetHelper){
            this.snippetHelper = snippetHelper;
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

        public Builder codecFactory(CodecFactory factory){
            this.codecFactory = factory;
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

