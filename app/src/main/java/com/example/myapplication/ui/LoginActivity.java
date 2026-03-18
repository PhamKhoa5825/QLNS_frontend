package com.example.myapplication.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.example.myapplication.model.AuthModels;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText edtUsername, edtPassword;
    private CheckBox cbRemember;
    private MaterialButton btnLogin;
    private SharedPreferences prefs;

    private static final String PREF_NAME = "qlns_pref";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        RetrofitClient.init(this);
        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        if (prefs.getString("token", null) != null) {
            goToDashboard();
            return;
        }

        edtUsername = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        cbRemember  = findViewById(R.id.cbRemember);
        btnLogin    = findViewById(R.id.btnLogin);

        String savedUsername = prefs.getString("saved_username", "");
        if (!savedUsername.isEmpty()) {
            edtUsername.setText(savedUsername);
            cbRemember.setChecked(true);
        }

        btnLogin.setOnClickListener(v -> doLogin());
    }

    private void doLogin() {
        String username = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (username.isEmpty()) { edtUsername.setError("Nhập tên đăng nhập"); return; }
        if (password.isEmpty()) { edtPassword.setError("Nhập mật khẩu"); return; }

        btnLogin.setEnabled(false);
        btnLogin.setText("Đang đăng nhập...");

        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.login(new AuthModels.LoginRequest(username, password))
                .enqueue(new Callback<AuthModels.AuthResponse>() {
                    @Override
                    public void onResponse(Call<AuthModels.AuthResponse> call,
                                           Response<AuthModels.AuthResponse> response) {
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Đăng nhập");

                        if (response.isSuccessful() && response.body() != null) {
                            AuthModels.AuthResponse auth = response.body();

                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("token",      auth.token);
                            editor.putString("role",       auth.role);
                            editor.putLong("userId",       auth.userId != null   ? auth.userId   : -1);
                            editor.putLong("employeeId",   auth.employeeId != null ? auth.employeeId : -1);
                            editor.putString("fullName",   auth.fullName);
                            editor.putString("avatarUrl",  auth.avatarUrl);

                            if (cbRemember.isChecked()) {
                                editor.putString("saved_username", username);
                            } else {
                                editor.remove("saved_username");
                            }
                            editor.apply();

                            goToDashboard();
                        } else {
                            int code = response.code();
                            if (code == 401) {
                                Toast.makeText(LoginActivity.this,
                                        "Sai tên đăng nhập hoặc mật khẩu", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(LoginActivity.this,
                                        "Lỗi đăng nhập: " + code, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<AuthModels.AuthResponse> call, Throwable t) {
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Đăng nhập");
                        Toast.makeText(LoginActivity.this,
                                "Không kết nối được server", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void goToDashboard() {
        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }
}