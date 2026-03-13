package com.example.myapplication.repository;

import com.example.myapplication.model.AuthenticationRequest;
import com.example.myapplication.model.AuthenticationResponse;
import com.example.myapplication.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;

public class AuthRepository {
    public void login(AuthenticationRequest request, Callback<AuthenticationResponse> callback) {
        RetrofitClient.getApiService().login(request).enqueue(callback);
    }
}
