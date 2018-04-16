package com.nicky.litefiledownloader.dao;

import com.nicky.litefiledownloader.Request;
import com.nicky.litefiledownloader.internal.Util;
import com.nicky.litefiledownloader.internal.binary.MD5Utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nickyang on 2018/4/1.
 * 通过RandomAccessFile来保存各个下载分段记录
 */

public class ChannelFileSnippetHelper implements SnippetHelper {

    @Override
    public Snippet getSnippet(Request request, int num, long startPoint, long endPoint, long downloadedPoint) {
        return new FileSnippet(request, num, startPoint, endPoint, downloadedPoint);
    }

    @Override
    public List<Snippet> getDownloadedSnippets(Request request) {
        final File cacheFile = new File(getCacheFilePath(request));
        if(!cacheFile.exists()) return null;

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
        String cacheMD5 = MD5Utils.md5Hex(request.getReqUrl()+".cache");
        return request.getStoragePath() + cacheMD5;
    }

    private class FileSnippet implements Snippet {
        Request request;
        int num;
        long startPoint;
        long endPoint;
        long downloadedPoint;
        RandomAccessFile cacheAccessFile;
        long seekPoint;
        FileChannel cacheFileChannels;
        java.nio.ByteBuffer buffers;

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
            cacheFileChannels.position(seekPoint);
            buffers.clear();
            buffers.putLong(downloadedPoint);
            buffers.flip();
            cacheFileChannels.write(buffers);

//            LogUtil.e(" ----> 分段下载: startPoint: " + startPoint + "  endPoint: " + endPoint + " downloadedPoint: "
//                    + downloadedPoint);
        }

        @Override
        public void startDownload() throws IOException {
            initCacheFile();
        }

        @Override
        public void stopDownload() {
            buffers.clear();
            Util.close(cacheFileChannels);
            Util.close(cacheAccessFile);
            cacheAccessFile = null;
        }

        private void initCacheFile() throws IOException {
            if (cacheAccessFile == null) {
                cacheAccessFile = new RandomAccessFile(getCacheFilePath(request), "rwd");
                cacheFileChannels = cacheAccessFile.getChannel();
                buffers = java.nio.ByteBuffer.allocate(8);
            }
        }

        @Override
        public void serializeToLocal() throws IOException {
            initCacheFile();
            //定位到自己的位置
            cacheFileChannels.position(num * 24);
            buffers.putLong(startPoint);
            buffers.flip();
            cacheFileChannels.write(buffers);
            buffers.clear();
            buffers.putLong(endPoint);
            buffers.flip();
            cacheFileChannels.write(buffers);
            buffers.clear();
            buffers.putLong(downloadedPoint);
            buffers.flip();
            cacheFileChannels.write(buffers);
            buffers.clear();
        }

        @Override
        public String toString() {
            return "req: "+request.getReqUrl() + " NO:"+num+" start:"+startPoint+" end:"+endPoint+" cur:"+downloadedPoint;
        }
    }
}
