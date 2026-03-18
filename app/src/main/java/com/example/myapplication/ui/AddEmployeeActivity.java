package com.example.myapplication.ui;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.model.CreateEmployeeRequest;
import com.example.myapplication.model.Department;
import com.example.myapplication.model.Employee;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddEmployeeActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 2002;

    private TextInputEditText etFullName, etEmail, etPassword, etPhone,
            etAddress, etPosition, etDateOfBirth, etJoinDate;
    private TextInputLayout tilFullName, tilEmail, tilPassword, tilPosition, tilPhone;
    private Spinner spinnerGender, spinnerDept, spinnerRole;
    private MaterialButton btnSave, btnCancel;
    private ProgressBar progressBar;
    private ImageView ivAvatar;
    private TextView tvAvatarFallback;
    private FloatingActionButton btnChangeAvatar;

    private ApiService apiService;
    private List<Department> deptList = new ArrayList<>();
    private Long selectedDeptId = null;
    private String avatarBase64 = null;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        displaySelectedImage(uri);
                        convertImageToBase64(uri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Status bar trong suốt, trùng màu toolbar
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(R.layout.activity_add_employee);

        apiService = RetrofitClient.getClient().create(ApiService.class);
        bindViews();
        loadDepartments();
        setupGenderSpinner();
        setupRoleSpinner();
    }

    private void bindViews() {
        etFullName    = findViewById(R.id.etFullName);
        etEmail       = findViewById(R.id.etEmail);
        etPassword    = findViewById(R.id.etPassword);
        etPhone       = findViewById(R.id.etPhone);
        etAddress     = findViewById(R.id.etAddress);
        etPosition    = findViewById(R.id.etPosition);
        etDateOfBirth = findViewById(R.id.etDateOfBirth);
        etJoinDate    = findViewById(R.id.etJoinDate);

        tilFullName   = findViewById(R.id.tilFullName);
        tilEmail      = findViewById(R.id.tilEmail);
        tilPassword   = findViewById(R.id.tilPassword);
        tilPosition   = findViewById(R.id.tilPosition);
        tilPhone      = findViewById(R.id.tilPhone);

        spinnerGender = findViewById(R.id.spinnerGender);
        spinnerDept   = findViewById(R.id.spinnerDept);
        spinnerRole   = findViewById(R.id.spinnerRole);
        progressBar   = findViewById(R.id.progressBar);
        btnSave       = findViewById(R.id.btnSave);
        btnCancel     = findViewById(R.id.btnCancel);

        ivAvatar         = findViewById(R.id.ivAvatar);
        tvAvatarFallback = findViewById(R.id.tvAvatarFallback);
        btnChangeAvatar  = findViewById(R.id.btnChangeAvatar);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> finish());

        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> doSave());
        btnChangeAvatar.setOnClickListener(v -> checkPermissionAndPickImage());

        // Click vào ngày → mở DatePicker
        etDateOfBirth.setOnClickListener(v -> showDatePicker(etDateOfBirth));
        etJoinDate.setOnClickListener(v -> showDatePicker(etJoinDate));

        // Xóa lỗi khi user bắt đầu nhập
        clearErrorOnType(etFullName, tilFullName);
        clearErrorOnType(etEmail, tilEmail);
        clearErrorOnType(etPassword, tilPassword);
        clearErrorOnType(etPosition, tilPosition);
        clearErrorOnType(etPhone, tilPhone);
    }

    // ── DATE PICKER ───────────────────────────────────────────────

    private void showDatePicker(TextInputEditText target) {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        try {
            String[] parts = target.getText().toString().split("-");
            year = Integer.parseInt(parts[0]);
            month = Integer.parseInt(parts[1]) - 1;
            day = Integer.parseInt(parts[2]);
        } catch (Exception ignored) {}

        new DatePickerDialog(this, (view, y, m, d) ->
                target.setText(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)),
                year, month, day).show();
    }

    // ── CHỌN ẢNH ─────────────────────────────────────────────────

    private void checkPermissionAndPickImage() {
        String permission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQUEST_CODE);
        } else {
            openImagePicker();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
        } else {
            Toast.makeText(this, "Cần cấp quyền truy cập ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void displaySelectedImage(Uri uri) {
        ivAvatar.setVisibility(View.VISIBLE);
        tvAvatarFallback.setVisibility(View.GONE);
        Glide.with(this).load(uri).circleCrop()
                .placeholder(R.drawable.ic_user_placeholder).into(ivAvatar);
    }

    private void convertImageToBase64(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (is != null) is.close();
            int maxSize = 300;
            float scale = Math.min((float) maxSize / bitmap.getWidth(), (float) maxSize / bitmap.getHeight());
            Bitmap resized = Bitmap.createScaledBitmap(bitmap,
                    (int)(bitmap.getWidth() * scale), (int)(bitmap.getHeight() * scale), true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            avatarBase64 = "data:image/jpeg;base64," + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi xử lý ảnh", Toast.LENGTH_SHORT).show();
            avatarBase64 = null;
        }
    }

    // ── SPINNERS ──────────────────────────────────────────────────

    private void setupGenderSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"Nam", "Nữ", "Khác"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(adapter);
    }

    private void setupRoleSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"EMPLOYEE", "MANAGER", "ADMIN"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);
    }

    private void loadDepartments() {
        apiService.getDepartments().enqueue(new Callback<List<Department>>() {
            @Override public void onResponse(Call<List<Department>> call, Response<List<Department>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    deptList = response.body();
                    List<String> names = new ArrayList<>();
                    for (Department d : deptList) names.add(d.getName());
                    ArrayAdapter<String> a = new ArrayAdapter<>(AddEmployeeActivity.this,
                            android.R.layout.simple_spinner_item, names);
                    a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerDept.setAdapter(a);
                    spinnerDept.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                            selectedDeptId = deptList.get(pos).getId();
                        }
                        @Override public void onNothingSelected(AdapterView<?> p) {}
                    });
                }
            }
            @Override public void onFailure(Call<List<Department>> c, Throwable t) {
                Toast.makeText(AddEmployeeActivity.this, "Không tải được phòng ban", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── VALIDATION ────────────────────────────────────────────────

    private boolean validate() {
        boolean valid = true;
        tilFullName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilPosition.setError(null);
        tilPhone.setError(null);

        String fullName = getText(etFullName);
        String email    = getText(etEmail);
        String password = getText(etPassword);
        String position = getText(etPosition);
        String phone    = getText(etPhone);

        if (fullName.isEmpty()) { tilFullName.setError("Vui lòng nhập họ và tên"); valid = false; }
        if (email.isEmpty()) { tilEmail.setError("Vui lòng nhập email"); valid = false; }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { tilEmail.setError("Email không hợp lệ"); valid = false; }
        if (password.isEmpty()) { tilPassword.setError("Vui lòng nhập mật khẩu"); valid = false; }
        else if (password.length() < 6) { tilPassword.setError("Mật khẩu phải ít nhất 6 ký tự"); valid = false; }
        if (position.isEmpty()) { tilPosition.setError("Vui lòng nhập chức vụ"); valid = false; }
        if (!phone.isEmpty() && phone.length() < 9) { tilPhone.setError("Số điện thoại không hợp lệ"); valid = false; }

        return valid;
    }

    // ── SAVE ──────────────────────────────────────────────────────

    private void doSave() {
        if (!validate()) return;

        String[] genderValues = {"MALE", "FEMALE", "OTHER"};
        CreateEmployeeRequest req = new CreateEmployeeRequest(
                getText(etFullName), getText(etEmail), getText(etPassword),
                getText(etPosition), selectedDeptId);
        req.setPhone(getText(etPhone));
        req.setAddress(getText(etAddress));
        req.setGender(genderValues[spinnerGender.getSelectedItemPosition()]);

        String dob = getText(etDateOfBirth);
        String joinDate = getText(etJoinDate);
        if (!dob.isEmpty()) req.setDateOfBirth(dob);
        if (!joinDate.isEmpty()) req.setJoinDate(joinDate);

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        apiService.createEmployee(req).enqueue(new Callback<Employee>() {
            @Override public void onResponse(Call<Employee> call, Response<Employee> response) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(AddEmployeeActivity.this, "Đã thêm nhân viên thành công", Toast.LENGTH_SHORT).show();
                    finish();
                } else if (response.code() == 409) {
                    tilEmail.setError("Email đã được sử dụng");
                } else {
                    Toast.makeText(AddEmployeeActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Employee> c, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(AddEmployeeActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void clearErrorOnType(TextInputEditText et, TextInputLayout til) {
        et.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) til.setError(null); });
    }
}