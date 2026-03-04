package com.example.myapplication;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class TaskActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private List<Task> taskList;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        btnBack = findViewById(R.id.btnBackTask);
        btnBack.setOnClickListener(v -> finish()); // Nút quay lại

        recyclerView = findViewById(R.id.recyclerViewTask);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Nạp dữ liệu giả lập giống ảnh
        taskList = new ArrayList<>();
        taskList.add(new Task("Hoàn thành báo cáo tháng 1", "Cao", "Tổng hợp báo cáo tài chính và nhân sự tháng 1/2026", "Nguyễn Văn A", "Hạn: 25/01/2026", "Đang thực hiện"));
        taskList.add(new Task("Thiết kế giao diện website mới", "Trung bình", "Thiết kế UI/UX cho trang chủ và các trang chính", "Hoàng Văn E", "Hạn: 30/01/2026", "Đang thực hiện"));
        taskList.add(new Task("Cập nhật hệ thống quản lý", "Cao", "Cập nhật các tính năng mới cho hệ thống nội bộ", "Nguyễn Văn A", "Hạn: 22/01/2026", "Chưa bắt đầu"));

        adapter = new TaskAdapter(taskList);
        recyclerView.setAdapter(adapter);
    }
}
