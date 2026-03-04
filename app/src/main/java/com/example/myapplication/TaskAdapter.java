package com.example.myapplication;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    private List<Task> list;

    public TaskAdapter(List<Task> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = list.get(position);

        holder.tvTaskTitle.setText(task.getTitle());
        holder.tvTaskDesc.setText(task.getDescription());
        holder.tvTaskAssignee.setText(task.getAssignee());
        holder.tvTaskDeadline.setText(task.getDeadline());
        holder.tvTaskStatus.setText(task.getStatus());
        holder.tvTaskPriority.setText(task.getPriority());

        // Đổi màu Badge Mức độ ưu tiên
        if (task.getPriority().equals("Cao")) {
            holder.tvTaskPriority.setBackgroundResource(R.drawable.bg_badge_high);
            holder.tvTaskPriority.setTextColor(Color.parseColor("#EF4444")); // Đỏ
        } else if (task.getPriority().equals("Trung bình")) {
            holder.tvTaskPriority.setBackgroundResource(R.drawable.bg_badge_medium);
            holder.tvTaskPriority.setTextColor(Color.parseColor("#F97316")); // Cam
        }

        // Đổi màu Trạng thái (Chưa bắt đầu xám, Đang thực hiện cam)
        if (task.getStatus().equals("Chưa bắt đầu")) {
            holder.tvTaskStatus.setTextColor(Color.parseColor("#6B7280"));
        } else {
            holder.tvTaskStatus.setTextColor(Color.parseColor("#EA580C"));
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskTitle, tvTaskPriority, tvTaskDesc, tvTaskAssignee, tvTaskDeadline, tvTaskStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvTaskPriority = itemView.findViewById(R.id.tvTaskPriority);
            tvTaskDesc = itemView.findViewById(R.id.tvTaskDesc);
            tvTaskAssignee = itemView.findViewById(R.id.tvTaskAssignee);
            tvTaskDeadline = itemView.findViewById(R.id.tvTaskDeadline);
            tvTaskStatus = itemView.findViewById(R.id.tvTaskStatus);
        }
    }
}
