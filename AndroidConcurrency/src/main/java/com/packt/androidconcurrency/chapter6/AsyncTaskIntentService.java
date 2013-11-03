package com.packt.androidconcurrency.chapter6;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.packt.androidconcurrency.LaunchActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * A Service that implements Intent-driven behavior like IntentService,
 * but uses AsyncTask to perform the background work instead of
 * IntentService's single worker thread.
 *
 * Note that AsyncTask's level of concurrency varies by platform, so the
 * level of concurrency achieved by this class is 1 for Android 1.6-2.x
 * and up to 128 for other Android versions.
 *
 * See ExecutorIntentService for consistent behaviour across all API levels.
 */
public abstract class AsyncTaskIntentService extends Service {

    private final List<AsyncTask> active = new ArrayList<AsyncTask>();

    /**
     * Runs in the background using AsyncTask, using up to 128 threads.
     *
     * When all requests have been handled, the IntentService stops itself,
     * so you should not call {@link #stopSelf}.
     *
     * @param intent The value passed to {@link
     *               android.content.Context#startService(Intent)}.
     */
    protected abstract void onHandleIntent(Intent intent);

    @Deprecated
    @Override
    public void onStart(Intent intent, final int startId) {
        execute(new AsyncTask<Intent, Void, Void>() {
            @Override
            protected void onPreExecute() {
                active.add(this);
            }

            @Override
            protected Void doInBackground(Intent... params) {
                onHandleIntent(params[0]);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                onComplete();
            }

            @Override
            protected void onCancelled() {
                onComplete();
            }

            private void onComplete() {
                active.remove(this);
                if (active.isEmpty()) {
                    Log.i(LaunchActivity.TAG, "No more active tasks, stopping");
                    AsyncTaskIntentService.this.stopSelf(startId);
                } else {
                    Log.i(LaunchActivity.TAG, active.size() + " tasks still active, not stopping");
                }
            }
        }, intent);
    }

    /**
     * Don't override this - instead implement onHandleIntent.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(LaunchActivity.TAG, "stopping " + getClass().getName());
        for (AsyncTask<Intent,Void,Void> at : active) {
            at.cancel(true);
        }
        Log.i(LaunchActivity.TAG, "stopped " + getClass().getName());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Execute the task using the thread-pool executor on API levels >= 11,
     * or simply by invoking execute otherwise (which on lower API levels
     * has the same effect of running the task with a max concurrency of 128).
     *
     * @param task
     * @param intent
     */
    private void execute(AsyncTask<Intent,Void,Void> task, Intent intent) {
        if (Build.VERSION.SDK_INT >= 11) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, intent);
        } else {
            task.execute(intent);
        }
    }
}