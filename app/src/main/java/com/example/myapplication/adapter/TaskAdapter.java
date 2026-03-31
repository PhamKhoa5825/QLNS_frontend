package com.example.myapplication.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.myapplication.network.RetrofitClient;

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
        holder.tvDeadline.setText("Hạn: " + formatDateTime(task.getDeadline()));

        // Avatar logic
        String assigneeName = task.getAssignedToName();
        if (assigneeName == null || assigneeName.isEmpty()) {
            holder.tvAvatar.setText("?");
            holder.ivAvatar.setVisibility(View.GONE);
            holder.tvAvatar.setVisibility(View.VISIBLE);
        } else {
            String initial = assigneeName.substring(0, 1).toUpperCase();
            holder.tvAvatar.setText(initial);

            String avatarUrl = task.getAssignedToAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                String fullUrl = avatarUrl.startsWith("http") ? avatarUrl : RetrofitClient.BASE_URL + avatarUrl;
                Glide.with(context)
                        .load(fullUrl)
                        .circleCrop()
                        .into(holder.ivAvatar);
                holder.ivAvatar.setVisibility(View.VISIBLE);
                holder.tvAvatar.setVisibility(View.GONE);
            } else {
                holder.ivAvatar.setVisibility(View.GONE);
                holder.tvAvatar.setVisibility(View.VISIBLE);
            }
        }

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
        
        // Remove any old overlapping compound drawables
        holder.tvStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        holder.tvStatus.setBackgroundResource(R.drawable.bg_chip_surface);

        switch (status) {
            case "DONE":
            case "COMPLETED":
                holder.tvStatus.setText("HOÀN THÀNH");
                holder.tvStatus.setTextColor(Color.parseColor("#10B981"));
                break;
            case "UNDER_REVIEW":
                holder.tvStatus.setText("CHỜ DUYỆT");
                holder.tvStatus.setTextColor(Color.parseColor("#8B5CF6"));
                break;
            default:
                if (isOverdue(task.getDeadline())) {
                    holder.tvStatus.setText("QUÁ HẠN");
                    holder.tvStatus.setTextColor(Color.parseColor("#EF4444")); // Red color
                } else {
                    switch (status) {
                        case "ACCEPTED":
                        case "IN_PROGRESS":
                            holder.tvStatus.setText("ĐANG THỰC HIỆN");
                            holder.tvStatus.setTextColor(Color.parseColor("#3B82F6"));
                            break;
                        case "REJECTED":
                            holder.tvStatus.setText("CẦN LÀM LẠI");
                            holder.tvStatus.setTextColor(Color.parseColor("#F43F5E"));
                            break;
                        default:
                            holder.tvStatus.setText("CHƯA BẮT ĐẦU");
                            holder.tvStatus.setTextColor(Color.parseColor("#EA580C"));
                            break;
                    }
                }
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onStatusClick(task);
        });
    }

    private boolean isOverdue(String deadlineStr) {
        if (deadlineStr == null || deadlineStr.isEmpty()) return false;
        try {
            java.text.SimpleDateFormat sdf;
            if (deadlineStr.contains("T")) {
                sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);
            } else {
                sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            }
            java.util.Date deadline = sdf.parse(deadlineStr);
            return deadline != null && deadline.before(new java.util.Date());
        } catch (Exception e) {
            return false;
        }
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
        TextView tvTitle, tvPriority, tvDesc, tvAssignee, tvDeadline, tvStatus, tvAvatar;
        ImageView ivAvatar;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvPriority = itemView.findViewById(R.id.tvTaskPriority);
            tvDesc = itemView.findViewById(R.id.tvTaskDesc);
            tvAssignee = itemView.findViewById(R.id.tvTaskAssignee);
            tvDeadline = itemView.findViewById(R.id.tvTaskDeadline);
            tvStatus = itemView.findViewById(R.id.tvTaskStatus);
            tvAvatar = itemView.findViewById(R.id.tvTaskAvatar);
            ivAvatar = itemView.findViewById(R.id.ivTaskAvatar);
        }
    }
}
