package com.example.myapplication.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        View view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new AttendanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendanceViewHolder holder, int position) {
        Attendance attendance = attendanceList.get(position);
        holder.tvName.setText(attendance.getEmployeeName() != null ? attendance.getEmployeeName() : "No Name");
        
        String in = attendance.getCheckInTime() != null ? attendance.getCheckInTime() : "--:--";
        String out = attendance.getCheckOutTime() != null ? attendance.getCheckOutTime() : "--:--";
        String status = attendance.getStatus();
        
        holder.tvDetails.setText("In: " + in + " | Out: " + out + " | " + status);
        
        if ("PRESENT".equalsIgnoreCase(status)) {
            holder.tvDetails.setTextColor(Color.parseColor("#10B981")); // Green
        } else if ("LATE".equalsIgnoreCase(status)) {
            holder.tvDetails.setTextColor(Color.parseColor("#EF4444")); // Red
        } else {
            holder.tvDetails.setTextColor(Color.parseColor("#6B7280")); // Gray
        }
    }

    @Override
    public int getItemCount() {
        return attendanceList == null ? 0 : attendanceList.size();
    }

    public static class AttendanceViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDetails;

        public AttendanceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(android.R.id.text1);
            tvDetails = itemView.findViewById(android.R.id.text2);
            tvName.setTextSize(16f);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            tvDetails.setTextSize(14f);
        }
    }
}
