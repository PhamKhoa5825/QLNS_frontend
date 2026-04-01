package com.example.myapplication.network;

import android.content.Context;
import android.content.SharedPreferences;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class RetrofitClient {

    public static final String BASE_URL = "http://10.129.161.118:8080/";

    public static String getBaseUrl() {
        return BASE_URL;
    }

    public static String getBaseUrl(Context context) {
        return BASE_URL;
    }
    private static Retrofit retrofit;
    private static Context appContext;

    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static Retrofit getInstance() {
        if (retrofit == null) {
            // Thêm Logging để xem chi tiết Request/Response trong Logcat
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .addInterceptor(logging)
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Request.Builder requestBuilder = original.newBuilder();

                        if (appContext != null) {
                            String token = com.example.myapplication.utils.SharedPrefsManager.getInstance(appContext).getToken();
                            
                            if (token != null) {
                                requestBuilder.header("Authorization", "Bearer " + token);
                            }
                        }

                        return chain.proceed(requestBuilder.build());
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static ApiService getApiService() {
        return getInstance().create(ApiService.class);
    }

    public static ApiService getApiService(Context context) {
        if (appContext == null && context != null) {
            init(context);
        }
        return getApiService();
    }
}
