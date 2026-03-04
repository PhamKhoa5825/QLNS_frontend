package com.example.myapplication;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DepartmentAdapter extends RecyclerView.Adapter<DepartmentAdapter.ViewHolder> {

    private List<Department> list;

    public DepartmentAdapter(List<Department> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_department_demo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Department dept = list.get(position);
        holder.tvDeptName.setText(dept.getName());
        holder.tvManagerName.setText("Trưởng phòng: " + dept.getManagerName());
        holder.tvEmpCount.setText(dept.getEmployeeCount() + " người");
        holder.tvPerformanceStr.setText(dept.getPerformance() + "%");
        holder.progressBar.setProgress(dept.getPerformance());

        // Đổi màu nền cho Icon tùy thuộc vào phòng ban
        holder.imgDeptIcon.setBackgroundTintList(ColorStateList.valueOf(dept.getColorBg()));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeptName, tvManagerName, tvEmpCount, tvPerformanceStr;
        ImageView imgDeptIcon;
        ProgressBar progressBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeptName = itemView.findViewById(R.id.tvDeptName);
            tvManagerName = itemView.findViewById(R.id.tvManagerName);
            tvEmpCount = itemView.findViewById(R.id.tvEmpCount);
            tvPerformanceStr = itemView.findViewById(R.id.tvPerformanceStr);
            imgDeptIcon = itemView.findViewById(R.id.imgDeptIcon);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
}
