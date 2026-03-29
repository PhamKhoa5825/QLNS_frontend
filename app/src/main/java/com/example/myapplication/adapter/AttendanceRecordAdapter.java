package com.example.myapplication.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.AttendanceRecord;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class AttendanceRecordAdapter extends RecyclerView.Adapter<AttendanceRecordAdapter.ViewHolder> {
    private List<AttendanceRecord> records = new ArrayList<>();
    private Context context;

    public void setRecords(List<AttendanceRecord> records) {
        this.records = records;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_attendance_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendanceRecord record = records.get(position);
        
        // Date handling
        String[] dateParts = record.getDate().split("-"); // yyyy-MM-dd
        holder.tvDay.setText(dateParts[2]);
        holder.tvMonth.setText("Thg " + dateParts[1]);
        holder.tvWeekday.setText(record.getDate()); // Simplify for now

        holder.tvCheckIn.setText(record.getCheckIn().substring(11, 16));
        holder.tvCheckOut.setText(record.getCheckOut() != null ? record.getCheckOut().substring(11, 16) : "--:--");
        holder.tvTotalHours.setText(record.getWorkHours() + " giờ");

        if (record.getLateMinutes() > 0) {
            holder.tvLateBadge.setVisibility(View.VISIBLE);
            holder.tvLateBadge.setText("+" + record.getLateMinutes() + "'");
        } else {
            holder.tvLateBadge.setVisibility(View.GONE);
        }

        // Status Badge
        if ("ON_TIME".equals(record.getStatus())) {
            holder.statusChip.setText("Đúng giờ");
            holder.statusChip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.success_bg)));
            holder.statusChip.setTextColor(ContextCompat.getColor(context, R.color.success));
            holder.statusChip.setChipIconTint(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.success)));
            holder.statusBar.setBackgroundColor(ContextCompat.getColor(context, R.color.success));
        } else if ("LATE".equals(record.getStatus())) {
            holder.statusChip.setText("Đi trễ");
            holder.statusChip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.danger_bg)));
            holder.statusChip.setTextColor(ContextCompat.getColor(context, R.color.danger));
            holder.statusChip.setChipIconTint(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.danger)));
            holder.statusBar.setBackgroundColor(ContextCompat.getColor(context, R.color.danger));
        }
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay, tvMonth, tvWeekday, tvCheckIn, tvCheckOut, tvTotalHours, tvLateBadge;
        Chip statusChip;
        View statusBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tvDay);
            tvMonth = itemView.findViewById(R.id.tvMonth);
            tvWeekday = itemView.findViewById(R.id.tvWeekday);
            tvCheckIn = itemView.findViewById(R.id.tvCheckIn);
            tvCheckOut = itemView.findViewById(R.id.tvCheckOut);
            tvTotalHours = itemView.findViewById(R.id.tvTotalHours);
            tvLateBadge = itemView.findViewById(R.id.tvLateBadge);
            statusChip = itemView.findViewById(R.id.statusChip);
            statusBar = itemView.findViewById(R.id.statusBar);
        }
    }
}