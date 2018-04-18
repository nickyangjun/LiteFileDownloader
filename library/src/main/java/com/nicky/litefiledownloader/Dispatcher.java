package com.nicky.litefiledownloader;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.Nullable;

import com.nicky.litefiledownloader.internal.Util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by nickyang on 2018/3/29.
 */

public final class Dispatcher {
    private int maxRequests = 24;
    private @Nullable
    Runnable idleCallback;

    /** Executes calls. Created lazily. */
    private @Nullable
    ExecutorService executorService;

    /** Ready async calls in the order they'll be run. */
    private final Deque<RealTask.AsyncTask> readyAsyncCalls = new ArrayDeque<>();

    /** Running asynchronous calls. Includes canceled calls that haven't finished yet. */
    private final Deque<RealTask.AsyncTask> runningAsyncCalls = new ArrayDeque<>();

    /** Running synchronous calls. Includes canceled calls that haven't finished yet. */
    private final Deque<RealTask> runningSyncCalls = new ArrayDeque<>();

    public Dispatcher(ExecutorService executorService) {
        this.executorService = executorService;
    }

    private HandlerThread mThread;
    private Handler mAsyncHandler;
    private Handler mMainHandler;

    public Dispatcher() {
        mThread = new HandlerThread("DispatcherCallback", Process.THREAD_PRIORITY_BACKGROUND);
        mThread.start();
        mAsyncHandler = new Handler(mThread.getLooper(), new AsyncThreadCallback());
        mMainHandler = new Handler(Looper.getMainLooper(), new AsyncThreadCallback());
    }

    public synchronized ExecutorService executorService() {
        if (executorService == null) {
            executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(), Util.threadFactory("LiteFileDownloader Dispatcher", false));
        }
        return executorService;
    }

    /**
     * Set a callback to be invoked each time the dispatcher becomes idle (when the number of running
     * calls returns to zero).
     *
     */
    public synchronized void setIdleCallback(@Nullable Runnable idleCallback) {
        this.idleCallback = idleCallback;
    }

    synchronized void enqueue(RealTask.AsyncTask task) {
        if (runningAsyncCalls.size() < maxRequests) {
            runningAsyncCalls.add(task);
            executorService().execute(task);
        } else {
            readyAsyncCalls.add(task);
        }
    }

    public synchronized void setMaxRequests(int maxRequests) {
        if (maxRequests < 1) {
            throw new IllegalArgumentException("max < 1: " + maxRequests);
        }
        this.maxRequests = maxRequests;
        promoteTasks();
    }

    public synchronized int getMaxRequests() {
        return maxRequests;
    }

    private void promoteTasks() {
        if (runningAsyncCalls.size() >= maxRequests) return; // Already running max capacity.
        if (readyAsyncCalls.isEmpty()) return; // No ready calls to promote.

        for (Iterator<RealTask.AsyncTask> i = readyAsyncCalls.iterator(); i.hasNext(); ) {
            RealTask.AsyncTask call = i.next();

            i.remove();
            runningAsyncCalls.add(call);
            executorService().execute(call);

            if (runningAsyncCalls.size() >= maxRequests) return; // Reached max capacity.
        }
    }

    synchronized void executed(RealTask task) {
        runningSyncCalls.add(task);
    }

    void finished(RealTask.AsyncTask call) {
        finished(runningAsyncCalls, call, true);
    }

    void finished(RealTask call) {
        finished(runningSyncCalls, call, false);
    }

    private <T> void finished(Deque<T> calls, T call, boolean promoteCalls) {
        int runningCallsCount;
        Runnable idleCallback;
        synchronized (this) {
            if (!calls.remove(call)) throw new AssertionError("Call wasn't in-flight!");
            if (promoteCalls) promoteTasks();
            runningCallsCount = runningCallsCount();
            idleCallback = this.idleCallback;
        }

        if (runningCallsCount == 0 && idleCallback != null) {
            idleCallback.run();
        }
    }

    public synchronized int runningCallsCount() {
        return runningAsyncCalls.size() + runningSyncCalls.size();
    }

    private class AsyncThreadCallback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            RealTask realTask = (RealTask) msg.obj;
            realTask.handleCallback(msg.what);
            return true;
        }
    }

    void postCallback(int code, RealTask realTask){
        mAsyncHandler.obtainMessage(code,realTask).sendToTarget();
    }

    void postDelayCallback(int delayMillis, int code, RealTask realTask){
        Message message = mAsyncHandler.obtainMessage(code,realTask);
        mAsyncHandler.sendMessageDelayed(message, delayMillis);
    }

    void removeCallback(int code, RealTask realTask){
        mAsyncHandler.removeMessages(code, realTask);
    }

    void postMainCallback(int code, RealTask realTask){
        mMainHandler.obtainMessage(code,realTask).sendToTarget();
    }

    void postMainDelayCallback(int delayMillis, int code, RealTask realTask){
        Message message = mMainHandler.obtainMessage(code,realTask);
        mMainHandler.sendMessageDelayed(message, delayMillis);
    }

    void removeMainCallback(int code, RealTask realTask){
        mMainHandler.removeMessages(code, realTask);
    }
}
