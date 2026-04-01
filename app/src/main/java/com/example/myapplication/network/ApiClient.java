package com.example.myapplication.network;

import android.content.Context;
import android.content.SharedPreferences;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public final class ApiClient {

    // [Chat] Địa chỉ localhost của máy thật khi app chạy trên Android Emulator.
    public static final String BASE_URL = "http://10.129.161.118:8080/";
    private static final String PREFS_NAME = "qlns_pref";
    private static final String TOKEN_KEY = "token";

    private static volatile Retrofit retrofit;

    private ApiClient() {
        // [Chat] Class tiện ích, không cho khởi tạo đối tượng.
    }

    public static Retrofit getInstance(Context context) {
        if (retrofit == null) {
            synchronized (ApiClient.class) {
                if (retrofit == null) {
                    Context appContext = context.getApplicationContext();

                    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                    logging.setLevel(HttpLoggingInterceptor.Level.BODY);

                    OkHttpClient client = new OkHttpClient.Builder()
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(60, TimeUnit.SECONDS)
                            .writeTimeout(60, TimeUnit.SECONDS)
                            .addInterceptor(chain -> {
                                Request original = chain.request();
                                Request.Builder requestBuilder = original.newBuilder();

                                // [Chat] Lấy JWT trong SharedPreferences và tự động gắn Bearer token vào header.
                                String token = com.example.myapplication.utils.SharedPrefsManager.getInstance(appContext).getToken();
                                if (token != null && !token.isEmpty()) {
                                    requestBuilder.header("Authorization", "Bearer " + token);
                                }

                                return chain.proceed(requestBuilder.build());
                            })
                            .addInterceptor(logging)
                            .build();

                    retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }
            }
        }

        return retrofit;
    }

    // [Chat] Tạo service tổng quát để Repository có thể inject bất kỳ Retrofit interface nào.
    public static <T> T getService(Context context, Class<T> cls) {
        return getInstance(context).create(cls);
    }

    // [Chat] Hàm tiện lợi để lấy service cho các API chat.
    public static ChatApiService getChatApiService(Context context) {
        return getService(context, ChatApiService.class);
    }
}
