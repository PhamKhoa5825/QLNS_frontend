package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.R;
import com.example.myapplication.model.AuthenticationResponse;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.SharedPrefsManager;
import com.example.myapplication.viewmodel.AuthViewModel;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        RetrofitClient.init(this);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        
        initViews();
        observeViewModel();
    }

    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            } else {
                authViewModel.login(username, password);
            }
        });

        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordRequestActivity.class));
        });
    }

    private void observeViewModel() {
        authViewModel.isLoading.observe(this, isLoading -> {
            btnLogin.setEnabled(!isLoading);
        });

        authViewModel.loginResponse.observe(this, response -> {
            if (response != null) {
                saveAuthData(response);
                Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, HomeEmployeeActivity.class));
                finish();
            }
        });

        authViewModel.errorMessage.observe(this, message -> {
            if (message != null) {
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveAuthData(AuthenticationResponse authResponse) {
        SharedPrefsManager.getInstance(this).saveUserLogin(
            authResponse.getToken(),
            authResponse.getUserId(),
            authResponse.getUsername(),
            authResponse.getEmail(),
            authResponse.getRole(),
            authResponse.getDepartmentId(),
            authResponse.getEmployeeId()
        );
    }
}
