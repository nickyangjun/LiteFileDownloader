package com.nicky.litefiledownloader.demo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.nicky.litefiledownloader.DownloadListener;
import com.nicky.litefiledownloader.FileDownloader;
import com.nicky.litefiledownloader.Request;
import com.nicky.litefiledownloader.Task;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.et_addr)
    EditText addressText;
    @BindView(R.id.btn_start)
    Button startButton;
    @BindView(R.id.btn_pause)
    Button pauseButton;
    @BindView(R.id.btn_cancel)
    Button cancelButton;

    FileDownloader downloader;
    Task task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

//        addressText.setText("http://download.xianliao.updrips.com/apk/xianliao.apk");
        addressText.setText("http://chatfile.updrips.com/1522398701156_10002_90000005172_8Efxdd.png");

        downloader = FileDownloader.createBuilder().maxThreadPerTask(4).build();

        checkPermissions();
    }

    @OnClick({R.id.btn_start, R.id.btn_pause, R.id.btn_cancel})
    void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.btn_start:
                Request request = Request.createBuilder().url(addressText.getText().toString()).build();
                task = downloader.newTask(request);
                task.enqueue(new DownloadListener() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onProgress(float progress) {

                    }

                    @Override
                    public void onPause() {

                    }

                    @Override
                    public void onFinished() {

                    }

                    @Override
                    public void onCancel() {

                    }

                    @Override
                    public void onFailed(Exception e) {

                    }
                });
                break;
            case R.id.btn_pause:
                task.pause();
                break;
            case R.id.btn_cancel:
                task.cancel();
                break;
        }

    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager
                .PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    0);
        }
    }
}
