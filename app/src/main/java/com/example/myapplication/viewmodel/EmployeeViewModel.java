package com.example.myapplication.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.model.Department;
import com.example.myapplication.model.Employee;
import com.example.myapplication.model.EmployeeSummary;
import com.example.myapplication.repository.EmployeeRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EmployeeViewModel extends AndroidViewModel {
    private final EmployeeRepository repository;

    private final MutableLiveData<List<Employee>> _employees = new MutableLiveData<>();
    public final LiveData<List<Employee>> employees = _employees;

    private final MutableLiveData<List<Department>> _departments = new MutableLiveData<>();
    public final LiveData<List<Department>> departments = _departments;

    private final MutableLiveData<Employee> _userProfile = new MutableLiveData<>();
    public final LiveData<Employee> userProfile = _userProfile;

    private final MutableLiveData<EmployeeSummary> _employeeSummary = new MutableLiveData<>();
    public final LiveData<EmployeeSummary> employeeSummary = _employeeSummary;

    private final MutableLiveData<Boolean> _isAuthorized = new MutableLiveData<>();
    public final LiveData<Boolean> isAuthorized = _isAuthorized;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public final LiveData<String> errorMessage = _errorMessage;

    private final MutableLiveData<Boolean> _updateSuccess = new MutableLiveData<>();
    public final LiveData<Boolean> updateSuccess = _updateSuccess;

    private List<Employee> fullEmployeeList = new ArrayList<>();

    public EmployeeViewModel(@NonNull Application application) {
        super(application);
        this.repository = new EmployeeRepository(application);
    }

    public void checkAuth() {
        boolean hasToken = repository.hasToken();
        _isAuthorized.setValue(hasToken);
    }

    public void loadEmployeeSummary() {
        Long userId = repository.getSavedUserId();
        if (userId == -1) {
            _isAuthorized.setValue(false);
            return;
        }
        repository.getEmployeeSummary(userId, new EmployeeRepository.RepositoryCallback<EmployeeSummary>() {
            @Override
            public void onSuccess(EmployeeSummary data) {
                _employeeSummary.setValue(data);
            }

            @Override
            public void onError(String message) {
                _errorMessage.setValue(message);
            }
        });
    }

    public void loadMyProfile() {
        _isLoading.setValue(true);
        repository.getMyProfile(new EmployeeRepository.RepositoryCallback<Employee>() {
            @Override
            public void onSuccess(Employee data) {
                _isLoading.setValue(false);
                _userProfile.setValue(data);
            }

            @Override
            public void onError(String message) {
                _isLoading.setValue(false);
                if ("UNAUTHORIZED".equals(message)) {
                    repository.clearToken();
                    _isAuthorized.setValue(false);
                } else {
                    _errorMessage.setValue(message);
                }
            }
        });
    }

    public void loadEmployeeDetail(Long id) {
        _isLoading.setValue(true);
        repository.getEmployeeDetail(id, new EmployeeRepository.RepositoryCallback<Employee>() {
            @Override
            public void onSuccess(Employee data) {
                _isLoading.setValue(false);
                _userProfile.setValue(data);
            }

            @Override
            public void onError(String message) {
                _isLoading.setValue(false);
                _errorMessage.setValue(message);
            }
        });
    }

    public void updateEmployeeProfile(Long id, Map<String, Object> data) {
        _isLoading.setValue(true);
        repository.updateEmployeeProfile(id, data, new EmployeeRepository.RepositoryCallback<Employee>() {
            @Override
            public void onSuccess(Employee updatedEmployee) {
                _isLoading.setValue(false);
                _userProfile.setValue(updatedEmployee);
                _updateSuccess.setValue(true);
            }

            @Override
            public void onError(String message) {
                _isLoading.setValue(false);
                _errorMessage.setValue(message);
                _updateSuccess.setValue(false);
            }
        });
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

    private String currentSearchKeyword = "";
    private String currentFilterStatus = null;
    private Long currentFilterDeptId = null;
    private String currentFilterPosition = null;

    public void searchEmployees(String keyword) {
        this.currentSearchKeyword = keyword != null ? keyword : "";
        applyFilters();
    }

    public void filterEmployees(String status, Long deptId, String position) {
        this.currentFilterStatus = status;
        this.currentFilterDeptId = deptId;
        this.currentFilterPosition = position;
        applyFilters();
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

    private void applyFilters() {
        String kw = currentSearchKeyword.toLowerCase().trim();
        List<Employee> filtered = new ArrayList<>();
        
        for (Employee emp : fullEmployeeList) {
            // Search match
            boolean searchOk = kw.isEmpty();
            if (!searchOk) {
                String name = emp.getFullName() != null ? emp.getFullName().toLowerCase() : "";
                String email = emp.getEmail() != null ? emp.getEmail().toLowerCase() : "";
                String pos = emp.getPosition() != null ? emp.getPosition().toLowerCase() : "";
                searchOk = name.contains(kw) || email.contains(kw) || pos.contains(kw);
            }

            // Filter match
            boolean statusOk = currentFilterStatus == null || currentFilterStatus.equals(emp.getStatusRaw());
            boolean deptOk = currentFilterDeptId == null || (emp.getDepartmentId() != null && emp.getDepartmentId().equals(currentFilterDeptId));
            boolean posOk = currentFilterPosition == null || currentFilterPosition.equals(emp.getPosition());

            if (searchOk && statusOk && deptOk && posOk) {
                filtered.add(emp);
            }
        }
        _employees.setValue(filtered);
    }

    public Long getSavedUserId() {
        return repository.getSavedUserId();
    }

    public void logout() {
        repository.clearToken();
        _isAuthorized.setValue(false);
    }

    public List<Employee> getFullEmployeeList() {
        return fullEmployeeList;
    }
}
