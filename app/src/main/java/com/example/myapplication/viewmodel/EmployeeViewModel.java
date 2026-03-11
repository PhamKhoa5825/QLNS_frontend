package com.example.myapplication.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.model.Department;
import com.example.myapplication.model.Employee;
import com.example.myapplication.repository.EmployeeRepository;

import java.util.ArrayList;
import java.util.List;

public class EmployeeViewModel extends AndroidViewModel {
    private final EmployeeRepository repository;

    private final MutableLiveData<List<Employee>> _employees = new MutableLiveData<>();
    public final LiveData<List<Employee>> employees = _employees;

    private final MutableLiveData<List<Department>> _departments = new MutableLiveData<>();
    public final LiveData<List<Department>> departments = _departments;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public final LiveData<String> errorMessage = _errorMessage;

    private List<Employee> fullEmployeeList = new ArrayList<>();

    public EmployeeViewModel(@NonNull Application application) {
        super(application);
        this.repository = new EmployeeRepository(application.getApplicationContext());
    }

    public void loadEmployees() {
        _isLoading.setValue(true);
        repository.getEmployees(new EmployeeRepository.RepositoryCallback<List<Employee>>() {
            @Override
            public void onSuccess(List<Employee> data) {
                _isLoading.setValue(false);
                fullEmployeeList = data;
                _employees.setValue(data);
            }

            @Override
            public void onError(String message) {
                _isLoading.setValue(false);
                _errorMessage.setValue(message);
            }
        });
    }

    public void loadEmployeesByDepartment(Long deptId) {
        _isLoading.setValue(true);
        repository.getEmployeesByDepartment(deptId, new EmployeeRepository.RepositoryCallback<List<Employee>>() {
            @Override
            public void onSuccess(List<Employee> data) {
                _isLoading.setValue(false);
                fullEmployeeList = data;
                _employees.setValue(data);
            }

            @Override
            public void onError(String message) {
                _isLoading.setValue(false);
                _errorMessage.setValue(message);
            }
        });
    }

    public void searchEmployees(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            _employees.setValue(fullEmployeeList);
            return;
        }
        String lowerKeyword = keyword.toLowerCase();
        List<Employee> filtered = new ArrayList<>();
        for (Employee emp : fullEmployeeList) {
            if (emp.getFullName().toLowerCase().contains(lowerKeyword) || 
                (emp.getEmail() != null && emp.getEmail().toLowerCase().contains(lowerKeyword))) {
                filtered.add(emp);
            }
        }
        _employees.setValue(filtered);
    }

    public void loadDepartments() {
        repository.getDepartments(new EmployeeRepository.RepositoryCallback<List<Department>>() {
            @Override
            public void onSuccess(List<Department> data) {
                _departments.setValue(data);
            }

            @Override
            public void onError(String message) {
                _errorMessage.setValue(message);
            }
        });
    }

    public void filterEmployees(String status, Long deptId, String position) {
        List<Employee> filtered = new ArrayList<>();
        for (Employee emp : fullEmployeeList) {
            boolean statusOk = status == null || status.equals(emp.getStatusRaw());
            boolean deptOk = deptId == null || (emp.getDepartmentId() != null && emp.getDepartmentId().equals(deptId));
            boolean roleOk = position == null || position.equals(emp.getPosition());

            if (statusOk && deptOk && roleOk) {
                filtered.add(emp);
            }
        }
        _employees.setValue(filtered);
    }

    public List<Employee> getFullEmployeeList() {
        return fullEmployeeList;
    }
}
