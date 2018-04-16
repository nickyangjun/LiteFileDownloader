package com.nicky.litefiledownloader.demo;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.nicky.litefiledownloader.internal.LogUtil;
import com.nicky.litefiledownloader.internal.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class FileIOActivity extends AppCompatActivity {

    static String DEFAULT_FILE_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +
            "fileDownload" + File.separator;
    static int TEST_COUNTS = 10000;

    @BindView(R.id.accessFile)
    Button accessBtn;
    @BindView(R.id.channelFile)
    Button channelBtn;

    @BindView(R.id.text)
    TextView text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_io);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ButterKnife.bind(this);


    }

    @OnClick({R.id.channelFile, R.id.accessFile})
    void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.accessFile:
                try {
                    new Thread(new AccessRunnable()).start();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.channelFile:
                try {
                    new Thread(new ChannelRunnable()).start();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    class AccessRunnable implements Runnable {
        RandomAccessFile cacheAccessFile;
        long seekPoint = 2;
        byte[] points;

        AccessRunnable() throws FileNotFoundException {
            cacheAccessFile = new RandomAccessFile(DEFAULT_FILE_DIR + "accessFile", "rwd");
            points = new byte[1024*10];
            for(int i=0;i<points.length;i++){
                points[i] = (byte) (i & 0xff);
            }
        }

        @Override
        public void run() {
            long t = System.currentTimeMillis();
            LogUtil.e("accessFile --------------------->");
            try {
                for (int i = 0; i < TEST_COUNTS; i++) {
//                    cacheAccessFile.seek(seekPoint);
                    cacheAccessFile.write(points,0,points.length);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                Util.close(cacheAccessFile);
                LogUtil.e("accessFile --------------------->"+(System.currentTimeMillis() - t));
            }
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


    class ChannelRunnable implements Runnable {
        RandomAccessFile cacheAccessFile;
        long seekPoint = 2;
        FileChannel cacheFileChannels;
        java.nio.ByteBuffer buffers;
        byte[] points;

        ChannelRunnable() throws FileNotFoundException {
            cacheAccessFile = new RandomAccessFile(DEFAULT_FILE_DIR + "channelFile", "rwd");
            cacheFileChannels = cacheAccessFile.getChannel();
            buffers = java.nio.ByteBuffer.allocateDirect(1024*10);
            points = new byte[1024*10];
            for(int i=0;i<points.length;i++){
                points[i] = (byte) (i & 0xff);
            }
        }

        @Override
        public void run() {
            long t = System.currentTimeMillis();
            LogUtil.e("channelFile --------------------->");
            try {
                for (int i = 0; i < TEST_COUNTS; i++) {
//                    cacheFileChannels.position(seekPoint);
                    buffers.clear();
                    buffers.put(points);
                    buffers.flip();
                    cacheFileChannels.write(buffers);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                buffers.clear();
                Util.close(cacheFileChannels);
                Util.close(cacheAccessFile);
                LogUtil.e("channelFile --------------------->"+(System.currentTimeMillis() - t));
            }
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
