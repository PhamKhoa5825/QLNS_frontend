package com.example.myapplication.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.dto.NotificationDto;
import com.example.myapplication.model.entity.Department;
import com.example.myapplication.model.entity.Employee;
import com.example.myapplication.model.entity.Notification;
import com.example.myapplication.network.ApiErrorHelper;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateNotificationActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etContent;
    private RadioGroup rgTarget;
    private RadioButton rbCompany, rbDepartment, rbEmployee;
    private TextView tvDeptLabel, tvEmpLabel;
    private Spinner spinnerDept;
    private MaterialButton btnSend, btnCancel;
    private ProgressBar progressBar;

    // Employee autocomplete
    private TextInputLayout tilEmployee;
    private AutoCompleteTextView actvEmployee;
    private LinearLayout layoutSelectedEmployee;
    private TextView tvSelectedAvatar, tvSelectedName, tvSelectedInfo;
    private ImageView btnClearEmployee;

    private ApiService apiService;
    private Long createdById;
    private List<Department> deptList = new ArrayList<>();
    private List<Employee> empList = new ArrayList<>();
    private Long selectedDeptId = null;
    private Employee selectedEmployee = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(R.layout.activity_create_notification);

        // Đẩy AppBarLayout xuống bằng chiều cao status bar
        View appBar = findViewById(R.id.toolbar).getParent() instanceof View
                ? (View) findViewById(R.id.toolbar).getParent() : null;
        if (appBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
                int statusBarHeight = insets.getInsets(
                        androidx.core.view.WindowInsetsCompat.Type.statusBars()).top;
                v.setPadding(v.getPaddingLeft(), statusBarHeight,
                        v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        apiService  = RetrofitClient.getClient().create(ApiService.class);
        createdById = getSharedPreferences("qlns_pref", MODE_PRIVATE).getLong("employeeId", -1);

        bindViews();
        // Nhận prefill từ RequestListActivity (nếu có)
        String prefillTitle = getIntent().getStringExtra("prefillTitle");
        if (prefillTitle != null && etTitle != null) etTitle.setText(prefillTitle);

        String prefillContent = getIntent().getStringExtra("prefillContent");
        if (prefillContent != null && etContent != null) etContent.setText(prefillContent);

        loadDepartments();
        loadEmployees();
    }

    private void bindViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> finish());

        etTitle      = findViewById(R.id.etTitle);
        etContent    = findViewById(R.id.etContent);
        rgTarget     = findViewById(R.id.rgTarget);
        rbCompany    = findViewById(R.id.rbCompany);
        rbDepartment = findViewById(R.id.rbDepartment);
        rbEmployee   = findViewById(R.id.rbEmployee);
        tvDeptLabel  = findViewById(R.id.tvDeptLabel);
        tvEmpLabel   = findViewById(R.id.tvEmpLabel);
        spinnerDept  = findViewById(R.id.spinnerDept);
        progressBar  = findViewById(R.id.progressBar);
        btnSend      = findViewById(R.id.btnSend);
        btnCancel    = findViewById(R.id.btnCancel);

        // Employee autocomplete views
        tilEmployee           = findViewById(R.id.tilEmployee);
        actvEmployee          = findViewById(R.id.actvEmployee);
        layoutSelectedEmployee = findViewById(R.id.layoutSelectedEmployee);
        tvSelectedAvatar      = findViewById(R.id.tvSelectedAvatar);
        tvSelectedName        = findViewById(R.id.tvSelectedName);
        tvSelectedInfo        = findViewById(R.id.tvSelectedInfo);
        btnClearEmployee      = findViewById(R.id.btnClearEmployee);

        btnCancel.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> doSend());

        // Clear selected employee
        if (btnClearEmployee != null) {
            btnClearEmployee.setOnClickListener(v -> clearSelectedEmployee());
        }

        rgTarget.setOnCheckedChangeListener((group, checkedId) -> {
            boolean showDept = (checkedId == R.id.rbDepartment);
            boolean showEmp  = (rbEmployee != null && checkedId == R.id.rbEmployee);

            tvDeptLabel.setVisibility(showDept ? View.VISIBLE : View.GONE);
            spinnerDept.setVisibility(showDept ? View.VISIBLE : View.GONE);

            if (tvEmpLabel != null) tvEmpLabel.setVisibility(showEmp ? View.VISIBLE : View.GONE);
            if (tilEmployee != null) tilEmployee.setVisibility(showEmp ? View.VISIBLE : View.GONE);

            // Ẩn card khi đổi target
            if (!showEmp) {
                if (layoutSelectedEmployee != null) layoutSelectedEmployee.setVisibility(View.GONE);
            } else if (selectedEmployee != null) {
                if (layoutSelectedEmployee != null) layoutSelectedEmployee.setVisibility(View.VISIBLE);
            }
        });
    }

    // ── LOAD DATA ─────────────────────────────────────────────────

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
                }
            }
            @Override public void onFailure(Call<List<Department>> c, Throwable t) {}
        });
    }

    private void loadEmployees() {
        apiService.getEmployees().enqueue(new Callback<List<Employee>>() {
            @Override public void onResponse(Call<List<Employee>> c, Response<List<Employee>> r) {
                if (r.isSuccessful() && r.body() != null && actvEmployee != null) {
                    empList = r.body();
                    setupEmployeeAutocomplete();
                }
            }
            @Override public void onFailure(Call<List<Employee>> c, Throwable t) {}
        });
    }

    // ── AUTOCOMPLETE EMPLOYEE (custom 2-dòng) ──────────────────

    private void setupEmployeeAutocomplete() {
        EmployeeDropdownAdapter adapter = new EmployeeDropdownAdapter(empList);
        actvEmployee.setAdapter(adapter);

        actvEmployee.setOnItemClickListener((parent, view, position, id) -> {
            Employee emp = (Employee) parent.getItemAtPosition(position);
            if (emp != null) selectEmployee(emp);
        });
    }

    /**
     * Custom ArrayAdapter: hiện 2 dòng (tên + phòng ban · chức vụ),
     * filter theo tên/phòng ban/chức vụ khi gõ.
     */
    private class EmployeeDropdownAdapter extends ArrayAdapter<Employee> {
        private final List<Employee> allEmployees;
        private List<Employee> filtered;

        EmployeeDropdownAdapter(List<Employee> employees) {
            super(CreateNotificationActivity.this, R.layout.item_employee_dropdown, employees);
            this.allEmployees = new ArrayList<>(employees);
            this.filtered = new ArrayList<>(employees);
        }

        @Override public int getCount() { return filtered.size(); }
        @Override public Employee getItem(int pos) { return filtered.get(pos); }

        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            if (convertView == null)
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.item_employee_dropdown, parent, false);

            Employee emp = getItem(position);

            TextView tvAvatar = convertView.findViewById(R.id.tvDropdownAvatar);
            TextView tvName   = convertView.findViewById(R.id.tvDropdownName);
            TextView tvInfo   = convertView.findViewById(R.id.tvDropdownInfo);

            tvName.setText(emp.getFullName());

            // Avatar chữ cái
            String name = emp.getFullName();
            if (name != null && !name.trim().isEmpty()) {
                String[] parts = name.trim().split(" ");
                tvAvatar.setText(String.valueOf(parts[parts.length - 1].charAt(0)).toUpperCase());
            } else {
                tvAvatar.setText("?");
            }

            // Dòng 2: Phòng ban · Chức vụ
            StringBuilder info = new StringBuilder();
            String dept = emp.getDepartmentName();
            if (dept != null && !dept.isEmpty()) info.append(dept);
            String pos = emp.getPosition();
            if (pos != null && !pos.isEmpty()) {
                if (info.length() > 0) info.append(" · ");
                info.append(pos);
            }
            tvInfo.setText(info.length() > 0 ? info.toString() : "—");

            return convertView;
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    if (constraint == null || constraint.length() == 0) {
                        results.values = new ArrayList<>(allEmployees);
                        results.count = allEmployees.size();
                    } else {
                        String query = constraint.toString().toLowerCase().trim();
                        List<Employee> matches = new ArrayList<>();
                        for (Employee e : allEmployees) {
                            String name = e.getFullName() != null ? e.getFullName().toLowerCase() : "";
                            String dept = e.getDepartmentName() != null ? e.getDepartmentName().toLowerCase() : "";
                            String pos  = e.getPosition() != null ? e.getPosition().toLowerCase() : "";
                            if (name.contains(query) || dept.contains(query) || pos.contains(query)) {
                                matches.add(e);
                            }
                        }
                        results.values = matches;
                        results.count = matches.size();
                    }
                    return results;
                }

                @SuppressWarnings("unchecked")
                @Override protected void publishResults(CharSequence constraint, FilterResults results) {
                    filtered = (List<Employee>) results.values;
                    if (results.count > 0) notifyDataSetChanged();
                    else notifyDataSetInvalidated();
                }

                @Override public CharSequence convertResultToString(Object resultValue) {
                    // Khi chọn, hiện tên trong ô input (sẽ bị clear ngay sau)
                    return ((Employee) resultValue).getFullName();
                }
            };
        }
    }

    private void selectEmployee(Employee emp) {
        selectedEmployee = emp;
        actvEmployee.setText("");
        actvEmployee.clearFocus();

        // Hiện card preview
        if (layoutSelectedEmployee != null) {
            layoutSelectedEmployee.setVisibility(View.VISIBLE);

            String name = emp.getFullName();
            tvSelectedName.setText(name);

            // Avatar chữ cái
            if (name != null && !name.trim().isEmpty()) {
                String[] parts = name.trim().split(" ");
                String last = parts[parts.length - 1];
                tvSelectedAvatar.setText(!last.isEmpty() ? String.valueOf(last.charAt(0)).toUpperCase() : "?");
            } else {
                tvSelectedAvatar.setText("?");
            }

            // Info: "Phòng ban - Chức vụ"
            StringBuilder info = new StringBuilder();
            String dept = emp.getDepartmentName();
            if (dept != null && !dept.isEmpty()) info.append(dept);
            String pos = emp.getPosition();
            if (pos != null && !pos.isEmpty()) {
                if (info.length() > 0) info.append(" · ");
                info.append(pos);
            }
            tvSelectedInfo.setText(info.length() > 0 ? info.toString() : "Chưa có phòng ban");
        }
    }

    private void clearSelectedEmployee() {
        selectedEmployee = null;
        if (layoutSelectedEmployee != null) layoutSelectedEmployee.setVisibility(View.GONE);
        if (actvEmployee != null) actvEmployee.setText("");
    }

    // ── SEND ──────────────────────────────────────────────────────

    private void doSend() {
        String title   = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String content = etContent.getText() != null ? etContent.getText().toString().trim() : "";

        if (title.isEmpty()) { etTitle.setError("Tiêu đề không được trống"); return; }
        if (content.isEmpty()) { etContent.setError("Nội dung không được trống"); return; }

        String targetType;
        Long deptId = null;
        Long targetEmpId = null;

        int checkedId = rgTarget.getCheckedRadioButtonId();
        if (checkedId == R.id.rbDepartment) {
            targetType = "DEPARTMENT";
            deptId = selectedDeptId;
            if (deptId == null) { Toast.makeText(this, "Vui lòng chọn phòng ban", Toast.LENGTH_SHORT).show(); return; }
        } else if (rbEmployee != null && checkedId == R.id.rbEmployee) {
            targetType = "EMPLOYEE";
            if (selectedEmployee == null) {
                Toast.makeText(this, "Vui lòng chọn nhân viên", Toast.LENGTH_SHORT).show();
                return;
            }
            targetEmpId = selectedEmployee.getId();
        } else {
            targetType = "COMPANY";
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSend.setEnabled(false);

        NotificationDto.CreateNotificationRequest req =
                new NotificationDto.CreateNotificationRequest(title, content, targetType, deptId, targetEmpId, createdById);

        apiService.createNotification(req).enqueue(new Callback<Notification>() {
            @Override public void onResponse(Call<Notification> c,
                                             Response<Notification> r) {
                progressBar.setVisibility(View.GONE);
                btnSend.setEnabled(true);
                if (r.isSuccessful()) {
                    Toast.makeText(CreateNotificationActivity.this, "Đã gửi thông báo", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    ApiErrorHelper.show(CreateNotificationActivity.this, r, "Gửi thông báo thất bại");
                }
            }
            @Override public void onFailure(Call<Notification> c, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnSend.setEnabled(true);
                Toast.makeText(CreateNotificationActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}