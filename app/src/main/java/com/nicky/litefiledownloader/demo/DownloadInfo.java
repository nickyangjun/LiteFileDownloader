package com.nicky.litefiledownloader.demo;

/**
 * 下载信息实体
 * Created by seaky on 2017/4/24.
 */

public class DownloadInfo {
    //获取文件大小失败时，设置-1， 有些文件下载前无法获取大小
    public static final long TOTAL_ERROR = -1;

    private String url;
    private long total;
    private long progress;
    private String filePath;

    public DownloadInfo(String url, String filePath){
        this.url = url;
        this.filePath = filePath;
    }

    public String getUrl() {
        return url;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getProgress() {
        if(this.total == TOTAL_ERROR){
            return 0;  //无法获取文件总大小时，都认为下载了0
        }
        return progress;
    }

    public void setProgress(long progress) {
        this.progress = progress;
    }

    public boolean isGetFileTotalLengthOk(){
        return total != TOTAL_ERROR;
    }

    public boolean isFileDownloadFinish(){
        return total == progress;
    }
}
