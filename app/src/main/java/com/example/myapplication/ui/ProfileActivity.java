package com.example.myapplication.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;

public class ProfileActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private ImageButton btnBack;
    private TextView tvProfileName, tvProfilePosition, tvProfileDept,
            tvProfileCode, tvProfileEmail, tvProfilePhone;
    private MaterialButton btnEditProfile, btnChangePassword, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

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
        // Hiển thị thông tin đã lưu lúc login
        String fullName = prefs.getString("fullName", "");
        String role     = prefs.getString("role", "");

        if (tvProfileName != null) tvProfileName.setText(fullName);
        if (tvProfilePosition != null) tvProfilePosition.setText(role);
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
        // Xóa toàn bộ SharedPreferences
        prefs.edit().clear().apply();

        // Về LoginActivity, xóa back stack
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}