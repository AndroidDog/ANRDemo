package com.example.sunxt.anrdemo;

import android.app.Application;
import android.os.StrictMode;

/**
 * Created by xtsun on 2018/3/29.
 */

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
        super.onCreate();
    }
}
