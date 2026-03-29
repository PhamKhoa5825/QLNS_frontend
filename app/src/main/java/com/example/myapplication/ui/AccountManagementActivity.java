package com.example.myapplication.ui;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.dto.AccountDto;
import com.example.myapplication.model.entity.Employee;
import com.example.myapplication.network.ApiErrorHelper;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountManagementActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private EditText edtSearch;
    private ImageView btnFilter;
    private TextView tvFilterInfo;
    private TextView tvTotalAccounts, tvActiveAccounts;
    private ApiService apiService;
    private AccountAdapter adapter;
    private List<Employee> allEmployees = new ArrayList<>();
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    // Filter states
    private List<String> filterRoles = new ArrayList<>();
    private String filterStatus = "";
    private List<String> filterDepts = new ArrayList<>();
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(R.layout.activity_account_management);

        apiService = RetrofitClient.getClient().create(ApiService.class);

        // AppBarLayout padding cho status bar
        View appBar = findViewById(R.id.toolbar).getParent() instanceof View
                ? (View) findViewById(R.id.toolbar).getParent() : null;
        if (appBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
                int top = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top;
                v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBar     = findViewById(R.id.progressBar);
        tvTotalAccounts = findViewById(R.id.tvTotalAccounts);
        tvActiveAccounts = findViewById(R.id.tvActiveAccounts);
        edtSearch       = findViewById(R.id.edtSearchAccount);
        btnFilter       = findViewById(R.id.btnFilter);
        tvFilterInfo    = findViewById(R.id.tvFilterInfo);

        btnFilter.setOnClickListener(v -> showFilterDialog());

        recyclerView = findViewById(R.id.recyclerViewAccounts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AccountAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        setupSearch();
        loadEmployees();
    }

    private void setupSearch() {
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                currentQuery = s.toString().trim().toLowerCase();
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> applyFilters();
                searchHandler.postDelayed(searchRunnable, 300);
            }
        });
    }

    private void applyFilters() {
        updateFilterInfoText();

        List<Employee> filtered = allEmployees.stream().filter(e -> {
            // 1. Lọc theo từ khoá (Search)
            boolean matchSearch = true;
            if (!currentQuery.isEmpty()) {
                String name = e.getFullName() != null ? e.getFullName().toLowerCase() : "";
                String email = e.getEmail() != null ? e.getEmail().toLowerCase() : "";
                String dept = e.getDepartmentName() != null ? e.getDepartmentName().toLowerCase() : "";
                matchSearch = name.contains(currentQuery) || email.contains(currentQuery) || dept.contains(currentQuery);
            }
            if (!matchSearch) return false;

            // 2. Lọc theo Role
            boolean matchRole = filterRoles.isEmpty() || filterRoles.contains(e.getRoleRaw());
            if (!matchRole) return false;

            // 3. Lọc theo Department
            boolean matchDept = filterDepts.isEmpty() || filterDepts.contains(e.getDepartmentName());
            if (!matchDept) return false;

            // 4. Lọc theo Account Status
            boolean matchStatus = true;
            if (!filterStatus.isEmpty()) {
                boolean isActive = e.isAccountActive();
                if ("Hoạt động".equals(filterStatus)) matchStatus = isActive;
                else if ("Bị khoá".equals(filterStatus)) matchStatus = !isActive;
            }
            return matchStatus;

        }).collect(Collectors.toList());

        adapter.updateData(filtered);
    }

    private void loadEmployees() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getEmployees().enqueue(new Callback<List<Employee>>() {
            @Override public void onResponse(Call<List<Employee>> c, Response<List<Employee>> r) {
                progressBar.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null) {
                    allEmployees = r.body();
                    adapter.updateData(allEmployees);
                    updateStats();
                }
            }
            @Override public void onFailure(Call<List<Employee>> c, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AccountManagementActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStats() {
        tvTotalAccounts.setText(String.valueOf(allEmployees.size()));
        long active = allEmployees.stream()
                .filter(Employee::isAccountActive)
                .count();
        tvActiveAccounts.setText(String.valueOf(active));
    }

    private void updateFilterInfoText() {
        if (filterRoles.isEmpty() && filterStatus.isEmpty() && filterDepts.isEmpty()) {
            tvFilterInfo.setVisibility(View.GONE);
            btnFilter.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
            return;
        }

        StringBuilder sb = new StringBuilder("Đang lọc: ");
        if (!filterDepts.isEmpty()) {
            sb.append("PB: ").append(String.join(", ", filterDepts)).append("  ");
        }
        if (!filterStatus.isEmpty()) {
            sb.append("TT: ").append(filterStatus).append("  ");
        }
        if (!filterRoles.isEmpty()) {
            sb.append("Quyền: ").append(String.join(", ", filterRoles));
        }

        tvFilterInfo.setText(sb.toString().trim());
        tvFilterInfo.setVisibility(View.VISIBLE);
        btnFilter.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFBBDEFB));
    }

    // ── ACTIONS & FILTERS ───────────────────────────────────────────────────

    private void showFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_account_filter, null);
        dialog.setContentView(view);

        ChipGroup cgRole = view.findViewById(R.id.cgRole);
        ChipGroup cgAccountStatus = view.findViewById(R.id.cgAccountStatus);
        ChipGroup cgDept = view.findViewById(R.id.cgDepartment);
        MaterialButton btnReset = view.findViewById(R.id.btnReset);
        MaterialButton btnApply = view.findViewById(R.id.btnApply);

        // Populate Roles
        String[] roles = {"ADMIN", "MANAGER", "EMPLOYEE"};
        for (String r : roles) {
            Chip chip = new Chip(this);
            chip.setText(r);
            chip.setCheckable(true);
            chip.setChecked(filterRoles.contains(r));
            cgRole.addView(chip);
        }

        // Populate Status
        String[] statuses = {"Hoạt động", "Bị khoá"};
        for (String s : statuses) {
            Chip chip = new Chip(this);
            chip.setText(s);
            chip.setCheckable(true);
            chip.setChecked(s.equals(filterStatus));
            cgAccountStatus.addView(chip);
        }

        // Populate Departments
        List<String> validDepts = allEmployees.stream()
                .map(Employee::getDepartmentName)
                .filter(d -> d != null && !d.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        for (String d : validDepts) {
            Chip chip = new Chip(this);
            chip.setText(d);
            chip.setCheckable(true);
            chip.setChecked(filterDepts.contains(d));
            cgDept.addView(chip);
        }

        btnReset.setOnClickListener(v -> {
            filterRoles.clear();
            filterStatus = "";
            filterDepts.clear();
            applyFilters();
            dialog.dismiss();
        });

        btnApply.setOnClickListener(v -> {
            filterRoles.clear();
            for (int i = 0; i < cgRole.getChildCount(); i++) {
                Chip chip = (Chip) cgRole.getChildAt(i);
                if (chip.isChecked()) filterRoles.add(chip.getText().toString());
            }

            filterStatus = "";
            for (int i = 0; i < cgAccountStatus.getChildCount(); i++) {
                Chip chip = (Chip) cgAccountStatus.getChildAt(i);
                if (chip.isChecked()) {
                    filterStatus = chip.getText().toString();
                    break;
                }
            }

            filterDepts.clear();
            for (int i = 0; i < cgDept.getChildCount(); i++) {
                Chip chip = (Chip) cgDept.getChildAt(i);
                if (chip.isChecked()) filterDepts.add(chip.getText().toString());
            }

            applyFilters();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showResetPasswordDialog(Employee emp) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_reset_password, null);
        TextInputEditText etNewPassword = view.findViewById(R.id.etNewPassword);
        MaterialButton btnConfirm = view.findViewById(R.id.btnConfirm);
        MaterialButton btnCancel  = view.findViewById(R.id.btnCancel);

        AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String newPass = etNewPassword.getText() != null
                    ? etNewPassword.getText().toString().trim() : "";
            if (newPass.length() < 6) {
                etNewPassword.setError("Mật khẩu tối thiểu 6 ký tự");
                return;
            }
            Long uid = emp.getUserId() != null ? emp.getUserId() : emp.getId();
            apiService.resetPassword(uid, newPass).enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> c, Response<Void> r) {
                    dialog.dismiss();
                    if (r.isSuccessful()) {
                        Toast.makeText(AccountManagementActivity.this, "Đã đặt lại mật khẩu", Toast.LENGTH_SHORT).show();
                    } else {
                        ApiErrorHelper.show(AccountManagementActivity.this, r, "Đặt lại mật khẩu thất bại");
                    }
                }
                @Override public void onFailure(Call<Void> c, Throwable t) {
                    dialog.dismiss();
                    Toast.makeText(AccountManagementActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            });
        });
        dialog.show();
    }

    private void toggleAccountLock(Employee emp) {
        boolean isActive = emp.isAccountActive();
        String newStatus = isActive ? "INACTIVE" : "ACTIVE";
        String actionText = isActive ? "Khoá" : "Mở khoá";
        Long uid = emp.getUserId() != null ? emp.getUserId() : emp.getId();

        new AlertDialog.Builder(this)
                .setTitle(actionText + " tài khoản")
                .setMessage(actionText + " tài khoản của " + emp.getFullName() + "?\n\n"
                        + (isActive
                        ? "Nhân viên sẽ không thể đăng nhập cho đến khi được mở khoá."
                        : "Nhân viên sẽ có thể đăng nhập lại bình thường."))
                .setPositiveButton(actionText, (d, w) ->
                        apiService.updateAccountStatus(uid, newStatus)
                                .enqueue(new Callback<Void>() {
                                    @Override public void onResponse(Call<Void> c, Response<Void> r) {
                                        if (r.isSuccessful()) {
                                            Toast.makeText(AccountManagementActivity.this,
                                                    "Đã " + actionText.toLowerCase() + " tài khoản",
                                                    Toast.LENGTH_SHORT).show();
                                            loadEmployees();
                                        } else {
                                            ApiErrorHelper.show(AccountManagementActivity.this, r, "Thay đổi trạng thái tài khoản thất bại");
                                        }
                                    }
                                    @Override public void onFailure(Call<Void> c, Throwable t) {
                                        Toast.makeText(AccountManagementActivity.this,
                                                "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                                    }
                                }))
                .setNegativeButton("Huỷ", null).show();
    }

    private void showChangeRoleDialog(Employee emp) {
        String[] roles = {"EMPLOYEE", "MANAGER", "ADMIN"};
        int currentIndex = 0;
        String currentRole = emp.getRoleRaw();
        for (int i = 0; i < roles.length; i++) {
            if (roles[i].equals(currentRole)) { currentIndex = i; break; }
        }

        new AlertDialog.Builder(this)
                .setTitle("Đổi quyền: " + emp.getFullName())
                .setSingleChoiceItems(roles, currentIndex, null)
                .setPositiveButton("Xác nhận", (d, w) -> {
                    int selected = ((AlertDialog) d).getListView().getCheckedItemPosition();
                    String newRole = roles[selected];
                    apiService.updateEmployeeRole(emp.getId(), new AccountDto.UpdateRoleRequest(newRole))
                            .enqueue(new Callback<Employee>() {
                                @Override public void onResponse(Call<Employee> c, Response<Employee> r) {
                                    if (r.isSuccessful()) {
                                        Toast.makeText(AccountManagementActivity.this,
                                                "Đã đổi role → " + newRole, Toast.LENGTH_SHORT).show();
                                        loadEmployees();
                                    } else {
                                        ApiErrorHelper.show(AccountManagementActivity.this, r, "Đổi vai trò thất bại");
                                    }
                                }
                                @Override public void onFailure(Call<Employee> c, Throwable t) {
                                    Toast.makeText(AccountManagementActivity.this,
                                            "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Huỷ", null).show();
    }

    // ── ADAPTER ───────────────────────────────────────────────────

    class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.VH> {
        private List<Employee> list;
        AccountAdapter(List<Employee> list) { this.list = list; }
        void updateData(List<Employee> newList) { this.list = newList; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_employee_admin, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Employee emp = list.get(pos);

            // Tên
            h.tvName.setText(emp.getFullName());

            // Avatar chữ cái
            String name = emp.getFullName();
            if (name != null && !name.trim().isEmpty()) {
                String[] parts = name.trim().split(" ");
                h.tvAvatar.setText(String.valueOf(parts[parts.length - 1].charAt(0)).toUpperCase());
            } else {
                h.tvAvatar.setText("?");
            }

            // Role badge
            h.tvRole.setText(emp.getRoleRaw() != null ? emp.getRoleRaw() : "EMPLOYEE");

            // Phòng ban · Email
            StringBuilder info = new StringBuilder();
            String dept = emp.getDepartmentName();
            if (dept != null && !dept.isEmpty()) info.append(dept);
            String email = emp.getEmail();
            if (email != null && !email.isEmpty()) {
                if (info.length() > 0) info.append(" · ");
                info.append(email);
            }
            h.tvDept.setText(info.length() > 0 ? info.toString() : "—");

            // Trạng thái tài khoản
            boolean isActive = emp.isAccountActive();
            h.tvStatus.setText(isActive ? "Hoạt động" : "Bị khoá");
            GradientDrawable statusBg = new GradientDrawable();
            statusBg.setCornerRadius(40f);
            statusBg.setColor(Color.parseColor(isActive ? "#DCFCE7" : "#FEE2E2"));
            h.tvStatus.setBackground(statusBg);
            h.tvStatus.setTextColor(Color.parseColor(isActive ? "#16A34A" : "#DC2626"));

            // Nút khoá/mở — đổi text + icon + màu theo trạng thái
            if (isActive) {
                h.btnToggleLock.setText("Khoá");
                h.btnToggleLock.setTextColor(Color.parseColor("#EF4444"));
                h.btnToggleLock.setIconResource(android.R.drawable.ic_lock_lock);
                h.btnToggleLock.setIconTint(android.content.res.ColorStateList.valueOf(Color.parseColor("#EF4444")));
            } else {
                h.btnToggleLock.setText("Mở khoá");
                h.btnToggleLock.setTextColor(Color.parseColor("#16A34A"));
                h.btnToggleLock.setIconResource(android.R.drawable.ic_lock_idle_lock);
                h.btnToggleLock.setIconTint(android.content.res.ColorStateList.valueOf(Color.parseColor("#16A34A")));
            }

            // Click handlers
            h.btnResetPass.setOnClickListener(v -> showResetPasswordDialog(emp));
            h.btnChangeRole.setOnClickListener(v -> showChangeRoleDialog(emp));
            h.btnToggleLock.setOnClickListener(v -> toggleAccountLock(emp));
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvAvatar, tvName, tvRole, tvDept, tvStatus;
            MaterialButton btnResetPass, btnChangeRole, btnToggleLock;
            VH(View v) {
                super(v);
                tvAvatar      = v.findViewById(R.id.tvAvatar);
                tvName        = v.findViewById(R.id.tvEmployeeName);
                tvRole        = v.findViewById(R.id.tvEmployeeRole);
                tvDept        = v.findViewById(R.id.tvEmployeeDept);
                tvStatus      = v.findViewById(R.id.tvAccountStatus);
                btnResetPass  = v.findViewById(R.id.btnResetPass);
                btnChangeRole = v.findViewById(R.id.btnChangeRole);
                btnToggleLock = v.findViewById(R.id.btnToggleLock);
            }
        }
    }

    @Override protected void onResume() { super.onResume(); loadEmployees(); }
}