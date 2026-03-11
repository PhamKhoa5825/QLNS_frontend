package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

public class DashboardActivity extends AppCompatActivity {

    private LinearLayout btnNavEmployee, btnNavTimekeeping, btnNavDepartment, btnNavChat, btnNavTask, btnNavNotification;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        btnNavEmployee = findViewById(R.id.btnNavEmployee);
        btnNavTimekeeping = findViewById(R.id.btnNavTimekeeping);
        btnNavDepartment = findViewById(R.id.btnNavDepartment);
        btnNavChat = findViewById(R.id.btnNavChat); // Thêm ánh xạ Chat
        btnNavTask = findViewById(R.id.btnNavTask);
        btnNavNotification = findViewById(R.id.btnNavNotification);

        // Các nút đã có
        btnNavEmployee.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, EmployeeActivity.class)));
//        btnNavTimekeeping.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, TimekeepingActivity.class)));
        btnNavDepartment.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, DepartmentActivity.class)));

        // Chuyển sang màn hình Tin nhắn (Chat)
//        btnNavChat.setOnClickListener(v -> {
//            Intent intent = new Intent(DashboardActivity.this, ChatActivity.class);
//            startActivity(intent);
//        });
//
//        btnNavTask.setOnClickListener(v -> {
//            Intent intent = new Intent(DashboardActivity.this, TaskActivity.class);
//            startActivity(intent);
//        });
//
//        btnNavNotification.setOnClickListener(v -> {
//            Intent intent = new Intent(DashboardActivity.this, NotificationActivity.class);
//            startActivity(intent);
//        });


    }
}