package com.example.myapplication.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Attendance;

import java.util.List;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder> {

    private Context context;
    private List<Attendance> attendanceList;

    public AttendanceAdapter(Context context, List<Attendance> attendanceList) {
        this.context = context;
        this.attendanceList = attendanceList;
    }

    public void setAttendanceList(List<Attendance> attendanceList) {
        this.attendanceList = attendanceList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AttendanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_item_attendance_history, parent, false);
        return new AttendanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendanceViewHolder holder, int position) {
        Attendance attendance = attendanceList.get(position);

        // Date
        String date = attendance.getDate() != null ? attendance.getDate() : "--/--/----";
        holder.tvDate.setText(date);

        // Check-in / Check-out times
        String inTime = attendance.getCheckInTime() != null ? attendance.getCheckInTime() : "--:--";
        String outTime = attendance.getCheckOutTime() != null ? attendance.getCheckOutTime() : "--:--";
        holder.tvCheckIn.setText("Vào: " + inTime);
        holder.tvCheckOut.setText("Ra: " + outTime);

        // Status badge & icon color
        String status = attendance.getStatus();
        if ("ON_TIME".equalsIgnoreCase(status)) {
            holder.tvStatusBadge.setText("Đúng giờ");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#10B981"));
            holder.tvStatusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#D1FAE5")));
            holder.iconContainer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#D1FAE5")));
            holder.ivStatusIcon.setColorFilter(Color.parseColor("#10B981"));
            holder.ivStatusIcon.setImageResource(android.R.drawable.checkbox_on_background);
        } else if ("LATE".equalsIgnoreCase(status)) {
            holder.tvStatusBadge.setText("Đi muộn");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#F59E0B"));
            holder.tvStatusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FEF3C7")));
            holder.iconContainer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FEF3C7")));
            holder.ivStatusIcon.setColorFilter(Color.parseColor("#F59E0B"));
            holder.ivStatusIcon.setImageResource(android.R.drawable.ic_dialog_alert);
        } else {
            holder.tvStatusBadge.setText("Vắng");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#EF4444"));
            holder.tvStatusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FEE2E2")));
            holder.iconContainer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FEE2E2")));
            holder.ivStatusIcon.setColorFilter(Color.parseColor("#EF4444"));
            holder.ivStatusIcon.setImageResource(android.R.drawable.ic_delete);
        }
    }

    @Override
    public int getItemCount() {
        return attendanceList == null ? 0 : attendanceList.size();
    }

    public static class AttendanceViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvCheckIn, tvCheckOut, tvStatusBadge;
        LinearLayout iconContainer;
        ImageView ivStatusIcon;

        public AttendanceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvCheckIn = itemView.findViewById(R.id.tvCheckIn);
            tvCheckOut = itemView.findViewById(R.id.tvCheckOut);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            iconContainer = itemView.findViewById(R.id.iconContainer);
            ivStatusIcon = itemView.findViewById(R.id.ivStatusIcon);
        }
    }
}
