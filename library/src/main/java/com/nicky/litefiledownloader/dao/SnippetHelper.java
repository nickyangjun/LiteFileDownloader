package com.nicky.litefiledownloader.dao;

import com.nicky.litefiledownloader.Request;

import java.io.IOException;
import java.util.List;

/**
 * Created by nickyang on 2018/4/1.
 */

public interface SnippetHelper {

    Snippet getSnippet(Request request, int num, long startPoint, long endPoint, long downloadedPoint);

    List<Snippet> getDownloadedSnippets(Request request);

    /**
     * 只在下载完成或下载取消时回调，应该在此方法里清除所有Snippet记录
     * @param code 0 下载成功， 1 下载取消
     * @param request
     */
    void downloadDone(int code, Request request);

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

        void serializeToLocal() throws IOException;
    }
}
