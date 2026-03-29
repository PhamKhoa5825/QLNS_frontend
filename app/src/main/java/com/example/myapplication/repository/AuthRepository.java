package com.example.myapplication.repository;

import android.content.Context;

import com.example.myapplication.model.AuthenticationRequest;
import com.example.myapplication.model.AuthenticationResponse;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;

import retrofit2.Callback;

public class AuthRepository {
    private final ApiService apiService;

    public AuthRepository(Context context) {
        this.apiService = RetrofitClient.getApiService(context);
    }

    public void login(AuthenticationRequest request, Callback<AuthenticationResponse> callback) {
        apiService.login(request).enqueue(callback);
    }
}
