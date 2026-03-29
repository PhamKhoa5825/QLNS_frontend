package com.example.myapplication.ui;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.example.myapplication.model.dto.RequestDto;
import com.example.myapplication.model.entity.Request;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateRequestActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private AutoCompleteTextView spinnerLeaveType;
    private TextInputEditText etStartDate, etEndDate, etReason;
    private LinearLayout layoutUpload;
    private MaterialButton btnSubmit;
    private ProgressBar progressBar;

    private ApiService apiService;
    private SharedPreferences prefs;
    private Long employeeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_leave_application);

        // Đẩy btnBack xuống bằng chiều cao status bar
        View back = findViewById(R.id.btnBack);
        ViewCompat.setOnApplyWindowInsetsListener(back, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int dp16 = (int) (16 * getResources().getDisplayMetrics().density);
            ConstraintLayout.LayoutParams params =
                    (ConstraintLayout.LayoutParams) v.getLayoutParams();
            params.topMargin = dp16 + statusBarHeight;
            v.setLayoutParams(params);
            return insets;
        });

        prefs      = getSharedPreferences("qlns_pref", MODE_PRIVATE);
        employeeId = prefs.getLong("employeeId", -1);
        apiService = RetrofitClient.getClient().create(ApiService.class);

        initViews();
        setupLeaveTypeSpinner();
        setupDatePickers();
        setupListeners();
    }

    private void initViews() {
        btnBack         = findViewById(R.id.btnBack);
        spinnerLeaveType = findViewById(R.id.spinnerLeaveType);
        etStartDate     = findViewById(R.id.etStartDate);
        etEndDate       = findViewById(R.id.etEndDate);
        etReason        = findViewById(R.id.etReason);
        layoutUpload    = findViewById(R.id.layoutUpload);
        btnSubmit       = findViewById(R.id.btnSubmit);
        progressBar     = findViewById(R.id.progressBar);
    }

    private void setupLeaveTypeSpinner() {
        String[] leaveTypes = {"Nghỉ phép năm", "Nghỉ việc riêng", "Nghỉ ốm", "Nghỉ thai sản", "Khác"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, leaveTypes);
        spinnerLeaveType.setAdapter(adapter);
    }

    private void setupDatePickers() {
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));
    }

    private void showDatePicker(TextInputEditText editText) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            // Định dạng yyyy-MM-dd để gửi lên API
            editText.setText(String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, day));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        if (layoutUpload != null)
            layoutUpload.setOnClickListener(v ->
                    Toast.makeText(this, "Tính năng đính kèm file sẽ cập nhật sau", Toast.LENGTH_SHORT).show());

        btnSubmit.setOnClickListener(v -> {
            if (validateForm()) doSubmit();
        });
    }

    private boolean validateForm() {
        if (spinnerLeaveType.getText().toString().isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn loại đơn", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etStartDate.getText() == null || etStartDate.getText().toString().isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ngày bắt đầu", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etReason.getText() == null || etReason.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập lý do", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void doSubmit() {
        String leaveType = spinnerLeaveType.getText().toString();
        String startDate = etStartDate.getText().toString();
        String endDate   = etEndDate.getText() != null ? etEndDate.getText().toString() : "";
        String reason    = etReason.getText().toString().trim();

        // Tạo tiêu đề từ loại đơn + ngày
        String title = leaveType + " từ " + startDate + (endDate.isEmpty() ? "" : " đến " + endDate);
        String description = reason;

        btnSubmit.setEnabled(false);
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        apiService.createRequest(employeeId, new RequestDto.CreateRequestBody(title, description))
                .enqueue(new Callback<Request>() {
                    @Override
                    public void onResponse(Call<Request> call,
                                           Response<Request> response) {
                        btnSubmit.setEnabled(true);
                        if (progressBar != null) progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful()) {
                            Toast.makeText(CreateRequestActivity.this,
                                    "Đã gửi đơn thành công!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(CreateRequestActivity.this,
                                    "Gửi đơn thất bại: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onFailure(Call<Request> c, Throwable t) {
                        btnSubmit.setEnabled(true);
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(CreateRequestActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}