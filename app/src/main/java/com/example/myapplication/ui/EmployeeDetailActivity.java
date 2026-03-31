package com.example.myapplication.ui;

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

import com.example.myapplication.R;
import com.example.myapplication.model.Attendance;
import com.example.myapplication.model.Employee;
import com.example.myapplication.model.Request;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.SharedPrefsManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import androidx.appcompat.app.AlertDialog;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * EmployeeDetailActivity — Chi tiết NV với cross-reference
 */
public class EmployeeDetailActivity extends AppCompatActivity {

    private ApiService apiService;
    private Long employeeId;

    // Tab 1: Thông tin
    private View layoutInfo, layoutAdminActions;
    private TextView tvName, tvEmail, tvPhone, tvDept, tvPosition, tvJoinDate, tvStatus;
    private TextView tvDetailHeaderName, tvDetailHeaderRole, tvAvatarFallback;
    private ImageView ivAvatar;
    private View btnReactivate;

    // Tab 2: Chấm công
    private View layoutAttendance;
    private RecyclerView rvAttendance;
    private TextView tvAttSummary, tvAttEmpty;

    // Tab 3: Đơn từ
    private View layoutRequests;
    private RecyclerView rvRequests;
    private TextView tvReqEmpty;

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_employee_detail);

        employeeId = getIntent().getLongExtra("employeeId", -1);
        if (employeeId == -1) { finish(); return; }

        apiService = RetrofitClient.getApiService(this);

        bindViews();
        setupTabs();
        loadEmployeeInfo();
    }

    private void bindViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        progressBar = findViewById(R.id.progressBar);

        // Tab 1
        layoutInfo = findViewById(R.id.layoutInfo);
        tvName = findViewById(R.id.tvDetailName);
        tvEmail = findViewById(R.id.tvDetailEmail);
        tvPhone = findViewById(R.id.tvDetailPhone);
        tvDept = findViewById(R.id.tvDetailDept);
        tvPosition = findViewById(R.id.tvDetailPosition);
        tvJoinDate = findViewById(R.id.tvDetailJoinDate);
        tvStatus = findViewById(R.id.tvDetailStatus);
        
        tvDetailHeaderName = findViewById(R.id.tvDetailHeaderName);
        tvDetailHeaderRole = findViewById(R.id.tvDetailHeaderRole);
        tvAvatarFallback = findViewById(R.id.tvDetailAvatarFallback);
        ivAvatar = findViewById(R.id.ivDetailAvatar);
        
        layoutAdminActions = findViewById(R.id.layoutAdminActions);
        btnReactivate = findViewById(R.id.btnReactivate);

        btnReactivate.setOnClickListener(v -> performReactivate());

        // Tab 2
        layoutAttendance = findViewById(R.id.layoutAttendance);
        rvAttendance = findViewById(R.id.rvAttendance);
        tvAttSummary = findViewById(R.id.tvAttSummary);
        tvAttEmpty = findViewById(R.id.tvAttEmpty);
        rvAttendance.setLayoutManager(new LinearLayoutManager(this));

        // Tab 3
        layoutRequests = findViewById(R.id.layoutRequests);
        rvRequests = findViewById(R.id.rvRequests);
        tvReqEmpty = findViewById(R.id.tvReqEmpty);
        rvRequests.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupTabs() {
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().setText("Thông tin"));
        tabLayout.addTab(tabLayout.newTab().setText("Chấm công"));
        tabLayout.addTab(tabLayout.newTab().setText("Đơn từ"));

        showTab(0);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                showTab(pos);
                if (pos == 1) loadAttendance();
                if (pos == 2) loadRequests();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void showTab(int index) {
        layoutInfo.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        layoutAttendance.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        layoutRequests.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
    }

    private void loadEmployeeInfo() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getEmployeeById(employeeId).enqueue(new Callback<Employee>() {
            @Override
            public void onResponse(Call<Employee> c, Response<Employee> r) {
                progressBar.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null) {
                    Employee emp = r.body();
                    tvName.setText(emp.getFullName() != null ? emp.getFullName() : "—");
                    tvEmail.setText(emp.getEmail() != null ? emp.getEmail() : "—");
                    tvPhone.setText(emp.getPhone() != null ? emp.getPhone() : "—");
                    tvDept.setText(emp.getDepartmentName() != null ? emp.getDepartmentName() : "Chưa phân công");
                    tvPosition.setText(emp.getPosition() != null ? emp.getPosition() : "—");
                    tvJoinDate.setText(emp.getJoinDate() != null ? emp.getJoinDate() : "—");
                    tvStatus.setText(emp.getStatus() != null ? emp.getStatus() : "—");
 
                    // Update Header
                    if (tvDetailHeaderName != null) tvDetailHeaderName.setText(emp.getFullName());
                    if (tvDetailHeaderRole != null) tvDetailHeaderRole.setText(emp.getPosition());
 
                    String avatarUrl = emp.getAvatarUrl();
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        ivAvatar.setVisibility(View.VISIBLE);
                        tvAvatarFallback.setVisibility(View.GONE);
                        com.bumptech.glide.Glide.with(EmployeeDetailActivity.this)
                                .load(avatarUrl)
                                .circleCrop()
                                .into(ivAvatar);
                    } else {
                        ivAvatar.setVisibility(View.GONE);
                        tvAvatarFallback.setVisibility(View.VISIBLE);
                        tvAvatarFallback.setText(emp.getAvatarText());
                    }

                    updateAdminActions(emp);

                    MaterialToolbar toolbar = findViewById(R.id.toolbar);
                    toolbar.setTitle(emp.getFullName());
                } else {
                    Toast.makeText(EmployeeDetailActivity.this, "Lỗi tải thông tin", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Employee> c, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(EmployeeDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean attendanceLoaded = false;
    private void updateAdminActions(Employee emp) {
        String myRole = SharedPrefsManager.getInstance(this).getRole();
        if (!"ADMIN".equals(myRole)) {
            layoutAdminActions.setVisibility(View.GONE);
            return;
        }

        layoutAdminActions.setVisibility(View.VISIBLE);
        String status = emp.getStatusRaw(); 

        // Nút "Đi làm lại" chỉ hiện khi nhân viên đã nghỉ việc
        if ("RESIGNED".equalsIgnoreCase(status)) {
            btnReactivate.setVisibility(View.VISIBLE);
        } else {
            btnReactivate.setVisibility(View.GONE);
        }
    }

    private void performReactivate() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.reactivateEmployee(employeeId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> c, Response<Void> r) {
                progressBar.setVisibility(View.GONE);
                if (r.isSuccessful()) {
                    Toast.makeText(EmployeeDetailActivity.this, "Đã kích hoạt lại nhân viên", Toast.LENGTH_SHORT).show();
                    loadEmployeeInfo();
                } else {
                    Toast.makeText(EmployeeDetailActivity.this, "Thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> c, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(EmployeeDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAttendance() {
        if (attendanceLoaded) return;
        attendanceLoaded = true;

        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);

        apiService.getAttendanceByMonth(employeeId, month, year)
                .enqueue(new Callback<List<Attendance>>() {
                    @Override
                    public void onResponse(Call<List<Attendance>> c, Response<List<Attendance>> r) {
                        if (r.isSuccessful() && r.body() != null) {
                            List<Attendance> records = r.body();
                            if (records.isEmpty()) {
                                tvAttEmpty.setVisibility(View.VISIBLE);
                                return;
                            }
                            tvAttEmpty.setVisibility(View.GONE);
                            long onTimeCount = records.stream().filter(a -> "ON_TIME".equals(a.getStatus())).count();
                            long lateCount = records.stream().filter(a -> "LATE".equals(a.getStatus())).count();
                            
                            tvAttSummary.setText("Tháng " + month + ": " + records.size() + " ngày (" + onTimeCount + " đúng giờ, " + lateCount + " muộn)");
                            rvAttendance.setAdapter(new AttendanceAdapter(records));
                        } else {
                            tvAttEmpty.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Attendance>> c, Throwable t) {
                        tvAttEmpty.setVisibility(View.VISIBLE);
                    }
                });
    }

    private boolean requestsLoaded = false;
    private void loadRequests() {
        if (requestsLoaded) return;
        requestsLoaded = true;

        apiService.getMyRequests(employeeId, null, null).enqueue(new Callback<List<Request>>() {
            @Override
            public void onResponse(Call<List<Request>> c, Response<List<Request>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    List<Request> reqs = r.body();
                    if (reqs.isEmpty()) {
                        tvReqEmpty.setVisibility(View.VISIBLE);
                        return;
                    }
                    tvReqEmpty.setVisibility(View.GONE);
                    rvRequests.setAdapter(new SimpleRequestAdapter(reqs));
                } else {
                    tvReqEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<Request>> c, Throwable t) {
                tvReqEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    // ── INNER ADAPTERS ───────────────────────────────────────────

    class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.VH> {
        private final List<Attendance> items;
        AttendanceAdapter(List<Attendance> items) { this.items = items; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendance_simple, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Attendance att = items.get(pos);
            h.tvDate.setText(att.getDate() != null ? att.getDate() : "—");
            h.tvTime.setText((att.getCheckIn() != null ? att.getCheckIn().substring(0, 5) : "—") + " → " + (att.getCheckOut() != null ? att.getCheckOut().substring(0, 5) : "Chưa"));
            h.tvStatus.setText("LATE".equals(att.getStatus()) ? "Đi muộn" : "Đúng giờ");
            h.tvStatus.setTextColor("LATE".equals(att.getStatus()) ? 0xFFEF4444 : 0xFF10B981);
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvDate, tvTime, tvStatus;
            VH(View v) {
                super(v);
                tvDate = v.findViewById(R.id.tvAttDate);
                tvTime = v.findViewById(R.id.tvAttTime);
                tvStatus = v.findViewById(R.id.tvAttStatus);
            }
        }
    }

    class SimpleRequestAdapter extends RecyclerView.Adapter<SimpleRequestAdapter.VH> {
        private final List<Request> items;
        SimpleRequestAdapter(List<Request> items) { this.items = items; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_request_simple, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Request req = items.get(pos);
            h.tvTitle.setText(req.getTitle() != null ? req.getTitle() : "—");
            h.tvDate.setText(req.getCreatedAt() != null ? req.getCreatedAt().substring(0, 10) : "—");
            String st = req.getStatus() != null ? req.getStatus() : "";
            h.tvStatus.setText(st);
            if ("PENDING".equals(st)) h.tvStatus.setTextColor(0xFFF59E0B);
            else if ("APPROVED".equals(st)) h.tvStatus.setTextColor(0xFF10B981);
            else if ("REJECTED".equals(st)) h.tvStatus.setTextColor(0xFFEF4444);
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDate, tvStatus;
            VH(View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tvReqTitle);
                tvDate = v.findViewById(R.id.tvReqDate);
                tvStatus = v.findViewById(R.id.tvReqStatus);
            }
        }
    }
}
