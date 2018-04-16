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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by nickyang on 2018/4/13.
 *
 * 用FileChannel + RandomAccessFile的分段读写功能实现多线程下载文件
 */

public class ChannelRandomAccessFileCodec implements StreamCodec {
    static final int BUFF_SIZE = 1024*10;

    Request request;
    String tmpDownloadFilePath;
    ThreadLocal<RandomAccessFile> tmpAccessFile;
    ThreadLocal<FileChannel> tmpFileChannels;
    ThreadLocal<java.nio.ByteBuffer> buffers;


    @Override
    public void setRequest(Request request) {
        this.request = request;
        tmpAccessFile = new ThreadLocal<>();
        tmpFileChannels = new ThreadLocal<>();
        buffers = new ThreadLocal<>();
        String tmpFileMD5 = MD5Utils.md5Hex(request.getReqUrl()+".tmp");
        tmpDownloadFilePath = request.getStoragePath() + tmpFileMD5;
    }

    @Override
    public void startSeek(int no, long startPoint, long size) throws IOException {
        RandomAccessFile accessFile = new RandomAccessFile(tmpDownloadFilePath, "rw");
        tmpAccessFile.set(accessFile);

        FileChannel fileChannel = accessFile.getChannel();
        fileChannel.position(startPoint);

        tmpFileChannels.set(fileChannel);

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(BUFF_SIZE);
        buffers.set(byteBuffer);
    }

    @Override
    public int write(int no, byte[] data, int offset, int counts) throws IOException {
        ByteBuffer byteBuffer = buffers.get();
        if (byteBuffer.remaining() >= counts) {
            byteBuffer.put(data, offset, counts);
            return 0;
        }

        FileChannel channel = tmpFileChannels.get();

        byteBuffer.flip();
        int writeSize = byteBuffer.limit();

        channel.write(byteBuffer);
        byteBuffer.clear();
        byteBuffer.put(data, offset, counts);
        return writeSize;
    }

    @Override
    public int flush() throws IOException {
        ByteBuffer byteBuffer = buffers.get();
        byteBuffer.flip();
        int writeSize = byteBuffer.limit();
        if (writeSize > 0) {
            FileChannel channel = tmpFileChannels.get();
            channel.write(byteBuffer);
        }
        byteBuffer.clear();
        return writeSize;
    }

    @Override
    public void onSnippetDone(int no) {
        buffers.remove();
        FileChannel channel = tmpFileChannels.get();
        Util.close(channel);
        RandomAccessFile accessFile = tmpAccessFile.get();
        tmpAccessFile.remove();
        Util.close(accessFile);
    }

    @Override
    public void onSnippetDownloadError(int no, Exception e) {
        buffers.remove();
        FileChannel channel = tmpFileChannels.get();
        Util.close(channel);
        RandomAccessFile accessFile = tmpAccessFile.get();
        tmpAccessFile.remove();
        Util.close(accessFile);
    }

    @Override
    public void allSnippetDone(@Nullable String md5B64, int code) throws IOException {
        if(code == 2){ //取消下载
            Util.deleteFile(tmpDownloadFilePath);
            return;
        }

        if(!TextUtils.isEmpty(md5B64)){
            byte[] md5 = MD5Utils.md5(new FileInputStream(tmpDownloadFilePath));
            String b64 = Base64.encodeBase64String(md5);
            if(!b64.equalsIgnoreCase(md5B64)) {
                throw new IOException("download file check MD5 failed!!!");
            }
        }
        File tmp = new File(tmpDownloadFilePath);
        String real = request.getStoragePath() + request.getFileName();
        boolean result = tmp.renameTo(new File(real));
        if(!result){
            throw new IOException("file rename to " + real +" failed!!!");
        }
    }
}
