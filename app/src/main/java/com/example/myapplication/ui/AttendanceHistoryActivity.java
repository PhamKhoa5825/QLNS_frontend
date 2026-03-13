package com.example.myapplication.ui;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.AttendanceRecordAdapter;
import com.example.myapplication.adapter.CalendarAdapter;
import com.example.myapplication.model.AttendanceRecord;
import com.example.myapplication.model.AttendanceStats;
import com.example.myapplication.viewmodel.AttendanceHistoryViewModel;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AttendanceHistoryActivity extends AppCompatActivity {
    private AttendanceHistoryViewModel viewModel;
    private AttendanceRecordAdapter recordAdapter;
    private CalendarAdapter calendarAdapter;
    private BarChart barChart;
    private TextView tvCurrentMonth, tvAverageHoursLabel;
    
    private int selectedMonth;
    private int selectedYear;
    private Long employeeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_history);

        // Lấy employeeId từ SharedPreferences
        SharedPreferences prefs = getSharedPreferences("qlns_pref", Context.MODE_PRIVATE);
        employeeId = prefs.getLong("userId", -1L);

        // Khởi tạo tháng/năm hiện tại
        Calendar calendar = Calendar.getInstance();
        selectedMonth = calendar.get(Calendar.MONTH) + 1;
        selectedYear = calendar.get(Calendar.YEAR);

        initUI();
        setupRecyclerViews();
        setupViewModel();

        loadData();
    }

    private void initUI() {
        barChart = findViewById(R.id.barChartWorkHours);
        tvCurrentMonth = findViewById(R.id.tvCurrentMonth);
        tvAverageHoursLabel = findViewById(R.id.tvAverageHoursLabel);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        findViewById(R.id.btnPrevMonth).setOnClickListener(v -> {
            selectedMonth--;
            if (selectedMonth < 1) {
                selectedMonth = 12;
                selectedYear--;
            }
            loadData();
        });

        findViewById(R.id.btnNextMonth).setOnClickListener(v -> {
            selectedMonth++;
            if (selectedMonth > 12) {
                selectedMonth = 1;
                selectedYear++;
            }
            loadData();
        });

        findViewById(R.id.monthSelector).setOnClickListener(v -> showMonthYearPicker());
        
        updateMonthDisplay();
    }

    private void showMonthYearPicker() {
        // Sử dụng DatePickerDialog nhưng chỉ quan tâm tháng và năm
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedYear = year;
                    selectedMonth = month + 1;
                    loadData();
                },
                selectedYear,
                selectedMonth - 1,
                1
        );
        datePickerDialog.show();
    }

    private void updateMonthDisplay() {
        tvCurrentMonth.setText(String.format(Locale.getDefault(), "Tháng %d, %d", selectedMonth, selectedYear));
    }

    private void loadData() {
        updateMonthDisplay();
        if (calendarAdapter != null) {
            calendarAdapter.updateDays(selectedMonth, selectedYear);
        }
        if (employeeId != -1L) {
            viewModel.loadAttendanceData(employeeId, selectedMonth, selectedYear);
        }
    }

    private void setupRecyclerViews() {
        RecyclerView recyclerAttendance = findViewById(R.id.recyclerAttendance);
        recyclerAttendance.setLayoutManager(new LinearLayoutManager(this));
        recordAdapter = new AttendanceRecordAdapter();
        recyclerAttendance.setAdapter(recordAdapter);

        RecyclerView recyclerCalendar = findViewById(R.id.recyclerCalendar);
        recyclerCalendar.setLayoutManager(new GridLayoutManager(this, 7));
        calendarAdapter = new CalendarAdapter();
        recyclerCalendar.setAdapter(calendarAdapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(AttendanceHistoryViewModel.class);

        viewModel.getStats().observe(this, this::updateSummaryCards);
        viewModel.getRecords().observe(this, records -> {
            if (records != null) {
                calendarAdapter.setAttendanceData(records);
                updateBarChart(records);
                recordAdapter.setRecords(records);
            }
        });

        viewModel.getError().observe(this, msg -> {
            // Xử lý lỗi nếu cần
        });
    }

    private void updateSummaryCards(AttendanceStats stats) {
        if (stats == null) return;

        ((TextView) findViewById(R.id.tvTotalValue)).setText(String.format(Locale.getDefault(), "%.1fh", stats.getTotalWorkHours()));
        ((TextView) findViewById(R.id.tvOnTimeValue)).setText(String.valueOf(stats.getOnTimeCount()));
        ((TextView) findViewById(R.id.tvLateValue)).setText(String.valueOf(stats.getLateCount()));
        
        String avgStr = String.format(Locale.getDefault(), "%.1fh", stats.getAverageHoursPerDay());
        ((TextView) findViewById(R.id.tvAverageValue)).setText(avgStr);
        
        if (tvAverageHoursLabel != null) {
            tvAverageHoursLabel.setText("TB: " + avgStr);
        }
    }

    private void updateBarChart(List<AttendanceRecord> records) {
        if (records == null || records.isEmpty()) {
            barChart.clear();
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        int start = Math.max(0, records.size() - 7);
        for (int i = start; i < records.size(); i++) {
            AttendanceRecord record = records.get(i);
            entries.add(new BarEntry(i - start, (float) record.getWorkHours()));
            labels.add("Thứ");
        }

        BarDataSet dataSet = new BarDataSet(entries, "Giờ làm việc");
        dataSet.setColor(Color.parseColor("#1A73E8"));
        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        barChart.setData(barData);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.parseColor("#9CA3AF"));
        
        barChart.getAxisLeft().setDrawAxisLine(false);
        barChart.getAxisLeft().setGridColor(Color.parseColor("#F3F4F6"));
        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.getDescription().setEnabled(false);
        barChart.animateY(800);
        barChart.invalidate();
    }
}