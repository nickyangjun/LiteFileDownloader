package com.nicky.litefiledownloader.engine;


import android.support.annotation.Nullable;

import com.nicky.litefiledownloader.internal.LogUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Created by nickyang on 2018/3/29.
 */

public class OkHttpEngine implements HttpEngine {

    private OkHttpClient mOkHttpClient;
    //连接超时时间
    private static final int CONNECT_TIME_OUT = 10;
    //读写超时时间
    private static final int IO_TIME_OUT = 30;

    public OkHttpEngine(){
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(CONNECT_TIME_OUT, TimeUnit.SECONDS)
                .readTimeout(IO_TIME_OUT, TimeUnit.SECONDS)
                .writeTimeout(IO_TIME_OUT, TimeUnit.SECONDS)
                .addInterceptor(LogInterceptor());
        mOkHttpClient = builder.build();
    }

    private static HttpLoggingInterceptor LogInterceptor(){
        return new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(String message) {
                LogUtil.e("Http Request  =====  ", "log: " + message);
            }
        }).setLevel(HttpLoggingInterceptor.Level.HEADERS);//设置打印数据的级别
    }



    @Override
    public Call getHttpReq(String url) throws IOException {
        Request request = new Request
                .Builder()
                .url(url)
                .build();
        return get(request);
    }

    @Override
    public Call getHttpReq(String url, long startPosition, long endPosition) throws IOException {
        Request request = new Request
                .Builder()
                .header("RANGE", "bytes=" + startPosition + "-" + endPosition)
                .url(url)
                .build();
        return get(request);
    }

    private Call get(Request request) throws IOException {
        okhttp3.Call call = mOkHttpClient.newCall(request);
        return new HttpCall(call);
    }

    class HttpCall implements Call{
        okhttp3.Call call;

        HttpCall(okhttp3.Call call){
            this.call = call;
        }

        @Override
        public void cancel() {
            this.call.cancel();
        }

        @Override
        public boolean isCancel() {
            return this.call.isCanceled();
        }

        @Override
        public Response execute() throws IOException {
            okhttp3.Response response = call.execute();
            return new HttpResponse(response);
        }
    }

    class HttpResponse implements Response{
        okhttp3.Response response;
        int code;
        HttpHeaders httpHeaders;
        HttpResponseBody httpResponseBody;

        HttpResponse(okhttp3.Response response){
            this.response = response;
            code = response.code();
            httpHeaders = new HttpHeaders(response.headers());
            httpResponseBody =  new HttpResponseBody(response.body());
        }

        @Override
        public String reqUrl() {
            return response.request().url().toString();
        }

        @Override
        public Headers headers() {
            return httpHeaders;
        }

        @Override
        public int code() {
            return code;
        }

        @Override
        public ResponseBody body() {
            return httpResponseBody;
        }
    }

    class HttpHeaders implements Headers{
        okhttp3.Headers headers;

        HttpHeaders(okhttp3.Headers headers){
            this.headers = headers;
        }

        @Nullable
        @Override
        public String header(String name) {
            return headers.get(name);
        }
    }

    class HttpResponseBody implements ResponseBody{
        okhttp3.ResponseBody responseBody;

        HttpResponseBody(okhttp3.ResponseBody responseBody){
            this.responseBody = responseBody;
        }

        @Override
        public InputStream byteStream() {
            return responseBody.byteStream();
        }
    }
}
