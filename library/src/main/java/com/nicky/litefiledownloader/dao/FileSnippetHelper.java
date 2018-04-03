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
 * 通过RandomAccessFile来保存各个下载分段记录
 */

public class FileSnippetHelper implements SnippetHelper {

    @Override
    public Snippet getSnippet(Request request, int num, long startPoint, long endPoint, long downloadedPoint) {
        return new FileSnippet(request, num, startPoint, endPoint, downloadedPoint);
    }

    @Override
    public List<Snippet> getDownloadedSnippets(Request request) {
        final File cacheFile = new File(getCacheFilePath(request));
        final RandomAccessFile cacheAccessFile;
        try {
            cacheAccessFile = new RandomAccessFile(cacheFile, "rwd");
            if (cacheFile.exists() && cacheFile.length() > 0) {// 如果文件存在
                int counts = (int) (cacheAccessFile.length() / 24); //每个snippet占24个字节
                cacheAccessFile.seek(0);
                List<Snippet> snippets = new ArrayList<>(counts);
                for (int i = 0; i < counts; i++) {
                    long startPoint = cacheAccessFile.readLong();
                    long endPoint = cacheAccessFile.readLong();
                    long downloadedPoint = cacheAccessFile.readLong();
                    snippets.add(getSnippet(request, i, startPoint, endPoint, downloadedPoint));
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
        Util.deleteFile(getCacheFilePath(request));
    }


    private String getCacheFilePath(Request request) {
        return request.getStoragePath() + request.getFileName() + ".cache";
    }

    private class FileSnippet implements Snippet {
        Request request;
        int num;
        long startPoint;
        long endPoint;
        long downloadedPoint;
        RandomAccessFile cacheAccessFile;
        long seekPoint;
        byte point[] = new byte[8];

        FileSnippet(Request request, int num, long startPoint, long endPoint, long downloadedPoint) {
            this.request = request;
            this.num = num;
            this.startPoint = startPoint;
            this.endPoint = endPoint;
            this.downloadedPoint = downloadedPoint;
            this.seekPoint = num * 24 + 16;
        }

        @Override
        public int getNum() {
            return num;
        }

        @Override
        public long getStartPoint() {
            return startPoint;
        }

        @Override
        public void setStartPoint(long startPoint) {
            this.startPoint = startPoint;
        }

        @Override
        public long getEndPoint() {
            return endPoint;
        }

        @Override
        public void setEndPoint(long endPoint) {
            this.endPoint = endPoint;
        }

        @Override
        public long getDownloadedPoint() {
            return downloadedPoint;
        }

        @Override
        public void updateCurDownloadedPoint(long downloadedPoint) throws IOException {
            this.downloadedPoint = downloadedPoint;
            cacheAccessFile.seek(seekPoint);
            cacheAccessFile.write(writeLong(point, 0, downloadedPoint));

//            LogUtil.e(" ----> 分段下载: startPoint: " + startPoint + "  endPoint: " + endPoint + " downloadedPoint: " +
//                    downloadedPoint);
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
            if (cacheAccessFile == null) {
                cacheAccessFile = new RandomAccessFile(getCacheFilePath(request), "rwd");
            }

            //定位到自己的位置
            cacheAccessFile.seek(num * 24);

            byte data[] = new byte[24];
            writeLong(data, 0, startPoint);
            writeLong(data, 8, endPoint);
            writeLong(data, 16, downloadedPoint);
            cacheAccessFile.write(data);
        }

        private byte[] writeLong(byte[] bytes, int i, long data) {
            bytes[i] = (byte) ((data >>> 56) & 0xFF);
            bytes[i + 1] = (byte) ((data >>> 48) & 0xFF);
            bytes[i + 2] = (byte) ((data >>> 40) & 0xFF);
            bytes[i + 3] = (byte) ((data >>> 32) & 0xFF);
            bytes[i + 4] = (byte) ((data >>> 24) & 0xFF);
            bytes[i + 5] = (byte) ((data >>> 16) & 0xFF);
            bytes[i + 6] = (byte) ((data >>> 8) & 0xFF);
            bytes[i + 7] = (byte) (data & 0xFF);
            return bytes;
        }
    }
}
