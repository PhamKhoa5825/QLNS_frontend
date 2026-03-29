package com.example.myapplication.adapter;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.entity.Request;
import com.example.myapplication.ui.RequestDetailActivity;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * RequestAdapter — Hiển thị danh sách đơn từ.
 *
 * Tách ra khỏi RequestListActivity để đảm bảo Single Responsibility.
 * Cùng pattern với EmployeeAdapter, TaskAdapter, NotificationAdapter.
 */
public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.VH> {

    /**
     * Listener cho các hành động trên đơn từ.
     */
    public interface OnActionListener {
        void onApprove(Request request);
        void onReject(Request request);
        void onItemClick(Request request);
    }

    private List<Request> list;
    private final OnActionListener listener;
    private final boolean canReview; // Admin/Manager mới có quyền duyệt

    public RequestAdapter(List<Request> list,
                          OnActionListener listener, boolean canReview) {
        this.list = list;
        this.listener = listener;
        this.canReview = canReview;
    }

    public void updateData(List<Request> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_request, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Request req = list.get(position);

        // ── Avatar chữ cái đầu ──
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
        } else {
            h.tvAvatar.setText("?");
        }

        // ── Thông tin cơ bản ──
        h.tvEmployeeName.setText(req.employeeName != null ? req.employeeName : "");
        h.tvDept.setText(req.departmentName != null ? req.departmentName : "");
        h.tvTitle.setText(req.title != null ? req.title : "");
        h.tvDesc.setText(req.description != null ? req.description : "");
        h.tvDate.setText(formatDate(req.createdAt));

        // ── Status badge ──
        String status = req.status != null ? req.status : "";
        switch (status) {
            case "PENDING":  setBadge(h.tvStatus, "Chờ duyệt", "#F59E0B"); break;
            case "APPROVED": setBadge(h.tvStatus, "Đã duyệt", "#10B981"); break;
            case "REJECTED": setBadge(h.tvStatus, "Từ chối", "#EF4444"); break;
            default: h.tvStatus.setText(status);
        }

        // ── Lý do từ chối (chỉ hiện khi REJECTED) ──
        if ("REJECTED".equals(status) && req.rejectionReason != null && !req.rejectionReason.isEmpty()) {
            h.tvRejectionReason.setVisibility(View.VISIBLE);
            h.tvRejectionReason.setText("Lý do: " + req.rejectionReason);
        } else {
            h.tvRejectionReason.setVisibility(View.GONE);
        }

        // ── Người duyệt ──
        if (req.reviewedByName != null && !req.reviewedByName.isEmpty()) {
            h.tvReviewer.setVisibility(View.VISIBLE);
            h.tvReviewer.setText("Duyệt bởi: " + req.reviewedByName);
        } else {
            h.tvReviewer.setVisibility(View.GONE);
        }

        // ── Nút duyệt/từ chối (Admin/Manager + PENDING) ──
        if (canReview && "PENDING".equals(status)) {
            h.layoutActions.setVisibility(View.VISIBLE);
            h.btnApprove.setOnClickListener(v -> {
                if (listener != null) listener.onApprove(req);
            });
            h.btnReject.setOnClickListener(v -> {
                if (listener != null) listener.onReject(req);
            });
        } else {
            h.layoutActions.setVisibility(View.GONE);
        }

        // CLICK ITEM → MỞ TRANG CHI TIẾT
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(req);
            openRequestDetail(v, req);
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    // ── Helpers ───────────────────────────────────────────────

    private void setBadge(TextView tv, String text, String color) {
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        GradientDrawable gd = new GradientDrawable();
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
        } catch (Exception e) {
            return iso;
        }
    }

    // ── ViewHolder ────────────────────────────────────────────

    static class VH extends RecyclerView.ViewHolder {
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

    /**
     * MỚI: Mở trang chi tiết đơn từ
     */
    private void openRequestDetail(View v, Request req) {
        Intent intent = new Intent(v.getContext(), RequestDetailActivity.class);
        intent.putExtra("requestId", req.id);
        intent.putExtra("employeeName", req.employeeName);
        intent.putExtra("title", req.title);
        intent.putExtra("description", req.description);
        intent.putExtra("status", req.status);
        v.getContext().startActivity(intent);
    }
}