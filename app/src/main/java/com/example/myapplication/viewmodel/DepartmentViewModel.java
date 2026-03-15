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

    private final MutableLiveData<List<Department>> _departments = new MutableLiveData<>();
    public final LiveData<List<Department>> departments = _departments;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public final LiveData<String> errorMessage = _errorMessage;

    private final MutableLiveData<String> _successMessage = new MutableLiveData<>();
    public final LiveData<String> successMessage = _successMessage;

    public DepartmentViewModel(@NonNull Application application) {
        super(application);
        this.repository = new EmployeeRepository(application);
    }

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

    public void createDepartment(String name, String description) {
        _isLoading.setValue(true);
        Department dept = new Department();
        dept.setName(name);
        dept.setDescription(description);
        repository.createDepartment(dept, new EmployeeRepository.RepositoryCallback<Department>() {
            @Override
            public void onSuccess(Department data) {
                _successMessage.setValue("Đã thêm phòng ban mới");
                loadDepartments();
            }
            @Override
            public void onError(String message) {
                _isLoading.setValue(false);
                _errorMessage.setValue(message);
            }
        });
    }

    public void updateDepartment(Long id, String name, String description) {
        _isLoading.setValue(true);
        Department dept = new Department();
        dept.setName(name);
        dept.setDescription(description);
        repository.updateDepartment(id, dept, new EmployeeRepository.RepositoryCallback<Department>() {
            @Override
            public void onSuccess(Department data) {
                _successMessage.setValue("Đã cập nhật thông tin");
                loadDepartments();
            }
            @Override
            public void onError(String message) {
                _isLoading.setValue(false);
                _errorMessage.setValue(message);
            }
        });
    }

    public void deleteDepartment(Long id) {
        _isLoading.setValue(true);
        repository.deleteDepartment(id, new EmployeeRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                _successMessage.setValue("Đã xóa phòng ban");
                loadDepartments();
            }
            @Override
            public void onError(String message) {
                _isLoading.setValue(false);
                _errorMessage.setValue(message);
            }
        });
    }
}