package com.example.myapplication.ui;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.example.myapplication.model.dto.CreateEmployeeRequest;
import com.example.myapplication.model.entity.Employee;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView btnBack;
    private FloatingActionButton btnChangeAvatar;
    private TextInputEditText etFullName, etPhoneNumber, etEmail, etAddress;
    private MaterialButton btnSave;
    private ProgressBar progressBar;

    private ApiService apiService;
    private SharedPreferences prefs;
    private Long employeeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_edit_profile);

        View headerBar = findViewById(R.id.headerBar);
        ViewCompat.setOnApplyWindowInsetsListener(headerBar, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int dp48 = (int) (48 * getResources().getDisplayMetrics().density);
            v.setPadding(v.getPaddingLeft(), dp48 + statusBarHeight,
                    v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        prefs = getSharedPreferences("qlns_pref", MODE_PRIVATE);
        apiService = RetrofitClient.getClient().create(ApiService.class);
        employeeId = prefs.getLong("employeeId", -1);

        initViews();
        setupListeners();
        loadProfile();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        etFullName = findViewById(R.id.etFullName);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etEmail = findViewById(R.id.etEmail);
        etAddress = findViewById(R.id.etAddress);
        btnSave = findViewById(R.id.btnSave);

        // Tìm progressBar nếu có trong layout, không có thì null
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnChangeAvatar.setOnClickListener(v -> {
            Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
        });

        btnSave.setOnClickListener(v -> doSave());
    }

    // ── LOAD dữ liệu từ API ──────────────────────────────────

    private void loadProfile() {
        if (employeeId == -1) {
            Toast.makeText(this, "Không tìm thấy thông tin nhân viên", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.getEmployeeById(employeeId).enqueue(new Callback<Employee>() {
            @Override
            public void onResponse(Call<Employee> c, Response<Employee> r) {
                if (r.isSuccessful() && r.body() != null) {
                    Employee emp = r.body();

                    if (etFullName != null && emp.getFullName() != null)
                        etFullName.setText(emp.getFullName());
                    if (etEmail != null && emp.getEmail() != null)
                        etEmail.setText(emp.getEmail());
                    if (etPhoneNumber != null && emp.getPhone() != null)
                        etPhoneNumber.setText(emp.getPhone());
                    if (etAddress != null) {
                        // emp.getAddress() - cần getter trong model
                        String address = emp.getAddress();
                        if (address != null) etAddress.setText(address);
                    }

                    // Disable email (không cho sửa email)
                    if (etEmail != null) {
                        etEmail.setEnabled(false);
                        etEmail.setAlpha(0.6f);
                    }
                }
            }
            @Override
            public void onFailure(Call<Employee> c, Throwable t) {
                Toast.makeText(EditProfileActivity.this, "Lỗi tải thông tin", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── SAVE gọi API update ───────────────────────────────────

    private void doSave() {
        String fullName = getText(etFullName);
        String phone = getText(etPhoneNumber);
        String address = getText(etAddress);

        if (fullName.isEmpty()) {
            etFullName.setError("Tên không được trống");
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Đang lưu...");
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        // Dùng CreateEmployeeRequest để gửi update
        // (endpoint PUT /api/employees/{id} chỉ cập nhật field có giá trị)
        CreateEmployeeRequest req = new CreateEmployeeRequest(fullName, "", "", "", null);
        req.setPhone(phone);
        req.setAddress(address);

        apiService.updateEmployee(employeeId, req).enqueue(new Callback<Employee>() {
            @Override
            public void onResponse(Call<Employee> c, Response<Employee> r) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                btnSave.setText("Lưu thay đổi");

                if (r.isSuccessful()) {
                    // Cập nhật prefs cho ProfileActivity + Dashboard
                    if (r.body() != null && r.body().getFullName() != null) {
                        prefs.edit().putString("fullName", r.body().getFullName()).apply();
                    }
                    Toast.makeText(EditProfileActivity.this, "Đã cập nhật thành công", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditProfileActivity.this, "Lỗi: " + r.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Employee> c, Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                btnSave.setText("Lưu thay đổi");
                Toast.makeText(EditProfileActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getText(TextInputEditText et) {
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }
}