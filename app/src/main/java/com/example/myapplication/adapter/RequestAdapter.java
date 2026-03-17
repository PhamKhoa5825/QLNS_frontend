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

        holder.tvEmployeeName.setText(request.getEmployeeName() != null ? request.getEmployeeName() : "Unknown");
        holder.tvRequestTitle.setText("Đơn xin " + (request.getType() != null ? request.getType() : "khác"));
        holder.tvRequestReason.setText("Lý do: " + request.getReason());
        holder.tvRequestDate.setText(request.getCreatedAt());
        
        // Avatar text
        if (request.getEmployeeName() != null && !request.getEmployeeName().isEmpty()) {
            String[] parts = request.getEmployeeName().trim().split(" ");
            holder.tvEmployeeAvatar.setText(String.valueOf(parts[parts.length - 1].charAt(0)).toUpperCase());
        }

        // Status
        String status = request.getStatus();
        String currentRole = com.example.myapplication.utils.SharedPrefsManager.getInstance(context).getRole();
        
        if ("PENDING".equalsIgnoreCase(status)) {
            holder.tvRequestStatus.setText("Chờ duyệt");
            holder.tvRequestStatus.setTextColor(android.graphics.Color.parseColor("#D97706")); // Orange
            
            // Only show action buttons for Manager/Admin
            if ("EMPLOYEE".equals(currentRole)) {
                holder.layoutActionButtons.setVisibility(View.GONE);
            } else {
                holder.layoutActionButtons.setVisibility(View.VISIBLE);
            }
        } else if ("APPROVED".equalsIgnoreCase(status)) {
            holder.tvRequestStatus.setText("Đã duyệt");
            holder.tvRequestStatus.setTextColor(android.graphics.Color.parseColor("#10B981")); // Green
            holder.layoutActionButtons.setVisibility(View.GONE);
        } else {
            holder.tvRequestStatus.setText("Từ chối");
            holder.tvRequestStatus.setTextColor(android.graphics.Color.parseColor("#EF4444")); // Red
            holder.layoutActionButtons.setVisibility(View.GONE);
        }

        // Action Buttons
        holder.btnApprove.setOnClickListener(v -> {
            if (listener != null) listener.onApprove(request);
        });
        
        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) listener.onReject(request);
        });
    }

    @Override
    public int getItemCount() {
        return requestList == null ? 0 : requestList.size();
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmployeeAvatar, tvEmployeeName, tvRequestDate, tvRequestStatus, tvRequestTitle, tvRequestReason;
        LinearLayout layoutActionButtons;
        Button btnApprove, btnReject;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmployeeAvatar = itemView.findViewById(R.id.tvEmployeeAvatar);
            tvEmployeeName = itemView.findViewById(R.id.tvEmployeeName);
            tvRequestDate = itemView.findViewById(R.id.tvRequestDate);
            tvRequestStatus = itemView.findViewById(R.id.tvRequestStatus);
            tvRequestTitle = itemView.findViewById(R.id.tvRequestTitle);
            tvRequestReason = itemView.findViewById(R.id.tvRequestReason);
            layoutActionButtons = itemView.findViewById(R.id.layoutActionButtons);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
