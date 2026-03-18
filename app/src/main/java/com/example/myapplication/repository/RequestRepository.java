package com.example.myapplication.repository;

import com.example.myapplication.model.CreateRequestRequest;
import com.example.myapplication.model.Request;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;

import java.io.IOException;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RequestRepository {

    public interface RepositoryCallback<T> {
        void onSuccess(T data);

        void onError(String message);
    }

    private final ApiService apiService;

    public RequestRepository() {
        this.apiService = RetrofitClient.getApiService();
    }

    public void getMyRequests(long employeeId, RepositoryCallback<List<Request>> callback) {
        apiService.getMyRequests(employeeId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<Request>> call, Response<List<Request>> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                    return;
                }
                callback.onError(readError(response, "Khong tai duoc danh sach don"));
            }

            @Override
            public void onFailure(Call<List<Request>> call, Throwable t) {
                callback.onError("Loi ket noi: " + safeMessage(t));
            }
        });
    }

    public void submitRequest(
            long employeeId,
            Long requestId,
            CreateRequestRequest body,
            boolean isEdit,
            RepositoryCallback<Request> callback
    ) {
        Call<Request> call = isEdit
                ? apiService.updateRequest(requestId, employeeId, body)
                : apiService.createRequest(employeeId, body);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Request> call, Response<Request> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                    return;
                }
                callback.onError(readError(response, isEdit ? "Cap nhat don that bai" : "Gui don that bai"));
            }

            @Override
            public void onFailure(Call<Request> call, Throwable t) {
                callback.onError("Loi ket noi: " + safeMessage(t));
            }
        });
    }

    public void cancelRequest(long requestId, long employeeId, RepositoryCallback<Void> callback) {
        apiService.cancelRequest(requestId, employeeId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
                    return;
                }
                callback.onError(readError(response, "Xoa don that bai"));
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onError("Loi ket noi: " + safeMessage(t));
            }
        });
    }

    private String readError(Response<?> response, String fallback) {
        try (ResponseBody errorBody = response.errorBody()) {
            if (errorBody != null) {
                String errorText = errorBody.string();
                if (!errorText.trim().isEmpty()) {
                    return errorText;
                }
            }
        } catch (IOException ignored) {
            // Keep fallback below.
        }
        return fallback + " (" + response.code() + ")";
    }

    private String safeMessage(Throwable t) {
        String message = t.getMessage();
        return message == null || message.trim().isEmpty() ? "Khong the ket noi toi may chu" : message;
    }
}

