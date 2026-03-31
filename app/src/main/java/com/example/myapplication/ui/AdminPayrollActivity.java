package com.example.myapplication.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.SalaryRecord;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.ui.adapter.AdminPayrollAdapter;
import com.example.myapplication.utils.BottomNavHelper;
import com.example.myapplication.utils.TopBarHelper;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminPayrollActivity extends AppCompatActivity {

    private TextView tvMonthYear, tvTotalNetSalary, tvTotalEmployeeCount, tvPayrollStatus;
    private ImageButton btnPrevMonth, btnNextMonth;
    private RecyclerView rvAdminPayroll;
    private com.google.android.material.button.MaterialButton btnGenerateAll, btnFinalizeAll;
    private androidx.appcompat.widget.SearchView svAdminPayroll;
    
    private AdminPayrollAdapter adapter;
    private List<SalaryRecord> payrollList = new ArrayList<>();
    
    private int currentMonth;
    private int currentYear;
    private final NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_payroll);

        LocalDate now = LocalDate.now();
        currentMonth = now.getMonthValue();
        currentYear = now.getYear();

        initViews();
        fetchPayrollSummary();

        BottomNavHelper.setupBottomNav(this, R.id.nav_home);
    }

    private void initViews() {
        TopBarHelper.setupTopBar(this);
        
        tvMonthYear = findViewById(R.id.tvMonthYear);
        tvTotalNetSalary = findViewById(R.id.tvTotalNetSalary);
        tvTotalEmployeeCount = findViewById(R.id.tvTotalEmployeeCount);
        tvPayrollStatus = findViewById(R.id.tvPayrollStatus);
        btnFinalizeAll = findViewById(R.id.btnFinalizeAll);
        
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        rvAdminPayroll = findViewById(R.id.rvAdminPayroll);
        btnGenerateAll = findViewById(R.id.btnGenerateAll);
        svAdminPayroll = findViewById(R.id.svAdminPayroll);

        rvAdminPayroll.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminPayrollAdapter(this, payrollList);
        rvAdminPayroll.setAdapter(adapter);

        btnPrevMonth.setOnClickListener(v -> {
            currentMonth--;
            if (currentMonth < 1) { currentMonth = 12; currentYear--; }
            updateMonthDisplay();
            fetchPayrollSummary();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentMonth++;
            if (currentMonth > 12) { currentMonth = 1; currentYear++; }
            updateMonthDisplay();
            fetchPayrollSummary();
        });

        btnGenerateAll.setOnClickListener(v -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Xác nhận tạo/cập nhật")
                .setMessage("Hệ thống sẽ tính toán lại toàn bộ lương tháng " + currentMonth + "/" + currentYear + ". Bạn có chắc chắn?")
                .setPositiveButton("Xác nhận", (d, w) -> generateAll())
                .setNegativeButton("Hủy", null)
                .show();
        });

        btnFinalizeAll.setOnClickListener(v -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Xác nhận chốt lương")
                .setMessage("Sau khi chốt, bảng lương sẽ không thể chỉnh sửa. Bạn có chắc chắn muốn chốt toàn bộ bảng lương tháng này?")
                .setPositiveButton("Chốt lương", (d, w) -> finalizeAll())
                .setNegativeButton("Hủy", null)
                .show();
        });

        svAdminPayroll.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });

        updateMonthDisplay();
    }

    private void updateMonthDisplay() {
        tvMonthYear.setText(String.format("Tháng %02d/%d", currentMonth, currentYear));
    }

    private void fetchPayrollSummary() {
        ApiService api = RetrofitClient.getApiService();
        api.getAllPayrollSummary(currentMonth, currentYear).enqueue(new Callback<List<SalaryRecord>>() {
            @Override
            public void onResponse(Call<List<SalaryRecord>> call, Response<List<SalaryRecord>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    payrollList.clear();
                    payrollList.addAll(response.body());
                    adapter.updateList(payrollList);
                    updateSummaryCard();
                } else {
                    Toast.makeText(AdminPayrollActivity.this, "Lỗi khi tải dữ liệu", Toast.LENGTH_SHORT).show();
                    payrollList.clear();
                    adapter.updateList(payrollList);
                    updateSummaryCard();
                }
            }

            @Override
            public void onFailure(Call<List<SalaryRecord>> call, Throwable t) {
                Toast.makeText(AdminPayrollActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSummaryCard() {
        double totalNet = 0;
        int count = payrollList.size();
        boolean allFinalized = !payrollList.isEmpty();
        boolean anyDraft = false;

        for (SalaryRecord r : payrollList) {
            totalNet += (r.getGrossSalary() != null ? r.getGrossSalary() : 0.0);
            String status = r.getStatus();
            if (!"FINALIZED".equals(status) && !"PAID".equals(status)) {
                allFinalized = false;
            }
            if ("DRAFT".equals(status) || "ESTIMATE".equals(status)) {
                anyDraft = true;
            }
        }

        tvTotalNetSalary.setText(fmt.format((long) totalNet) + " ₫");
        tvTotalEmployeeCount.setText(String.valueOf(count));
        
        if (payrollList.isEmpty()) {
            tvPayrollStatus.setText("CHƯA TẠO");
            tvPayrollStatus.setTextColor(getResources().getColor(R.color.secondary));
            btnFinalizeAll.setVisibility(View.GONE);
        } else if (allFinalized) {
            tvPayrollStatus.setText("HOÀN TẤT");
            tvPayrollStatus.setTextColor(getResources().getColor(R.color.greenSuccess));
            btnFinalizeAll.setVisibility(View.GONE);
        } else if (anyDraft) {
            tvPayrollStatus.setText("BẢN NHÁP");
            tvPayrollStatus.setTextColor(getResources().getColor(R.color.primary));
            btnFinalizeAll.setVisibility(View.VISIBLE);
        } else {
            tvPayrollStatus.setText("ĐANG XỬ LÝ");
            tvPayrollStatus.setTextColor(getResources().getColor(R.color.orangeWarning));
            btnFinalizeAll.setVisibility(View.VISIBLE);
        }
    }

    private void finalizeAll() {
        List<SalaryRecord> drafts = new ArrayList<>();
        for (SalaryRecord r : payrollList) {
            if (!"FINALIZED".equals(r.getStatus()) && !"PAID".equals(r.getStatus())) {
                drafts.add(r);
            }
        }

        if (drafts.isEmpty()) return;

        ApiService api = RetrofitClient.getApiService();
        final int total = drafts.size();
        final int[] successCount = {0};
        
        Toast.makeText(this, "Đang chốt " + total + " bản ghi...", Toast.LENGTH_SHORT).show();

        for (SalaryRecord r : drafts) {
            api.finalizePayroll(r.getId()).enqueue(new Callback<SalaryRecord>() {
                @Override
                public void onResponse(Call<SalaryRecord> call, Response<SalaryRecord> response) {
                    successCount[0]++;
                    if (successCount[0] == total) {
                        Toast.makeText(AdminPayrollActivity.this, "✅ Đã chốt toàn bộ bảng lương!", Toast.LENGTH_SHORT).show();
                        fetchPayrollSummary();
                    }
                }

                @Override
                public void onFailure(Call<SalaryRecord> call, Throwable t) {
                    successCount[0]++;
                    if (successCount[0] == total) {
                        fetchPayrollSummary();
                    }
                }
            });
        }
    }

    private void generateAll() {
        ApiService api = RetrofitClient.getApiService();
        api.generateAllPayroll(currentMonth, currentYear).enqueue(new Callback<List<SalaryRecord>>() {
            @Override
            public void onResponse(Call<List<SalaryRecord>> call, Response<List<SalaryRecord>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminPayrollActivity.this, "✅ Đã tạo bảng lương nháp thành công!", Toast.LENGTH_LONG).show();
                    fetchPayrollSummary();
                } else {
                    Toast.makeText(AdminPayrollActivity.this, "Lỗi khi tạo bảng lương", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<SalaryRecord>> call, Throwable t) {
                Toast.makeText(AdminPayrollActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
