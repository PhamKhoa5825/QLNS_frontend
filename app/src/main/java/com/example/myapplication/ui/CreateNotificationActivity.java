package com.example.myapplication.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.example.myapplication.model.Department;
import com.example.myapplication.model.NotificationModels;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateNotificationActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etContent;
    private RadioGroup rgTarget;
    private RadioButton rbCompany, rbDepartment;
    private Spinner spinnerDept;
    private TextView tvDeptLabel;
    private MaterialButton btnSend, btnCancel;
    private ProgressBar progressBar;

    private ApiService apiService;
    private SharedPreferences prefs;
    private Long employeeId;
    private List<Department> deptList = new ArrayList<>();
    private Long selectedDeptId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_notification);

        prefs      = getSharedPreferences("qlns_pref", MODE_PRIVATE);
        employeeId = prefs.getLong("employeeId", -1);
        apiService = RetrofitClient.getClient().create(ApiService.class);

        bindViews();
        loadDepartments();
    }

    private void bindViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etTitle      = findViewById(R.id.etTitle);
        etContent    = findViewById(R.id.etContent);
        rgTarget     = findViewById(R.id.rgTarget);
        rbCompany    = findViewById(R.id.rbCompany);
        rbDepartment = findViewById(R.id.rbDepartment);
        spinnerDept  = findViewById(R.id.spinnerDept);
        tvDeptLabel  = findViewById(R.id.tvDeptLabel);
        progressBar  = findViewById(R.id.progressBar);
        btnSend   = findViewById(R.id.btnSend);
        btnCancel = findViewById(R.id.btnCancel);

        // Mặc định: toàn công ty → ẩn spinner phòng ban
        spinnerDept.setVisibility(View.GONE);
        tvDeptLabel.setVisibility(View.GONE);

        rgTarget.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isDept = checkedId == R.id.rbDepartment;
            spinnerDept.setVisibility(isDept ? View.VISIBLE : View.GONE);
            tvDeptLabel.setVisibility(isDept ? View.VISIBLE : View.GONE);
        });

        btnCancel.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> doSend());
    }

    private void loadDepartments() {
        apiService.getDepartments().enqueue(new Callback<List<Department>>() {
            @Override public void onResponse(Call<List<Department>> c, Response<List<Department>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    deptList = r.body();
                    List<String> names = new ArrayList<>();
                    for (Department d : deptList) names.add(d.getName());
                    ArrayAdapter<String> a = new ArrayAdapter<>(CreateNotificationActivity.this,
                            android.R.layout.simple_spinner_item, names);
                    a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerDept.setAdapter(a);
                    spinnerDept.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                            selectedDeptId = deptList.get(pos).getId();
                        }
                        @Override public void onNothingSelected(AdapterView<?> p) {}
                    });
                    if (!deptList.isEmpty()) selectedDeptId = deptList.get(0).getId();
                }
            }
            @Override public void onFailure(Call<List<Department>> c, Throwable t) {}
        });
    }

    private void doSend() {
        String title   = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String content = etContent.getText() != null ? etContent.getText().toString().trim() : "";

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền tiêu đề và nội dung", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isDept = rbDepartment.isChecked();
        String targetType = isDept ? "DEPARTMENT" : "COMPANY";
        Long deptId = isDept ? selectedDeptId : null;

        if (isDept && deptId == null) {
            Toast.makeText(this, "Vui lòng chọn phòng ban", Toast.LENGTH_SHORT).show();
            return;
        }

        NotificationModels.CreateNotificationRequest req =
                new NotificationModels.CreateNotificationRequest(
                        title, content, targetType, deptId, employeeId);

        progressBar.setVisibility(View.VISIBLE);
        btnSend.setEnabled(false);

        apiService.createNotification(req)
                .enqueue(new Callback<NotificationModels.NotificationResponse>() {
                    @Override
                    public void onResponse(Call<NotificationModels.NotificationResponse> c,
                                           Response<NotificationModels.NotificationResponse> r) {
                        progressBar.setVisibility(View.GONE);
                        btnSend.setEnabled(true);
                        if (r.isSuccessful()) {
                            Toast.makeText(CreateNotificationActivity.this,
                                    "Đã gửi thông báo thành công", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(CreateNotificationActivity.this,
                                    "Lỗi gửi thông báo: " + r.code(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onFailure(Call<NotificationModels.NotificationResponse> c, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        btnSend.setEnabled(true);
                        Toast.makeText(CreateNotificationActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}