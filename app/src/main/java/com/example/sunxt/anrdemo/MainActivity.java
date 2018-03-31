package com.example.sunxt.anrdemo;

import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Matrix;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Process;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AnrDemo";

    private static final String GRADLE_TASK_TREE_LOG = "task-tree.log";

    private Object lock1 = new Object();
    private Object lock2 = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(TAG, "activity pid : " + Process.myPid());

        // 主线程慢代码
        Button slowCodeMainThread = findViewById(R.id.btn_slow_code_main_thread);
        slowCodeMainThread.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                slowCode();
//                new AsyncTask<Integer, Integer, Matrix>() {
//
//                    @Override
//                    protected Matrix doInBackground(Integer... integers) {
//                        slowCode();
//                        return null;
//                    }
//                }.execute(0);
            }
        });

        // 主线程io
        Button ioMainThread = findViewById(R.id.btn_io_main_thread);
        ioMainThread.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < 100; i++) {
                    copyFile();
                }
            }
        });

        Button lockContention = findViewById(R.id.btn_lock_contention);
        lockContention.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (lock1) {
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                synchronized (lock1) {
                    Log.i(TAG, "main thread get lock1");
                }
            }
        });

        // 死锁
        Button deadLockBtn = findViewById(R.id.btn_deadlock);
        deadLockBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                synchronized (lock1) {
                    Log.i(TAG, "main thread got lock1");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    synchronized (lock2) {
                        Log.i(TAG, "main thread got lock2");
                    }
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (lock2) {
                            Log.i(TAG, "worker thread got lock2");
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            synchronized (lock1) {
                                Log.i(TAG, "worker thread got lock1");
                            }
                        }
                    }
                }).start();
            }
        });

        Button broadcastReceiverSlowCode = findViewById(R.id.btn_slow_code_broadcastreceiver);
        broadcastReceiverSlowCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // broadcast receiver 慢代码
                IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
                NetworkReceiver networkReceiver = new NetworkReceiver();
                registerReceiver(networkReceiver, intentFilter);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();


    }

    private void slowCode() {
        Log.i(TAG, "slow code on main thread, start at :" + System.currentTimeMillis());
        int i = 0;
        Matrix matrix = new Matrix();
        matrix.setValues(new float[]{100f, 110f, 120f, 130f, 140f, 150f, 160f, 170f, 180f});
        while (i++ < Integer.MAX_VALUE) {
            matrix.postRotate(30f, 10f, 10f);
        }
        Log.i(TAG, "slow code on main thread, end at :" + System.currentTimeMillis());
    }

    private void copyFile() {
        Log.i(TAG, "io on main thread, start at :" + System.currentTimeMillis());
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            AssetManager assetManager = getAssets();
            inputStream = assetManager.open(GRADLE_TASK_TREE_LOG);
            fileOutputStream = new FileOutputStream(new File(Environment.getExternalStorageDirectory() + File.separator + "task-tree.log"));
            byte[] buffer = new byte[1024];
            int byteCount = 0;
            while ((byteCount = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, byteCount);
            }
            fileOutputStream.flush();//刷新缓冲区
        } catch (IOException e) {
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
                inputStream = null;
            }
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                }
                fileOutputStream = null;
            }
        }
        Log.i(TAG, "io on main thread, end at :" + System.currentTimeMillis());
    }

    private class NetworkReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "broadcast Receiver pid: " + Process.myPid());
            slowCode();
//            final PendingResult pendingResult = goAsync();
//            new AsyncTask<Integer, Integer, Integer>() {
//                @Override
//                protected Integer doInBackground(Integer... integers) {
//                    slowCode();
//                    pendingResult.finish();
//                    return 0;
//                }
//            }.execute(0);
        }
    }
}
