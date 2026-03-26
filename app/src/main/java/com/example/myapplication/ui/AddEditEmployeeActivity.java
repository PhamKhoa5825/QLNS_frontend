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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
import com.example.myapplication.network.ApiErrorHelper;
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

/**
 * AddEditEmployeeActivity — Gộp Add + Edit thành 1
 *
 * Cách dùng:
 * - Thêm mới:  new Intent(this, AddEditEmployeeActivity.class)   (không có extra)
 * - Sửa:       intent.putExtra("employeeId", empId)               (có extra)
 *
 * Chế độ Edit sẽ: ẩn ô email/password/role, load data fill vào form, gọi PUT thay POST.
 */
public class AddEditEmployeeActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 2001;

    // ── Views ──
    private TextInputEditText etFullName, etEmail, etPassword, etPhone,
            etAddress, etPosition, etDateOfBirth, etJoinDate;
    private TextInputLayout tilFullName, tilEmail, tilPassword, tilPosition, tilPhone;
    private Spinner spinnerGender, spinnerDept, spinnerRole;
    private MaterialButton btnSave, btnCancel;
    private ProgressBar progressBar;
    private ImageView ivAvatar;
    private TextView tvAvatarFallback;
    private FloatingActionButton btnChangeAvatar;

    // ── Views chỉ dùng khi Add (ẩn khi Edit) ──
    private View layoutEmailGroup, layoutPasswordGroup, layoutRoleGroup;

    // ── Data ──
    private ApiService apiService;
    private boolean isEditMode = false;
    private Long employeeId;
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

        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_add_edit_employee);

        apiService = RetrofitClient.getClient().create(ApiService.class);

        // Kiểm tra chế độ: có employeeId → Edit, không có → Add
        employeeId = getIntent().getLongExtra("employeeId", -1);
        isEditMode = (employeeId != -1);

        bindViews();
        setupMode();
        setupGenderSpinner();
        loadDepartments();

        if (isEditMode) {
            loadEmployee();
        } else {
            setupRoleSpinner();
        }
    }

    // ══════════════════════════════════════════════════════════
    //  BIND VIEWS
    // ══════════════════════════════════════════════════════════

    private void bindViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> finish());

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

        // Group views (để ẩn/hiện theo mode)
        layoutEmailGroup    = findViewById(R.id.layoutEmailGroup);
        layoutPasswordGroup = findViewById(R.id.layoutPasswordGroup);
        layoutRoleGroup     = findViewById(R.id.layoutRoleGroup);

        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> doSave());
        btnChangeAvatar.setOnClickListener(v -> checkPermissionAndPickImage());

        // Date picker
        if (etDateOfBirth != null) etDateOfBirth.setOnClickListener(v -> showDatePicker(etDateOfBirth));
        if (etJoinDate != null) etJoinDate.setOnClickListener(v -> showDatePicker(etJoinDate));

        // Clear error khi gõ
        if (tilFullName != null) clearErrorOnType(etFullName, tilFullName);
        if (tilEmail != null) clearErrorOnType(etEmail, tilEmail);
        if (tilPassword != null) clearErrorOnType(etPassword, tilPassword);
        if (tilPosition != null) clearErrorOnType(etPosition, tilPosition);
        if (tilPhone != null) clearErrorOnType(etPhone, tilPhone);
    }

    // ══════════════════════════════════════════════════════════
    //  CHẾ ĐỘ ADD / EDIT
    // ══════════════════════════════════════════════════════════

    private void setupMode() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        if (isEditMode) {
            // Chế độ SỬA
            if (toolbar != null) toolbar.setTitle("Chỉnh sửa nhân viên");
            btnSave.setText("Cập nhật");

            // Ẩn email, password, role (không cho sửa qua form này)
            if (layoutEmailGroup != null) layoutEmailGroup.setVisibility(View.GONE);
            if (layoutPasswordGroup != null) layoutPasswordGroup.setVisibility(View.GONE);
            if (layoutRoleGroup != null) layoutRoleGroup.setVisibility(View.GONE);

            // Ẩn ngày sinh + ngày vào làm nếu có (edit chỉ sửa thông tin cơ bản)
            View tilDob = findViewById(R.id.tilDateOfBirth);
            View tilJoin = findViewById(R.id.tilJoinDate);
            View hintDob = findViewById(R.id.tvHintDateOfBirth);
            View hintJoin = findViewById(R.id.tvHintJoinDate);
            if (tilDob != null) tilDob.setVisibility(View.GONE);
            if (tilJoin != null) tilJoin.setVisibility(View.GONE);
            if (hintDob != null) hintDob.setVisibility(View.GONE);
            if (hintJoin != null) hintJoin.setVisibility(View.GONE);

        } else {
            // Chế độ THÊM MỚI
            if (toolbar != null) toolbar.setTitle("Thêm nhân viên mới");
            btnSave.setText("Lưu lại");
        }
    }

    // ══════════════════════════════════════════════════════════
    //  LOAD DATA (Edit mode)
    // ══════════════════════════════════════════════════════════

    private void loadEmployee() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getEmployeeById(employeeId).enqueue(new Callback<Employee>() {
            @Override
            public void onResponse(Call<Employee> c, Response<Employee> r) {
                progressBar.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null) {
                    Employee emp = r.body();

                    etFullName.setText(emp.getFullName());
                    etPosition.setText(emp.getPosition());
                    etPhone.setText(emp.getPhone());
                    if (etAddress != null) etAddress.setText(emp.getAddress());

                    selectedDeptId = emp.getDepartmentId();

                    if ("FEMALE".equals(emp.getGender())) spinnerGender.setSelection(1);
                    else if ("OTHER".equals(emp.getGender())) spinnerGender.setSelection(2);
                    else spinnerGender.setSelection(0);

                    selectDeptInSpinner(emp.getDepartmentId());
                    loadCurrentAvatar(emp);
                }
            }

            @Override
            public void onFailure(Call<Employee> c, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AddEditEmployeeActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
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

    // ══════════════════════════════════════════════════════════
    //  SPINNERS
    // ══════════════════════════════════════════════════════════

    private void setupGenderSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"Nam", "Nữ", "Khác"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(adapter);
    }

    private void setupRoleSpinner() {
        if (spinnerRole == null) return;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"EMPLOYEE", "MANAGER", "ADMIN"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);
    }

    private void loadDepartments() {
        apiService.getDepartments().enqueue(new Callback<List<Department>>() {
            @Override
            public void onResponse(Call<List<Department>> c, Response<List<Department>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    deptList = r.body();
                    List<String> names = new ArrayList<>();
                    for (Department d : deptList) names.add(d.getName());
                    ArrayAdapter<String> a = new ArrayAdapter<>(AddEditEmployeeActivity.this,
                            android.R.layout.simple_spinner_item, names);
                    a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerDept.setAdapter(a);
                    spinnerDept.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                            selectedDeptId = deptList.get(pos).getId();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> p) {}
                    });

                    // Edit mode: chọn đúng PB hiện tại
                    if (isEditMode && selectedDeptId != null) {
                        selectDeptInSpinner(selectedDeptId);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Department>> c, Throwable t) {
                Toast.makeText(AddEditEmployeeActivity.this, "Không tải được phòng ban", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selectDeptInSpinner(Long deptId) {
        if (deptId == null || deptList.isEmpty()) return;
        for (int i = 0; i < deptList.size(); i++) {
            if (deptList.get(i).getId().equals(deptId)) {
                spinnerDept.setSelection(i);
                break;
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    //  VALIDATION
    // ══════════════════════════════════════════════════════════

    private boolean validate() {
        boolean valid = true;
        if (tilFullName != null) tilFullName.setError(null);
        if (tilEmail != null) tilEmail.setError(null);
        if (tilPassword != null) tilPassword.setError(null);
        if (tilPosition != null) tilPosition.setError(null);
        if (tilPhone != null) tilPhone.setError(null);

        String fullName = getText(etFullName);
        String position = getText(etPosition);
        String phone = getText(etPhone);

        if (fullName.isEmpty()) {
            tilFullName.setError("Vui lòng nhập họ và tên");
            valid = false;
        } else if (fullName.split("\\s+").length < 2) {
            tilFullName.setError("Họ tên phải có ít nhất 2 từ");
            valid = false;
        }

        if (position.isEmpty()) {
            tilPosition.setError("Vui lòng nhập chức vụ");
            valid = false;
        }

        if (!phone.isEmpty() && phone.length() < 9) {
            tilPhone.setError("Số điện thoại phải có ít nhất 9 chữ số");
            valid = false;
        }

        // Chỉ validate email + password khi Add
        if (!isEditMode) {
            String email = getText(etEmail);
            String password = getText(etPassword);

            if (email.isEmpty()) {
                tilEmail.setError("Vui lòng nhập email");
                valid = false;
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.setError("Email không đúng định dạng (ví dụ: abc@company.com)");
                valid = false;
            }

            if (password.isEmpty()) {
                tilPassword.setError("Vui lòng nhập mật khẩu");
                valid = false;
            } else if (password.length() < 6) {
                tilPassword.setError("Mật khẩu tối thiểu 6 ký tự");
                valid = false;
            }
        }

        return valid;
    }

    // ══════════════════════════════════════════════════════════
    //  SAVE
    // ══════════════════════════════════════════════════════════

    private void doSave() {
        if (!validate()) return;

        String[] genderValues = {"MALE", "FEMALE", "OTHER"};
        String fullName = getText(etFullName);
        String position = getText(etPosition);
        String phone = getText(etPhone);
        String address = getText(etAddress);
        String gender = genderValues[spinnerGender.getSelectedItemPosition()];

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        if (isEditMode) {
            // ── PUT: Cập nhật ──
            CreateEmployeeRequest req = new CreateEmployeeRequest(fullName, "", "", position, selectedDeptId);
            req.setPhone(phone);
            req.setAddress(address);
            req.setGender(gender);

            apiService.updateEmployee(employeeId, req).enqueue(new Callback<Employee>() {
                @Override
                public void onResponse(Call<Employee> c, Response<Employee> r) {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    if (r.isSuccessful()) {
                        Toast.makeText(AddEditEmployeeActivity.this, "Đã cập nhật thành công", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        ApiErrorHelper.show(AddEditEmployeeActivity.this, r, "Cập nhật nhân viên thất bại");
                    }
                }

                @Override
                public void onFailure(Call<Employee> c, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(AddEditEmployeeActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            // ── POST: Thêm mới ──
            String email = getText(etEmail);
            String password = getText(etPassword);
            String dob = getText(etDateOfBirth);
            String joinDate = getText(etJoinDate);

            CreateEmployeeRequest req = new CreateEmployeeRequest(fullName, email, password, position, selectedDeptId);
            req.setPhone(phone);
            req.setAddress(address);
            req.setGender(gender);
            if (!dob.isEmpty()) req.setDateOfBirth(dob);
            if (!joinDate.isEmpty()) req.setJoinDate(joinDate);

            apiService.createEmployee(req).enqueue(new Callback<Employee>() {
                @Override
                public void onResponse(Call<Employee> c, Response<Employee> r) {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    if (r.isSuccessful()) {
                        Toast.makeText(AddEditEmployeeActivity.this, "Đã thêm nhân viên thành công", Toast.LENGTH_SHORT).show();
                        finish();
                    } else if (r.code() == 409) {
                        if (tilEmail != null) tilEmail.setError("Email đã được sử dụng");
                    } else {
                        ApiErrorHelper.show(AddEditEmployeeActivity.this, r, "Thêm nhân viên thất bại");
                    }
                }

                @Override
                public void onFailure(Call<Employee> c, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(AddEditEmployeeActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // ══════════════════════════════════════════════════════════
    //  AVATAR
    // ══════════════════════════════════════════════════════════

    private void checkPermissionAndPickImage() {
        String permission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            pickImage();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pickImage();
        } else {
            Toast.makeText(this, "Cần quyền truy cập ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void displaySelectedImage(Uri uri) {
        ivAvatar.setVisibility(View.VISIBLE);
        tvAvatarFallback.setVisibility(View.GONE);
        Glide.with(this).load(uri).circleCrop().into(ivAvatar);
    }

    private void convertImageToBase64(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap original = BitmapFactory.decodeStream(is);
            int maxSize = 256;
            float scale = Math.min((float) maxSize / original.getWidth(), (float) maxSize / original.getHeight());
            Bitmap resized = Bitmap.createScaledBitmap(original,
                    (int) (original.getWidth() * scale), (int) (original.getHeight() * scale), true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            avatarBase64 = "data:image/jpeg;base64," + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi xử lý ảnh", Toast.LENGTH_SHORT).show();
            avatarBase64 = null;
        }
    }

    // ══════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════

    private void showDatePicker(TextInputEditText target) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (dp, year, month, day) -> {
            target.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private String getText(TextInputEditText et) {
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void clearErrorOnType(TextInputEditText et, TextInputLayout til) {
        if (et != null && til != null) {
            et.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) til.setError(null);
            });
        }
    }
}