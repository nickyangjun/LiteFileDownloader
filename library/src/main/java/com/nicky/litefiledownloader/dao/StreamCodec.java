package com.nicky.litefiledownloader.dao;

import android.support.annotation.Nullable;

import com.nicky.litefiledownloader.Request;

import java.io.IOException;

/**
 * Created by nickyang on 2018/4/13.
 *
 * 此接口用于自定义保存下载文件，默认实现了RandomAccessFileCodec, 详情见RandomAccessFileCodec
 */

public interface StreamCodec {

    void setRequest(Request request);

    /**
     * 下载时文件分段定位, 每个下载线程在下载前会调用此方法，且只调用一次
     * @param no            第几个下载线程编号 >= 0
     * @param startPoint    下载点其实位置
     * @param size          总的大小
     */
    void startSeek(int no, long startPoint, long size) throws IOException;

    /**
     * 下载数据
     * @param no        第几个下载线程编号 >= 0
     * @param data      从网络上下载的数据
     * @param offset    data的起始位置
     * @param counts    data的有效数据大小
     *
     * @return          写入分段下载文件中的字节数
     */
    int write(int no, byte[] data, int offset, int counts) throws IOException;

    /**
     * 下载完毕回调，socket数据读取完毕
     *
     * @return 回写数据大小
     */
    int flush() throws IOException;

    /**
     * 某个分段已经下载完
     * @param no 第几个下载线程编号 >= 0
     */
    void onSnippetDone(int no);

    /**
     * 某个分段下载异常
     * @param no    第几个下载线程编号 >= 0
     * @param e     异常
     */
    void onSnippetDownloadError(int no, Exception e);

    /**
     * 所有分段下载完毕 或 取消，一次下载最多只会调用一次
     * @param md5B64       所下载文件的MD5码，经过了Base64编码，有可能没有，可用于校验文件是否下载正确
     * @param code         1 下载完毕，2 取消下载
     *
     * 如果抛出异常代表下载失败
     */
    void allSnippetDone(@Nullable String md5B64, int code) throws IOException;

}
