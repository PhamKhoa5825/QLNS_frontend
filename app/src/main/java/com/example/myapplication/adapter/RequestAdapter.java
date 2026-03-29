package com.example.myapplication.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Request;

import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    private Context context;
    private List<Request> requestList;
    private OnRequestActionClickListener listener;

    public interface OnRequestActionClickListener {
        void onApprove(Request request);
        void onReject(Request request);
        void onCancel(Request request);
    }

    public RequestAdapter(Context context, List<Request> requestList, OnRequestActionClickListener listener) {
        this.context = context;
        this.requestList = requestList;
        this.listener = listener;
    }

    public void setRequestList(List<Request> requestList) {
        this.requestList = requestList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        Request request = requestList.get(position);

        // ── Avatar chữ cái đầu ──
        if (request.getEmployeeName() != null && !request.getEmployeeName().isEmpty()) {
            String trimmed = request.getEmployeeName().trim();
            if (!trimmed.isEmpty()) {
                String[] parts = trimmed.split(" ");
                String lastWord = parts[parts.length - 1];
                holder.tvRequestAvatar.setText(!lastWord.isEmpty()
                        ? String.valueOf(lastWord.charAt(0)).toUpperCase() : "?");
            } else {
                holder.tvRequestAvatar.setText("?");
            }
        } else {
            holder.tvRequestAvatar.setText("?");
        }

        // ── Thông tin cơ bản ──
        holder.tvRequestEmployeeName.setText(request.getEmployeeName() != null ? request.getEmployeeName() : "Unknown");
        holder.tvRequestDept.setText(request.getDepartmentName() != null ? request.getDepartmentName() : "Phòng ban: N/A");
        
        String typeStr = "KHÁC";
        if (request.getType() != null) {
            switch (request.getType()) {
                case LEAVE_ANNUAL:
                case LEAVE_UNPAID:
                    typeStr = "NGHỈ PHÉP"; break;
                case SICK_LEAVE: typeStr = "NGHỈ ỐM"; break;
                case OVERTIME: typeStr = "LÀM THÊM GIỜ"; break;
                case BUSINESS_TRIP: typeStr = "CÔNG TÁC"; break;
            }
        }
        if (request.getType() == com.example.myapplication.model.RequestType.SICK_LEAVE) {
            holder.tvRequestTitle.setText(typeStr + " (BHXH chi trả)");
        } else {
            holder.tvRequestTitle.setText(typeStr);
        }

        // Build a summary of dates
        StringBuilder summary = new StringBuilder();
        if (request.getTitle() != null && !request.getTitle().isEmpty()) {
            summary.append(request.getTitle()).append("\n");
        }
        if (request.getDetails() != null && !request.getDetails().isEmpty()) {
            double totalDays = 0;
            if (request.getType() == com.example.myapplication.model.RequestType.LEAVE_ANNUAL
                    || request.getType() == com.example.myapplication.model.RequestType.LEAVE_UNPAID
                    || request.getType() == com.example.myapplication.model.RequestType.SICK_LEAVE) {
                for (com.example.myapplication.model.RequestDetail d : request.getDetails()) {
                    if (d.getLeaveSession() == com.example.myapplication.model.LeaveSession.ALL_DAY) totalDays += 1.0;
                    else totalDays += 0.5;
                }
            } else {
                totalDays = request.getDetails().size();
            }
            summary.append("Chi tiết: ").append(totalDays).append(" ngày (");
            for (int i = 0; i < Math.min(request.getDetails().size(), 3); i++) {
                com.example.myapplication.model.RequestDetail d = request.getDetails().get(i);
                summary.append(d.getSpecificDate());
                if ((request.getType() == com.example.myapplication.model.RequestType.LEAVE_ANNUAL
                        || request.getType() == com.example.myapplication.model.RequestType.LEAVE_UNPAID
                        || request.getType() == com.example.myapplication.model.RequestType.SICK_LEAVE) && d.getLeaveSession() != null) {
                    summary.append(" ").append(formatSession(d.getLeaveSession()));
                } else if (request.getType() == com.example.myapplication.model.RequestType.OVERTIME && d.getOvertimeHours() != null) {
                    summary.append(" ").append(d.getOvertimeHours()).append("h");
                }
                if (i < Math.min(request.getDetails().size(), 3) - 1) summary.append(", ");
            }
            if (request.getDetails().size() > 3) {
                summary.append("...");
            }
            summary.append(")");
        } else if (request.getDescription() != null) {
            summary.append(request.getDescription());
        }

        holder.tvRequestDesc.setText(summary.toString().trim());

        // Date Display: Created At
        if (request.getCreatedAt() != null) {
            String iso = request.getCreatedAt();
            try {
                String datePart = iso.length() >= 10
                        ? iso.substring(8, 10) + "/" + iso.substring(5, 7) + "/" + iso.substring(0, 4) : iso;
                String timePart = iso.length() >= 16
                        ? " " + iso.substring(11, 16) : "";
                holder.tvRequestDate.setText(datePart + timePart);
            } catch (Exception e) {
                holder.tvRequestDate.setText(iso);
            }
        }

        // Status Styling using setBadge
        String status = request.getStatus();
        String currentRole = com.example.myapplication.utils.SharedPrefsManager.getInstance(context).getRole();
        long currentEmployeeId = com.example.myapplication.utils.SharedPrefsManager.getInstance(context).getEmployeeId();
        boolean isOwnRequest = (request.getEmployeeId() != null && request.getEmployeeId() == currentEmployeeId);

        if ("PENDING".equalsIgnoreCase(status)) {
            setBadge(holder.tvRequestStatus, "CHỜ DUYỆT", "#F59E0B");
            if ("EMPLOYEE".equalsIgnoreCase(currentRole) || isOwnRequest) {
                holder.layoutActions.setVisibility(View.GONE);
                holder.btnCancelRequest.setVisibility(isOwnRequest ? View.VISIBLE : View.GONE);
            } else {
                holder.layoutActions.setVisibility(View.VISIBLE);
                holder.btnCancelRequest.setVisibility(View.GONE);
            }
        } else if ("APPROVED".equalsIgnoreCase(status)) {
            setBadge(holder.tvRequestStatus, "ĐÃ DUYỆT", "#10B981");
            holder.layoutActions.setVisibility(View.GONE);
            holder.btnCancelRequest.setVisibility(View.GONE);
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            setBadge(holder.tvRequestStatus, "TỪ CHỐI", "#EF4444");
            holder.layoutActions.setVisibility(View.GONE);
            holder.btnCancelRequest.setVisibility(View.GONE);
            if (request.getRejectionReason() != null && !request.getRejectionReason().isEmpty()) {
                holder.tvRejectionReason.setVisibility(View.VISIBLE);
                holder.tvRejectionReason.setText("Lý do: " + request.getRejectionReason());
            } else {
                holder.tvRejectionReason.setVisibility(View.GONE);
            }
        } else {
            setBadge(holder.tvRequestStatus, status != null ? status.toUpperCase() : "UNK", "#6B7280");
            holder.layoutActions.setVisibility(View.GONE);
            holder.btnCancelRequest.setVisibility(View.GONE);
        }

        // Reviewer info
        if (request.getReviewedByName() != null && !request.getReviewedByName().isEmpty()) {
            holder.tvRequestReviewer.setVisibility(View.VISIBLE);
            holder.tvRequestReviewer.setText("• " + request.getReviewedByName());
        } else {
            holder.tvRequestReviewer.setVisibility(View.GONE);
        }

        // Action Buttons
        holder.btnApprove.setOnClickListener(v -> {
            if (listener != null) listener.onApprove(request);
        });

        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) listener.onReject(request);
        });

        holder.btnCancelRequest.setOnClickListener(v -> {
            if (listener != null) listener.onCancel(request);
        });
    }

    @Override
    public int getItemCount() {
        return requestList == null ? 0 : requestList.size();
    }

    private String formatSession(com.example.myapplication.model.LeaveSession session) {
        if (session == null) return "";
        switch (session) {
            case MORNING: return "Sáng";
            case AFTERNOON: return "Chiều";
            case ALL_DAY: return "Cả ngày";
            default: return "";
        }
    }

    private void setBadge(TextView tv, String text, String color) {
        tv.setText(text);
        tv.setTextColor(android.graphics.Color.WHITE);
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(android.graphics.Color.parseColor(color));
        gd.setCornerRadius(40f);
        tv.setBackground(gd);
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView tvRequestAvatar, tvRequestEmployeeName, tvRequestDept, tvRequestTitle, tvRequestDesc, tvRequestDate, tvRequestStatus, tvRequestReviewer, tvRejectionReason;
        LinearLayout layoutActions;
        Button btnApprove, btnReject, btnCancelRequest;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRequestAvatar = itemView.findViewById(R.id.tvRequestAvatar);
            tvRequestEmployeeName = itemView.findViewById(R.id.tvRequestEmployeeName);
            tvRequestDept = itemView.findViewById(R.id.tvRequestDept);
            tvRequestTitle = itemView.findViewById(R.id.tvRequestTitle);
            tvRequestDesc = itemView.findViewById(R.id.tvRequestDesc);
            tvRequestDate = itemView.findViewById(R.id.tvRequestDate);
            tvRequestStatus = itemView.findViewById(R.id.tvRequestStatus);
            tvRequestReviewer = itemView.findViewById(R.id.tvRequestReviewer);
            tvRejectionReason = itemView.findViewById(R.id.tvRejectionReason);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnCancelRequest = itemView.findViewById(R.id.btnCancelRequest);
        }
    }
}
