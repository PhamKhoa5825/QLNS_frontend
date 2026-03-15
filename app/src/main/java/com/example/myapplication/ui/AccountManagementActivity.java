package com.example.myapplication.ui;

import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import com.example.myapplication.R;
import com.example.myapplication.model.AccountModels;
import com.example.myapplication.model.Employee;
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

public class AccountManagementActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private ApiService apiService;
    private AccountAdapter adapter;
    private List<Employee> employeeList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_management);

        apiService = RetrofitClient.getClient().create(ApiService.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBar  = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.recyclerViewAccounts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AccountAdapter(employeeList, this::showAccountOptions);
        recyclerView.setAdapter(adapter);

        loadEmployees();
    }

    private void loadEmployees() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getEmployees().enqueue(new Callback<List<Employee>>() {
            @Override public void onResponse(Call<List<Employee>> c, Response<List<Employee>> r) {
                progressBar.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null) {
                    employeeList = r.body();
                    adapter.updateData(employeeList);
                }
            }
            @Override public void onFailure(Call<List<Employee>> c, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AccountManagementActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAccountOptions(Employee emp) {
        String[] options = {"Đặt lại mật khẩu", "Khoá tài khoản", "Mở khoá tài khoản", "Đổi Role"};
        new AlertDialog.Builder(this)
                .setTitle(emp.getFullName())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: showResetPasswordDialog(emp); break;
                        case 1: updateStatus(emp, "INACTIVE"); break;
                        case 2: updateStatus(emp, "ACTIVE"); break;
                        case 3: showChangeRoleDialog(emp); break;
                    }
                })
                .show();
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
            apiService.resetPassword(emp.getId(), newPass)
                    .enqueue(new Callback<Void>() {
                        @Override public void onResponse(Call<Void> c, Response<Void> r) {
                            dialog.dismiss();
                            String msg = r.isSuccessful() ? "Đã đặt lại mật khẩu" : "Lỗi: " + r.code();
                            Toast.makeText(AccountManagementActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                        @Override public void onFailure(Call<Void> c, Throwable t) {
                            dialog.dismiss();
                            Toast.makeText(AccountManagementActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        dialog.show();
    }

    private void updateStatus(Employee emp, String status) {
        String msg = "ACTIVE".equals(status) ? "Mở khoá" : "Khoá";
        new AlertDialog.Builder(this)
                .setMessage(msg + " tài khoản " + emp.getFullName() + "?")
                .setPositiveButton("Xác nhận", (d, w) ->
                        apiService.updateAccountStatus(emp.getId(), status)
                                .enqueue(new Callback<Void>() {
                                    @Override public void onResponse(Call<Void> c, Response<Void> r) {
                                        String result = r.isSuccessful() ? "Đã " + msg.toLowerCase() + " tài khoản"
                                                : "Lỗi: " + r.code();
                                        Toast.makeText(AccountManagementActivity.this, result, Toast.LENGTH_SHORT).show();
                                        if (r.isSuccessful()) loadEmployees();
                                    }
                                    @Override public void onFailure(Call<Void> c, Throwable t) {
                                        Toast.makeText(AccountManagementActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                                    }
                                }))
                .setNegativeButton("Huỷ", null)
                .show();
    }

    /**
     * ĐÃ SỬA: Gọi API updateEmployeeRole thay vì chỉ hiện Toast
     */
    private void showChangeRoleDialog(Employee emp) {
        String[] roles = {"EMPLOYEE", "MANAGER", "ADMIN"};
        // Tìm role hiện tại để pre-select
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

                    apiService.updateEmployeeRole(emp.getId(), new AccountModels.UpdateRoleRequest(newRole))
                            .enqueue(new Callback<Employee>() {
                                @Override
                                public void onResponse(Call<Employee> c, Response<Employee> r) {
                                    if (r.isSuccessful()) {
                                        Toast.makeText(AccountManagementActivity.this,
                                                "Đã đổi role → " + newRole, Toast.LENGTH_SHORT).show();
                                        loadEmployees();
                                    } else {
                                        Toast.makeText(AccountManagementActivity.this,
                                                "Lỗi đổi role: " + r.code(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                                @Override
                                public void onFailure(Call<Employee> c, Throwable t) {
                                    Toast.makeText(AccountManagementActivity.this,
                                            "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    // ── Adapter nội bộ ────────────────────────────────────────────
    static class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.VH> {

        interface OnClickListener { void onClick(Employee emp); }

        private List<Employee> list;
        private final OnClickListener listener;

        AccountAdapter(List<Employee> list, OnClickListener listener) {
            this.list = list; this.listener = listener;
        }

        void updateData(List<Employee> newList) {
            this.list = newList; notifyDataSetChanged();
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_employee_admin, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Employee emp = list.get(pos);
            h.tvName.setText(emp.getFullName());
            h.tvRole.setText(emp.getRoleRaw());
            h.tvDept.setText(emp.getDepartment());
            h.btnEdit.setOnClickListener(v -> listener.onClick(emp));
            h.btnDelete.setOnClickListener(v -> listener.onClick(emp));
        }

        @Override public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvRole, tvDept;
            MaterialButton btnEdit, btnDelete;
            VH(View v) {
                super(v);
                tvName    = v.findViewById(R.id.tvEmployeeName);
                tvRole    = v.findViewById(R.id.tvEmployeeRole);
                tvDept    = v.findViewById(R.id.tvEmployeeDept);
                btnEdit   = v.findViewById(R.id.btnEdit);
                btnDelete = v.findViewById(R.id.btnDelete);
            }
        }
    }
}