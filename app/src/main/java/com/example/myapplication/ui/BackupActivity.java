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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * BackupActivity — Quản lý sao lưu/khôi phục database
 *
 * Chức năng:
 * - Xem danh sách bản sao lưu
 * - Tạo backup thủ công
 * - Download file .sql
 * - Restore (confirm 2 lần)
 * - Xóa bản cũ
 * - Hiện trạng thái hệ thống backup
 */
public class BackupActivity extends AppCompatActivity {

    private ApiService apiService;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty, tvStatus;
    private MaterialButton btnBackupNow;

    private BackupAdapter adapter;
    private List<Map<String, Object>> backupList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_backup);

        apiService = RetrofitClient.getClient().create(ApiService.class);

        bindViews();
        loadStatus();
        loadBackups();
    }

    private void bindViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

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

    // ── Load trạng thái hệ thống backup ──

    private void loadStatus() {
        apiService.getBackupStatus().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> c, Response<Map<String, Object>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    Map<String, Object> status = r.body();
                    boolean found = Boolean.TRUE.equals(status.get("mysqldumpFound"));
                    boolean autoEnabled = Boolean.TRUE.equals(status.get("autoEnabled"));

                    String statusText = found
                            ? "✓ Hệ thống backup sẵn sàng"
                            : "✗ Chưa tìm thấy mysqldump — liên hệ quản trị viên";
                    if (found && autoEnabled) {
                        statusText += " · Auto-backup: BẬT (2h sáng)";
                    }
                    tvStatus.setText(statusText);
                    tvStatus.setTextColor(found ? 0xFF10B981 : 0xFFEF4444);
                    btnBackupNow.setEnabled(found);
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> c, Throwable t) {
                tvStatus.setText("Không thể kiểm tra trạng thái backup");
                tvStatus.setTextColor(0xFF6B7280);
            }
        });
    }

    // ── Load danh sách backup ──

    private void loadBackups() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.listBackups().enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> c,
                                   Response<List<Map<String, Object>>> r) {
                progressBar.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null) {
                    backupList = r.body();
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(backupList.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }
            @Override public void onFailure(Call<List<Map<String, Object>>> c, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(BackupActivity.this, "Lỗi tải danh sách backup", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Tạo backup ──

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

                if (r.isSuccessful() && r.body() != null) {
                    String filename = (String) r.body().get("filename");
                    String size = (String) r.body().get("sizeFormatted");
                    Toast.makeText(BackupActivity.this,
                            "Đã sao lưu: " + filename + " (" + size + ")",
                            Toast.LENGTH_LONG).show();
                    loadBackups();
                } else {
                    ApiErrorHelper.show(BackupActivity.this, r, "Tạo bản sao lưu thất bại");
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> c, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnBackupNow.setEnabled(true);
                btnBackupNow.setText("Sao lưu ngay");
                Toast.makeText(BackupActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Restore ──

    private void confirmRestore(String filename) {
        // Confirm lần 1
        new AlertDialog.Builder(this)
                .setTitle("Khôi phục dữ liệu")
                .setMessage("Bạn chắc chắn muốn khôi phục từ bản:\n" + filename + "\n\n"
                        + "⚠ DỮ LIỆU HIỆN TẠI SẼ BỊ GHI ĐÈ TOÀN BỘ!")
                .setPositiveButton("Tiếp tục", (d, w) -> {
                    // Confirm lần 2
                    new AlertDialog.Builder(this)
                            .setTitle("XÁC NHẬN LẦN CUỐI")
                            .setMessage("Hành động này KHÔNG THỂ HOÀN TÁC.\n\n"
                                    + "Bạn nên tạo bản sao lưu mới trước khi khôi phục.\n\n"
                                    + "Bấm XÁC NHẬN để tiếp tục.")
                            .setPositiveButton("XÁC NHẬN KHÔI PHỤC", (d2, w2) -> doRestore(filename))
                            .setNegativeButton("Huỷ", null)
                            .show();
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void doRestore(String filename) {
        progressBar.setVisibility(View.VISIBLE);
        apiService.restoreBackup(filename).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> c, Response<Map<String, Object>> r) {
                progressBar.setVisibility(View.GONE);
                if (r.isSuccessful()) {
                    Toast.makeText(BackupActivity.this,
                            "Đã khôi phục thành công từ " + filename, Toast.LENGTH_LONG).show();
                } else {
                    ApiErrorHelper.show(BackupActivity.this, r, "Khôi phục dữ liệu thất bại");
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> c, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(BackupActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Xóa backup ──

    private void confirmDelete(String filename) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa bản sao lưu")
                .setMessage("Xóa file: " + filename + "?")
                .setPositiveButton("Xóa", (d, w) -> {
                    apiService.deleteBackup(filename).enqueue(new Callback<Void>() {
                        @Override public void onResponse(Call<Void> c, Response<Void> r) {
                            Toast.makeText(BackupActivity.this,
                                    r.isSuccessful() ? "Đã xóa" : "Lỗi: " + r.code(),
                                    Toast.LENGTH_SHORT).show();
                            loadBackups();
                        }
                        @Override public void onFailure(Call<Void> c, Throwable t) {
                            Toast.makeText(BackupActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    // ── Download backup ──

    private void downloadBackup(String filename) {
        SharedPreferences prefs = getSharedPreferences("qlns_pref", MODE_PRIVATE);
        String token = prefs.getString("token", "");
        String baseUrl = RetrofitClient.getBaseUrl();

        String url = baseUrl + "api/admin/backup/" + filename + "/download";

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.addRequestHeader("Authorization", "Bearer " + token);
        request.setTitle("Backup QLNS");
        request.setDescription("Đang tải " + filename);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);

        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        dm.enqueue(request);
        Toast.makeText(this, "Đang tải xuống: " + filename, Toast.LENGTH_SHORT).show();
    }

    // ══════════════════════════════════════════════════════════
    //  ADAPTER
    // ══════════════════════════════════════════════════════════

    class BackupAdapter extends RecyclerView.Adapter<BackupAdapter.VH> {

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_backup, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Map<String, Object> backup = backupList.get(pos);
            String filename = (String) backup.get("filename");
            String size = (String) backup.get("sizeFormatted");
            String created = backup.get("createdAt") != null
                    ? backup.get("createdAt").toString() : "—";

            // Rút gọn tên file hiển thị
            h.tvFilename.setText(filename != null ? filename : "—");
            h.tvSize.setText(size != null ? size : "—");
            h.tvDate.setText(created.length() > 19 ? created.substring(0, 19) : created);

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
