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
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.R;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.viewmodel.RequestViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class RequestFormActivity extends AppCompatActivity {

    private static final String EXTRA_EMP_ID = "empId";
    private static final String EXTRA_REQUEST_ID = "requestId";
    private static final String EXTRA_REQUEST_TITLE = "requestTitle";
    private static final String EXTRA_REQUEST_DESCRIPTION = "requestDescription";

    private TextInputEditText edtTitle;
    private TextInputEditText edtDescription;
    private TextView tvSelectedFile;
    private MaterialButton btnSubmit;
    private Uri selectedAttachmentUri;
    private RequestViewModel viewModel;
    private long employeeId;
    private long requestId = -1L;
    private boolean isEditMode = false;

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
        TextView tvTitle = findViewById(R.id.tvTitle);

        viewModel = new ViewModelProvider(this).get(RequestViewModel.class);

        employeeId = resolveEmployeeId();
        configureFormMode(tvTitle);
        observeViewModel();

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

    private void configureFormMode(TextView tvTitle) {
        requestId = getIntent().getLongExtra(EXTRA_REQUEST_ID, -1L);
        isEditMode = requestId > 0;

        if (!isEditMode) {
            return;
        }

        String title = getIntent().getStringExtra(EXTRA_REQUEST_TITLE);
        String description = getIntent().getStringExtra(EXTRA_REQUEST_DESCRIPTION);

        if (title != null) {
            edtTitle.setText(title);
        }
        if (description != null) {
            edtDescription.setText(description);
        }

        tvTitle.setText("Cap nhat don tu");
        btnSubmit.setText("Cap nhat don");
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

    private void observeViewModel() {
        viewModel.isLoading.observe(this, isLoading -> btnSubmit.setEnabled(!Boolean.TRUE.equals(isLoading)));

        viewModel.errorMessage.observe(this, message -> {
            if (message != null && !message.trim().isEmpty()) {
                Toast.makeText(RequestFormActivity.this, message, Toast.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });

        viewModel.submitSuccess.observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                String message = isEditMode ? "Cap nhat don thanh cong" : "Da gui don thanh cong";
                if (selectedAttachmentUri != null && !isEditMode) {
                    message = "Da gui don thanh cong. API hien tai chua luu tep dinh kem.";
                }
                Toast.makeText(RequestFormActivity.this, message, Toast.LENGTH_LONG).show();
                viewModel.clearSubmitSuccessEvent();
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    private long resolveEmployeeId() {
        long intentEmployeeId = getIntent().getLongExtra(EXTRA_EMP_ID, -1L);
        if (intentEmployeeId > 0) {
            return intentEmployeeId;
        }

        SharedPreferences prefs = getSharedPreferences("qlns_pref", Context.MODE_PRIVATE);
        long storedEmployeeId = prefs.getLong("employeeId", -1L);
        if (storedEmployeeId > 0) {
            return storedEmployeeId;
        }

        return prefs.getLong("userId", -1L);
    }

    private void submitRequest(String title, String description) {
        if (isEditMode && requestId <= 0) {
            Toast.makeText(this, "Don khong hop le", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.submitRequest(employeeId, isEditMode ? requestId : null, title, description, isEditMode);
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
