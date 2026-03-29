package com.example.myapplication.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.model.entity.Task;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.VH> {

    public interface ActionListener {
        void onAction(Task task, String action);
    }

    private List<Task> list;
    private final ActionListener listener;

    public TaskAdapter(List<Task> list, ActionListener listener) {
        this.list     = list;
        this.listener = listener;
    }

    public void updateData(List<Task> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Task task = list.get(position);

        h.tvTitle.setText(task.title);
        h.tvDesc.setText(task.description != null ? task.description : "");
        h.tvAssignee.setText(task.assignedToName != null ? "Người thực hiện: " + task.assignedToName : "");
        h.tvDeadline.setText("Hạn: " + (task.deadline != null ? task.deadline : "--"));

        switch (task.priority != null ? task.priority : "") {
            case "HIGH":
                h.tvPriority.setText("Cao");
                h.tvPriority.setTextColor(Color.parseColor("#EF4444"));
                break;
            case "MEDIUM":
                h.tvPriority.setText("TB");
                h.tvPriority.setTextColor(Color.parseColor("#F59E0B"));
                break;
            default:
                h.tvPriority.setText("Thấp");
                h.tvPriority.setTextColor(Color.parseColor("#10B981"));
        }

        switch (task.status != null ? task.status : "") {
            case "PENDING":
                h.tvStatus.setText("Chưa nhận");
                h.tvStatus.setTextColor(Color.parseColor("#6B7280"));
                h.btnAction.setVisibility(View.VISIBLE);
                h.btnAction.setText("Nhận việc");
                h.btnAction.setOnClickListener(v -> listener.onAction(task, "ACCEPT"));
                break;
            case "ACCEPTED":
                h.tvStatus.setText("Đang làm");
                h.tvStatus.setTextColor(Color.parseColor("#3B82F6"));
                h.btnAction.setVisibility(View.VISIBLE);
                h.btnAction.setText("Hoàn thành");
                h.btnAction.setOnClickListener(v -> listener.onAction(task, "DONE"));
                break;
            case "DONE":
                h.tvStatus.setText("Hoàn thành ✓");
                h.tvStatus.setTextColor(Color.parseColor("#10B981"));
                h.btnAction.setVisibility(View.GONE);
                break;
            case "OVERDUE":
                h.tvStatus.setText("Quá hạn !");
                h.tvStatus.setTextColor(Color.parseColor("#EF4444"));
                h.btnAction.setVisibility(View.GONE);
                break;
            default:
                h.tvStatus.setText(task.status);
                h.btnAction.setVisibility(View.GONE);
        }
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc, tvAssignee, tvDeadline, tvStatus, tvPriority;
        com.google.android.material.button.MaterialButton btnAction;
        VH(View v) {
            super(v);
            tvTitle    = v.findViewById(R.id.tvTaskTitle);
            tvDesc     = v.findViewById(R.id.tvTaskDesc);
            tvAssignee = v.findViewById(R.id.tvTaskAssignee);
            tvDeadline = v.findViewById(R.id.tvTaskDeadline);
            tvStatus   = v.findViewById(R.id.tvTaskStatus);
            tvPriority = v.findViewById(R.id.tvTaskPriority);
            btnAction  = v.findViewById(R.id.btnTaskAction);
        }
    }
}