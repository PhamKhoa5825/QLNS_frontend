package com.example.myapplication.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.example.myapplication.model.entity.Employee;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.google.android.material.button.MaterialButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private ImageButton btnBack;
    private TextView tvProfileName, tvProfilePosition, tvProfileDept,
            tvProfileCode, tvProfileEmail, tvProfilePhone;
    private MaterialButton btnEditProfile, btnChangePassword, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_profile);

        // Đẩy headerContent xuống bằng chiều cao status bar
        View headerContent = findViewById(R.id.headerContent);
        ViewCompat.setOnApplyWindowInsetsListener(headerContent, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int dp48 = (int) (48 * getResources().getDisplayMetrics().density);
            v.setPadding(v.getPaddingLeft(), dp48 + statusBarHeight,
                    v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        prefs = getSharedPreferences("qlns_pref", MODE_PRIVATE);

        initViews();
        loadFromPrefs();
        setupListeners();
    }

    private void initViews() {
        btnBack           = findViewById(R.id.btnBack);
        tvProfileName     = findViewById(R.id.tvProfileName);
        tvProfilePosition = findViewById(R.id.tvProfilePosition);
        tvProfileDept     = findViewById(R.id.tvProfileDept);
        tvProfileCode     = findViewById(R.id.tvProfileCode);
        tvProfileEmail    = findViewById(R.id.tvProfileEmail);
        tvProfilePhone    = findViewById(R.id.tvProfilePhone);
        btnEditProfile    = findViewById(R.id.btnEditProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout         = findViewById(R.id.btnLogout);
    }

    private void loadFromPrefs() {
        // Hiển thị tạm từ prefs (nhanh)
        String fullName = prefs.getString("fullName", "");
        if (tvProfileName != null) tvProfileName.setText(fullName);

        // Load đầy đủ từ API
        Long empId = prefs.getLong("employeeId", -1);
        if (empId != -1) {
            loadProfileFromApi(empId);
        }
    }

    private void loadProfileFromApi(Long empId) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getEmployeeById(empId).enqueue(new Callback<Employee>() {
            @Override
            public void onResponse(Call<Employee> c, Response<Employee> r) {
                if (r.isSuccessful() && r.body() != null) {
                    Employee emp = r.body();
                    if (tvProfileName != null)
                        tvProfileName.setText(emp.getFullName() != null ? emp.getFullName() : "—");
                    if (tvProfilePosition != null)
                        tvProfilePosition.setText(emp.getPosition() != null ? emp.getPosition() : "—");
                    if (tvProfileDept != null)
                        tvProfileDept.setText(emp.getDepartmentName() != null ? emp.getDepartmentName() : "Chưa phân công");
                    if (tvProfileEmail != null)
                        tvProfileEmail.setText(emp.getEmail() != null ? emp.getEmail() : "—");
                    if (tvProfilePhone != null)
                        tvProfilePhone.setText(emp.getPhone() != null ? emp.getPhone() : "—");
                    if (tvProfileCode != null)
                        tvProfileCode.setText("NV" + String.format("%04d", emp.getId()));
                }
            }
            @Override
            public void onFailure(Call<Employee> c, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Lỗi tải hồ sơ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (btnEditProfile != null)
            btnEditProfile.setOnClickListener(v ->
                    startActivity(new Intent(this, EditProfileActivity.class)));

        if (btnChangePassword != null)
            btnChangePassword.setOnClickListener(v ->
                    startActivity(new Intent(this, ChangePasswordActivity.class)));

        if (btnLogout != null)
            btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void showLogoutDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_logout_confirmation);

        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        ImageView btnClose      = dialog.findViewById(R.id.btnCloseDialog);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirmLogout);
        MaterialButton btnCancel  = dialog.findViewById(R.id.btnCancelLogout);

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

        if (btnConfirm != null) btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            doLogout();
        });

        dialog.show();
    }

    private void doLogout() {
        String savedEmail = prefs.getString("saved_username", null);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        if (savedEmail != null) editor.putString("saved_username", savedEmail);
        editor.apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}