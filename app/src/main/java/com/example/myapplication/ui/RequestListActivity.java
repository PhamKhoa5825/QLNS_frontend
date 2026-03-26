package com.example.myapplication.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.RequestModels;
import com.example.myapplication.network.ApiErrorHelper;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
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

public class RequestListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TabLayout tabLayout;
    private TextView tvStatPending, tvStatApproved, tvStatRejected;

    private ApiService apiService;
    private SharedPreferences prefs;
    private String role;
    private Long employeeId;

    private RequestAdapter adapter;
    private List<RequestModels.RequestResponse> allRequests = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_list);

        prefs      = getSharedPreferences("qlns_pref", MODE_PRIVATE);
        apiService = RetrofitClient.getClient().create(ApiService.class);
        role       = prefs.getString("role", "EMPLOYEE");
        employeeId = prefs.getLong("employeeId", -1);

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
        adapter = new RequestAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        findViewById(R.id.fabCreateRequest).setOnClickListener(v ->
                startActivity(new Intent(this, LeaveApplicationActivity.class)));
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { filterByTab(tab.getPosition()); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void filterByTab(int pos) {
        List<RequestModels.RequestResponse> filtered;
        switch (pos) {
            case 1: filtered = allRequests.stream().filter(r -> "PENDING".equals(r.status)).collect(Collectors.toList()); break;
            case 2: filtered = allRequests.stream().filter(r -> "APPROVED".equals(r.status)).collect(Collectors.toList()); break;
            case 3: filtered = allRequests.stream().filter(r -> "REJECTED".equals(r.status)).collect(Collectors.toList()); break;
            default: filtered = new ArrayList<>(allRequests);
        }
        adapter.updateData(filtered);
    }

    private void loadRequests() {
        progressBar.setVisibility(View.VISIBLE);
        Call<List<RequestModels.RequestResponse>> call =
                "ADMIN".equals(role) ? apiService.getAllRequests() : apiService.getMyRequests(employeeId);

        call.enqueue(new Callback<List<RequestModels.RequestResponse>>() {
            @Override
            public void onResponse(Call<List<RequestModels.RequestResponse>> c,
                                   Response<List<RequestModels.RequestResponse>> r) {
                progressBar.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null) {
                    allRequests = r.body();
                    filterByTab(tabLayout.getSelectedTabPosition());
                    updateStats();
                } else {
                    Toast.makeText(RequestListActivity.this, "Lỗi: " + r.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<List<RequestModels.RequestResponse>> c, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(RequestListActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStats() {
        long pending  = allRequests.stream().filter(r -> "PENDING".equals(r.status)).count();
        long approved = allRequests.stream().filter(r -> "APPROVED".equals(r.status)).count();
        long rejected = allRequests.stream().filter(r -> "REJECTED".equals(r.status)).count();
        tvStatPending.setText(pending + " chờ duyệt");
        tvStatApproved.setText(approved + " đã duyệt");
        tvStatRejected.setText(rejected + " từ chối");
    }

    private void approveRequest(RequestModels.RequestResponse req) {
        new AlertDialog.Builder(this)
                .setTitle("Duyệt đơn")
                .setMessage("Duyệt đơn \"" + req.title + "\" của " + req.employeeName + "?")
                .setPositiveButton("Duyệt", (d, w) ->
                        apiService.reviewRequest(req.id, employeeId,
                                        new RequestModels.ReviewRequestBody(true, null))
                                .enqueue(new Callback<RequestModels.RequestResponse>() {
                                    @Override public void onResponse(Call<RequestModels.RequestResponse> c,
                                                                     Response<RequestModels.RequestResponse> r) {

                                        if (r.isSuccessful()) {
                                            Toast.makeText(RequestListActivity.this, "Đã duyệt", Toast.LENGTH_SHORT).show();
                                            loadRequests();
                                            // MỚI: Gợi ý tạo thông báo phản hồi
                                            showQuickNotifyDialog(req);
                                        } else {
                                            ApiErrorHelper.show(RequestListActivity.this, r, "Duyệt đơn thất bại");
                                        }
                                    }
                                    @Override public void onFailure(Call<RequestModels.RequestResponse> c, Throwable t) {
                                        Toast.makeText(RequestListActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                                    }
                                }))
                .setNegativeButton("Huỷ", null).show();
    }

    private void showQuickNotifyDialog(RequestModels.RequestResponse req) {
        String[] options = {
                "Gửi TB cho " + req.employeeName,
                "Gửi TB cho phòng " + (req.departmentName != null ? req.departmentName : ""),
                "Gửi TB toàn công ty",
                "Không cần"
        };

        new AlertDialog.Builder(this)
                .setTitle("Tạo thông báo phản hồi?")
                .setItems(options, (d, which) -> {
                    if (which == 3) return; // Không cần

                    Intent intent = new Intent(this, CreateNotificationActivity.class);
                    intent.putExtra("prefillTitle", "Phản hồi đơn: " + req.title);
                    intent.putExtra("prefillContent",
                            "Đơn \"" + req.title + "\" của " + req.employeeName + " đã được duyệt.");

                    if (which == 0) {
                        // Gửi cho cá nhân
                        intent.putExtra("targetType", "EMPLOYEE");
                        intent.putExtra("targetEmployeeId", req.employeeId);
                    } else if (which == 1) {
                        // Gửi cho phòng ban
                        intent.putExtra("targetType", "DEPARTMENT");
                    } else {
                        // Gửi toàn công ty
                        intent.putExtra("targetType", "COMPANY");
                    }

                    startActivity(intent);
                })
                .show();
    }

    private void showRejectDialog(RequestModels.RequestResponse req) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_review_request, null);
        TextInputEditText etReason = view.findViewById(R.id.etRejectionReason);
        MaterialButton btnCancel   = view.findViewById(R.id.btnCancelReject);
        MaterialButton btnConfirm  = view.findViewById(R.id.btnConfirmReject);

        AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String reason = etReason.getText() != null ? etReason.getText().toString().trim() : "";
            if (reason.isEmpty()) { etReason.setError("Vui lòng nhập lý do"); return; }
            apiService.reviewRequest(req.id, employeeId, new RequestModels.ReviewRequestBody(false, reason))
                    .enqueue(new Callback<RequestModels.RequestResponse>() {
                        @Override public void onResponse(Call<RequestModels.RequestResponse> c,
                                                         Response<RequestModels.RequestResponse> r) {
                            dialog.dismiss();
                            if (r.isSuccessful()) {
                                Toast.makeText(RequestListActivity.this, "Đã từ chối", Toast.LENGTH_SHORT).show();
                                loadRequests();
                            } else {
                                ApiErrorHelper.show(RequestListActivity.this, r, "Từ chối đơn thất bại");
                            }
                        }
                        @Override public void onFailure(Call<RequestModels.RequestResponse> c, Throwable t) {
                            dialog.dismiss();
                            Toast.makeText(RequestListActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        dialog.show();
    }

    @Override protected void onResume() { super.onResume(); loadRequests(); }

    // ══════════════════════════════════════════════════════════════
    class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.VH> {
        private List<RequestModels.RequestResponse> list;
        RequestAdapter(List<RequestModels.RequestResponse> list) { this.list = list; }
        void updateData(List<RequestModels.RequestResponse> newList) { this.list = newList; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_request, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            RequestModels.RequestResponse req = list.get(position);

            // Avatar chữ cái đầu
            if (req.employeeName != null && !req.employeeName.isEmpty()) {
                String trimmed = req.employeeName.trim();
                if (!trimmed.isEmpty()) {
                    String[] parts = trimmed.split(" ");
                    String lastWord = parts[parts.length - 1];
                    h.tvAvatar.setText(!lastWord.isEmpty()
                            ? String.valueOf(lastWord.charAt(0)).toUpperCase() : "?");
                } else {
                    h.tvAvatar.setText("?");
                }
            } else { h.tvAvatar.setText("?"); }

            h.tvEmployeeName.setText(req.employeeName != null ? req.employeeName : "");
            h.tvDept.setText(req.departmentName != null ? req.departmentName : "");
            h.tvTitle.setText(req.title != null ? req.title : "");
            h.tvDesc.setText(req.description != null ? req.description : "");
            h.tvDate.setText(formatDate(req.createdAt));

            // Status badge
            String status = req.status != null ? req.status : "";
            switch (status) {
                case "PENDING":  setBadge(h.tvStatus, "Chờ duyệt", "#F59E0B"); break;
                case "APPROVED": setBadge(h.tvStatus, "Đã duyệt", "#10B981"); break;
                case "REJECTED": setBadge(h.tvStatus, "Từ chối", "#EF4444"); break;
                default: h.tvStatus.setText(status);
            }

            // Lý do từ chối
            if ("REJECTED".equals(status) && req.rejectionReason != null) {
                h.tvRejectionReason.setVisibility(View.VISIBLE);
                h.tvRejectionReason.setText("Lý do: " + req.rejectionReason);
            } else { h.tvRejectionReason.setVisibility(View.GONE); }

            // Người duyệt — dùng reviewedByName (khớp backend)
            if (req.reviewedByName != null && !req.reviewedByName.isEmpty()) {
                h.tvReviewer.setVisibility(View.VISIBLE);
                h.tvReviewer.setText("Duyệt bởi: " + req.reviewedByName);
            } else { h.tvReviewer.setVisibility(View.GONE); }

            // Nút duyệt/từ chối (Admin/Manager + PENDING)
            if (("ADMIN".equals(role) || "MANAGER".equals(role)) && "PENDING".equals(status)) {
                h.layoutActions.setVisibility(View.VISIBLE);
                h.btnApprove.setOnClickListener(v -> approveRequest(req));
                h.btnReject.setOnClickListener(v -> showRejectDialog(req));
            } else { h.layoutActions.setVisibility(View.GONE); }
        }

        private void setBadge(TextView tv, String text, String color) {
            tv.setText(text);
            tv.setTextColor(Color.WHITE);
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setColor(Color.parseColor(color));
            gd.setCornerRadius(40f);
            tv.setPadding(24, 8, 24, 8);
            tv.setBackground(gd);
        }

        private String formatDate(String iso) {
            if (iso == null) return "";
            try {
                String datePart = iso.length() >= 10
                        ? iso.substring(8, 10) + "/" + iso.substring(5, 7) : iso;
                String timePart = iso.length() >= 16
                        ? " " + iso.substring(11, 16) : "";
                return datePart + timePart;
                }
            catch (Exception e) { return iso; }
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvAvatar, tvEmployeeName, tvDept, tvTitle, tvDesc, tvDate,
                    tvStatus, tvReviewer, tvRejectionReason;
            LinearLayout layoutActions;
            MaterialButton btnApprove, btnReject;
            VH(View v) {
                super(v);
                tvAvatar          = v.findViewById(R.id.tvRequestAvatar);
                tvEmployeeName    = v.findViewById(R.id.tvRequestEmployeeName);
                tvDept            = v.findViewById(R.id.tvRequestDept);
                tvTitle           = v.findViewById(R.id.tvRequestTitle);
                tvDesc            = v.findViewById(R.id.tvRequestDesc);
                tvDate            = v.findViewById(R.id.tvRequestDate);
                tvStatus          = v.findViewById(R.id.tvRequestStatus);
                tvReviewer        = v.findViewById(R.id.tvRequestReviewer);
                tvRejectionReason = v.findViewById(R.id.tvRejectionReason);
                layoutActions     = v.findViewById(R.id.layoutActions);
                btnApprove        = v.findViewById(R.id.btnApprove);
                btnReject         = v.findViewById(R.id.btnReject);
            }
        }
    }
}