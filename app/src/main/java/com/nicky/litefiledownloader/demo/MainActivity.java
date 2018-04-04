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
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nicky.litefiledownloader.DownloadListener;
import com.nicky.litefiledownloader.FileDownloader;
import com.nicky.litefiledownloader.Request;
import com.nicky.litefiledownloader.Task;
import com.nicky.litefiledownloader.internal.LogUtil;

import java.util.HashMap;
import java.util.Map;

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
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.btn_batch_start)
    Button batchStart;
    @BindView(R.id.btn_batch_stop)
    Button batchStop;
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
        addressText.setText("http://chatfile.updrips.com/1522398701156_10002_90000005172_8Efxdd.png");

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
//                        .maxDownloadThreads(1)
                        .build();
                task = downloader.newTask(request);
                task.enqueue(new FileDownloadListener(progressBar));
                break;
            case R.id.btn_pause:
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
            progressBar.setMax(100);
        }

        @Override
        public void onStart() {

        }

        @Override
        public void onProgress(final int progress) {
            progressBar.post(new Runnable() {
                @Override
                public void run() {
                    progressBar.setProgress(progress);
                }
            });
        }

        @Override
        public void onPause() {

        }

        @Override
        public void onRestart() {

        }

        @Override
        public void onFinished() {
            LogUtil.e("--------> onFinished");
            progressBar.post(new Runnable() {
                @Override
                public void run() {
                    progressBar.setProgress(100);
                }
            });
        }

        @Override
        public void onCancel() {
            progressBar.post(new Runnable() {
                @Override
                public void run() {
                    progressBar.setProgress(0);
                }
            });
        }

        @Override
        public void onFailed(Exception e) {
            LogUtil.e(e.getMessage() + Log.getStackTraceString(e));
        }
    }


    private class DownloadAdapter extends RecyclerView.Adapter<Holder>{
        Map<Integer, RequestCase> requestCaseMap = new HashMap<>(Constants.downloadUrls.length);

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_download,parent,false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(final Holder holder, int position) {
            RequestCase requestCase = requestCaseMap.get(position);
            if(requestCase == null) {
                String url = Constants.downloadUrls[position];
                holder.text.setText(url);
                Request request = Request.createBuilder().url(url).build();
                requestCase = new RequestCase();
                requestCase.request = request;
                requestCaseMap.put(position, requestCase);
            }

            final Task task = downloader.newTask(requestCase.request);
            holder.startBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    task.enqueue(new FileDownloadListener(holder.progressBar));
                    holder.startBtn.setText("暂停");
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

    private class RequestCase{
        Request request;
        int progres;
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
