package com.example.myapplication.ui;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.network.ApiErrorHelper;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.BottomNavHelper;
import com.example.myapplication.utils.TopBarHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BackupActivity extends AppCompatActivity {

    private ApiService apiService;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private View tvEmpty;
    private TextView tvStatus;
    private MaterialButton btnBackupNow;

    private BackupAdapter adapter;
    private List<Map<String, Object>> backupList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_backup);

        apiService = RetrofitClient.getApiService(this);

        bindViews();
        loadStatus();
        loadBackups();
        BottomNavHelper.setupBottomNav(this, -1);
    }

    private void bindViews() {
        TopBarHelper.setupTopBar(this);

        recyclerView = findViewById(R.id.recyclerViewBackups);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvStatus = findViewById(R.id.tvBackupStatus);
        btnBackupNow = findViewById(R.id.btnBackupNow);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BackupAdapter();
        recyclerView.setAdapter(adapter);

        btnBackupNow.setOnClickListener(v -> createBackup());
    }

    private void loadStatus() {
        apiService.getBackupStatus().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> c, Response<Map<String, Object>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    Map<String, Object> status = r.body();
                    boolean found = status.get("mysqldumpFound") != null && (boolean) status.get("mysqldumpFound");
                    boolean autoEnabled = status.get("autoEnabled") != null && (boolean) status.get("autoEnabled");

                    String statusText = found ? "✓ Hệ thống backup sẵn sàng" : "✗ Chưa tìm thấy công cụ backup";
                    if (found && autoEnabled) statusText += " · Tự động: BẬT";
                    tvStatus.setText(statusText);
                    tvStatus.setTextColor(found ? 0xFF10B981 : 0xFFEF4444);
                    btnBackupNow.setEnabled(found);
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> c, Throwable t) {}
        });
    }

    private void loadBackups() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.listBackups().enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> c, Response<List<Map<String, Object>>> r) {
                progressBar.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null) {
                    backupList = r.body();
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(backupList.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }
            @Override public void onFailure(Call<List<Map<String, Object>>> c, Throwable t) { progressBar.setVisibility(View.GONE); }
        });
    }

    private void createBackup() {
        btnBackupNow.setEnabled(false);
        btnBackupNow.setText("Đang sao lưu...");
        progressBar.setVisibility(View.VISIBLE);

        apiService.createBackup().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> c, Response<Map<String, Object>> r) {
                progressBar.setVisibility(View.GONE);
                btnBackupNow.setEnabled(true);
                btnBackupNow.setText("Sao lưu ngay");
                if (r.isSuccessful()) {
                    Toast.makeText(BackupActivity.this, "Đã tạo bản sao lưu", Toast.LENGTH_SHORT).show();
                    loadBackups();
                } else ApiErrorHelper.show(BackupActivity.this, r, "Thất bại");
            }
            @Override public void onFailure(Call<Map<String, Object>> c, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnBackupNow.setEnabled(true);
                btnBackupNow.setText("Sao lưu ngay");
            }
        });
    }

    private void confirmRestore(String filename) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Khôi phục dữ liệu")
                .setMessage("Dữ liệu hiện tại trên hệ thống sẽ bị ghi đè bởi bản sao lưu này. Bạn có chắc chắn muốn tiếp tục?")
                .setPositiveButton("Khôi phục", (d, w) -> doRestore(filename))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void doRestore(String filename) {
        progressBar.setVisibility(View.VISIBLE);
        apiService.restoreBackup(filename).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> c, Response<Map<String, Object>> r) {
                progressBar.setVisibility(View.GONE);
                if (r.isSuccessful()) Toast.makeText(BackupActivity.this, "Thành công", Toast.LENGTH_SHORT).show();
                else ApiErrorHelper.show(BackupActivity.this, r, "Thất bại");
            }
            @Override public void onFailure(Call<Map<String, Object>> c, Throwable t) { progressBar.setVisibility(View.GONE); }
        });
    }

    private void confirmDelete(String filename) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Xóa bản sao lưu")
                .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn bản sao lưu: " + filename + "?")
                .setPositiveButton("Xóa", (d, w) -> {
                    apiService.deleteBackup(filename).enqueue(new Callback<Void>() {
                        @Override public void onResponse(Call<Void> c, Response<Void> r) {
                            if (r.isSuccessful()) loadBackups();
                        }
                        @Override public void onFailure(Call<Void> c, Throwable t) {}
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void downloadBackup(String filename) {
        String baseUrl = RetrofitClient.getBaseUrl(this);
        String url = baseUrl + "api/admin/backup/" + filename + "/download";
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("Backup QLNS");
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        dm.enqueue(request);
        Toast.makeText(this, "Đang tải xuống...", Toast.LENGTH_SHORT).show();
    }

    class BackupAdapter extends RecyclerView.Adapter<BackupAdapter.VH> {
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_backup, p, false));
        }
        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Map<String, Object> backup = backupList.get(pos);
            String filename = (String) backup.get("filename");
            h.tvFilename.setText(filename);
            h.tvSize.setText((String) backup.get("sizeFormatted"));
            h.tvDate.setText((String) backup.get("createdAt"));
            h.btnDownload.setOnClickListener(v -> downloadBackup(filename));
            h.btnRestore.setOnClickListener(v -> confirmRestore(filename));
            h.btnDelete.setOnClickListener(v -> confirmDelete(filename));
        }
        @Override public int getItemCount() { return backupList.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView tvFilename, tvSize, tvDate;
            View btnDownload, btnRestore, btnDelete;
            VH(View v) {
                super(v);
                tvFilename = v.findViewById(R.id.tvBackupFilename);
                tvSize = v.findViewById(R.id.tvBackupSize);
                tvDate = v.findViewById(R.id.tvBackupDate);
                btnDownload = v.findViewById(R.id.btnDownload);
                btnRestore = v.findViewById(R.id.btnRestore);
                btnDelete = v.findViewById(R.id.btnDeleteBackup);
            }
        }
    }
}
