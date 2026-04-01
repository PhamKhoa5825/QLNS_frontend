package com.example.myapplication.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.Employee;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.SharedPrefsManager;
import com.google.android.material.appbar.MaterialToolbar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * EmployeeDetailActivity — Chi tiết NV (Đã lược bỏ Chấm công & Đơn từ)
 */
public class EmployeeDetailActivity extends AppCompatActivity {

    private ApiService apiService;
    private Long employeeId;

    private View layoutInfo, layoutAdminActions;
    private TextView tvName, tvEmail, tvPhone, tvDept, tvPosition, tvJoinDate, tvStatus;
    private TextView tvDetailHeaderName, tvDetailHeaderRole, tvAvatarFallback;
    private ImageView ivAvatar;
    private View btnReactivate;

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_employee_detail);

        employeeId = getIntent().getLongExtra("employeeId", -1);
        if (employeeId == -1) { finish(); return; }

        apiService = RetrofitClient.getApiService(this);

        bindViews();
        loadEmployeeInfo();
    }

    private void bindViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        progressBar = findViewById(R.id.progressBar);

        layoutInfo = findViewById(R.id.layoutInfo);
        tvName = findViewById(R.id.tvDetailName);
        tvEmail = findViewById(R.id.tvDetailEmail);
        tvPhone = findViewById(R.id.tvDetailPhone);
        tvDept = findViewById(R.id.tvDetailDept);
        tvPosition = findViewById(R.id.tvDetailPosition);
        tvJoinDate = findViewById(R.id.tvDetailJoinDate);
        tvStatus = findViewById(R.id.tvDetailStatus);
        
        tvDetailHeaderName = findViewById(R.id.tvDetailHeaderName);
        tvDetailHeaderRole = findViewById(R.id.tvDetailHeaderRole);
        tvAvatarFallback = findViewById(R.id.tvDetailAvatarFallback);
        ivAvatar = findViewById(R.id.ivDetailAvatar);
        
        layoutAdminActions = findViewById(R.id.layoutAdminActions);
        btnReactivate = findViewById(R.id.btnReactivate);

        btnReactivate.setOnClickListener(v -> performReactivate());
    }

    private void loadEmployeeInfo() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getEmployeeById(employeeId).enqueue(new Callback<Employee>() {
            @Override
            public void onResponse(Call<Employee> c, Response<Employee> r) {
                progressBar.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null) {
                    Employee emp = r.body();
                    tvName.setText(emp.getFullName() != null ? emp.getFullName() : "—");
                    tvEmail.setText(emp.getEmail() != null ? emp.getEmail() : "—");
                    tvPhone.setText(emp.getPhone() != null ? emp.getPhone() : "—");
                    tvDept.setText(emp.getDepartmentName() != null ? emp.getDepartmentName() : "Chưa phân công");
                    tvPosition.setText(emp.getPosition() != null ? emp.getPosition() : "—");
                    tvJoinDate.setText(emp.getJoinDate() != null ? emp.getJoinDate() : "—");
                    tvStatus.setText(emp.getStatus() != null ? emp.getStatus() : "—");
 
                    // Update Header
                    if (tvDetailHeaderName != null) tvDetailHeaderName.setText(emp.getFullName());
                    if (tvDetailHeaderRole != null) tvDetailHeaderRole.setText(emp.getPosition());
 
                    String avatarUrl = emp.getAvatarUrl();
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        ivAvatar.setVisibility(View.VISIBLE);
                        tvAvatarFallback.setVisibility(View.GONE);
                        com.bumptech.glide.Glide.with(EmployeeDetailActivity.this)
                                .load(avatarUrl)
                                .circleCrop()
                                .into(ivAvatar);
                    } else {
                        ivAvatar.setVisibility(View.GONE);
                        tvAvatarFallback.setVisibility(View.VISIBLE);
                        tvAvatarFallback.setText(emp.getAvatarText());
                    }

                    updateAdminActions(emp);

                    MaterialToolbar toolbar = findViewById(R.id.toolbar);
                    toolbar.setTitle(emp.getFullName());
                } else {
                    Toast.makeText(EmployeeDetailActivity.this, "Lỗi tải thông tin", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Employee> c, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(EmployeeDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateAdminActions(Employee emp) {
        String myRole = SharedPrefsManager.getInstance(this).getRole();
        if (!"ADMIN".equals(myRole)) {
            layoutAdminActions.setVisibility(View.GONE);
            return;
        }

        layoutAdminActions.setVisibility(View.VISIBLE);
        String status = emp.getStatusRaw(); 

        if ("RESIGNED".equalsIgnoreCase(status)) {
            btnReactivate.setVisibility(View.VISIBLE);
        } else {
            btnReactivate.setVisibility(View.GONE);
        }
    }

    private void performReactivate() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.reactivateEmployee(employeeId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> c, Response<Void> r) {
                progressBar.setVisibility(View.GONE);
                if (r.isSuccessful()) {
                    Toast.makeText(EmployeeDetailActivity.this, "Đã kích hoạt lại nhân viên", Toast.LENGTH_SHORT).show();
                    loadEmployeeInfo();
                } else {
                    Toast.makeText(EmployeeDetailActivity.this, "Thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> c, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(EmployeeDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
