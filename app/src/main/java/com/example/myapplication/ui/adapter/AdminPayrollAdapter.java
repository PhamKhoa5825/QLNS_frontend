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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminPayrollAdapter extends RecyclerView.Adapter<AdminPayrollAdapter.ViewHolder> {

    private final Context context;
    private List<SalaryRecord> payrollList;
    private List<SalaryRecord> payrollListFull;
    private static final NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));

    public AdminPayrollAdapter(Context context, List<SalaryRecord> payrollList) {
        this.context = context;
        this.payrollList = payrollList;
        this.payrollListFull = new ArrayList<>(payrollList);
    }

    public void updateList(List<SalaryRecord> newList) {
        this.payrollList = newList;
        this.payrollListFull = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        if (query == null || query.isEmpty()) {
            payrollList = new ArrayList<>(payrollListFull);
        } else {
            List<SalaryRecord> filtered = new ArrayList<>();
            String lowerQuery = query.toLowerCase().trim();
            for (SalaryRecord r : payrollListFull) {
                if ((r.getEmployeeName() != null && r.getEmployeeName().toLowerCase().contains(lowerQuery)) ||
                    (r.getDepartmentName() != null && r.getDepartmentName().toLowerCase().contains(lowerQuery))) {
                    filtered.add(r);
                }
            }
            payrollList = filtered;
        }
        notifyDataSetChanged();
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
            case "PAID":
                holder.tvStatus.setText("ĐÃ CHỐT");
                holder.tvStatus.setTextColor(context.getResources().getColor(R.color.greenSuccess));
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pill);
                holder.tvStatus.getBackground().setTint(context.getResources().getColor(R.color.success_bg));
                break;
            case "DRAFT":
                holder.tvStatus.setText("BẢN NHÁP");
                holder.tvStatus.setTextColor(context.getResources().getColor(R.color.primary));
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pill);
                holder.tvStatus.getBackground().setTint(context.getResources().getColor(R.color.soft_primary));
                break;
            default:
                holder.tvStatus.setText("TẠM TÍNH");
                holder.tvStatus.setTextColor(context.getResources().getColor(R.color.orangeWarning));
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pill);
                holder.tvStatus.getBackground().setTint(context.getResources().getColor(R.color.icon_bg_amber));
                break;
        }

        holder.itemView.setOnClickListener(v -> {
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
