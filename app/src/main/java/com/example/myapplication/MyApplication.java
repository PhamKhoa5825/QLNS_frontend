package com.example.myapplication;

import android.app.Application;
import com.example.myapplication.network.RetrofitClient;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        RetrofitClient.init(this);
    }
}