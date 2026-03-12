package com.example.myapplication.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.Locale;

public class LeaveApplicationActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private AutoCompleteTextView spinnerLeaveType;
    private TextInputEditText etStartDate, etEndDate, etReason;
    private LinearLayout layoutUpload;
    private MaterialButton btnSubmit;
    private TextView tvRemainingDays;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leave_application);

        initViews();
        setupLeaveTypeSpinner();
        setupDatePickers();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        spinnerLeaveType = findViewById(R.id.spinnerLeaveType);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        etReason = findViewById(R.id.etReason);
        layoutUpload = findViewById(R.id.layoutUpload);
        btnSubmit = findViewById(R.id.btnSubmit);
        tvRemainingDays = findViewById(R.id.tvRemainingDays);
    }

    private void setupLeaveTypeSpinner() {
        String[] leaveTypes = {"Nghỉ phép năm", "Nghỉ việc riêng", "Nghỉ ốm", "Nghỉ thai sản", "Khác"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, leaveTypes);
        spinnerLeaveType.setAdapter(adapter);
    }

    private void setupDatePickers() {
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));
    }

    private void showDatePicker(final TextInputEditText editText) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%d", monthOfYear + 1, dayOfMonth, year1);
                    editText.setText(selectedDate);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        layoutUpload.setOnClickListener(v -> {
            // TODO: Implement file picker
            Toast.makeText(this, "Tính năng tải tài liệu sẽ được cập nhật sau", Toast.LENGTH_SHORT).show();
        });

        btnSubmit.setOnClickListener(v -> {
            if (validateForm()) {
                // TODO: Handle data submission to backend
                Toast.makeText(this, "Đã gửi đơn xin nghỉ phép thành công!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private boolean validateForm() {
        if (spinnerLeaveType.getText().toString().isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn loại nghỉ phép", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etStartDate.getText().toString().isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ngày bắt đầu", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etEndDate.getText().toString().isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ngày kết thúc", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etReason.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập lý do nghỉ", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}