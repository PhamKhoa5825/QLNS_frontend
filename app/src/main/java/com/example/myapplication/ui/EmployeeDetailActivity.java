package com.example.myapplication.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.entity.Employee;
import com.example.myapplication.model.entity.Request;
import com.example.myapplication.network.ApiErrorHelper;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

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
    private View layoutInfo;
    private TextView tvName, tvEmail, tvPhone, tvDept, tvPosition, tvJoinDate, tvStatus;

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

        apiService = RetrofitClient.getClient().create(ApiService.class);

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

    // ── LOAD EMPLOYEE INFO ───────────────────────────────────────

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
                    tvStatus.setText(emp.getStatusRaw() != null ? emp.getStatusRaw() : "—");

                    MaterialToolbar toolbar = findViewById(R.id.toolbar);
                    toolbar.setTitle(emp.getFullName());
                } else {
                    Toast.makeText(EmployeeDetailActivity.this,
                            ApiErrorHelper.parse(r, "Lỗi tải thông tin nhân viên"),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Employee> c, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(EmployeeDetailActivity.this,
                        "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── LOAD ATTENDANCE ──────────────────────────────────────────

    private boolean attendanceLoaded = false;

    private void loadAttendance() {
        if (attendanceLoaded) return;
        attendanceLoaded = true;

        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);

        apiService.getAttendanceByMonth(employeeId, month, year)
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> c,
                                           Response<List<Map<String, Object>>> r) {
                        if (r.isSuccessful() && r.body() != null) {
                            List<Map<String, Object>> records = r.body();
                            if (records.isEmpty()) {
                                tvAttEmpty.setVisibility(View.VISIBLE);
                                return;
                            }
                            tvAttEmpty.setVisibility(View.GONE);
                            int onTimeCount = 0;
                            int lateCount = 0;
                            for (Map<String, Object> a : records) {
                                if ("ON_TIME".equals(a.get("status"))) onTimeCount++;
                                else if ("LATE".equals(a.get("status"))) lateCount++;
                            }
                            tvAttSummary.setText("Tháng " + month + ": "
                                    + records.size() + " ngày ("
                                    + onTimeCount + " đúng giờ, "
                                    + lateCount + " đi muộn)");
                            rvAttendance.setAdapter(new AttendanceAdapter(records));
                        } else {
                            tvAttEmpty.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> c, Throwable t) {
                        tvAttEmpty.setVisibility(View.VISIBLE);
                    }
                });
    }

    // ── LOAD REQUESTS ────────────────────────────────────────────

    private boolean requestsLoaded = false;

    private void loadRequests() {
        if (requestsLoaded) return;
        requestsLoaded = true;

        apiService.getMyRequests(employeeId).enqueue(new Callback<List<Request>>() {
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
        private final List<Map<String, Object>> items;
        AttendanceAdapter(List<Map<String, Object>> items) { this.items = items; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_attendance_simple, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Map<String, Object> att = items.get(pos);
            h.tvDate.setText(att.get("date") != null ? att.get("date").toString() : "—");

            String checkIn = att.get("checkIn") != null ? att.get("checkIn").toString() : "—";
            String checkOut = att.get("checkOut") != null ? att.get("checkOut").toString() : "Chưa";
            if (checkIn.contains("T")) checkIn = checkIn.substring(11, 16);
            if (checkOut.contains("T")) checkOut = checkOut.substring(11, 16);
            h.tvTime.setText(checkIn + " → " + checkOut);

            String status = att.get("status") != null ? att.get("status").toString() : "";
            h.tvStatus.setText("LATE".equals(status) ? "Đi muộn" : "Đúng giờ");
            h.tvStatus.setTextColor("LATE".equals(status) ? 0xFFEF4444 : 0xFF10B981);
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
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_request_simple, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Request req = items.get(pos);
            h.tvTitle.setText(req.title != null ? req.title : "—");
            h.tvDate.setText(req.createdAt != null ? req.createdAt.substring(0, 10) : "—");
            String st = req.status != null ? req.status : "";
            switch (st) {
                case "PENDING":
                    h.tvStatus.setText("Chờ duyệt");
                    h.tvStatus.setTextColor(0xFFF59E0B);
                    break;
                case "APPROVED":
                    h.tvStatus.setText("Đã duyệt");
                    h.tvStatus.setTextColor(0xFF10B981);
                    break;
                case "REJECTED":
                    h.tvStatus.setText("Từ chối");
                    h.tvStatus.setTextColor(0xFFEF4444);
                    break;
                default:
                    h.tvStatus.setText(st);
                    h.tvStatus.setTextColor(0xFF6B7280);
            }
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
