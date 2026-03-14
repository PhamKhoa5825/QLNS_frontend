package com.example.myapplication.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.R;
import com.example.myapplication.viewmodel.EmployeeViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvAvatarHint;
    private TextInputEditText etFullName, etPhoneNumber, etEmail, etAddress;
    private MaterialButton btnSave;

    private EmployeeViewModel viewModel;
    private Long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        viewModel = new ViewModelProvider(this).get(EmployeeViewModel.class);
        userId = getIntent().getLongExtra("USER_ID", -1);

        initViews();
        setupObservers();
        setupListeners();
        loadCurrentData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        // tvAvatarText = findViewById(R.id.tvAvatarText); 
        
        etFullName = findViewById(R.id.etFullName);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etEmail = findViewById(R.id.etEmail);
        etAddress = findViewById(R.id.etAddress);
        btnSave = findViewById(R.id.btnSave);
    }

    private void setupObservers() {
        viewModel.userProfile.observe(this, employee -> {
            if (employee != null) {
                etFullName.setText(employee.getFullName());
                etPhoneNumber.setText(employee.getPhone());
                etEmail.setText(employee.getEmail());
                etAddress.setText(employee.getAddress());
                // tvAvatarText.setText(employee.getAvatarText()); // Bỏ dòng gây crash
            }
        });

        viewModel.updateSuccess.observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
        });

        viewModel.errorMessage.observe(this, message -> {
            if (message != null) Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });

        viewModel.isLoading.observe(this, isLoading -> {
            btnSave.setEnabled(!isLoading);
            btnSave.setText(isLoading ? "Đang lưu..." : "Lưu thay đổi");
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void loadCurrentData() {
        if (userId != -1) {
            viewModel.loadEmployeeDetail(userId);
        }
    }

    private void saveProfile() {
        String phone = etPhoneNumber.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        if (phone.isEmpty() || email.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("phone", phone);
        updateData.put("email", email);
        updateData.put("address", address);

        viewModel.updateEmployeeProfile(userId, updateData);
    }
}
