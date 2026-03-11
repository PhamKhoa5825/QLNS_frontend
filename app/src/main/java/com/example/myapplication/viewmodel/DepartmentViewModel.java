package com.example.myapplication.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.model.Department;
import com.example.myapplication.repository.EmployeeRepository;

import java.util.List;

public class DepartmentViewModel extends AndroidViewModel {
    private final EmployeeRepository repository;

    public DepartmentViewModel(@NonNull Application application) {
        super(application);
        this.repository = new EmployeeRepository(application.getApplicationContext());
    }

    private final MutableLiveData<List<Department>> _departments = new MutableLiveData<>();
    public final LiveData<List<Department>> departments = _departments;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public final LiveData<String> errorMessage = _errorMessage;

    public void loadDepartments() {
        _isLoading.setValue(true);
        repository.getDepartments(new EmployeeRepository.RepositoryCallback<List<Department>>() {
            @Override
            public void onSuccess(List<Department> data) {
                _isLoading.setValue(false);
                _departments.setValue(data);
            }

            @Override
            public void onError(String message) {
                _isLoading.setValue(false);
                _errorMessage.setValue(message);
            }
        });
    }
}
