package com.example.myapplication.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.myapplication.R;
import com.example.myapplication.viewmodel.ChangePasswordViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ChangePasswordActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextInputLayout tilOldPassword;
    private TextInputLayout tilNewPassword;
    private TextInputLayout tilConfirmPassword;
    private TextInputEditText etOldPassword;
    private TextInputEditText etNewPassword;
    private TextInputEditText etConfirmPassword;
    private MaterialButton btnCancel;
    private MaterialButton btnConfirm;

    private ChangePasswordViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(ChangePasswordViewModel.class);

        initViews();
        setupObservers();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tilOldPassword = findViewById(R.id.tilOldPassword);
        tilNewPassword = findViewById(R.id.tilNewPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etOldPassword = findViewById(R.id.etOldPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnCancel = findViewById(R.id.btnCancel);
        btnConfirm = findViewById(R.id.btnConfirm);
    }

    private void setupObservers() {
        // Quan sát trạng thái thành công
        viewModel.isSuccess.observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(this, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        // Quan sát lỗi
        viewModel.errorMessage.observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });

        // Quan sát trạng thái Loading để cập nhật UI
        viewModel.isLoading.observe(this, isLoading -> {
            btnConfirm.setEnabled(!isLoading);
            btnConfirm.setText(isLoading ? "Đang xử lý..." : "Xác nhận");
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());
        btnConfirm.setOnClickListener(v -> {
            if (validateInput()) {
                String oldPass = etOldPassword.getText().toString().trim();
                String newPass = etNewPassword.getText().toString().trim();
                viewModel.changePassword(oldPass, newPass);
            }
        });
    }

    private boolean validateInput() {
        String oldPassword = etOldPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(oldPassword)) {
            tilOldPassword.setError("Vui lòng nhập mật khẩu cũ");
            return false;
        } else {
            tilOldPassword.setError(null);
        }

        if (TextUtils.isEmpty(newPassword)) {
            tilNewPassword.setError("Vui lòng nhập mật khẩu mới");
            return false;
        } else {
            tilNewPassword.setError(null);
        }

        if (!newPassword.equals(confirmPassword)) {
            tilConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            return false;
        } else {
            tilConfirmPassword.setError(null);
        }

        return true;
    }
}
