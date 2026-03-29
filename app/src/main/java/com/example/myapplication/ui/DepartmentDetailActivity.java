package com.example.myapplication.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.model.entity.Employee;
import com.example.myapplication.network.ApiErrorHelper;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * DepartmentDetailActivity — Trang chi tiết phòng ban
 *
 * Hiển thị:
 * - Tên, mô tả, trưởng phòng, số nhân viên
 * - Danh sách nhân viên trong phòng ban
 * - Click nhân viên → EmployeeDetailActivity
 *
 * Nhận extras:
 * - deptId, deptName, deptDescription, deptManagerName, deptEmployeeCount
 */
public class DepartmentDetailActivity extends AppCompatActivity {

    private ApiService apiService;
    private Long deptId;

    private RecyclerView rvEmployees;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private TextView tvEmpCount;
    private DeptEmployeeAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_department_detail);

        apiService = RetrofitClient.getClient().create(ApiService.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Nhận dữ liệu
        deptId = getIntent().getLongExtra("deptId", -1);
        String deptName = getIntent().getStringExtra("deptName");
        String deptDescription = getIntent().getStringExtra("deptDescription");
        String deptManagerName = getIntent().getStringExtra("deptManagerName");
        int deptEmployeeCount = getIntent().getIntExtra("deptEmployeeCount", 0);

        // Set toolbar title
        toolbar.setTitle(deptName != null ? deptName : "Chi tiết phòng ban");

        // Hiển thị info
        ((TextView) findViewById(R.id.tvDeptName)).setText(
                deptName != null ? deptName : "—");
        ((TextView) findViewById(R.id.tvDeptDescription)).setText(
                deptDescription != null && !deptDescription.isEmpty()
                        ? deptDescription : "Không có mô tả");
        ((TextView) findViewById(R.id.tvDeptManager)).setText(
                deptManagerName != null ? deptManagerName : "Chưa có trưởng phòng");

        tvEmpCount = findViewById(R.id.tvDeptEmpCount);
        tvEmpCount.setText(deptEmployeeCount + " nhân viên");

        // Avatar chữ cái
        TextView tvAvatar = findViewById(R.id.tvDeptAvatar);
        if (deptName != null && !deptName.isEmpty()) {
            tvAvatar.setText(String.valueOf(deptName.charAt(0)).toUpperCase());
        }

        // RecyclerView
        rvEmployees = findViewById(R.id.rvEmployees);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty     = findViewById(R.id.tvEmpty);

        rvEmployees.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeptEmployeeAdapter(new ArrayList<>());
        rvEmployees.setAdapter(adapter);

        if (deptId != -1) {
            loadEmployees();
        }
    }

    private void loadEmployees() {
        tvEmpty.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        apiService.getEmployeesByDept(deptId).enqueue(new Callback<List<Employee>>() {
            @Override
            public void onResponse(Call<List<Employee>> c, Response<List<Employee>> r) {
                progressBar.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null) {
                    List<Employee> employees = r.body();
                    if (employees.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        adapter.updateData(employees);
                        tvEmpCount.setText(employees.size() + " nhân viên");
                    }
                } else {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText(ApiErrorHelper.parse(r, "Lỗi tải nhân viên phòng ban"));
                }
            }

            @Override
            public void onFailure(Call<List<Employee>> c, Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("Lỗi kết nối");
            }
        });
    }

    // ══════════════════════════════════════════════════════════
    //  ADAPTER: Nhân viên trong phòng ban
    // ══════════════════════════════════════════════════════════

    class DeptEmployeeAdapter extends RecyclerView.Adapter<DeptEmployeeAdapter.VH> {
        private List<Employee> list;

        DeptEmployeeAdapter(List<Employee> list) {
            this.list = list;
        }

        void updateData(List<Employee> newList) {
            this.list = newList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_employee, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Employee emp = list.get(pos);

            h.tvName.setText(emp.getFullName());
            h.tvRole.setText(emp.getRole());
            h.tvDepartment.setText(emp.getDepartment());
            h.tvStatus.setText(emp.getStatus());

            if (emp.isWorking()) {
                h.tvStatus.setBackgroundResource(R.drawable.bg_status_working);
                h.tvStatus.setTextColor(Color.WHITE);
            } else {
                h.tvStatus.setBackgroundResource(R.drawable.bg_status_leave);
                h.tvStatus.setTextColor(Color.parseColor("#6B7280"));
            }

            // Avatar
            String avatarUrl = emp.getAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                h.ivAvatarImage.setVisibility(View.VISIBLE);
                h.tvAvatar.setVisibility(View.GONE);
                Glide.with(h.itemView.getContext())
                        .load(avatarUrl)
                        .circleCrop()
                        .placeholder(R.drawable.ic_user_placeholder)
                        .error(R.drawable.ic_user_placeholder)
                        .into(h.ivAvatarImage);
            } else {
                h.ivAvatarImage.setVisibility(View.GONE);
                h.tvAvatar.setVisibility(View.VISIBLE);
                h.tvAvatar.setText(emp.getAvatarText());
            }

            // Click → EmployeeDetailActivity
            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(DepartmentDetailActivity.this, EmployeeDetailActivity.class);
                intent.putExtra("employeeId", emp.getId());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvRole, tvDepartment, tvAvatar, tvStatus;
            ImageView ivAvatarImage;

            VH(View v) {
                super(v);
                tvName        = v.findViewById(R.id.tvName);
                tvRole        = v.findViewById(R.id.tvRole);
                tvDepartment  = v.findViewById(R.id.tvDepartment);
                tvAvatar      = v.findViewById(R.id.tvAvatar);
                tvStatus      = v.findViewById(R.id.tvStatus);
                ivAvatarImage = v.findViewById(R.id.ivAvatarImage);
            }
        }
    }
}