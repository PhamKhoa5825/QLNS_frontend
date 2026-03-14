package com.example.myapplication.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.network.RetrofitClient;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePasswordViewModel extends ViewModel {

    private final MutableLiveData<Boolean> _isSuccess = new MutableLiveData<>();
    public LiveData<Boolean> isSuccess = _isSuccess;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> errorMessage = _errorMessage;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public LiveData<Boolean> isLoading = _isLoading;

    public void changePassword(String oldPassword, String newPassword) {
        _isLoading.setValue(true);
        Map<String, String> request = new HashMap<>();
        request.put("oldPassword", oldPassword);
        request.put("newPassword", newPassword);

        RetrofitClient.getApiService().changePassword(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                _isLoading.setValue(false);
                if (response.isSuccessful()) {
                    _isSuccess.setValue(true);
                } else {
                    if (response.code() == 400) {
                        _errorMessage.setValue("Mật khẩu cũ không chính xác");
                    } else {
                        _errorMessage.setValue("Đổi mật khẩu thất bại");
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                _isLoading.setValue(false);
                _errorMessage.setValue("Lỗi kết nối: " + t.getMessage());
            }
        });
    }
}
