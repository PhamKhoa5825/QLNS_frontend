package com.example.myapplication.ui;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.R;
import com.example.myapplication.model.Employee;
import com.example.myapplication.viewmodel.EmployeeViewModel;
import com.google.android.material.button.MaterialButton;

public class ProfileActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvProfileName, tvProfilePosition, tvProfileDept, tvProfileCode, tvProfileEmail, tvProfilePhone, tvProfileAvatar;
    private ImageView ivProfileAvatar;
    private MaterialButton btnEditProfile, btnChangePassword, btnLogout;
    
    private EmployeeViewModel viewModel;
    private Long userId;
    private static final int EDIT_PROFILE_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        viewModel = new ViewModelProvider(this).get(EmployeeViewModel.class);
        userId = viewModel.getSavedUserId();

        initViews();
        setupObservers();
        setupClickListeners();
        
        loadData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfilePosition = findViewById(R.id.tvProfilePosition);
        tvProfileDept = findViewById(R.id.tvProfileDept);
        tvProfileCode = findViewById(R.id.tvProfileCode);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfilePhone = findViewById(R.id.tvProfilePhone);
        tvProfileAvatar = findViewById(R.id.tvProfileAvatar);
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupObservers() {
        viewModel.userProfile.observe(this, this::displayProfile);
        
        viewModel.errorMessage.observe(this, message -> {
            if (message != null) Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });

        viewModel.isAuthorized.observe(this, isAuth -> {
            if (!isAuth) {
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivityForResult(intent, EDIT_PROFILE_REQUEST);
        });

        btnChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void loadData() {
        if (userId != -1) {
            viewModel.loadEmployeeDetail(userId);
        } else {
            Toast.makeText(this, "Không tìm thấy ID người dùng", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayProfile(Employee employee) {
        if (employee == null) return;
        tvProfileName.setText(employee.getFullName());
        tvProfilePosition.setText(employee.getPosition());
        tvProfileDept.setText(employee.getDepartmentName());
        tvProfileCode.setText(String.valueOf(employee.getId()));
        tvProfileEmail.setText(employee.getEmail());
        tvProfilePhone.setText(employee.getPhone());

        ivProfileAvatar.setVisibility(View.GONE); 
        tvProfileAvatar.setVisibility(View.VISIBLE);
        tvProfileAvatar.setText(employee.getAvatarText());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_PROFILE_REQUEST && resultCode == RESULT_OK) {
            loadData();
        }
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
            viewModel.logout();
        });

        dialog.show();
    }
}
