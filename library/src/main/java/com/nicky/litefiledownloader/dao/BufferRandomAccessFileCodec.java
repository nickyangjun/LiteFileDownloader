package com.nicky.litefiledownloader.dao;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.nicky.litefiledownloader.Request;
import com.nicky.litefiledownloader.internal.Util;
import com.nicky.litefiledownloader.internal.binary.Base64;
import com.nicky.litefiledownloader.internal.binary.MD5Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by nickyang on 2018/4/13.
 * <p>
 * 用RandomAccessFile的分段读写功能实现多线程下载文件
 */

public class BufferRandomAccessFileCodec implements StreamCodec {
    static final int BUFF_SIZE = 1024*10;

    Request request;
    String tmpDownloadFilePath;
    ThreadLocal<RandomAccessFile> tmpAccessFile;
    ThreadLocal<ByteBuffer> buffers;

    @Override
    public void setRequest(Request request) {
        this.request = request;
        tmpAccessFile = new ThreadLocal<>();
        buffers = new ThreadLocal<>();
        String tmpFileMD5 = MD5Utils.md5Hex(request.getReqUrl() + ".tmp");
        tmpDownloadFilePath = request.getStoragePath() + tmpFileMD5;
    }

    @Override
    public void startSeek(int no, long startPoint, long size) throws IOException {
        RandomAccessFile accessFile = new RandomAccessFile(tmpDownloadFilePath, "rw");
        tmpAccessFile.set(accessFile);
        accessFile.seek(startPoint);// 文件写入的开始位置.
        ByteBuffer byteBuffer = new ByteBuffer(BUFF_SIZE);
        buffers.set(byteBuffer);
    }

    @Override
    public int write(int no, byte[] data, int offset, int counts) throws IOException {
        ByteBuffer byteBuffer = buffers.get();
        if (byteBuffer.remaining() >= counts) {
            byteBuffer.put(data, offset, counts);
            return 0;
        }
        RandomAccessFile accessFile = tmpAccessFile.get();
        int writeSize = byteBuffer.size();
        accessFile.write(byteBuffer.getBuffer(), 0, writeSize);
        byteBuffer.reset();
        byteBuffer.put(data, offset, counts);
        return writeSize;
    }

    @Override
    public int flush() throws IOException {
        ByteBuffer byteBuffer = buffers.get();
        int writeSize = byteBuffer.size();
        if (writeSize > 0) {
            RandomAccessFile accessFile = tmpAccessFile.get();
            accessFile.write(byteBuffer.getBuffer(), 0, writeSize);
        }
        return writeSize;
    }

    @Override
    public void onSnippetDone(int no) {
        RandomAccessFile accessFile = tmpAccessFile.get();
        tmpAccessFile.remove();
        Util.close(accessFile);
    }

    @Override
    public void onSnippetDownloadError(int no, Exception e) {
        RandomAccessFile accessFile = tmpAccessFile.get();
        Util.close(accessFile);
    }

    @Override
    public void allSnippetDone(@Nullable String md5B64, int code) throws IOException {
        if (code == 2) { //取消下载
            Util.deleteFile(tmpDownloadFilePath);
            return;
        }

        if (!TextUtils.isEmpty(md5B64)) {
            byte[] md5 = MD5Utils.md5(new FileInputStream(tmpDownloadFilePath));
            String b64 = Base64.encodeBase64String(md5);
            if (!b64.equalsIgnoreCase(md5B64)) {
                throw new IOException("download file check MD5 failed!!!");
            }
        }

        File tmp = new File(tmpDownloadFilePath);
        String real = request.getStoragePath() + request.getFileName();
        boolean result = tmp.renameTo(new File(real));
        if (!result) {
            throw new IOException("file rename to " + real + " failed!!!");
        }
    }
}
