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

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private Context context;
    private List<Task> taskList;
    private OnTaskStatusClickListener listener;

    public interface OnTaskStatusClickListener {
        void onStatusClick(Task task);
    }

    public TaskAdapter(Context context, List<Task> taskList, OnTaskStatusClickListener listener) {
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
        View view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.tvTitle.setText(task.getTitle());
        holder.tvDesc.setText(task.getDescription() != null ? task.getDescription() : "Không có mô tả");
        holder.tvAssignee.setText(task.getAssignedToName() != null ? task.getAssignedToName() : "Chưa bàn giao");
        holder.tvDeadline.setText("Hạn: " + (task.getDeadline() != null ? task.getDeadline() : "--/--/----"));

        // Priority binding
        String priority = task.getPriority() != null ? task.getPriority() : "LOW";
        switch (priority) {
            case "HIGH":
                holder.tvPriority.setText("Cao");
                holder.tvPriority.setTextColor(Color.parseColor("#EF4444"));
                holder.tvPriority.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_badge_high));
                break;
            case "MEDIUM":
                holder.tvPriority.setText("Trung bình");
                holder.tvPriority.setTextColor(Color.parseColor("#D97706"));
                holder.tvPriority.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_badge_medium));
                break;
            default:
                holder.tvPriority.setText("Thấp");
                holder.tvPriority.setTextColor(Color.parseColor("#10B981"));
                holder.tvPriority.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_badge_low));
                break;
        }

        // Status binding
        String status = task.getStatus() != null ? task.getStatus() : "PENDING";
        switch (status) {
            case "DONE":
            case "COMPLETED":
                holder.tvStatus.setText("Hoàn thành");
                holder.tvStatus.setTextColor(Color.parseColor("#10B981"));
                holder.tvStatus.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_dialog_info, 0, 0, 0);
                holder.tvStatus.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#10B981")));
                break;
            case "ACCEPTED":
            case "IN_PROGRESS":
                holder.tvStatus.setText("Đang thực hiện");
                holder.tvStatus.setTextColor(Color.parseColor("#3B82F6"));
                holder.tvStatus.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_dialog_info, 0, 0, 0);
                holder.tvStatus.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#3B82F6")));
                break;
            default:
                holder.tvStatus.setText("Chưa bắt đầu");
                holder.tvStatus.setTextColor(Color.parseColor("#EA580C"));
                holder.tvStatus.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_dialog_info, 0, 0, 0);
                holder.tvStatus.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#EA580C")));
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onStatusClick(task);
        });
    }

    @Override
    public int getItemCount() {
        return taskList == null ? 0 : taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvPriority, tvDesc, tvAssignee, tvDeadline, tvStatus;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvPriority = itemView.findViewById(R.id.tvTaskPriority);
            tvDesc = itemView.findViewById(R.id.tvTaskDesc);
            tvAssignee = itemView.findViewById(R.id.tvTaskAssignee);
            tvDeadline = itemView.findViewById(R.id.tvTaskDeadline);
            tvStatus = itemView.findViewById(R.id.tvTaskStatus);
        }
    }
}
