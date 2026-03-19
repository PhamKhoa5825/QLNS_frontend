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

    private List<Department> list = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Department department);
    }

    public DepartmentAdapter(List<Department> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    public void setData(List<Department> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_department, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Department dept = list.get(position);

        holder.tvDeptName.setText(dept.getName());
        holder.tvManagerName.setText("Trưởng phòng: " + dept.getManagerName());
        holder.tvEmpCount.setText(String.valueOf(dept.getEmployeeCount()));

        if (holder.tvPerformanceStr != null) holder.tvPerformanceStr.setText("--");
        if (holder.progressBar != null) holder.progressBar.setProgress(0);

        int color = COLORS[position % COLORS.length];
        if (holder.imgDeptIcon != null)
            holder.imgDeptIcon.setBackgroundTintList(ColorStateList.valueOf(color));

        if (listener != null)
            holder.itemView.setOnClickListener(v -> listener.onItemClick(dept));
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeptName, tvManagerName, tvEmpCount, tvPerformanceStr;
        ImageView imgDeptIcon;
        ProgressBar progressBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeptName       = itemView.findViewById(R.id.tvDeptName);
            tvManagerName    = itemView.findViewById(R.id.tvManagerName);
            tvEmpCount       = itemView.findViewById(R.id.tvEmpCount);
            tvPerformanceStr = itemView.findViewById(R.id.tvPerformanceStr);
            imgDeptIcon      = itemView.findViewById(R.id.imgDeptIcon);
            progressBar      = itemView.findViewById(R.id.progressBar);
        }
    }
}
