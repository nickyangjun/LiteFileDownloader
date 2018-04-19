package com.nicky.litefiledownloader.demo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nicky.litefiledownloader.DownloadListener;
import com.nicky.litefiledownloader.FileDownloader;
import com.nicky.litefiledownloader.Request;
import com.nicky.litefiledownloader.Task;
import com.nicky.litefiledownloader.annotation.ExecuteMode;
import com.nicky.litefiledownloader.annotation.ThreadMode;
import com.nicky.litefiledownloader.dao.ChannelRandomAccessFileCodec;
import com.nicky.litefiledownloader.dao.CodecFactory;
import com.nicky.litefiledownloader.dao.StreamCodec;
import com.nicky.litefiledownloader.internal.LogUtil;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.et_addr)
    TextView addressText;
    @BindView(R.id.btn_start)
    Button startButton;
    @BindView(R.id.btn_pause)
    Button pauseButton;
    @BindView(R.id.btn_cancel)
    Button cancelButton;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    @BindView(R.id.et_addr1)
    TextView addressText1;
    @BindView(R.id.btn_start1)
    Button startButton1;
    @BindView(R.id.btn_pause1)
    Button pauseButton1;
    @BindView(R.id.btn_cancel1)
    Button cancelButton1;
    @BindView(R.id.progressBar1)
    ProgressBar progressBar1;

    @BindView(R.id.et_addr2)
    TextView addressText2;
    @BindView(R.id.btn_start2)
    Button startButton2;
    @BindView(R.id.btn_pause2)
    Button pauseButton2;
    @BindView(R.id.btn_cancel2)
    Button cancelButton2;
    @BindView(R.id.progressBar2)
    ProgressBar progressBar2;

    @BindView(R.id.recycler)
    RecyclerView recyclerView;

    FileDownloader downloader;
    FileDownloader downloader2;
    Task task;
    Task task1;
    Task task2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        checkPermissions();

        initView();

        downloader = FileDownloader
                .createBuilder()
                .build();

        downloader2 = FileDownloader.createBuilder()
                .codecFactory(new CodecFactory() {
            @Override
            public StreamCodec createCodec() {
                return new ChannelRandomAccessFileCodec();
            }
        }).maxThreadPerTask(4).build();
    }

    private void initView(){
//        String url = "https://res.wx.qq.com/open/zh_CN/htmledition/res/dev/download/sdk/WXVoice_Android_3.0.2.zip";
//        String url = "http://download.xianliao.updrips.com/apk/xianliao.apk";
        String url = "https://gw.alipayobjects.com/os/rmsportal/cTiZiJcYfqAncwCPxKob.zip";
        addressText.setText(url);
        addressText1.setText(url);
        addressText2.setText(url);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new DownloadAdapter());
    }

    @OnClick({R.id.file_test_btn, R.id.btn_start, R.id.btn_pause, R.id.btn_cancel,
              R.id.btn_start1, R.id.btn_pause1, R.id.btn_cancel1,
              R.id.btn_start2, R.id.btn_pause2, R.id.btn_cancel2})
    void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.file_test_btn:
                Intent intent = new Intent(this, FileIOActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_start:
                progressBar.setMax(100);
                Request request = Request
                        .createBuilder()
                        .maxDownloadThreads(1)
                        .url(addressText.getText().toString())
                        .build();
                task = downloader.newTask(request);
                task.enqueue(new FileDownloadListener(progressBar));
                break;
            case R.id.btn_pause:
                if(task == null) return;
                if(pauseButton.getText().toString().equalsIgnoreCase("暂停")){
                    task.pause();
                    pauseButton.setText("继续");
                }else {
                    task.resume();
                    pauseButton.setText("暂停");
                }
                break;
            case R.id.btn_cancel:
                if(task == null) return;
                task.cancel();
                break;

            case R.id.btn_start1:
                progressBar1.setMax(100);
                request = Request
                        .createBuilder()
                        .url(addressText1.getText().toString())
                        .build();
                task1 = downloader2.newTask(request);
                task1.enqueue(new FileDownloadListener(progressBar1));
                break;
            case R.id.btn_pause1:
                if(task1 == null) return;
                if(pauseButton1.getText().toString().equalsIgnoreCase("暂停")){
                    task1.pause();
                    pauseButton1.setText("继续");
                }else {
                    task1.resume();
                    pauseButton1.setText("暂停");
                }
                break;
            case R.id.btn_cancel1:
                if(task1 == null) return;
                task1.cancel();
                break;


            case R.id.btn_start2: {
                progressBar2.setMax(100);
                String url = addressText2.getText().toString();
                int index = url.lastIndexOf("/");
                String name = url.substring(index + 1);
                String path = downloader.getDefaultDirectory() + name;
                DownloadFactory.getInstance().download(url, path, new FileDownLoadObserver(progressBar2));
            }
                break;
            case R.id.btn_pause2: {
                progressBar2.setMax(100);
                String url = addressText2.getText().toString();
                int index = url.lastIndexOf("/");
                String name = url.substring(index + 1);
                String path = downloader.getDefaultDirectory() + name;
                DownloadFactory.getInstance().download(4, url, path, new FileDownLoadObserver(progressBar2));
            }
//                if(task2 == null) return;
//                if(pauseButton2.getText().toString().equalsIgnoreCase("暂停")){
//                    task2.pause();
//                    pauseButton2.setText("继续");
//                }else {
//                    task2.resume();
//                    pauseButton2.setText("暂停");
//                }
                break;
            case R.id.btn_cancel2:{
                progressBar2.setMax(100);
                String url = addressText2.getText().toString();
                int index = url.lastIndexOf("/");
                String name = url.substring(index + 1);
                String path = downloader.getDefaultDirectory() + name;
                DownloadFactory.getInstance().download2(url, path, new FileDownLoadObserver(progressBar2));
            }
//                task2.cancel();
                break;
        }

    }

    class FileDownloadListener implements DownloadListener{
        ProgressBar progressBar;
        long start;

        FileDownloadListener(ProgressBar progressBar){
            this.progressBar = progressBar;
        }

        FileDownloadListener(){}

        public void setProgressBar(ProgressBar progressBar) {
            this.progressBar = progressBar;
        }

        @Override
        @ExecuteMode(threadMode = ThreadMode.MAIN)
        public void onStart(Request request) {
            LogUtil.e("--------> onStart "+request.getReqUrl() + Thread.currentThread().getId());
            start = System.currentTimeMillis();
            progressBar.setMax(100);
        }

        @Override
        @ExecuteMode(threadMode = ThreadMode.MAIN)
        public void onProgress(Request request, long curBytes, long totalBytes) {
            int progress = (int) ((curBytes * 100 / totalBytes));
            progressBar.setProgress(progress);
        }

        @Override
        public void onPause(Request request) {
            LogUtil.e("--------> onPause");
        }

        @Override
        public void onRestart(Request request) {
            LogUtil.e("--------> onRestart");
        }

        @Override
        @ExecuteMode(threadMode = ThreadMode.MAIN)
        public void onFinished(Request request) {
            LogUtil.e("--------> onFinished elapsed time: "+(System.currentTimeMillis() - start) + " MS");
            progressBar.setProgress(100);
        }

        @Override
        @ExecuteMode(threadMode = ThreadMode.MAIN)
        public void onCancel(Request request) {
            LogUtil.e("--------> onCancel");
            progressBar.setProgress(0);
        }

        @Override
        public void onFailed(Request request, Exception e) {
            LogUtil.e("--------> onFailed");
            LogUtil.e( request.getReqUrl()+" "+e.getMessage() +"\n"+ Log.getStackTraceString(e));
        }
    }

    class FileDownLoadObserver extends DownLoadObserver{
        ProgressBar progressBar;
        long start;

        FileDownLoadObserver(ProgressBar progressBar){
            this.progressBar = progressBar;
        }

        @Override
        public void onStart() {
            LogUtil.e("--------> onStart");
            start = System.currentTimeMillis();
            progressBar.setMax(100);
        }

        @Override
        public void onNext(DownloadInfo downloadInfo) {
            int progress = (int) ((downloadInfo.getProgress() * 100 / downloadInfo.getTotal()));
            progressBar.setProgress(progress);
        }

        @Override
        public void onComplete() {
            LogUtil.e("--------> onFinished elapsed time: "+(System.currentTimeMillis() - start) + " MS");
            progressBar.setProgress(100);
        }
    }


    private class DownloadAdapter extends RecyclerView.Adapter<Holder>{
        Map<Integer, TaskCase> requestCaseMap = new HashMap<>(Constants.downloadUrls.length);

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_download,parent,false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(final Holder holder, int position) {
            TaskCase taskCase = requestCaseMap.get(position);
            if(taskCase == null) {
                String url = Constants.downloadUrls[position];
                holder.text.setText(url);
                Request request = Request.createBuilder().url(url).build();
                Task task = downloader.newTask(request);
                taskCase = new TaskCase();
                taskCase.task = task;
                taskCase.listener = new FileDownloadListener();
                requestCaseMap.put(position, taskCase);
            }

            final Task task = taskCase.task;
            final DownloadListener listener = taskCase.listener;
            taskCase.listener.setProgressBar(holder.progressBar);

            if(task.isExecuting()){
                holder.startBtn.setText("暂停");
            }
            holder.startBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(holder.startBtn.getText().equals("开始")){
                        task.enqueue(listener);
                        holder.startBtn.setText("暂停");
                    }else if(holder.startBtn.getText().equals("暂停")){
                        task.pause();
                        holder.startBtn.setText("继续");
                    }else {
                        task.resume();
                        holder.startBtn.setText("暂停");
                    }
                }
            });

            holder.stopBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    task.cancel();
                    holder.progressBar.setProgress(0);
                }
            });
        }

        @Override
        public int getItemCount() {
            return Constants.downloadUrls.length;
        }

        @Override
        public int getItemViewType(int position) {
            return Constants.downloadUrls.length;
        }
    }

    private class TaskCase{
        Task task;
        FileDownloadListener listener;
    }

    private class Holder extends RecyclerView.ViewHolder {
        TextView text;
        Button startBtn;
        Button stopBtn;
        ProgressBar progressBar;

        public Holder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.text);
            startBtn = itemView.findViewById(R.id.btn_start);
            stopBtn = itemView.findViewById(R.id.btn_stop);
            progressBar = itemView.findViewById(R.id.progressBar);
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
