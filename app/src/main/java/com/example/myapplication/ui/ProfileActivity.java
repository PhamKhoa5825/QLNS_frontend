package com.example.myapplication.ui;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;

public class ProfileActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvProfileName, tvProfilePosition, tvProfileDept, tvProfileCode, tvProfileEmail, tvProfilePhone;
    private MaterialButton btnEditProfile, btnChangePassword, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfilePosition = findViewById(R.id.tvProfilePosition);
        tvProfileDept = findViewById(R.id.tvProfileDept);
        tvProfileCode = findViewById(R.id.tvProfileCode);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfilePhone = findViewById(R.id.tvProfilePhone);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        btnChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void showLogoutDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_logout_confirmation);
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ImageView btnClose = dialog.findViewById(R.id.btnCloseDialog);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirmLogout);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancelLogout);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            // Xử lý đăng xuất (ví dụ quay về màn hình Login)
            Intent intent = new Intent(ProfileActivity.this, HomeEmployeeActivity.class); // Thay đổi tùy màn hình chính của bạn
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        dialog.show();
    }
}
