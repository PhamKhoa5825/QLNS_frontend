package com.example.myapplication.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.model.Department;
import com.example.myapplication.repository.DepartmentRepository;

import java.util.List;

public class DepartmentViewModel extends AndroidViewModel {
    private final DepartmentRepository repository;

    private final MutableLiveData<List<Department>> _departments = new MutableLiveData<>();
    public final LiveData<List<Department>> departments = _departments;

    private List<Department> fullDepartmentList = new java.util.ArrayList<>();

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public final LiveData<String> errorMessage = _errorMessage;

    private final MutableLiveData<String> _successMessage = new MutableLiveData<>();
    public final LiveData<String> successMessage = _successMessage;

    public DepartmentViewModel(@NonNull Application application) {
        super(application);
        this.repository = new DepartmentRepository(application);
    }

    public void loadDepartments() {
        _isLoading.setValue(true);
        repository.getDepartments(new DepartmentRepository.RepositoryCallback<List<Department>>() {
            @Override
            public void onSuccess(List<Department> data) {
                _isLoading.setValue(false);
                fullDepartmentList = data;
                _departments.setValue(data);
            }

            @Override
            public void onError(String message) {
                _isLoading.setValue(false);
                _errorMessage.setValue(message);
            }
        });
    }

    public void addDepartment(Department dept) {
        _isLoading.setValue(true);
        repository.createDepartment(dept, new DepartmentRepository.RepositoryCallback<Department>() {
            @Override
            public void onSuccess(Department data) {
                _isLoading.setValue(false);
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

    public void updateDepartment(Long id, Department dept) {
        _isLoading.setValue(true);
        repository.updateDepartment(id, dept, new DepartmentRepository.RepositoryCallback<Department>() {
            @Override
            public void onSuccess(Department data) {
                _isLoading.setValue(false);
                _successMessage.setValue("Đã cập nhật phòng ban");
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
        repository.deleteDepartment(id, new DepartmentRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                _isLoading.setValue(false);
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

    public void searchDepartments(String query) {
        if (query == null || query.isEmpty()) {
            _departments.setValue(fullDepartmentList);
            return;
        }
        String lowerQuery = query.toLowerCase();
        java.util.List<Department> filtered = new java.util.ArrayList<>();
        for (Department d : fullDepartmentList) {
            if (d.getName() != null && d.getName().toLowerCase().contains(lowerQuery)) {
                filtered.add(d);
            }
        }
        _departments.setValue(filtered);
    }
}
