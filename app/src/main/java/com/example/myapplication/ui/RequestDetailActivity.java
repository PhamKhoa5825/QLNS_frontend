package com.example.myapplication.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.dto.RequestDto;
import com.example.myapplication.model.entity.Request;
import com.example.myapplication.network.ApiErrorHelper;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.SharedPrefsManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * RequestDetailActivity — Trang chi tiết đơn từ
 *
 * Nhận extras từ RequestListActivity:
 * - requestId, title, description, employeeName, departmentName,
 *   status, reviewedByName, rejectionReason, targetRole, createdAt, updatedAt
 *
 * Admin/Manager có thể duyệt/từ chối trực tiếp từ đây.
 */
public class RequestDetailActivity extends AppCompatActivity {

    private ApiService apiService;
    private SharedPrefsManager pm;
    private String role;
    private Long employeeId;
    private Long requestId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_request_detail);

        pm = SharedPrefsManager.getInstance(this);
        role = pm.getRole();
        employeeId = pm.getEmployeeId();
        apiService = RetrofitClient.getClient().create(ApiService.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Nhận dữ liệu từ intent
        requestId = getIntent().getLongExtra("requestId", -1);
        String title = getIntent().getStringExtra("title");
        String description = getIntent().getStringExtra("description");
        String employeeName = getIntent().getStringExtra("employeeName");
        String departmentName = getIntent().getStringExtra("departmentName");
        String status = getIntent().getStringExtra("status");
        String reviewedByName = getIntent().getStringExtra("reviewedByName");
        String rejectionReason = getIntent().getStringExtra("rejectionReason");
        String targetRole = getIntent().getStringExtra("targetRole");
        String createdAt = getIntent().getStringExtra("createdAt");
        String updatedAt = getIntent().getStringExtra("updatedAt");

        // Avatar
        TextView tvAvatar = findViewById(R.id.tvDetailAvatar);
        if (employeeName != null && !employeeName.trim().isEmpty()) {
            String[] parts = employeeName.trim().split(" ");
            String last = parts[parts.length - 1];
            tvAvatar.setText(!last.isEmpty() ? String.valueOf(last.charAt(0)).toUpperCase() : "?");
        } else {
            tvAvatar.setText("?");
        }

        // Hiển thị thông tin
        ((TextView) findViewById(R.id.tvDetailEmployeeName)).setText(
                employeeName != null ? employeeName : "—");
        ((TextView) findViewById(R.id.tvDetailDept)).setText(
                departmentName != null ? departmentName : "—");
        ((TextView) findViewById(R.id.tvDetailTitle)).setText(
                title != null ? title : "—");
        ((TextView) findViewById(R.id.tvDetailDescription)).setText(
                description != null && !description.isEmpty() ? description : "Không có mô tả");
        ((TextView) findViewById(R.id.tvDetailCreatedAt)).setText(
                formatDateTime(createdAt));

        // Target Role
        Chip chipTargetRole = findViewById(R.id.chipTargetRole);
        if ("ADMIN".equals(targetRole)) {
            chipTargetRole.setText("Gửi đến Admin");
            chipTargetRole.setChipBackgroundColorResource(android.R.color.holo_red_light);
        } else {
            chipTargetRole.setText("Gửi đến Quản lý");
        }

        // Status chip
        Chip chipStatus = findViewById(R.id.chipStatus);
        switch (status != null ? status : "") {
            case "APPROVED":
                chipStatus.setText("Đã duyệt");
                chipStatus.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(
                        Color.parseColor("#10B981")));
                chipStatus.setTextColor(Color.WHITE);
                break;
            case "REJECTED":
                chipStatus.setText("Từ chối");
                chipStatus.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(
                        Color.parseColor("#EF4444")));
                chipStatus.setTextColor(Color.WHITE);
                break;
            default:
                chipStatus.setText("Chờ duyệt");
                chipStatus.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(
                        Color.parseColor("#F59E0B")));
                chipStatus.setTextColor(Color.WHITE);
        }

        // Reviewer info
        LinearLayout layoutReviewer = findViewById(R.id.layoutReviewerInfo);
        TextView tvReviewer = findViewById(R.id.tvDetailReviewer);
        TextView tvUpdatedAt = findViewById(R.id.tvDetailUpdatedAt);

        if (!"PENDING".equals(status) && reviewedByName != null) {
            layoutReviewer.setVisibility(View.VISIBLE);
            tvReviewer.setText(reviewedByName);
            tvUpdatedAt.setText(formatDateTime(updatedAt));
        } else {
            layoutReviewer.setVisibility(View.GONE);
        }

        // Rejection reason
        LinearLayout layoutRejection = findViewById(R.id.layoutRejectionReason);
        TextView tvRejection = findViewById(R.id.tvDetailRejectionReason);
        if ("REJECTED".equals(status) && rejectionReason != null && !rejectionReason.isEmpty()) {
            layoutRejection.setVisibility(View.VISIBLE);
            tvRejection.setText(rejectionReason);
        } else {
            layoutRejection.setVisibility(View.GONE);
        }

        // Action buttons (Admin/Manager chỉ thấy khi đơn PENDING)
        LinearLayout layoutActions = findViewById(R.id.layoutActions);
        if ("PENDING".equals(status) && ("ADMIN".equals(role) || "MANAGER".equals(role))) {
            layoutActions.setVisibility(View.VISIBLE);

            MaterialButton btnApprove = findViewById(R.id.btnApprove);
            MaterialButton btnReject = findViewById(R.id.btnReject);

            btnApprove.setOnClickListener(v -> approveRequest());
            btnReject.setOnClickListener(v -> showRejectDialog());
        } else {
            layoutActions.setVisibility(View.GONE);
        }
    }

    private void approveRequest() {
        new AlertDialog.Builder(this)
                .setTitle("Duyệt đơn")
                .setMessage("Xác nhận duyệt đơn này?")
                .setPositiveButton("Duyệt", (d, w) -> {
                    apiService.reviewRequest(requestId, employeeId,
                            new RequestDto.ReviewRequestBody(true, null))
                            .enqueue(new Callback<Request>() {
                                @Override
                                public void onResponse(Call<Request> c, Response<Request> r) {
                                    if (r.isSuccessful()) {
                                        Toast.makeText(RequestDetailActivity.this,
                                                "Đã duyệt", Toast.LENGTH_SHORT).show();
                                        setResult(RESULT_OK);
                                        finish();
                                    } else {
                                        ApiErrorHelper.show(RequestDetailActivity.this, r, "Duyệt đơn thất bại");
                                    }
                                }
                                @Override
                                public void onFailure(Call<Request> c, Throwable t) {
                                    Toast.makeText(RequestDetailActivity.this,
                                            "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void showRejectDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_reject_request, null);
        TextInputEditText etReason = view.findViewById(R.id.etReason);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Từ chối đơn")
                .setView(view)
                .setPositiveButton("Từ chối", null)
                .setNegativeButton("Huỷ", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String reason = etReason.getText() != null ? etReason.getText().toString().trim() : "";
                if (reason.isEmpty()) {
                    etReason.setError("Vui lòng nhập lý do");
                    return;
                }
                dialog.dismiss();
                apiService.reviewRequest(requestId, employeeId,
                        new RequestDto.ReviewRequestBody(false, reason))
                        .enqueue(new Callback<Request>() {
                            @Override
                            public void onResponse(Call<Request> c, Response<Request> r) {
                                if (r.isSuccessful()) {
                                    Toast.makeText(RequestDetailActivity.this,
                                            "Đã từ chối", Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish();
                                } else {
                                    ApiErrorHelper.show(RequestDetailActivity.this, r, "Từ chối đơn thất bại");
                                }
                            }
                            @Override
                            public void onFailure(Call<Request> c, Throwable t) {
                                Toast.makeText(RequestDetailActivity.this,
                                        "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                            }
                        });
            });
        });

        dialog.show();
    }

    private String formatDateTime(String iso) {
        if (iso == null) return "—";
        try {
            String datePart = iso.length() >= 10
                    ? iso.substring(8, 10) + "/" + iso.substring(5, 7) + "/" + iso.substring(0, 4) : iso;
            String timePart = iso.length() >= 16 ? " " + iso.substring(11, 16) : "";
            return datePart + timePart;
        } catch (Exception e) {
            return iso;
        }
    }
}