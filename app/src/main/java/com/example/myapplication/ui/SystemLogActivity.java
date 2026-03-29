package com.example.myapplication.ui;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.entity.Employee;
import com.example.myapplication.model.entity.SystemLog;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SystemLogActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private ChipGroup cgActionFilter;
    private ApiService apiService;
    private LogAdapter adapter;

    private static final String[] ACTIONS = {"Tất cả", "LOGIN", "CREATE", "UPDATE", "DELETE", "LOGOUT"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_log);

        apiService = RetrofitClient.getClient().create(ApiService.class);

        // ── Status bar trong suốt, header gradient chạy lên phía sau ──
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        // Đẩy AppBarLayout xuống dưới status bar
        AppBarLayout appBarLayout = findViewById(R.id.toolbar).getParent() instanceof AppBarLayout
                ? (AppBarLayout) findViewById(R.id.toolbar).getParent() : null;
        if (appBarLayout != null) {
            appBarLayout.setPadding(0, getStatusBarHeight(), 0, 0);
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Nút filter trên toolbar
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_filter) {
                showFilterDialog();
                return true;
            }
            return false;
        });

        progressBar    = findViewById(R.id.progressBar);
        tvEmpty        = findViewById(R.id.tvEmpty);
        recyclerView   = findViewById(R.id.recyclerViewLogs);
        cgActionFilter = findViewById(R.id.cgActionFilter);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LogAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        setupFilterChips();
        loadLogs();
    }

    // ==================== CHIP FILTER (nhanh) ====================

    private void setupFilterChips() {
        for (String action : ACTIONS) {
            Chip chip = new Chip(this);
            chip.setText(action);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(android.R.color.white);
            chip.setChipStrokeWidth(2f);
            if ("Tất cả".equals(action)) chip.setChecked(true);

            chip.setOnCheckedChangeListener((v, checked) -> {
                if (checked) {
                    if ("Tất cả".equals(action)) {
                        loadLogs();
                    } else {
                        loadLogsByAction(action);
                    }
                }
            });

            cgActionFilter.addView(chip);
        }
    }

    // ==================== FILTER DIALOG (nâng cao) ====================

    // Cache danh sách để không gọi API mỗi lần mở dialog
    private List<String> employeeNames = new ArrayList<>();
    private List<String> departmentNames = new ArrayList<>();
    private List<String> positionNames = new ArrayList<>();
    private boolean filterDataLoaded = false;

    private void showFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_filter_log, null);
        dialog.setContentView(view);

        // --- Action dropdown (chỉ chọn, không gõ) ---
        AutoCompleteTextView actvAction = view.findViewById(R.id.actvAction);
        String[] actionOptions = {"Tất cả", "LOGIN", "LOGOUT", "CREATE", "UPDATE", "DELETE"};
        actvAction.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, actionOptions));

        // --- Nhân viên (vừa gõ vừa chọn, autocomplete) ---
        AutoCompleteTextView actvEmployee = view.findViewById(R.id.actvEmployee);

        // --- Phòng ban (vừa gõ vừa chọn) ---
        AutoCompleteTextView actvDepartment = view.findViewById(R.id.actvDepartment);

        // --- Chức vụ (vừa gõ vừa chọn) ---
        AutoCompleteTextView actvPosition = view.findViewById(R.id.actvPosition);

        // Load danh sách cho autocomplete
        loadFilterData(actvEmployee, actvDepartment, actvPosition);

        // --- Date pickers ---
        TextInputEditText etFromDate = view.findViewById(R.id.etFromDate);
        TextInputEditText etToDate = view.findViewById(R.id.etToDate);

        etFromDate.setOnClickListener(v -> showDatePicker(etFromDate));
        etToDate.setOnClickListener(v -> showDatePicker(etToDate));

        // --- Buttons ---
        MaterialButton btnReset = view.findViewById(R.id.btnReset);
        MaterialButton btnApply = view.findViewById(R.id.btnApply);

        btnReset.setOnClickListener(v -> {
            actvAction.setText("", false);
            actvEmployee.setText("", false);
            actvDepartment.setText("", false);
            actvPosition.setText("", false);
            etFromDate.setText("");
            etToDate.setText("");
        });

        btnApply.setOnClickListener(v -> {
            String action     = actvAction.getText().toString().trim();
            String employee   = actvEmployee.getText().toString().trim();
            String department = actvDepartment.getText().toString().trim();
            String position   = actvPosition.getText().toString().trim();
            String fromDate   = etFromDate.getText().toString().trim();
            String toDate     = etToDate.getText().toString().trim();

            dialog.dismiss();
            applyFilter(action, employee, department, position, fromDate, toDate);
        });

        dialog.show();
    }

    /**
     * Load danh sách nhân viên, phòng ban, chức vụ cho autocomplete.
     * Gọi API 1 lần duy nhất rồi cache lại.
     */
    private void loadFilterData(AutoCompleteTextView actvEmployee,
                                AutoCompleteTextView actvDepartment,
                                AutoCompleteTextView actvPosition) {
        // Nếu đã load rồi thì set adapter luôn
        if (filterDataLoaded) {
            actvEmployee.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, employeeNames));
            actvDepartment.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, departmentNames));
            actvPosition.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, positionNames));
            return;
        }

        // Gọi API lấy danh sách nhân viên — reuse getEmployees() trả về Employee
        apiService.getEmployees().enqueue(new Callback<List<Employee>>() {
            @Override
            public void onResponse(Call<List<Employee>> call,
                                   Response<List<Employee>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    employeeNames.clear();
                    departmentNames.clear();
                    positionNames.clear();

                    for (Employee emp : response.body()) {
                        // Nhân viên
                        if (emp.getFullName() != null && !employeeNames.contains(emp.getFullName())) {
                            employeeNames.add(emp.getFullName());
                        }
                        // Phòng ban (loại trùng)
                        String deptName = emp.getDepartmentName();
                        if (deptName != null && !departmentNames.contains(deptName)) {
                            departmentNames.add(deptName);
                        }
                        // Chức vụ (loại trùng)
                        if (emp.getPosition() != null && !positionNames.contains(emp.getPosition())) {
                            positionNames.add(emp.getPosition());
                        }
                    }

                    filterDataLoaded = true;

                    actvEmployee.setAdapter(new ArrayAdapter<>(SystemLogActivity.this,
                            android.R.layout.simple_dropdown_item_1line, employeeNames));
                    actvDepartment.setAdapter(new ArrayAdapter<>(SystemLogActivity.this,
                            android.R.layout.simple_dropdown_item_1line, departmentNames));
                    actvPosition.setAdapter(new ArrayAdapter<>(SystemLogActivity.this,
                            android.R.layout.simple_dropdown_item_1line, positionNames));
                }
            }

            @Override
            public void onFailure(Call<List<Employee>> call, Throwable t) {
                // Nếu lỗi thì vẫn cho gõ tay, chỉ không có gợi ý
            }
        });
    }

    private void showDatePicker(TextInputEditText target) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (dp, year, month, day) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, day);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            target.setText(sdf.format(selected.getTime()));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * Áp dụng bộ lọc: load tất cả log rồi filter local theo nhiều điều kiện.
     * Khi backend hỗ trợ API search nâng cao, thay bằng gọi API trực tiếp.
     */
    private void applyFilter(String action, String employee, String department,
                             String position, String fromDate, String toDate) {
        showLoading();
        apiService.getSystemLogs().enqueue(new Callback<List<SystemLog>>() {
            @Override
            public void onResponse(Call<List<SystemLog>> c,
                                   Response<List<SystemLog>> r) {
                hideLoading();
                if (r.isSuccessful() && r.body() != null) {
                    List<SystemLog> filtered = new ArrayList<>();

                    for (SystemLog log : r.body()) {
                        // Lọc theo action
                        if (!action.isEmpty() && !"Tất cả".equals(action)) {
                            if (log.action == null || !log.action.equalsIgnoreCase(action)) continue;
                        }

                        // Lọc theo nhân viên (username chứa keyword)
                        if (!employee.isEmpty()) {
                            if (log.username == null ||
                                    !log.username.toLowerCase().contains(employee.toLowerCase())) continue;
                        }

                        // Lọc theo phòng ban (nếu log có field department)
                        if (!department.isEmpty()) {
                            if (log.department == null ||
                                    !log.department.toLowerCase().contains(department.toLowerCase())) continue;
                        }

                        // Lọc theo chức vụ (nếu log có field position)
                        if (!position.isEmpty()) {
                            if (log.position == null ||
                                    !log.position.toLowerCase().contains(position.toLowerCase())) continue;
                        }

                        // Lọc theo khoảng ngày
                        if (!fromDate.isEmpty() && !toDate.isEmpty() && log.createdAt != null) {
                            String logDate = log.createdAt.substring(0, 10);
                            if (logDate.compareTo(fromDate) < 0 || logDate.compareTo(toDate) > 0) continue;
                        }

                        filtered.add(log);
                    }

                    updateList(filtered);

                    if (filtered.isEmpty()) {
                        Toast.makeText(SystemLogActivity.this,
                                "Không tìm thấy kết quả phù hợp", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<SystemLog>> c, Throwable t) {
                hideLoading();
                Toast.makeText(SystemLogActivity.this, "Lỗi lọc nhật ký", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==================== API CALLS ====================

    private void loadLogs() {
        showLoading();
        apiService.getSystemLogs().enqueue(new Callback<List<SystemLog>>() {
            @Override
            public void onResponse(Call<List<SystemLog>> c,
                                   Response<List<SystemLog>> r) {
                hideLoading();
                if (r.isSuccessful() && r.body() != null) {
                    updateList(r.body());
                }
            }
            @Override
            public void onFailure(Call<List<SystemLog>> c, Throwable t) {
                hideLoading();
                Toast.makeText(SystemLogActivity.this, "Lỗi tải nhật ký", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadLogsByAction(String action) {
        showLoading();
        apiService.filterLogsByAction(action).enqueue(new Callback<List<SystemLog>>() {
            @Override
            public void onResponse(Call<List<SystemLog>> c,
                                   Response<List<SystemLog>> r) {
                hideLoading();
                if (r.isSuccessful() && r.body() != null) {
                    updateList(r.body());
                }
            }
            @Override
            public void onFailure(Call<List<SystemLog>> c, Throwable t) {
                hideLoading();
                Toast.makeText(SystemLogActivity.this, "Lỗi lọc nhật ký", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==================== UI HELPERS ====================

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }

    private void updateList(List<SystemLog> list) {
        adapter.updateData(list);
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // ==================== ADAPTER ====================

    static class LogAdapter extends RecyclerView.Adapter<LogAdapter.VH> {

        private List<SystemLog> list;

        LogAdapter(List<SystemLog> list) {
            this.list = list;
        }

        void updateData(List<SystemLog> newList) {
            this.list = newList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_system_log, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            SystemLog log = list.get(position);

            h.tvUsername.setText(log.username != null ? log.username : "System");
            h.tvDescription.setText(log.description != null ? log.description : "");
            h.tvAction.setText(log.action != null ? log.action : "");
            h.tvTime.setText(formatTime(log.createdAt));

            // Icon chữ cái đầu của action
            if (log.action != null && !log.action.isEmpty()) {
                h.tvIcon.setText(String.valueOf(log.action.charAt(0)));
            }

            // Màu badge theo action
            int color;
            switch (log.action != null ? log.action : "") {
                case "LOGIN":  color = Color.parseColor("#3B82F6"); break;
                case "CREATE": color = Color.parseColor("#10B981"); break;
                case "UPDATE": color = Color.parseColor("#F59E0B"); break;
                case "DELETE": color = Color.parseColor("#EF4444"); break;
                case "LOGOUT": color = Color.parseColor("#6B7280"); break;
                default:       color = Color.parseColor("#8B5CF6"); break;
            }
            h.tvAction.getBackground().setTint(color);
        }

        private String formatTime(String iso) {
            if (iso == null) return "";
            try {
                String date = iso.substring(8, 10) + "/" + iso.substring(5, 7);
                String time = iso.substring(11, 16);
                return date + " " + time;
            } catch (Exception e) {
                return iso;
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvIcon, tvAction, tvUsername, tvDescription, tvTime;

            VH(View v) {
                super(v);
                tvIcon        = v.findViewById(R.id.tvLogIcon);
                tvAction      = v.findViewById(R.id.tvLogAction);
                tvUsername     = v.findViewById(R.id.tvLogUsername);
                tvDescription = v.findViewById(R.id.tvLogDescription);
                tvTime        = v.findViewById(R.id.tvLogTime);
            }
        }
    }

    // ==================== HELPER ====================

    private int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return resourceId > 0 ? getResources().getDimensionPixelSize(resourceId) : 0;
    }
}