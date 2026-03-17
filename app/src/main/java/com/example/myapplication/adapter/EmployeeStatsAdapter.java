package com.example.myapplication.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.EmployeeAttendanceStats;

import java.util.List;

public class EmployeeStatsAdapter extends RecyclerView.Adapter<EmployeeStatsAdapter.StatsViewHolder> {

    // Avatar background colors palette
    private static final String[] AVATAR_COLORS = {
            "#EDE9FE", "#DBEAFE", "#D1FAE5", "#FEF3C7", "#FEE2E2", "#E0E7FF", "#FCE7F3"
    };
    private static final String[] AVATAR_TEXT_COLORS = {
            "#7C3AED", "#2563EB", "#059669", "#D97706", "#DC2626", "#4338CA", "#DB2777"
    };

    private Context context;
    private List<EmployeeAttendanceStats> statsList;

    public EmployeeStatsAdapter(Context context, List<EmployeeAttendanceStats> statsList) {
        this.context = context;
        this.statsList = statsList;
    }

    public void setStatsList(List<EmployeeAttendanceStats> statsList) {
        this.statsList = statsList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StatsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_item_employee_stats, parent, false);
        return new StatsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StatsViewHolder holder, int position) {
        EmployeeAttendanceStats stats = statsList.get(position);
        String name = stats.getEmployeeName() != null ? stats.getEmployeeName() : "Không tên";
        holder.tvEmpName.setText(name);

        // Avatar initials (first letter of each word, max 2)
        String initials = getInitials(name);
        holder.tvAvatar.setText(initials);
        int colorIndex = position % AVATAR_COLORS.length;
        holder.tvAvatar.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(Color.parseColor(AVATAR_COLORS[colorIndex])));
        holder.tvAvatar.setTextColor(Color.parseColor(AVATAR_TEXT_COLORS[colorIndex]));

        // Stats numbers
        holder.tvOnTimeCount.setText(String.valueOf(stats.getOnTime()));
        holder.tvLateCount.setText(String.valueOf(stats.getLate()));
        holder.tvAbsentCount.setText(String.valueOf(stats.getAbsent()));
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
        }
        return ("" + parts[0].charAt(0)).toUpperCase();
    }

    @Override
    public int getItemCount() {
        return statsList == null ? 0 : statsList.size();
    }

    public static class StatsViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvEmpName, tvOnTimeCount, tvLateCount, tvAbsentCount;

        public StatsViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvEmpName = itemView.findViewById(R.id.tvEmpName);
            tvOnTimeCount = itemView.findViewById(R.id.tvOnTimeCount);
            tvLateCount = itemView.findViewById(R.id.tvLateCount);
            tvAbsentCount = itemView.findViewById(R.id.tvAbsentCount);
        }
    }
}
