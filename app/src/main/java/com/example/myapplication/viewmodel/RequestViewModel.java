package com.example.myapplication.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.model.CreateRequestRequest;
import com.example.myapplication.model.Request;
import com.example.myapplication.repository.RequestRepository;

import java.util.ArrayList;
import java.util.List;

public class RequestViewModel extends AndroidViewModel {

    private final RequestRepository repository;

    private final MutableLiveData<List<Request>> _requests = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<Request>> requests = _requests;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public final LiveData<String> errorMessage = _errorMessage;

    private final MutableLiveData<Boolean> _submitSuccess = new MutableLiveData<>(false);
    public final LiveData<Boolean> submitSuccess = _submitSuccess;

    private final MutableLiveData<Boolean> _deleteSuccess = new MutableLiveData<>(false);
    public final LiveData<Boolean> deleteSuccess = _deleteSuccess;

    public RequestViewModel(@NonNull Application application) {
        super(application);
        this.repository = new RequestRepository();
    }

    public void loadRequests(long employeeId) {
        _isLoading.setValue(true);
        repository.getMyRequests(employeeId, new RequestRepository.RepositoryCallback<>() {
            @Override
            public void onSuccess(List<Request> data) {
                _isLoading.setValue(false);
                _requests.setValue(data == null ? new ArrayList<>() : data);
            }

            @Override
            public void onError(String message) {
                _isLoading.setValue(false);
                _errorMessage.setValue(message);
            }
        });
    }

    public void submitRequest(long employeeId, Long requestId, String title, String description, boolean isEdit) {
        _isLoading.setValue(true);
        _submitSuccess.setValue(false);

        CreateRequestRequest body = new CreateRequestRequest(title, description);
        repository.submitRequest(employeeId, requestId, body, isEdit, new RequestRepository.RepositoryCallback<>() {
            @Override
            public void onSuccess(Request data) {
                _isLoading.setValue(false);
                _submitSuccess.setValue(true);
            }

            @Override
            public void onError(String message) {
                _isLoading.setValue(false);
                _errorMessage.setValue(message);
            }
        });
    }

    public void deleteRequest(long requestId, long employeeId) {
        _isLoading.setValue(true);
        _deleteSuccess.setValue(false);

        repository.cancelRequest(requestId, employeeId, new RequestRepository.RepositoryCallback<>() {
            @Override
            public void onSuccess(Void data) {
                _isLoading.setValue(false);
                _deleteSuccess.setValue(true);
            }

            @Override
            public void onError(String message) {
                _isLoading.setValue(false);
                _errorMessage.setValue(message);
            }
        });
    }

    public void clearSubmitSuccessEvent() {
        _submitSuccess.setValue(false);
    }

    public void clearDeleteSuccessEvent() {
        _deleteSuccess.setValue(false);
    }

    public void clearError() {
        _errorMessage.setValue(null);
    }
}

