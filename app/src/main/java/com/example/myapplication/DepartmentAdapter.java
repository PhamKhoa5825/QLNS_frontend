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

import com.example.myapplication.model.Department;

import java.util.ArrayList;
import java.util.List;

public class DepartmentAdapter extends RecyclerView.Adapter<DepartmentAdapter.ViewHolder> {

    // Màu tự gán theo vị trí (thay colorBg hardcode cũ)
    private static final int[] COLORS = {
            Color.parseColor("#2563EB"),  // Xanh dương
            Color.parseColor("#EC4899"),  // Hồng
            Color.parseColor("#10B981"),  // Xanh lá
            Color.parseColor("#A855F7"),  // Tím
            Color.parseColor("#F59E0B"),  // Vàng
            Color.parseColor("#EF4444"),  // Đỏ
    };

    private List<Department> list = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Department department);
    }

    public DepartmentAdapter(List<Department> list) {
        this.list = list;
    }

    public DepartmentAdapter(List<Department> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    // Cập nhật data từ API
    public void setData(List<Department> newList) {
        this.list = newList;
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
        Department dept = list.get(position);

        holder.tvDeptName.setText(dept.getName());
        holder.tvManagerName.setText("Trưởng phòng: " + dept.getManagerName());

        // employeeCount và performance không có trong API → ẩn hoặc để mặc định
        holder.tvEmpCount.setText("--");
        holder.tvPerformanceStr.setText("--");
        holder.progressBar.setProgress(0);

        // Gán màu theo vị trí thay vì hardcode
        int color = COLORS[position % COLORS.length];
        holder.imgDeptIcon.setBackgroundTintList(ColorStateList.valueOf(color));

        if (listener != null) {
            holder.itemView.setOnClickListener(v -> listener.onItemClick(dept));
        }
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
