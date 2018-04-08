package com.nicky.litefiledownloader.dao;

import com.nicky.litefiledownloader.Request;

import java.io.IOException;
import java.util.List;

/**
 * Created by nickyang on 2018/4/1.
 */

public interface SnippetHelper {

    /**
     * 生成一个下载分段记录
     * @param request               当前下载请求
     * @param num                   分段序号， 从0开始
     * @param startPoint            分段下载其实点
     * @param endPoint              分段下载结束点
     * @param downloadedPoint       当前以及下载的位置  startPoint  <= downloadedPoint  <= endPoint+1
     * @return
     */
    Snippet getSnippet(Request request, int num, long startPoint, long endPoint, long downloadedPoint);


    /**
     * 根据请求，获取所有下载分段记录，在下载开始前，会调用此方法查询本地下载记录，如果有下载分段记录，则继续按原分段下载
     * @param request
     * @return
     */
    List<Snippet> getDownloadedSnippets(Request request);


    /**
     * 只在下载完成或下载取消时回调，应该在此方法里清除所有Snippet记录
     * @param code 1 下载成功， 2 下载取消
     * @param request
     */
    void downloadDone(int code, Request request);




    /**
     * 代表一个下载分段记录
     */
    interface Snippet{

        int getNum();

        long getStartPoint();

        void setStartPoint(long startPoint);

        long getEndPoint();

        void setEndPoint(long endPoint);

        long getDownloadedPoint();

        void updateCurDownloadedPoint(long downloadedPoint) throws IOException;

        void startDownload() throws IOException;

        void stopDownload();

        /**
         * 分段记录序列化保存到本地
         * @throws IOException
         */
        void serializeToLocal() throws IOException;
    }
}
