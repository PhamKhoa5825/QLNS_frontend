package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.SharedPrefsManager;
import com.example.myapplication.utils.TopBarHelper;
import com.example.myapplication.model.Employee;

public class ProfileActivity extends AppCompatActivity {

    private View progressBar;
    private ImageView ivProfileAvatar;
    private TextView tvProfileName, tvProfilePosition, tvProfileDept, tvProfileEmail, tvProfilePhone, tvProfileCode, tvProfileAvatar;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfilePosition = findViewById(R.id.tvProfilePosition);
        tvProfileDept = findViewById(R.id.tvProfileDept);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfilePhone = findViewById(R.id.tvProfilePhone);
        tvProfileCode = findViewById(R.id.tvProfileCode);
        tvProfileAvatar = findViewById(R.id.tvProfileAvatar);
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar);
        progressBar = findViewById(R.id.headerBg); // User placeholder for progress or similar
 
        apiService = RetrofitClient.getApiService(this);
 
        loadProfileFromServer();
        
        SharedPrefsManager prefs = SharedPrefsManager.getInstance(this);
        
        Button btnViewPayroll = findViewById(R.id.btnViewPayroll);
        if (btnViewPayroll != null) {
            btnViewPayroll.setOnClickListener(v -> startActivity(new Intent(this, PayrollActivity.class)));
        }

        Button btnChangePassword = findViewById(R.id.btnChangePassword);
        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        }

        Button btnLogout = findViewById(R.id.btnLogoutProfile);
        btnLogout.setOnClickListener(v -> {
            // Log logout to server (best effort)
            apiService.logout().enqueue(new retrofit2.Callback<Void>() {
                @Override
                public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {}
                @Override
                public void onFailure(retrofit2.Call<Void> call, Throwable t) {}
            });

            // Local logout
            prefs.logout();
            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View btnEditProfile = findViewById(R.id.btnEditProfile);
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                Intent intent = new Intent(this, AddEditEmployeeActivity.class);
                intent.putExtra("employeeId", prefs.getEmployeeId());
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        TopBarHelper.setupTopBar(this);
    }

    private void showChangePasswordDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText edtOld = dialogView.findViewById(R.id.edtOldPassword);
        EditText edtNew = dialogView.findViewById(R.id.edtNewPassword);
        EditText edtConfirm = dialogView.findViewById(R.id.edtConfirmPassword);
        Button btnSave = dialogView.findViewById(R.id.btnSavePassword);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String oldP = edtOld.getText().toString().trim();
            String newP = edtNew.getText().toString().trim();
            String confP = edtConfirm.getText().toString().trim();

            if (oldP.isEmpty() || newP.isEmpty() || confP.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newP.length() < 6) {
                Toast.makeText(this, "Mật khẩu mới phải từ 6 ký tự", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newP.equals(confP)) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
                return;
            }

            com.example.myapplication.model.ChangePasswordRequest req = 
                new com.example.myapplication.model.ChangePasswordRequest(oldP, newP, confP);

            RetrofitClient.getApiService(this).changePassword(req).enqueue(new retrofit2.Callback<Void>() {
                @Override
                public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(ProfileActivity.this, "Đổi mật khẩu thành công!", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    } else {
                        String msg = "Lỗi khi đổi mật khẩu";
                        if (response.code() == 401 || response.code() == 400) {
                            msg = "Mật khẩu cũ không chính xác hoặc dữ liệu không hợp lệ";
                        }
                        Toast.makeText(ProfileActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                    Toast.makeText(ProfileActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void loadProfileFromServer() {
        SharedPrefsManager prefs = SharedPrefsManager.getInstance(this);
        Long empId = prefs.getEmployeeId();
        if (empId == -1L) return;
 
        apiService.getMyProfile(empId).enqueue(new retrofit2.Callback<Employee>() {
            @Override
            public void onResponse(retrofit2.Call<Employee> call, retrofit2.Response<Employee> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateUI(response.body());
                }
            }
            @Override
            public void onFailure(retrofit2.Call<Employee> call, Throwable t) {
                // Fallback to local data if needed
            }
        });
    }
 
    private void updateUI(Employee emp) {
        if (tvProfileName != null) tvProfileName.setText(emp.getFullName());
        if (tvProfilePosition != null) tvProfilePosition.setText(emp.getPosition());
        if (tvProfileDept != null) tvProfileDept.setText(emp.getDepartmentName());
        if (tvProfileEmail != null) tvProfileEmail.setText(emp.getEmail());
        if (tvProfilePhone != null) tvProfilePhone.setText(emp.getPhone());
        if (tvProfileCode != null) tvProfileCode.setText("NV" + emp.getId());
 
        // Save to SharedPrefs for top bar synchronization across other activities
        SharedPrefsManager prefs = SharedPrefsManager.getInstance(this);
        prefs.setFullName(emp.getFullName());
        prefs.setDepartmentName(emp.getDepartmentName());
        prefs.setAvatarUrl(emp.getAvatarUrl());
 
        // Update local top bar
        TopBarHelper.setupTopBar(this);
 
        // Avatar Logic (Specific to Profile Page)
        String url = emp.getAvatarUrl();
        if (url != null && !url.isEmpty()) {
            // Main Avatar
            if (ivProfileAvatar != null) {
                ivProfileAvatar.setVisibility(View.VISIBLE);
                Glide.with(this).load(url).circleCrop().into(ivProfileAvatar);
            }
            if (tvProfileAvatar != null) tvProfileAvatar.setVisibility(View.GONE);
        } else {
            // Main Avatar Fallback
            if (ivProfileAvatar != null) ivProfileAvatar.setVisibility(View.VISIBLE); // Keep background circle maybe
            if (ivProfileAvatar != null) ivProfileAvatar.setImageResource(R.drawable.ic_user_placeholder);
            if (tvProfileAvatar != null) {
                tvProfileAvatar.setVisibility(View.VISIBLE);
                tvProfileAvatar.setText(emp.getAvatarText());
            }
        }
    }
 
}
