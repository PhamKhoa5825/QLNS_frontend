package com.example.myapplication.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.model.AdminModels;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SystemLogActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private ChipGroup cgActionFilter;
    private ApiService apiService;
    private LogAdapter adapter;

    private static final String[] ACTIONS = {"Tất cả", "LOGIN", "CREATE", "UPDATE", "DELETE", "LOGOUT"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_log);

        apiService = RetrofitClient.getClient().create(ApiService.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBar    = findViewById(R.id.progressBar);
        recyclerView   = findViewById(R.id.recyclerViewLogs);
        cgActionFilter = findViewById(R.id.cgActionFilter);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LogAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        setupFilterChips();
        loadLogs();
    }

    private void setupFilterChips() {
        for (String action : ACTIONS) {
            Chip chip = new Chip(this);
            chip.setText(action);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(android.R.color.white);
            chip.setChipStrokeWidth(2f);
            if ("Tất cả".equals(action)) chip.setChecked(true);

            chip.setOnCheckedChangeListener((v, checked) -> {
                if (checked) {
                    if ("Tất cả".equals(action)) {
                        loadLogs();
                    } else {
                        loadLogsByAction(action);
                    }
                }
            });

            cgActionFilter.addView(chip);
        }
    }

    private void loadLogs() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getSystemLogs().enqueue(new Callback<List<AdminModels.SystemLogResponse>>() {
            @Override
            public void onResponse(Call<List<AdminModels.SystemLogResponse>> c,
                                   Response<List<AdminModels.SystemLogResponse>> r) {
                progressBar.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null) {
                    adapter.updateData(r.body());
                }
            }
            @Override public void onFailure(Call<List<AdminModels.SystemLogResponse>> c, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SystemLogActivity.this, "Lỗi tải nhật ký", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadLogsByAction(String action) {
        progressBar.setVisibility(View.VISIBLE);
        apiService.filterLogsByAction(action).enqueue(new Callback<List<AdminModels.SystemLogResponse>>() {
            @Override
            public void onResponse(Call<List<AdminModels.SystemLogResponse>> c,
                                   Response<List<AdminModels.SystemLogResponse>> r) {
                progressBar.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null) {
                    adapter.updateData(r.body());
                }
            }
            @Override public void onFailure(Call<List<AdminModels.SystemLogResponse>> c, Throwable t) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    // ── Adapter nội bộ ────────────────────────────────────────────
    static class LogAdapter extends RecyclerView.Adapter<LogAdapter.VH> {

        private List<AdminModels.SystemLogResponse> list;

        LogAdapter(List<AdminModels.SystemLogResponse> list) { this.list = list; }

        void updateData(List<AdminModels.SystemLogResponse> newList) {
            this.list = newList;
            notifyDataSetChanged();
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_system_log, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            AdminModels.SystemLogResponse log = list.get(position);

            h.tvUsername.setText(log.username != null ? log.username : "System");
            h.tvDescription.setText(log.description != null ? log.description : "");
            h.tvAction.setText(log.action != null ? log.action : "");
            h.tvTime.setText(formatTime(log.createdAt));

            // Icon chữ cái đầu của action
            if (log.action != null && !log.action.isEmpty()) {
                h.tvIcon.setText(String.valueOf(log.action.charAt(0)));
            }

            // Màu badge theo action
            int color;
            switch (log.action != null ? log.action : "") {
                case "LOGIN":  color = Color.parseColor("#3B82F6"); break;
                case "CREATE": color = Color.parseColor("#10B981"); break;
                case "UPDATE": color = Color.parseColor("#F59E0B"); break;
                case "DELETE": color = Color.parseColor("#EF4444"); break;
                case "LOGOUT": color = Color.parseColor("#6B7280"); break;
                default:       color = Color.parseColor("#8B5CF6"); break;
            }
            h.tvAction.getBackground().setTint(color);
        }

        private String formatTime(String iso) {
            if (iso == null) return "";
            try {
                // "2026-03-16T14:30:00" → "16/03 14:30"
                String date = iso.substring(8, 10) + "/" + iso.substring(5, 7);
                String time = iso.substring(11, 16);
                return date + " " + time;
            } catch (Exception e) { return iso; }
        }

        @Override public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvIcon, tvAction, tvUsername, tvDescription, tvTime;
            VH(View v) {
                super(v);
                tvIcon        = v.findViewById(R.id.tvLogIcon);
                tvAction      = v.findViewById(R.id.tvLogAction);
                tvUsername     = v.findViewById(R.id.tvLogUsername);
                tvDescription = v.findViewById(R.id.tvLogDescription);
                tvTime        = v.findViewById(R.id.tvLogTime);
            }
        }
    }
}