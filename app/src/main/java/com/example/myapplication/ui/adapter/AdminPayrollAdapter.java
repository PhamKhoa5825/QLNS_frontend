package com.example.myapplication.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.SalaryRecord;
import com.example.myapplication.ui.PayrollActivity;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AdminPayrollAdapter extends RecyclerView.Adapter<AdminPayrollAdapter.ViewHolder> {

    private final Context context;
    private final List<SalaryRecord> payrollList;
    private static final NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));

    public AdminPayrollAdapter(Context context, List<SalaryRecord> payrollList) {
        this.context = context;
        this.payrollList = payrollList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_payroll, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SalaryRecord record = payrollList.get(position);
        
        holder.tvName.setText(record.getEmployeeName() != null ? record.getEmployeeName() : "Unknown");
        holder.tvDept.setText(record.getDepartmentName() != null ? record.getDepartmentName() : "No Department");
        
        String avatarText = "?";
        if (record.getEmployeeName() != null && !record.getEmployeeName().isEmpty()) {
            avatarText = String.valueOf(record.getEmployeeName().charAt(0)).toUpperCase();
        }
        holder.tvAvatar.setText(avatarText);

        Double gross = record.getGrossSalary() != null ? record.getGrossSalary() : 0.0;
        holder.tvGrossSalary.setText(fmt.format(gross.longValue()) + " ₫");
        holder.tvGrade.setText("Loại " + (record.getPerformanceGrade() != null ? record.getPerformanceGrade() : "--"));

        String status = record.getStatus() != null ? record.getStatus() : "ESTIMATE";
        switch (status) {
            case "FINALIZED":
                holder.tvStatus.setText("ĐÃ CHỐT");
                holder.tvStatus.setTextColor(Color.parseColor("#10B981")); // Green
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_working); 
                break;
            case "DRAFT":
                holder.tvStatus.setText("NHÁP");
                holder.tvStatus.setTextColor(Color.parseColor("#3B82F6")); // Blue
                holder.tvStatus.setBackgroundResource(R.drawable.bg_chip_unselected);
                break;
            default:
                holder.tvStatus.setText("ƯỚC TÍNH");
                holder.tvStatus.setTextColor(Color.parseColor("#F97316")); // Orange
                holder.tvStatus.setBackgroundResource(R.drawable.bg_chip_unselected);
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            // Open detail view (reusing PayrollActivity but maybe with extra Admin controls)
            Intent intent = new Intent(context, PayrollActivity.class);
            intent.putExtra("employeeId", record.getEmployeeId());
            intent.putExtra("month", record.getMonth());
            intent.putExtra("year", record.getYear());
            intent.putExtra("isAdminView", true);
            intent.putExtra("recordId", record.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return payrollList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvDept, tvStatus, tvGrossSalary, tvGrade;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvDept = itemView.findViewById(R.id.tvDept);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvGrossSalary = itemView.findViewById(R.id.tvGrossSalary);
            tvGrade = itemView.findViewById(R.id.tvGrade);
        }
    }
}
