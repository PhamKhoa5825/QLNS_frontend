package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.RequestAdapter;
import com.example.myapplication.model.dto.RequestDto;
import com.example.myapplication.model.entity.Request;
import com.example.myapplication.network.ApiErrorHelper;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.SharedPrefsManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RequestListActivity extends AppCompatActivity
        implements RequestAdapter.OnActionListener {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TabLayout tabLayout;
    private TextView tvStatPending, tvStatApproved, tvStatRejected;

    private ApiService apiService;
    private SharedPrefsManager pm;
    private String role;
    private Long employeeId;

    private RequestAdapter adapter;
    private List<Request> allRequests = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_list);

        pm = SharedPrefsManager.getInstance(this);
        role = pm.getRole();
        employeeId = pm.getEmployeeId();
        apiService = RetrofitClient.getClient().create(ApiService.class);

        bindViews();
        setupTabs();

        loadRequests();
    }

    private void bindViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBar    = findViewById(R.id.progressBar);
        recyclerView   = findViewById(R.id.recyclerViewRequests);
        tabLayout      = findViewById(R.id.tabLayoutRequest);
        tvStatPending  = findViewById(R.id.tvStatPending);
        tvStatApproved = findViewById(R.id.tvStatApproved);
        tvStatRejected = findViewById(R.id.tvStatRejected);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RequestAdapter(new ArrayList<>(), this, pm.isAdminOrManager());
        recyclerView.setAdapter(adapter);

        findViewById(R.id.fabCreateRequest).setOnClickListener(v ->
                startActivity(new Intent(this, CreateRequestActivity.class)));
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                filterByTab(tab.getPosition());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    // ── DATA ─────────────────────────────────────────────────────

    private void loadRequests() {
        progressBar.setVisibility(View.VISIBLE);

        Callback<List<Request>> callback = new Callback<List<Request>>() {
            @Override
            public void onResponse(Call<List<Request>> c, Response<List<Request>> r) {
                progressBar.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null) {
                    allRequests = r.body();
                    filterByTab(tabLayout.getSelectedTabPosition());
                    updateStats();
                } else {
                    Toast.makeText(RequestListActivity.this,
                            ApiErrorHelper.parse(r, "Lỗi tải đơn từ"),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Request>> c, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(RequestListActivity.this, "Lỗi kết nối", Toast.LENGTH_LONG).show();
            }
        };

        if ("ADMIN".equals(role)) {
            apiService.getAllRequests().enqueue(callback);
        } else {
            apiService.getMyRequests(employeeId).enqueue(callback);
        }
    }

    private void filterByTab(int tabPos) {
        List<Request> filtered;
        switch (tabPos) {
            case 1: filtered = allRequests.stream()
                    .filter(r -> "PENDING".equals(r.status)).collect(Collectors.toList()); break;
            case 2: filtered = allRequests.stream()
                    .filter(r -> "APPROVED".equals(r.status)).collect(Collectors.toList()); break;
            case 3: filtered = allRequests.stream()
                    .filter(r -> "REJECTED".equals(r.status)).collect(Collectors.toList()); break;
            default: filtered = new ArrayList<>(allRequests);
        }
        adapter.updateData(filtered);
    }

    private void updateStats() {
        long pending  = allRequests.stream().filter(r -> "PENDING".equals(r.status)).count();
        long approved = allRequests.stream().filter(r -> "APPROVED".equals(r.status)).count();
        long rejected = allRequests.stream().filter(r -> "REJECTED".equals(r.status)).count();
        tvStatPending.setText(pending + " chờ duyệt");
        tvStatApproved.setText(approved + " đã duyệt");
        tvStatRejected.setText(rejected + " từ chối");
    }

    // ══════════════════════════════════════════════════════════
    //  ADAPTER CALLBACKS (implements RequestAdapter.OnActionListener)
    // ══════════════════════════════════════════════════════════

    @Override
    public void onApprove(Request req) {
        new AlertDialog.Builder(this)
                .setTitle("Duyệt đơn")
                .setMessage("Duyệt đơn \"" + req.title + "\" của " + req.employeeName + "?")
                .setPositiveButton("Duyệt", (d, w) -> {
                    apiService.reviewRequest(req.id, employeeId,
                            new RequestDto.ReviewRequestBody(true, null))
                            .enqueue(new Callback<Request>() {
                                @Override
                                public void onResponse(Call<Request> c, Response<Request> r) {
                                    if (r.isSuccessful()) {
                                        Toast.makeText(RequestListActivity.this, "Đã duyệt", Toast.LENGTH_SHORT).show();
                                        loadRequests();
                                        showQuickNotifyDialog(req);
                                    } else {
                                        ApiErrorHelper.show(RequestListActivity.this, r, "Duyệt đơn thất bại");
                                    }
                                }
                                @Override
                                public void onFailure(Call<Request> c, Throwable t) {
                                    Toast.makeText(RequestListActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Huỷ", null).show();
    }

    @Override
    public void onReject(Request req) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_review_request, null);
        TextInputEditText etReason = view.findViewById(R.id.etRejectionReason);
        MaterialButton btnCancel   = view.findViewById(R.id.btnCancelReject);
        MaterialButton btnConfirm  = view.findViewById(R.id.btnConfirmReject);

        AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String reason = etReason.getText() != null ? etReason.getText().toString().trim() : "";
            if (reason.isEmpty()) { etReason.setError("Vui lòng nhập lý do"); return; }
            dialog.dismiss();
            apiService.reviewRequest(req.id, employeeId,
                    new RequestDto.ReviewRequestBody(false, reason))
                    .enqueue(new Callback<Request>() {
                        @Override
                        public void onResponse(Call<Request> c, Response<Request> r) {
                            if (r.isSuccessful()) {
                                Toast.makeText(RequestListActivity.this, "Đã từ chối", Toast.LENGTH_SHORT).show();
                                loadRequests();
                            } else {
                                ApiErrorHelper.show(RequestListActivity.this, r, "Từ chối đơn thất bại");
                            }
                        }
                        @Override
                        public void onFailure(Call<Request> c, Throwable t) {
                            Toast.makeText(RequestListActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        dialog.show();
    }

    @Override
    public void onItemClick(Request req) {
        // TODO: Mở trang chi tiết đơn từ (nếu có)
    }

    // ══════════════════════════════════════════════════════════
    //  QUICK NOTIFY — Gợi ý tạo TB phản hồi sau duyệt đơn
    // ══════════════════════════════════════════════════════════

    private void showQuickNotifyDialog(Request req) {
        String[] options = {
                "Gửi TB cho " + (req.employeeName != null ? req.employeeName : "người nộp"),
                "Gửi TB cho phòng " + (req.departmentName != null ? req.departmentName : "ban"),
                "Gửi TB toàn công ty",
                "Không cần"
        };

        new AlertDialog.Builder(this)
                .setTitle("Tạo thông báo phản hồi?")
                .setItems(options, (d, which) -> {
                    if (which == 3) return;

                    Intent intent = new Intent(this, CreateNotificationActivity.class);
                    intent.putExtra("prefillTitle", "Phản hồi đơn: " + req.title);
                    intent.putExtra("prefillContent",
                            "Đơn \"" + req.title + "\" của " + req.employeeName + " đã được duyệt.");

                    if (which == 0) {
                        intent.putExtra("targetType", "EMPLOYEE");
                        intent.putExtra("targetEmployeeId", req.employeeId);
                    } else if (which == 1) {
                        intent.putExtra("targetType", "DEPARTMENT");
                    } else {
                        intent.putExtra("targetType", "COMPANY");
                    }
                    startActivity(intent);
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRequests();
    }
}