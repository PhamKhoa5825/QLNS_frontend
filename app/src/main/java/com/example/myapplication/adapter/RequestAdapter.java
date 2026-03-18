package com.example.myapplication.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Request;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    public interface OnRequestActionListener {
        void onEditRequest(Request request);

        void onDeleteRequest(Request request);
    }

    private final Context context;
    private final OnRequestActionListener actionListener;
    private List<Request> requestList = new ArrayList<>();

    public RequestAdapter(Context context, OnRequestActionListener actionListener) {
        this.context = context;
        this.actionListener = actionListener;
    }

    public void submitList(List<Request> items) {
        requestList = items == null ? new ArrayList<>() : new ArrayList<>(items);
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

        holder.tvTitle.setText(request.getTitle());
        holder.tvDescription.setText(request.getDescription());
        holder.tvDate.setText(formatDate(request.getCreatedAt()));
        holder.tvEmployeeCode.setText(resolveEmployeeCode(request));

        String statusRaw = request.getStatus();
        bindStatus(holder.chipStatus, statusRaw);

        boolean canEdit = canEditOrDelete(statusRaw);
        holder.btnEdit.setEnabled(canEdit);
        holder.btnDelete.setEnabled(canEdit);
        holder.btnEdit.setAlpha(canEdit ? 1f : 0.5f);
        holder.btnDelete.setAlpha(canEdit ? 1f : 0.5f);

        holder.btnEdit.setOnClickListener(v -> {
            if (canEdit && actionListener != null) {
                actionListener.onEditRequest(request);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (canEdit && actionListener != null) {
                actionListener.onDeleteRequest(request);
            }
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    private String resolveEmployeeCode(Request request) {
        if (request.getEmployeeCode() != null && !request.getEmployeeCode().trim().isEmpty()) {
            return request.getEmployeeCode();
        }

        Long employeeId = request.getEmployeeId();
        if (employeeId == null || employeeId <= 0) {
            return "--";
        }

        return String.format(Locale.getDefault(), "ID: %03d", employeeId);
    }

    private boolean canEditOrDelete(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("PENDING") || normalized.equals("CHO_DUYET") || normalized.equals("CHODUYET");
    }

    private void bindStatus(Chip chip, String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);

        if (normalized.equals("APPROVED") || normalized.equals("DA_DUYET") || normalized.equals("DADUYET")) {
            chip.setText("Đã duyệt");
            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#DCFCE7")));
            chip.setTextColor(Color.parseColor("#15803D"));
        } else if (normalized.equals("REJECTED") || normalized.equals("TU_CHOI") || normalized.equals("TUCHOI")) {
            chip.setText("Từ chối");
            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#FEE2E2")));
            chip.setTextColor(Color.parseColor("#B91C1C"));
        } else {
            chip.setText("Chờ duyệt");
            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#FEF3C7")));
            chip.setTextColor(Color.parseColor("#B45309"));
        }
    }

    private String formatDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return "--/--/----";
        }

        // API may return ISO datetime or already formatted text; keep a safe fallback.
        Date parsed = parseIsoDate(rawDate);
        if (parsed != null) {
            return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(parsed);
        }

        if (rawDate.length() >= 10) {
            try {
                String firstTenChars = rawDate.substring(0, 10);
                Date yyyyMmDd = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(firstTenChars);
                if (yyyyMmDd != null) {
                    return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(yyyyMmDd);
                }
            } catch (ParseException ignored) {
                // Fall through.
            }
        }

        return rawDate;
    }

    private Date parseIsoDate(String rawDate) {
        try {
            SimpleDateFormat isoWithZone = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US);
            isoWithZone.setTimeZone(TimeZone.getTimeZone("UTC"));
            return isoWithZone.parse(rawDate);
        } catch (ParseException ignored) {
            // Try a second common shape without milliseconds.
        }

        try {
            SimpleDateFormat isoNoMs = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US);
            isoNoMs.setTimeZone(TimeZone.getTimeZone("UTC"));
            return isoNoMs.parse(rawDate);
        } catch (ParseException ignored) {
            return null;
        }
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvDescription;
        TextView tvDate;
        TextView tvEmployeeCode;
        Chip chipStatus;
        MaterialButton btnEdit;
        MaterialButton btnDelete;

        RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvRequestTitle);
            tvDescription = itemView.findViewById(R.id.tvRequestDescription);
            tvDate = itemView.findViewById(R.id.tvRequestDate);
            tvEmployeeCode = itemView.findViewById(R.id.tvRequestEmployeeCode);
            chipStatus = itemView.findViewById(R.id.chipRequestStatus);
            btnEdit = itemView.findViewById(R.id.btnEditRequest);
            btnDelete = itemView.findViewById(R.id.btnDeleteRequest);
        }
    }
}

