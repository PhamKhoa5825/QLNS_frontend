package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.AuthenticationRequest;
import com.example.myapplication.model.AuthenticationResponse;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.SharedPrefsManager;
import com.google.android.material.button.MaterialButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private MaterialButton btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // If already logged in, validate the token first
        if (SharedPrefsManager.getInstance(this).isLoggedIn()) {
            validateTokenAndProceed();
            return;
        }
        
        showLoginScreen();
    }

    private void validateTokenAndProceed() {
        ApiService apiService = RetrofitClient.getApiService(this);
        apiService.validateToken().enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    // Token is valid, go to Dashboard
                    startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                    finish();
                } else {
                    // Token is invalid/expired, force re-login
                    SharedPrefsManager.getInstance(LoginActivity.this).logout();
                    showLoginScreen();
                    Toast.makeText(LoginActivity.this, "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                // Network error - can't validate. Try to proceed anyway since 
                // the backend might just be starting up
                startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                finish();
            }
        });
    }

    private void showLoginScreen() {
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> handleLogin());
    }

    private void handleLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tài khoản và mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthenticationRequest request = new AuthenticationRequest(username, password);
        ApiService apiService = RetrofitClient.getApiService(this);
        
        apiService.login(request).enqueue(new Callback<AuthenticationResponse>() {
            @Override
            public void onResponse(Call<AuthenticationResponse> call, Response<AuthenticationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthenticationResponse authResponse = response.body();
                    
                    // Save session
                    SharedPrefsManager.getInstance(LoginActivity.this).saveUserLogin(
                            authResponse.getToken(),
                            authResponse.getUserId(),
                            authResponse.getUsername(),
                            authResponse.getRole()
                    );
                    
                    // Save department ID from login response
                    if (authResponse.getDepartmentId() != null) {
                        SharedPrefsManager.getInstance(LoginActivity.this).setDepartmentId(authResponse.getDepartmentId());
                    }
                    
                    // Save employee ID from login response
                    if (authResponse.getEmployeeId() != null) {
                        SharedPrefsManager.getInstance(LoginActivity.this).setEmployeeId(authResponse.getEmployeeId());
                        // Fetch descriptive details
                        fetchEmployeeDetails(authResponse.getEmployeeId());
                    } else {
                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                        finish();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Tài khoản hoặc mật khẩu không đúng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthenticationResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchEmployeeDetails(Long employeeId) {
        ApiService apiService = RetrofitClient.getApiService(this);
        apiService.getEmployeeById(employeeId).enqueue(new Callback<com.example.myapplication.model.Employee>() {
            @Override
            public void onResponse(Call<com.example.myapplication.model.Employee> call, Response<com.example.myapplication.model.Employee> response) {
                if (response.isSuccessful() && response.body() != null) {
                    com.example.myapplication.model.Employee emp = response.body();
                    SharedPrefsManager prefs = SharedPrefsManager.getInstance(LoginActivity.this);
                    prefs.setFullName(emp.getFullName());
                    prefs.setDepartmentName(emp.getDepartment());
                }
                // Proceed to Dashboard regardless of success (fallback to username happens there)
                startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                finish();
            }

            @Override
            public void onFailure(Call<com.example.myapplication.model.Employee> call, Throwable t) {
                startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                finish();
            }
        });
    }
}
