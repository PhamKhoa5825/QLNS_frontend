package com.example.myapplication.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditEmployeeActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 2001;

    private TextInputEditText etFullName, etPhone, etAddress, etPosition;
    private Spinner spinnerGender, spinnerDept;
    private MaterialButton btnSave, btnCancel;
    private ProgressBar progressBar;
    private ImageView ivAvatar;
    private TextView tvAvatarFallback;
    private FloatingActionButton btnChangeAvatar;

    private ApiService apiService;
    private Long employeeId;
    private List<Department> deptList = new ArrayList<>();
    private Long selectedDeptId;
    private Uri selectedImageUri = null;
    private String avatarBase64 = null;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        displaySelectedImage(selectedImageUri);
                        convertImageToBase64(selectedImageUri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_employee);

        employeeId = getIntent().getLongExtra("employeeId", -1);
        apiService = RetrofitClient.getClient().create(ApiService.class);

        bindViews();
        loadDepartments();
        loadEmployee();
    }

    private void bindViews() {
        etFullName       = findViewById(R.id.etFullName);
        etPhone          = findViewById(R.id.etPhone);
        etAddress        = findViewById(R.id.etAddress);
        etPosition       = findViewById(R.id.etPosition);
        spinnerGender    = findViewById(R.id.spinnerGender);
        spinnerDept      = findViewById(R.id.spinnerDept);
        progressBar      = findViewById(R.id.progressBar);
        btnSave          = findViewById(R.id.btnSave);
        btnCancel        = findViewById(R.id.btnCancel);
        ivAvatar         = findViewById(R.id.ivAvatar);
        tvAvatarFallback = findViewById(R.id.tvAvatarFallback);
        btnChangeAvatar  = findViewById(R.id.btnChangeAvatar);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> finish());

        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> doSave());
        btnChangeAvatar.setOnClickListener(v -> checkPermissionAndPickImage());

        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"Nam", "Nữ", "Khác"});
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(genderAdapter);
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
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Cần cấp quyền truy cập ảnh để đổi avatar", Toast.LENGTH_SHORT).show();
            }
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

        Glide.with(this)
                .load(uri)
                .circleCrop()
                .placeholder(R.drawable.ic_user_placeholder)
                .into(ivAvatar);
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

    // ── LOAD DỮ LIỆU ─────────────────────────────────────────────

    private void loadEmployee() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getEmployeeById(employeeId).enqueue(new Callback<Employee>() {
            @Override public void onResponse(Call<Employee> c, Response<Employee> r) {
                progressBar.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null) {
                    Employee emp = r.body();
                    etFullName.setText(emp.getFullName());
                    etPhone.setText(emp.getPhone());
                    etAddress.setText(emp.getAddress());
                    etPosition.setText(emp.getPosition());
                    selectedDeptId = emp.getDepartmentId();

                    if ("FEMALE".equals(emp.getGender())) spinnerGender.setSelection(1);
                    else if ("OTHER".equals(emp.getGender())) spinnerGender.setSelection(2);
                    else spinnerGender.setSelection(0);

                    selectDeptInSpinner(emp.getDepartmentId());
                    loadCurrentAvatar(emp);
                }
            }
            @Override public void onFailure(Call<Employee> c, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(EditEmployeeActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCurrentAvatar(Employee emp) {
        String url = emp.getAvatarUrl();
        if (url != null && !url.isEmpty()) {
            ivAvatar.setVisibility(View.VISIBLE);
            tvAvatarFallback.setVisibility(View.GONE);
            Glide.with(this).load(url).circleCrop()
                    .placeholder(R.drawable.ic_user_placeholder)
                    .error(R.drawable.ic_user_placeholder)
                    .into(ivAvatar);
        } else {
            ivAvatar.setVisibility(View.GONE);
            tvAvatarFallback.setVisibility(View.VISIBLE);
            tvAvatarFallback.setText(emp.getAvatarText());
        }
    }

    private void loadDepartments() {
        apiService.getDepartments().enqueue(new Callback<List<Department>>() {
            @Override public void onResponse(Call<List<Department>> c, Response<List<Department>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    deptList = r.body();
                    List<String> names = new ArrayList<>();
                    for (Department d : deptList) names.add(d.getName());
                    ArrayAdapter<String> a = new ArrayAdapter<>(EditEmployeeActivity.this,
                            android.R.layout.simple_spinner_item, names);
                    a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerDept.setAdapter(a);
                    spinnerDept.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                            selectedDeptId = deptList.get(pos).getId();
                        }
                        @Override public void onNothingSelected(AdapterView<?> p) {}
                    });
                    selectDeptInSpinner(selectedDeptId);
                }
            }
            @Override public void onFailure(Call<List<Department>> c, Throwable t) {}
        });
    }

    private void selectDeptInSpinner(Long deptId) {
        if (deptId == null || deptList.isEmpty()) return;
        for (int i = 0; i < deptList.size(); i++) {
            if (deptList.get(i).getId().equals(deptId)) { spinnerDept.setSelection(i); break; }
        }
    }

    // ── LƯU ───────────────────────────────────────────────────────

    private void doSave() {
        String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String phone    = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String address  = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
        String position = etPosition.getText() != null ? etPosition.getText().toString().trim() : "";

        if (fullName.isEmpty() || position.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền tên và chức vụ", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] genderValues = {"MALE", "FEMALE", "OTHER"};
        CreateEmployeeRequest req = new CreateEmployeeRequest(fullName, "", "", position, selectedDeptId);
        req.setPhone(phone);
        req.setAddress(address);
        req.setGender(genderValues[spinnerGender.getSelectedItemPosition()]);

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        apiService.updateEmployee(employeeId, req).enqueue(new Callback<Employee>() {
            @Override public void onResponse(Call<Employee> c, Response<Employee> r) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                if (r.isSuccessful()) {
                    Toast.makeText(EditEmployeeActivity.this, "Đã cập nhật", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditEmployeeActivity.this, "Lỗi: " + r.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Employee> c, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(EditEmployeeActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}