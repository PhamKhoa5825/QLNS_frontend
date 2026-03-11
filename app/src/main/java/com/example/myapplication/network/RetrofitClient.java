package com.example.myapplication.network;

import android.content.Context;
import com.example.myapplication.utils.SharedPrefsManager;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.IOException;

public class RetrofitClient {

    private static final String BASE_URL = "http://10.0.2.2:8080/";
    private static Retrofit retrofit;

    public static Retrofit getInstance(Context context) {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request original = chain.request();
                    
                    // Skip auth header for login and register endpoints
                    if (original.url().encodedPath().contains("/api/auth/login") || 
                        original.url().encodedPath().contains("/api/auth/register")) {
                        return chain.proceed(original);
                    }

                    String token = SharedPrefsManager.getInstance(context).getToken();
                    if (token != null && !token.isEmpty()) {
                        Request.Builder requestBuilder = original.newBuilder()
                                .addHeader("Authorization", "Bearer " + token);
                        Request request = requestBuilder.build();
                        return chain.proceed(request);
                    }
                    return chain.proceed(original);
                }
            }).build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static ApiService getApiService(Context context) {
        return getInstance(context).create(ApiService.class);
    }
}
