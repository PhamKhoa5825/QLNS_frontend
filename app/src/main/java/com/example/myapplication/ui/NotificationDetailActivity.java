package com.example.myapplication.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * NotificationDetailActivity — Trang chi tiết thông báo
 *
 * Nhận extras từ NotificationCenterActivity:
 * - notiId, title, content, targetType, departmentName, createdByName, createdAt
 * - userId (để mark read)
 */
public class NotificationDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_notification_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Nhận dữ liệu từ intent
        String title = getIntent().getStringExtra("title");
        String content = getIntent().getStringExtra("content");
        String targetType = getIntent().getStringExtra("targetType");
        String departmentName = getIntent().getStringExtra("departmentName");
        String createdByName = getIntent().getStringExtra("createdByName");
        String createdAt = getIntent().getStringExtra("createdAt");
        Long notiId = getIntent().getLongExtra("notiId", -1);
        Long userId = getIntent().getLongExtra("userId", -1);

        // Hiển thị
        TextView tvTitle = findViewById(R.id.tvDetailTitle);
        TextView tvContent = findViewById(R.id.tvDetailContent);
        TextView tvCreatedBy = findViewById(R.id.tvDetailCreatedBy);
        TextView tvTime = findViewById(R.id.tvDetailTime);
        Chip chipType = findViewById(R.id.chipTargetType);
        LinearLayout layoutDept = findViewById(R.id.layoutDeptInfo);
        TextView tvDept = findViewById(R.id.tvDetailDept);

        tvTitle.setText(title != null ? title : "—");
        tvContent.setText(content != null ? content : "Không có nội dung");
        tvCreatedBy.setText(createdByName != null ? createdByName : "Hệ thống");

        // Format thời gian
        if (createdAt != null && createdAt.length() >= 16) {
            String date = createdAt.substring(8, 10) + "/" + createdAt.substring(5, 7) + "/" + createdAt.substring(0, 4);
            String time = createdAt.substring(11, 16);
            tvTime.setText(date + " " + time);
        } else {
            tvTime.setText(createdAt != null ? createdAt : "—");
        }

        // Badge loại
        if (targetType != null) {
            switch (targetType) {
                case "DEPARTMENT":
                    chipType.setText(departmentName != null ? departmentName : "Phòng ban");
                    chipType.setChipBackgroundColorResource(R.color.soft_purple);
                    chipType.setTextColor(0xFF7C3AED);
                    layoutDept.setVisibility(View.VISIBLE);
                    tvDept.setText(departmentName != null ? departmentName : "—");
                    break;
                case "EMPLOYEE":
                    chipType.setText("Cá nhân");
                    chipType.setChipBackgroundColorResource(R.color.soft_green);
                    chipType.setTextColor(0xFF10B981);
                    break;
                default:
                    chipType.setText("Toàn công ty");
                    break;
            }
        }

        // Mark as read — gọi ApiService trực tiếp
        if (notiId != -1 && userId != -1) {
            ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
            apiService.markNotiRead(notiId, userId).enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> c, Response<Void> r) { /* silent */ }
                @Override public void onFailure(Call<Void> c, Throwable t) { /* silent */ }
            });
        }
    }
}
