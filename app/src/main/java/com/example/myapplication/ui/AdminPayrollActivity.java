package com.example.myapplication.ui;

import android.os.Bundle;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminPayrollActivity extends AppCompatActivity {

    private TextView tvMonthYear;
    private ImageButton btnPrevMonth, btnNextMonth;
    private RecyclerView rvAdminPayroll;
    private com.google.android.material.button.MaterialButton btnGenerateAll;
    
    private AdminPayrollAdapter adapter;
    private List<SalaryRecord> payrollList = new ArrayList<>();
    
    private int currentMonth;
    private int currentYear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_payroll);

        LocalDate now = LocalDate.now();
        currentMonth = now.getMonthValue();
        currentYear = now.getYear();

        initViews();
        fetchPayrollSummary();

        BottomNavHelper.setupBottomNav(this, 0); // No tab highlighted as this is an admin sub-screen
    }

    private void initViews() {
        tvMonthYear = findViewById(R.id.tvMonthYear);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        rvAdminPayroll = findViewById(R.id.rvAdminPayroll);
        btnGenerateAll = findViewById(R.id.btnGenerateAll);

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

        btnGenerateAll.setOnClickListener(v -> generateAll());

        updateMonthDisplay();
    }

    private void updateMonthDisplay() {
        tvMonthYear.setText(String.format("Tháng %d / %d", currentMonth, currentYear));
    }

    private void fetchPayrollSummary() {
        ApiService api = RetrofitClient.getApiService();
        api.getAllPayrollSummary(currentMonth, currentYear).enqueue(new Callback<List<SalaryRecord>>() {
            @Override
            public void onResponse(Call<List<SalaryRecord>> call, Response<List<SalaryRecord>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    payrollList.clear();
                    payrollList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(AdminPayrollActivity.this, "Lỗi khi tải dữ liệu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<SalaryRecord>> call, Throwable t) {
                Toast.makeText(AdminPayrollActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generateAll() {
        ApiService api = RetrofitClient.getApiService();
        api.generateAllPayroll(currentMonth, currentYear).enqueue(new Callback<List<SalaryRecord>>() {
            @Override
            public void onResponse(Call<List<SalaryRecord>> call, Response<List<SalaryRecord>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminPayrollActivity.this, "✅ Đã tạo bảng lương nháp thành công!", Toast.LENGTH_LONG).show();
                    fetchPayrollSummary(); // Refresh list to show DRAFT instead of ESTIMATE
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
