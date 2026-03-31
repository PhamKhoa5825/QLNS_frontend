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
import com.example.myapplication.model.Task;

import java.util.List;

public class TaskEmployeeAdapter extends RecyclerView.Adapter<TaskEmployeeAdapter.TaskViewHolder> {

    private Context context;
    private List<Task> taskList;
    private OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public TaskEmployeeAdapter(Context context, List<Task> taskList, OnTaskClickListener listener) {
        this.context = context;
        this.taskList = taskList;
        this.listener = listener;
    }

    public void setTaskList(List<Task> taskList) {
        this.taskList = taskList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_task_employee, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.tvTitle.setText(task.getTitle());
        holder.tvDateAssigned.setText("Ngày giao: " + formatDateTime(task.getCreatedAt()));
        holder.tvDeadline.setText("Hạn chót: " + formatDateTime(task.getDeadline()));

        // Priority binding
        String priority = task.getPriority() != null ? task.getPriority() : "LOW";
        switch (priority) {
            case "HIGH":
                holder.tvPriority.setText("⚠ Gấp");
                holder.tvPriority.setTextColor(Color.parseColor("#EF4444"));
                holder.tvPriority.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FEE2E2")));
                break;
            case "MEDIUM":
                holder.tvPriority.setText("Trung bình");
                holder.tvPriority.setTextColor(Color.parseColor("#D97706"));
                holder.tvPriority.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FEF3C7")));
                break;
            default:
                holder.tvPriority.setText("Thấp");
                holder.tvPriority.setTextColor(Color.parseColor("#10B981"));
                holder.tvPriority.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#D1FAE5")));
                break;
        }

        // Deadline color
        if ("PENDING".equals(task.getStatus()) || "ACCEPTED".equals(task.getStatus())) {
            holder.tvDeadline.setTextColor(Color.parseColor("#1976D2"));
            holder.ivDeadlineIcon.setImageTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1976D2")));
        } else {
            holder.tvDeadline.setTextColor(Color.parseColor("#888888"));
            holder.ivDeadlineIcon.setImageTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#888888")));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTaskClick(task);
        });
    }

    private String formatDateTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "--/--/----";
        try {
            if (dateStr.contains("T")) {
                String[] parts = dateStr.split("T");
                String[] dateParts = parts[0].split("-");
                String[] timeParts = parts[1].split(":");
                String formattedDate = dateParts[2] + "/" + dateParts[1] + "/" + dateParts[0];
                String formattedTime = timeParts[0] + ":" + timeParts[1];
                return formattedTime + " - " + formattedDate;
            } else if (dateStr.contains("-")) {
                String[] dateParts = dateStr.split("-");
                if(dateParts.length >= 3) {
                   return dateParts[2] + "/" + dateParts[1] + "/" + dateParts[0];
                }
            }
        } catch (Exception e) {}
        return dateStr;
    }

    @Override
    public int getItemCount() {
        return taskList == null ? 0 : taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvPriority, tvDateAssigned, tvDeadline;
        android.widget.ImageView ivDeadlineIcon;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvPriority = itemView.findViewById(R.id.tvPriorityTag);
            tvDateAssigned = itemView.findViewById(R.id.tvDateAssigned);
            tvDeadline = itemView.findViewById(R.id.tvDeadline);
            ivDeadlineIcon = itemView.findViewById(R.id.ivDeadlineIcon);
        }
    }
}
