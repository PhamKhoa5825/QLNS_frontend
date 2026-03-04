package com.example.myapplication;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.ViewHolder> {

    private List<Employee> employeeList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Employee employee);
    }

    public EmployeeAdapter(List<Employee> employeeList, OnItemClickListener listener) {
        this.employeeList = employeeList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employee_demo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Employee emp = employeeList.get(position);
        holder.tvName.setText(emp.getName());
        holder.tvRole.setText(emp.getRole());
        holder.tvDepartment.setText(emp.getDepartment());
        holder.tvAvatar.setText(emp.getAvatarText());
        holder.tvStatus.setText(emp.getStatus());

        if (emp.isWorking()) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_working);
            holder.tvStatus.setTextColor(Color.WHITE);
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_leave);
            holder.tvStatus.setTextColor(Color.parseColor("#6B7280"));
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(emp));
    }

    @Override
    public int getItemCount() {
        return employeeList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvRole, tvDepartment, tvAvatar, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvRole = itemView.findViewById(R.id.tvRole);
            tvDepartment = itemView.findViewById(R.id.tvDepartment);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
