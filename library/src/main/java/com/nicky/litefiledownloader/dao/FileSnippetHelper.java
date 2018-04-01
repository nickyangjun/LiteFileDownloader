package com.nicky.litefiledownloader.dao;

import com.nicky.litefiledownloader.Request;
import com.nicky.litefiledownloader.internal.LogUtil;
import com.nicky.litefiledownloader.internal.Util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nickyang on 2018/4/1.
 *
 */

public class FileSnippetHelper implements SnippetHelper {

    @Override
    public Snippet getSnippet(Request request, int num, int startPoint, int endPoint, int downloadedPoint) {
        return new FileSnippet(request, num, startPoint, endPoint, downloadedPoint);
    }

    @Override
    public List<Snippet> getDownloadedSnippets(Request request) {
        final File cacheFile = new File(getCacheFilePath(request));
        final RandomAccessFile cacheAccessFile;
        try {
            cacheAccessFile = new RandomAccessFile(cacheFile, "rwd");
            if (cacheFile.exists() && cacheFile.length() > 0) {// 如果文件存在
                int counts = (int) (cacheAccessFile.length()/12); //每个snippet占12个字节
                cacheAccessFile.seek(0);
                List<Snippet> snippets = new ArrayList<>(counts);
                for (int i = 0; i < counts; i++) {
                    int startPoint = cacheAccessFile.readInt();
                    int endPoint = cacheAccessFile.readInt();
                    int downloadedPoint = cacheAccessFile.readInt();
                    snippets.add(getSnippet(request,i,startPoint,endPoint,downloadedPoint));
                }
                Util.close(cacheAccessFile);
                return snippets;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Util.deleteFile(cacheFile);
        }
        return null;
    }

    @Override
    public void downloadDone(int code, Request request) {
        if(code == CANCEL || code == SUCCESS) {
            Util.deleteFile(getCacheFilePath(request));
        }
    }


    private String getCacheFilePath(Request request) {
        return request.getStoragePath() + request.getFileName() + ".cache";
    }

    private class FileSnippet implements Snippet {
        Request request;
        int num;
        int startPoint;
        int endPoint;
        int downloadedPoint;
        RandomAccessFile cacheAccessFile;
        int seekPoint;
        byte point[] = new byte[4];

        FileSnippet(Request request, int num, int startPoint, int endPoint, int downloadedPoint) {
            this.request = request;
            this.num = num;
            this.startPoint = startPoint;
            this.endPoint = endPoint;
            this.downloadedPoint = downloadedPoint;
            this.seekPoint = num * 12 + 8;
        }

        @Override
        public int getNum() {
            return num;
        }

        @Override
        public int getStartPoint() {
            return startPoint;
        }

        @Override
        public void setStartPoint(int startPoint) {
            this.startPoint = startPoint;
        }

        @Override
        public int getEndPoint() {
            return endPoint;
        }

        @Override
        public void setEndPoint(int endPoint) {
            this.endPoint = endPoint;
        }

        @Override
        public int getDownloadedPoint() {
            return downloadedPoint;
        }

        @Override
        public void updateCurDownloadedPoint(int downloadedPoint) throws IOException {
            this.downloadedPoint = downloadedPoint;
            cacheAccessFile.seek(seekPoint);
            cacheAccessFile.write(writeInt(point, 0, downloadedPoint));

            LogUtil.e(" ----> 分段下载: startPoint: " + startPoint + "  endPoint: " + endPoint + " downloadedPoint: " +
                    downloadedPoint);
            if (downloadedPoint == endPoint) { //分段下载完成
                Util.close(cacheAccessFile);
                LogUtil.e(" ----> 分段下载完成: startPoint: " + startPoint + "  endPoint: " + endPoint);
            }
        }

        @Override
        public void startDownload() throws IOException {
            if (cacheAccessFile == null) {
                cacheAccessFile = new RandomAccessFile(getCacheFilePath(request), "rwd");
            }
        }

        @Override
        public void stopDownload() {
            Util.close(cacheAccessFile);
            cacheAccessFile = null;
        }

        @Override
        public void serializeToLocal() throws IOException {
            if(cacheAccessFile == null) {
                cacheAccessFile = new RandomAccessFile(getCacheFilePath(request), "rwd");
            }

            //定位到自己的位置
            cacheAccessFile.seek(num * 12);

            byte data[] = new byte[12];
            writeInt(data, 0, startPoint);
            writeInt(data, 4, endPoint);
            writeInt(data, 8, downloadedPoint);
            cacheAccessFile.write(data);
        }

        private byte[] writeInt(byte[] bytes, int i, int data) {
            bytes[i] = (byte) ((data >>> 24) & 0xFF);
            bytes[i + 1] = (byte) ((data >>> 16) & 0xFF);
            bytes[i + 2] = (byte) ((data >>> 8) & 0xFF);
            bytes[i + 3] = (byte) (data & 0xFF);
            return bytes;
        }
    }
}
