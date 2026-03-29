package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.utils.SharedPrefsManager;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvHeaderName, tvHeaderDept, tvHeaderAvatarText;
    private View btnHeaderNotifications, btnHeaderExtra, containerProfileLink;
    private ImageView ivHeaderAvatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        TextView tvName = findViewById(R.id.tvProfileName);
        TextView tvRole = findViewById(R.id.tvProfileRole);
        TextView tvAvatar = findViewById(R.id.tvProfileAvatar);
        Button btnLogout = findViewById(R.id.btnLogoutProfile);

        // Top Bar
        tvHeaderName = findViewById(R.id.tvHeaderName);
        tvHeaderDept = findViewById(R.id.tvHeaderDept);
        tvHeaderAvatarText = findViewById(R.id.tvHeaderAvatarText);
        ivHeaderAvatar = findViewById(R.id.ivHeaderAvatar);
        btnHeaderNotifications = findViewById(R.id.btnHeaderNotifications);
        btnHeaderExtra = findViewById(R.id.btnHeaderExtra);
        containerProfileLink = findViewById(R.id.containerProfileLink);

        setupTopBar();
        
        if (containerProfileLink != null) {
            containerProfileLink.setOnClickListener(v -> finish());
        }

        if (btnHeaderNotifications != null) {
            btnHeaderNotifications.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));
        }

        SharedPrefsManager prefs = SharedPrefsManager.getInstance(this);
        String name = prefs.getFullName();
        String username = prefs.getUsername();
        
        if (tvName != null) tvName.setText(name.isEmpty() ? username : name);
        if (tvAvatar != null) tvAvatar.setText(String.valueOf(username.charAt(0)).toUpperCase());
        
        String role = prefs.getRole();
        if (tvRole != null && role != null) {
            tvRole.setText(role);
        }

        Button btnViewPayroll = findViewById(R.id.btnViewPayroll);
        if (btnViewPayroll != null) {
            btnViewPayroll.setOnClickListener(v -> startActivity(new Intent(this, PayrollActivity.class)));
        }

        btnLogout.setOnClickListener(v -> {
            prefs.logout();
            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void setupTopBar() {
        SharedPrefsManager prefs = SharedPrefsManager.getInstance(this);
        String name = prefs.getFullName();
        String dept = prefs.getDepartmentName();
        String username = prefs.getUsername();

        if (tvHeaderName != null) tvHeaderName.setText(name.isEmpty() ? username : name);
        if (tvHeaderDept != null) tvHeaderDept.setText(dept != null ? dept : "No Department");
        if (tvHeaderAvatarText != null && !username.isEmpty()) {
            tvHeaderAvatarText.setText(String.valueOf(username.charAt(0)).toUpperCase());
        }
    }
}
