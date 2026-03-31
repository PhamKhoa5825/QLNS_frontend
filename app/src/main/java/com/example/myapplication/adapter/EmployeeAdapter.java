package com.example.myapplication.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.model.Employee;
 
import android.widget.ImageView;
import java.util.ArrayList;
import java.util.List;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.ViewHolder> {

    private List<Employee> employeeList = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Employee employee);
    }

    public EmployeeAdapter(List<Employee> employeeList, OnItemClickListener listener) {
        this.employeeList = employeeList;
        this.listener = listener;
    }

    public void setData(List<Employee> newList) {
        this.employeeList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_employee_demo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Employee emp = employeeList.get(position);

        holder.tvName.setText(emp.getFullName());
        holder.tvRole.setText(emp.getRole());
        holder.tvDepartment.setText(emp.getDepartment());
 
        String avatarUrl = emp.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            holder.ivAvatar.setVisibility(View.VISIBLE);
            holder.tvAvatar.setVisibility(View.GONE);
            Glide.with(holder.itemView.getContext())
                    .load(avatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_user_placeholder)
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setVisibility(View.GONE);
            holder.tvAvatar.setVisibility(View.VISIBLE);
            holder.tvAvatar.setText(emp.getAvatarText());
        }
 
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
    public int getItemCount() { return employeeList == null ? 0 : employeeList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvRole, tvDepartment, tvAvatar, tvStatus;
        ImageView ivAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName       = itemView.findViewById(R.id.tvName);
            tvRole       = itemView.findViewById(R.id.tvRole);
            tvDepartment = itemView.findViewById(R.id.tvDepartment);
            tvAvatar     = itemView.findViewById(R.id.tvAvatar);
            ivAvatar     = itemView.findViewById(R.id.ivAvatar);
            tvStatus     = itemView.findViewById(R.id.tvStatus);
        }
    }
}
