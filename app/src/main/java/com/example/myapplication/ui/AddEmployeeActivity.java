package com.example.myapplication.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.example.myapplication.model.CreateEmployeeRequest;
import com.example.myapplication.model.Department;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.model.Employee;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddEmployeeActivity extends AppCompatActivity {

    private TextInputEditText etFullName, etEmail, etPassword, etPhone,
            etAddress, etPosition, etDateOfBirth, etJoinDate;
    private Spinner spinnerGender, spinnerDept, spinnerRole;
    private MaterialButton btnSave, btnCancel;
    private ProgressBar progressBar;

    private ApiService apiService;
    private List<Department> deptList = new ArrayList<>();
    private Long selectedDeptId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_employee);

        apiService = RetrofitClient.getClient().create(ApiService.class);
        bindViews();
        loadDepartments();
        setupGenderSpinner();
        setupRoleSpinner();
    }

    private void bindViews() {
        etFullName    = findViewById(R.id.etFullName);
        etEmail       = findViewById(R.id.etEmail);
        etPassword    = findViewById(R.id.etPassword);
        etPhone       = findViewById(R.id.etPhone);
        etAddress     = findViewById(R.id.etAddress);
        etPosition    = findViewById(R.id.etPosition);
        etDateOfBirth = findViewById(R.id.etDateOfBirth);
        etJoinDate    = findViewById(R.id.etJoinDate);
        spinnerGender = findViewById(R.id.spinnerGender);
        spinnerDept   = findViewById(R.id.spinnerDept);
        spinnerRole   = findViewById(R.id.spinnerRole);
        progressBar   = findViewById(R.id.progressBar);
        btnSave       = findViewById(R.id.btnSave);
        btnCancel     = findViewById(R.id.btnCancel);

        // ĐÃ SỬA: Dùng toolbar navigation thay vì btnBack (không tồn tại trong XML)
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> doSave());
    }

    private void setupGenderSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Nam", "Nữ", "Khác"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(adapter);
    }

    private void setupRoleSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"EMPLOYEE", "MANAGER", "ADMIN"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);
    }

    private void loadDepartments() {
        apiService.getDepartments().enqueue(new Callback<List<Department>>() {
            @Override
            public void onResponse(Call<List<Department>> call,
                                   Response<List<Department>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    deptList = response.body();
                    List<String> names = new ArrayList<>();
                    for (Department d : deptList) names.add(d.getName());
                    ArrayAdapter<String> a = new ArrayAdapter<>(AddEmployeeActivity.this,
                            android.R.layout.simple_spinner_item, names);
                    a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerDept.setAdapter(a);
                    spinnerDept.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                            selectedDeptId = deptList.get(pos).getId();
                        }
                        @Override public void onNothingSelected(AdapterView<?> p) {}
                    });
                }
            }
            @Override public void onFailure(Call<List<Department>> c, Throwable t) {
                Toast.makeText(AddEmployeeActivity.this, "Không tải được phòng ban", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void doSave() {
        String fullName  = getText(etFullName);
        String email     = getText(etEmail);
        String password  = getText(etPassword);
        String position  = getText(etPosition);
        String phone     = getText(etPhone);
        String address   = getText(etAddress);
        String dob       = getText(etDateOfBirth);
        String joinDate  = getText(etJoinDate);

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || position.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin bắt buộc", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] genderValues = {"MALE", "FEMALE", "OTHER"};
        String gender = genderValues[spinnerGender.getSelectedItemPosition()];

        CreateEmployeeRequest req = new CreateEmployeeRequest(
                fullName, email, password, position, selectedDeptId);
        req.setPhone(phone);
        req.setAddress(address);
        req.setGender(gender);
        if (!dob.isEmpty()) req.setDateOfBirth(dob);
        if (!joinDate.isEmpty()) req.setJoinDate(joinDate);

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        apiService.createEmployee(req).enqueue(new Callback<Employee>() {
            @Override
            public void onResponse(Call<Employee> call, Response<Employee> response) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(AddEmployeeActivity.this,
                            "Đã thêm nhân viên thành công", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AddEmployeeActivity.this,
                            "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Employee> c, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(AddEmployeeActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}