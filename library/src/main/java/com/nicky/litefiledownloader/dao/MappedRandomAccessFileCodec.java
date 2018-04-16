package com.nicky.litefiledownloader.dao;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.nicky.litefiledownloader.Request;
import com.nicky.litefiledownloader.internal.LogUtil;
import com.nicky.litefiledownloader.internal.Util;
import com.nicky.litefiledownloader.internal.binary.Base64;
import com.nicky.litefiledownloader.internal.binary.MD5Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by nickyang on 2018/4/13.
 *
 *
 * 用MappedByteBuffer的分段读写功能实现多线程下载文件
 */
@Deprecated // TODO: 2018/4/16 存在signal 7 异常，暂时不用
public class MappedRandomAccessFileCodec implements StreamCodec {
    Request request;
    String tmpDownloadFilePath;
    ThreadLocal<RandomAccessFile> tmpAccessFile;
    ThreadLocal<MappedByteBuffer> tmpMappedByteBuffers;


    @Override
    public void setRequest(Request request) {
        this.request = request;
        tmpAccessFile = new ThreadLocal<>();
        tmpMappedByteBuffers = new ThreadLocal<>();
        String tmpFileMD5 = MD5Utils.md5Hex(request.getReqUrl()+".tmp");
        tmpDownloadFilePath = request.getStoragePath() + tmpFileMD5;
    }

    @Override
    public void startSeek(int no, long startPoint, long size) throws IOException {
        RandomAccessFile accessFile = new RandomAccessFile(tmpDownloadFilePath, "rw");

        FileChannel fileChannel = accessFile.getChannel();
        LogUtil.e(" NO:" + no +" mapper start: "+ startPoint + " size: "+ size);
        MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE,startPoint,size);

        tmpAccessFile.set(accessFile);
        tmpMappedByteBuffers.set(mappedByteBuffer);
    }

    @Override
    public int write(int no, byte[] data, int offset, int counts) throws IOException {
        MappedByteBuffer buffer = tmpMappedByteBuffers.get();
        buffer.put(data,offset,counts);
        return counts;
    }

    @Override
    public int flush() {
        return 0;
    }

    @Override
    public void onSnippetDone(int no) {
        MappedByteBuffer buffer = tmpMappedByteBuffers.get();
        buffer.force();
        buffer.clear();
        tmpMappedByteBuffers.remove();

        RandomAccessFile accessFile = tmpAccessFile.get();
        tmpAccessFile.remove();
        Util.close(accessFile);
    }

    @Override
    public void onSnippetDownloadError(int no, Exception e) {
        MappedByteBuffer buffer = tmpMappedByteBuffers.get();
        if(buffer != null) {
            buffer.force();
            buffer.clear();
        }
        RandomAccessFile accessFile = tmpAccessFile.get();
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
