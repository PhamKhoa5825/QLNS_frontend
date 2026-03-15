package com.example.myapplication.ui;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView btnBack;
    private FloatingActionButton btnChangeAvatar;
    private TextInputEditText etFullName, etPhoneNumber, etEmail, etAddress;
    private MaterialButton btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        etFullName = findViewById(R.id.etFullName);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etEmail = findViewById(R.id.etEmail);
        etAddress = findViewById(R.id.etAddress);
        btnSave = findViewById(R.id.btnSave);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnChangeAvatar.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.avatar_change_developing), Toast.LENGTH_SHORT).show();
        });

        btnSave.setOnClickListener(v -> {
            // Hiển thị thông báo khi nhấn lưu (chưa xử lý logic)
            Toast.makeText(this, getString(R.string.save_success), Toast.LENGTH_SHORT).show();
            btnSave.postDelayed(this::finish, 1000);
        });
    }
}
