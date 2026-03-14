package com.example.myapplication.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.CreateRequestRequest;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RequestFormActivity extends AppCompatActivity {

    private TextInputEditText edtTitle;
    private TextInputEditText edtDescription;
    private TextView tvSelectedFile;
    private MaterialButton btnSubmit;
    private Uri selectedAttachmentUri;
    private ApiService apiService;
    private long employeeId;

    private ActivityResultLauncher<String[]> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_form);

        RetrofitClient.init(this);

        edtTitle = findViewById(R.id.edtTitle);
        edtDescription = findViewById(R.id.edtDescription);
        btnSubmit = findViewById(R.id.btnSubmit);
        ImageButton btnBack = findViewById(R.id.btnBack);
        LinearLayout layoutAttachmentPicker = findViewById(R.id.layoutAttachmentPicker);
        tvSelectedFile = findViewById(R.id.tvSelectedFile);

        apiService = RetrofitClient.getApiService();
        employeeId = resolveEmployeeId();

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri == null) {
                        return;
                    }

                    selectedAttachmentUri = uri;
                    try {
                        getContentResolver().takePersistableUriPermission(
                                uri,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (SecurityException ignored) {
                        // Some providers do not offer persistable permission; temporary access still works.
                    }

                    String fileName = getFileName(uri);
                    if (fileName == null || fileName.trim().isEmpty()) {
                        fileName = "Tệp đã chọn";
                    }

                    tvSelectedFile.setVisibility(View.VISIBLE);
                    tvSelectedFile.setText("Đã chọn: " + fileName);
                    Toast.makeText(this, "Đã chọn tệp", Toast.LENGTH_SHORT).show();
                }
        );

        btnBack.setOnClickListener(v -> finish());

        layoutAttachmentPicker.setOnClickListener(v -> filePickerLauncher.launch(new String[]{
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "image/*"
        }));

        btnSubmit.setOnClickListener(v -> validateAndSubmit());
    }

    private void validateAndSubmit() {
        String title = edtTitle.getText() != null ? edtTitle.getText().toString().trim() : "";
        String desc = edtDescription.getText() != null ? edtDescription.getText().toString().trim() : "";

        if (title.isEmpty() || desc.isEmpty()) {
            Toast.makeText(
                    RequestFormActivity.this,
                    "Vui lòng nhập đầy đủ thông tin bắt buộc",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (employeeId <= 0) {
            Toast.makeText(
                    RequestFormActivity.this,
                    "Không tìm thấy thông tin nhân viên. Vui lòng đăng nhập lại.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        submitRequest(title, desc);
    }

    private long resolveEmployeeId() {
        long intentEmployeeId = getIntent().getLongExtra("empId", -1L);
        if (intentEmployeeId > 0) {
            return intentEmployeeId;
        }

        SharedPreferences prefs = getSharedPreferences("qlns_pref", Context.MODE_PRIVATE);
        return prefs.getLong("userId", -1L);
    }

    private void submitRequest(String title, String description) {
        btnSubmit.setEnabled(false);

        CreateRequestRequest request = new CreateRequestRequest(title, description);

        apiService.createRequest(employeeId, request)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        btnSubmit.setEnabled(true);

                        if (response.isSuccessful()) {
                            if (selectedAttachmentUri != null) {
                                Toast.makeText(
                                        RequestFormActivity.this,
                                        "Đã gửi đơn thành công. API hiện tại chưa lưu tệp đính kèm.",
                                        Toast.LENGTH_LONG
                                ).show();
                            } else {
                                Toast.makeText(RequestFormActivity.this, "Đã gửi đơn thành công", Toast.LENGTH_SHORT).show();
                            }
                            finish();
                            return;
                        }

                        Toast.makeText(
                                RequestFormActivity.this,
                                getErrorMessage(response),
                                Toast.LENGTH_LONG
                        ).show();
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        btnSubmit.setEnabled(true);
                        String message = t.getMessage() == null || t.getMessage().trim().isEmpty()
                                ? "Không thể kết nối tới máy chủ"
                                : t.getMessage();
                        Toast.makeText(
                                RequestFormActivity.this,
                                "Gửi đơn thất bại: " + message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private String getErrorMessage(Response<Void> response) {
        try (okhttp3.ResponseBody errorBody = response.errorBody()) {
            if (errorBody != null) {
                String errorText = errorBody.string();
                if (!errorText.trim().isEmpty()) {
                    return "Gửi đơn thất bại: " + errorText;
                }
            }
        } catch (IOException ignored) {
            // Fallback to status code below.
        }

        return "Gửi đơn thất bại: " + response.code();
    }

    private String getFileName(Uri uri) {
        if (uri == null) {
            return null;
        }

        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    String displayName = cursor.getString(nameIndex);
                    if (displayName != null && !displayName.trim().isEmpty()) {
                        return displayName;
                    }
                }
            }
        } catch (Exception ignored) {
            // Fall back to URI parsing below if the provider does not support queries.
        }

        String lastSegment = uri.getLastPathSegment();
        if (lastSegment == null || lastSegment.trim().isEmpty()) {
            return null;
        }

        int cutIndex = lastSegment.lastIndexOf('/');
        return cutIndex >= 0 && cutIndex < lastSegment.length() - 1
                ? lastSegment.substring(cutIndex + 1)
                : lastSegment;
    }
}
