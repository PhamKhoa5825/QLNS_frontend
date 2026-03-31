package com.example.myapplication.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Department;

import java.util.*;

public class DepartmentAdapter extends RecyclerView.Adapter<DepartmentAdapter.ViewHolder> {

    private static final int[] COLORS = {
            Color.parseColor("#2563EB"),
            Color.parseColor("#EC4899"),
            Color.parseColor("#10B981"),
            Color.parseColor("#A855F7"),
            Color.parseColor("#F59E0B"),
            Color.parseColor("#EF4444"),
    };

    private List<Department> departmentList = new ArrayList<>();
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Department dept);
    }

    public DepartmentAdapter(List<Department> departmentList, OnItemClickListener listener) {
        this.departmentList = departmentList;
        this.listener = listener;
    }

    public void setData(List<Department> newList) {
        this.departmentList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_department_demo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Department dept = departmentList.get(position);

        if (holder.tvDeptName != null) holder.tvDeptName.setText(dept.getName());
        if (holder.tvManagerName != null) holder.tvManagerName.setText("Trưởng phòng: " + dept.getManagerName());
        if (holder.tvEmpCount != null) holder.tvEmpCount.setText(dept.getEmployeeCount() + " người");
        if (holder.tvPerformanceStr != null) holder.tvPerformanceStr.setText("--");
        if (holder.progressBar != null) holder.progressBar.setProgress(0);

        int color = COLORS[position % COLORS.length];
        if (holder.cardDeptIcon != null)
            holder.cardDeptIcon.setCardBackgroundColor(ColorStateList.valueOf(color));
        if (holder.imgDeptIcon != null)
            holder.imgDeptIcon.setImageTintList(ColorStateList.valueOf(Color.WHITE));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(dept);
        });
    }

    @Override
    public int getItemCount() { return departmentList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeptName, tvManagerName, tvEmpCount, tvPerformanceStr;
        ImageView imgDeptIcon;
        ProgressBar progressBar;
        com.google.android.material.card.MaterialCardView cardDeptIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeptName       = itemView.findViewById(R.id.tvDeptName);
            tvManagerName    = itemView.findViewById(R.id.tvManagerName);
            tvEmpCount       = itemView.findViewById(R.id.tvEmpCount);
            tvPerformanceStr = itemView.findViewById(R.id.tvPerformanceStr);
            imgDeptIcon      = itemView.findViewById(R.id.imgDeptIcon);
            progressBar      = itemView.findViewById(R.id.progressBar);
            cardDeptIcon     = itemView.findViewById(R.id.cardDeptIcon);
        }
    }
}
