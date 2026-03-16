package com.example.myapplication.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.google.android.material.textfield.TextInputEditText;

public class ForgotPasswordRequestActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password_request);

        TextInputEditText etUsername = findViewById(R.id.etUsername);
        Button btnCancel = findViewById(R.id.btnCancel);
        Button btnAgree = findViewById(R.id.btnAgree);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnAgree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString().trim();
                if (username.isEmpty()) {
                    Toast.makeText(ForgotPasswordRequestActivity.this, "Vui lòng nhập tên đăng nhập!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ForgotPasswordRequestActivity.this, "Gửi yêu cầu thành công! Quản trị viên sẽ liên hệ với bạn sớm.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }
}
