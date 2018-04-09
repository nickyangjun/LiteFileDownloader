package com.nicky.litefiledownloader.demo;

import android.Manifest;
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
    @BindView(R.id.recycler)
    RecyclerView recyclerView;

    FileDownloader downloader;
    Task task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        checkPermissions();

        initView();

        downloader = FileDownloader.createBuilder().maxThreadPerTask(4).build();
    }

    private void initView(){
        addressText.setText("http://download.xianliao.updrips.com/apk/xianliao.apk");

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new DownloadAdapter());
    }

    @OnClick({R.id.btn_start, R.id.btn_pause, R.id.btn_cancel})
    void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.btn_start:
                progressBar.setMax(100);
                Request request = Request
                        .createBuilder()
                        .url(addressText.getText().toString())
//                        .maxDownloadThreads(2)
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
                task.cancel();
                break;
        }

    }

    class FileDownloadListener implements DownloadListener{
        ProgressBar progressBar;

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
            LogUtil.e("--------> onStart");
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
            LogUtil.e("--------> onFinished");
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
            LogUtil.e(e.getMessage() + Log.getStackTraceString(e));
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
