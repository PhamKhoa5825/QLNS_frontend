package com.example.myapplication.adapter;

import android.content.Context;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {
    private List<Integer> days = new ArrayList<>();
    private Map<Integer, AttendanceRecord> attendanceMap = new HashMap<>();
    private Context context;

    public CalendarAdapter() {
        // Mặc định là tháng hiện tại
        Calendar calendar = Calendar.getInstance();
        updateDays(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR));
    }

    public void updateDays(int month, int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 1);
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        
        days.clear();
        for (int i = 1; i <= daysInMonth; i++) {
            days.add(i);
        }
        notifyDataSetChanged();
    }

    public void setAttendanceData(List<AttendanceRecord> records) {
        attendanceMap.clear();
        if (records != null) {
            for (AttendanceRecord record : records) {
                try {
                    // Định dạng yyyy-MM-dd
                    String[] parts = record.getDate().split("-");
                    int day = Integer.parseInt(parts[2]);
                    attendanceMap.put(day, record);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_calendar_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int day = days.get(position);
        holder.tvDayNumber.setText(String.valueOf(day));

        AttendanceRecord record = attendanceMap.get(day);
        if (record != null) {
            if ("ON_TIME".equals(record.getStatus())) {
                holder.dayContainer.setBackgroundResource(R.drawable.bg_legend_on_time);
                holder.tvDayNumber.setTextColor(Color.WHITE);
            } else if ("LATE".equals(record.getStatus())) {
                holder.dayContainer.setBackgroundResource(R.drawable.bg_legend_late);
                holder.tvDayNumber.setTextColor(Color.WHITE);
            } else {
                holder.dayContainer.setBackgroundResource(R.drawable.bg_calendar_day);
                holder.tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
            }
        } else {
            // Kiểm tra nếu là ngày trong quá khứ của tháng hiện tại thì có thể coi là vắng (tùy logic backend)
            holder.dayContainer.setBackgroundResource(R.drawable.bg_calendar_day);
            holder.tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        }
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayNumber;
        View dayContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayNumber = itemView.findViewById(R.id.tvDayNumber);
            dayContainer = itemView.findViewById(R.id.dayContainer);
        }
    }
}