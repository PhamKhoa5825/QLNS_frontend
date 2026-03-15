package com.example.myapplication.network;

import android.content.Context;
import android.content.SharedPreferences;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // Port 8081 theo application.properties
    private static final String BASE_URL = "http://10.0.2.2:8081/";
    private static Retrofit retrofit;

    public static void init(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("qlns_pref", Context.MODE_PRIVATE);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    String token = prefs.getString("token", null);
                    Request original = chain.request();
                    if (token != null) {
                        return chain.proceed(original.newBuilder()
                                .header("Authorization", "Bearer " + token)
                                .build());
                    }
                    return chain.proceed(original);
                })
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static Retrofit getClient() {
        if (retrofit == null)
            throw new IllegalStateException(
                    "RetrofitClient chưa init(). Gọi RetrofitClient.init(context) trong MainActivity trước.");
        return retrofit;
    }

    // Alias giữ tương thích với code cũ
    public static ApiService getApiService() {
        return getClient().create(ApiService.class);
    }
}