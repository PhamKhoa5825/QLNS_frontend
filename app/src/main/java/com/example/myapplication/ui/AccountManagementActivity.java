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
import com.example.myapplication.model.AccountDto;
import com.example.myapplication.model.Employee;
import com.example.myapplication.network.ApiErrorHelper;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.TopBarHelper;
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
    private View btnFilter;
    private TextView tvFilterInfo, tvTotalAccounts, tvActiveAccounts;
    private ApiService apiService;
    private AccountAdapter adapter;
    private List<Employee> allEmployees = new ArrayList<>();
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private List<String> filterRoles = new ArrayList<>();
    private String filterStatus = "";
    private List<String> filterDepts = new ArrayList<>();
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_management);

        apiService = RetrofitClient.getApiService(this);

        TopBarHelper.setupTopBar(this);
        View btnSearchHeader = findViewById(R.id.btnHeaderExtra);
        if (btnSearchHeader != null) {
            btnSearchHeader.setVisibility(View.VISIBLE);
            btnSearchHeader.setOnClickListener(v -> showFilterDialog());
        }

        progressBar = findViewById(R.id.progressBar);
        tvTotalAccounts = findViewById(R.id.tvTotalAccounts);
        tvActiveAccounts = findViewById(R.id.tvActiveAccounts);
        edtSearch = findViewById(R.id.edtSearchAccount);
        btnFilter = findViewById(R.id.btnFilter);
        tvFilterInfo = findViewById(R.id.tvFilterInfo);

        if (btnFilter != null) btnFilter.setOnClickListener(v -> showFilterDialog());

        recyclerView = findViewById(R.id.recyclerViewAccounts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AccountAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        setupSearch();
        loadEmployees();
    }

    private void setupSearch() {
        if (edtSearch == null) return;
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
            boolean matchSearch = currentQuery.isEmpty() || 
                (e.getFullName() != null && e.getFullName().toLowerCase().contains(currentQuery)) ||
                (e.getEmail() != null && e.getEmail().toLowerCase().contains(currentQuery)) ||
                (e.getDepartmentName() != null && e.getDepartmentName().toLowerCase().contains(currentQuery));
            if (!matchSearch) return false;
            if (!filterRoles.isEmpty() && !filterRoles.contains(e.getRoleRaw())) return false;
            if (!filterDepts.isEmpty() && !filterDepts.contains(e.getDepartmentName())) return false;
            if (!filterStatus.isEmpty()) {
                boolean isActive = e.isAccountActive();
                if ("Hoạt động".equals(filterStatus) && !isActive) return false;
                if ("Bị khoá".equals(filterStatus) && isActive) return false;
            }
            return true;
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
            @Override public void onFailure(Call<List<Employee>> c, Throwable t) { progressBar.setVisibility(View.GONE); }
        });
    }

    private void updateStats() {
        if (tvTotalAccounts != null) tvTotalAccounts.setText(String.valueOf(allEmployees.size()));
        if (tvActiveAccounts != null) {
            long active = allEmployees.stream().filter(Employee::isAccountActive).count();
            tvActiveAccounts.setText(String.valueOf(active));
        }
    }

    private void updateFilterInfoText() {
        if (tvFilterInfo == null) return;
        if (filterRoles.isEmpty() && filterStatus.isEmpty() && filterDepts.isEmpty()) {
            tvFilterInfo.setVisibility(View.GONE);
            return;
        }
        tvFilterInfo.setText("Đang lọc...");
        tvFilterInfo.setVisibility(View.VISIBLE);
    }

    private void showFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_account_filter, null);
        dialog.setContentView(view);

        ChipGroup cgRole = view.findViewById(R.id.cgRole);
        ChipGroup cgStatus = view.findViewById(R.id.cgAccountStatus);
        
        MaterialButton btnApply = view.findViewById(R.id.btnApply);
        btnApply.setOnClickListener(v -> {
            filterRoles.clear();
            for (int i = 0; i < cgRole.getChildCount(); i++) {
                Chip chip = (Chip) cgRole.getChildAt(i);
                if (chip.isChecked()) filterRoles.add(chip.getText().toString());
            }
            // More logic for status and depts...
            applyFilters();
            dialog.dismiss();
        });
        dialog.show();
    }

    private void showResetPasswordDialog(Employee emp) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_reset_password, null);
        TextInputEditText etPass = view.findViewById(R.id.etNewPassword);
        MaterialButton btnConfirm = view.findViewById(R.id.btnConfirm);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);
        
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String pass = etPass.getText().toString();
            Long uid = emp.getUserId() != null ? emp.getUserId() : emp.getId();
            apiService.resetPassword(uid, pass).enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> c, Response<Void> r) {
                    dialog.dismiss();
                    Toast.makeText(AccountManagementActivity.this, "Đã đặt lại mật khẩu", Toast.LENGTH_SHORT).show();
                }
                @Override public void onFailure(Call<Void> c, Throwable t) { dialog.dismiss(); }
            });
        });
        dialog.show();
    }

    private void toggleAccountLock(Employee emp) {
        boolean isActive = emp.isAccountActive();
        String newStatus = isActive ? "INACTIVE" : "ACTIVE";
        Long uid = emp.getUserId() != null ? emp.getUserId() : emp.getId();
        apiService.updateAccountStatus(uid, newStatus).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) { loadEmployees(); }
            @Override public void onFailure(Call<Void> c, Throwable t) {}
        });
    }

    class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.VH> {
        private List<Employee> list;
        AccountAdapter(List<Employee> list) { this.list = list; }
        void updateData(List<Employee> nl) { this.list = nl; notifyDataSetChanged(); }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_employee_admin, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            Employee emp = list.get(pos);
            h.tvName.setText(emp.getFullName());
            h.tvAvatar.setText(emp.getAvatarText());
            h.tvRole.setText(emp.getRoleRaw());
            h.tvDept.setText(emp.getDepartmentName());
            boolean active = emp.isAccountActive();
            h.tvStatus.setText(active ? "Hoạt động" : "Bị khoá");
            h.btnToggleLock.setText(active ? "Khoá" : "Mở khoá");
            h.btnToggleLock.setOnClickListener(v -> toggleAccountLock(emp));
            h.btnResetPass.setOnClickListener(v -> showResetPasswordDialog(emp));
        }
        @Override public int getItemCount() { return list.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView tvAvatar, tvName, tvRole, tvDept, tvStatus;
            MaterialButton btnResetPass, btnChangeRole, btnToggleLock;
            VH(View v) {
                super(v);
                tvAvatar = v.findViewById(R.id.tvAvatar);
                tvName = v.findViewById(R.id.tvEmployeeName);
                tvRole = v.findViewById(R.id.tvEmployeeRole);
                tvDept = v.findViewById(R.id.tvEmployeeDept);
                tvStatus = v.findViewById(R.id.tvAccountStatus);
                btnResetPass = v.findViewById(R.id.btnResetPass);
                btnChangeRole = v.findViewById(R.id.btnChangeRole);
                btnToggleLock = v.findViewById(R.id.btnToggleLock);
            }
        }
    }
}
