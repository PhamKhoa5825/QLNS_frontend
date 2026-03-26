package com.example.myapplication.network;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class RetrofitClient {

    private static final String BASE_URL = "http://10.0.2.2:8081/";
    private static volatile Retrofit retrofitInstance;
    private static Context appContext;
    // Tránh redirect về Login nhiều lần khi nhiều API cùng trả 401
    private static final AtomicBoolean isRedirecting = new AtomicBoolean(false);

    public static void init(Context context) {
        appContext = context.getApplicationContext();
        isRedirecting.set(false);
        rebuild();
    }

    private static void rebuild() {
        SharedPreferences prefs = appContext.getSharedPreferences("qlns_pref", Context.MODE_PRIVATE);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    String token = prefs.getString("token", null);
                    Request original = chain.request();
                    Request.Builder reqBuilder = original.newBuilder();
                    if (token != null) {
                        reqBuilder.header("Authorization", "Bearer " + token);
                    }
                    Response response = chain.proceed(reqBuilder.build());

                    // Token hết hạn → xóa token, redirect về Login (chỉ 1 lần)
                    if (response.code() == 401 && token != null
                            && !original.url().encodedPath().contains("/api/auth/login")
                            && isRedirecting.compareAndSet(false, true)) {
                        String savedUsername = prefs.getString("saved_username", null);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.clear();
                        if (savedUsername != null) editor.putString("saved_username", savedUsername);
                        editor.apply();

                        new Handler(Looper.getMainLooper()).post(() -> {
                            try {
                                Class<?> loginClass = Class.forName(
                                        "com.example.myapplication.ui.LoginActivity");
                                Intent intent = new Intent(appContext, loginClass);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                appContext.startActivity(intent);
                            } catch (ClassNotFoundException ignored) {}
                        });
                    }
                    return response;
                })
                .build();

        retrofitInstance = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static Retrofit getClient() {
        if (retrofitInstance == null) {
            if (appContext != null) {
                rebuild();
            } else {
                throw new IllegalStateException(
                        "RetrofitClient chưa init(). Gọi RetrofitClient.init(context) trước.");
            }
        }
        return retrofitInstance;
    }

    public static String getBaseUrl() {
        return BASE_URL;
    }

    public static ApiService getApiService() {
        return getClient().create(ApiService.class);
    }
}