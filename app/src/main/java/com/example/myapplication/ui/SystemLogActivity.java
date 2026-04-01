package com.example.myapplication.ui;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Employee;
import com.example.myapplication.model.SystemLog;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.BottomNavHelper;
import com.example.myapplication.utils.TopBarHelper;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SystemLogActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private ChipGroup cgActionFilter;
    private ApiService apiService;
    private LogAdapter adapter;

    private static final String[] ACTIONS = {"Tất cả", "LOGIN", "CREATE", "UPDATE", "DELETE", "LOGOUT", "BACKUP"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_log);

        apiService = RetrofitClient.getApiService(this);

        TopBarHelper.setupAdminHeader(this, "Nhật ký hệ thống");

        progressBar    = findViewById(R.id.progressBar);
        tvEmpty        = findViewById(R.id.tvEmpty);
        recyclerView   = findViewById(R.id.recyclerViewLogs);
        cgActionFilter = findViewById(R.id.cgActionFilter);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LogAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        setupFilterChips();
        loadLogs();
        BottomNavHelper.setupBottomNav(this, -1);
    }

    private void setupFilterChips() {
        if (cgActionFilter == null) return;
        for (String action : ACTIONS) {
            Chip chip = new Chip(this);
            chip.setText(action);
            chip.setCheckable(true);
            if ("Tất cả".equals(action)) chip.setChecked(true);

            chip.setOnCheckedChangeListener((v, checked) -> {
                if (checked) {
                    if ("Tất cả".equals(action)) loadLogs();
                    else loadLogsByAction(action);
                }
            });
            cgActionFilter.addView(chip);
        }
    }

    private void showFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_filter_log, null);
        dialog.setContentView(view);

        AutoCompleteTextView actvAction = view.findViewById(R.id.actvAction);
        actvAction.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, ACTIONS));

        AutoCompleteTextView actvEmployee = view.findViewById(R.id.actvEmployee);
        AutoCompleteTextView actvDepartment = view.findViewById(R.id.actvDepartment);
        AutoCompleteTextView actvPosition = view.findViewById(R.id.actvPosition);

        // Date pickers
        TextInputEditText etFromDate = view.findViewById(R.id.etFromDate);
        TextInputEditText etToDate = view.findViewById(R.id.etToDate);
        etFromDate.setOnClickListener(v -> showDatePicker(etFromDate));
        etToDate.setOnClickListener(v -> showDatePicker(etToDate));

        MaterialButton btnReset = view.findViewById(R.id.btnReset);
        MaterialButton btnApply = view.findViewById(R.id.btnApply);

        btnReset.setOnClickListener(v -> {
            actvAction.setText("", false);
            actvEmployee.setText("", false);
            actvDepartment.setText("", false);
            actvPosition.setText("", false);
            etFromDate.setText("");
            etToDate.setText("");
        });

        btnApply.setOnClickListener(v -> {
            dialog.dismiss();
            applyFilter(actvAction.getText().toString(), actvEmployee.getText().toString(),
                    actvDepartment.getText().toString(), actvPosition.getText().toString(),
                    etFromDate.getText().toString(), etToDate.getText().toString());
        });

        dialog.show();
    }

    private void showDatePicker(TextInputEditText target) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (dp, year, month, day) -> {
            target.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void applyFilter(String action, String employee, String department, String position, String fromDate, String toDate) {
        showLoading();
        apiService.getSystemLogs().enqueue(new Callback<List<SystemLog>>() {
            @Override
            public void onResponse(Call<List<SystemLog>> c, Response<List<SystemLog>> r) {
                hideLoading();
                if (r.isSuccessful() && r.body() != null) {
                    List<SystemLog> filtered = new ArrayList<>();
                    for (SystemLog log : r.body()) {
                        if (!action.isEmpty() && !"Tất cả".equals(action) && !action.equalsIgnoreCase(log.action)) continue;
                        if (!employee.isEmpty() && (log.username == null || !log.username.toLowerCase().contains(employee.toLowerCase()))) continue;
                        if (!department.isEmpty() && (log.department == null || !log.department.toLowerCase().contains(department.toLowerCase()))) continue;
                        if (!position.isEmpty() && (log.position == null || !log.position.toLowerCase().contains(position.toLowerCase()))) continue;
                        if (!fromDate.isEmpty() && !toDate.isEmpty() && log.createdAt != null) {
                            String logDate = log.createdAt.substring(0, 10);
                            if (logDate.compareTo(fromDate) < 0 || logDate.compareTo(toDate) > 0) continue;
                        }
                        filtered.add(log);
                    }
                    updateList(filtered);
                }
            }
            @Override
            public void onFailure(Call<List<SystemLog>> c, Throwable t) { hideLoading(); }
        });
    }

    private void loadLogs() {
        showLoading();
        apiService.getSystemLogs().enqueue(new Callback<List<SystemLog>>() {
            @Override
            public void onResponse(Call<List<SystemLog>> c, Response<List<SystemLog>> r) {
                hideLoading();
                if (r.isSuccessful() && r.body() != null) updateList(r.body());
            }
            @Override
            public void onFailure(Call<List<SystemLog>> c, Throwable t) { hideLoading(); }
        });
    }

    private void loadLogsByAction(String action) {
        showLoading();
        apiService.filterLogsByAction(action).enqueue(new Callback<List<SystemLog>>() {
            @Override
            public void onResponse(Call<List<SystemLog>> c, Response<List<SystemLog>> r) {
                hideLoading();
                if (r.isSuccessful() && r.body() != null) updateList(r.body());
            }
            @Override
            public void onFailure(Call<List<SystemLog>> c, Throwable t) { hideLoading(); }
        });
    }

    private void showLoading() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
    }

    private void hideLoading() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
    }

    private void updateList(List<SystemLog> list) {
        adapter.updateData(list);
        if (tvEmpty != null) tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    static class LogAdapter extends RecyclerView.Adapter<LogAdapter.VH> {
        private List<SystemLog> list;
        LogAdapter(List<SystemLog> list) { this.list = list; }
        void updateData(List<SystemLog> newList) { this.list = newList; notifyDataSetChanged(); }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_system_log, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            SystemLog log = list.get(pos);
            h.tvUsername.setText(log.getUserDisplay());
            h.tvDescription.setText(log.description);
            h.tvAction.setText(log.action);
            h.tvTime.setText(log.getTimeDisplay());
            h.tvIcon.setText(log.getActionIcon());

            int color;
            switch (log.action != null ? log.action.toUpperCase() : "") {
                case "LOGIN":  color = Color.parseColor("#3B82F6"); break; // Blue
                case "CREATE": color = Color.parseColor("#10B981"); break; // Green
                case "UPDATE": color = Color.parseColor("#F59E0B"); break; // Amber
                case "DELETE": color = Color.parseColor("#EF4444"); break; // Red
                case "BACKUP": color = Color.parseColor("#8B5CF6"); break; // Purple
                case "LOGOUT": color = Color.parseColor("#64748B"); break; // Slate
                default:       color = Color.parseColor("#94A3B8"); break; // Gray
            }
            if (h.tvAction.getBackground() != null) {
                h.tvAction.getBackground().setTint(color);
            }
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvIcon, tvAction, tvUsername, tvDescription, tvTime;
            VH(View v) {
                super(v);
                tvIcon = v.findViewById(R.id.tvLogIcon);
                tvAction = v.findViewById(R.id.tvLogAction);
                tvUsername = v.findViewById(R.id.tvLogUsername);
                tvDescription = v.findViewById(R.id.tvLogDescription);
                tvTime = v.findViewById(R.id.tvLogTime);
            }
        }
    }
}
