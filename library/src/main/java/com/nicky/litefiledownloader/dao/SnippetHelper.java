package com.nicky.litefiledownloader.dao;

import com.nicky.litefiledownloader.Request;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * Created by nickyang on 2018/4/1.
 */

public interface SnippetHelper {
    int SUCCESS = 0;  //下载成功
    int PAUSE = 1;   //下载取消
    int CANCEL = 2;   //下载取消
    int FAIL = 3;   //下载取消


    Snippet getSnippet(Request request, int num, int startPoint, int endPoint, int downloadedPoint);

    List<Snippet> getDownloadedSnippets(Request request);

    /**
     * 只在下载完成或下载取消时回调
     * @param code  { SUCCESS, CANCEL }
     * @param request
     */
    void downloadDone(int code, Request request);

    interface Snippet{

        int getNum();

        int getStartPoint();

        void setStartPoint(int startPoint);

        int getEndPoint();

        void setEndPoint(int endPoint);

        int getDownloadedPoint();

        void updateCurDownloadedPoint(int downloadedPoint) throws IOException;

        void startDownload() throws IOException;

        void stopDownload();

        void serializeToLocal() throws IOException;
    }
}
