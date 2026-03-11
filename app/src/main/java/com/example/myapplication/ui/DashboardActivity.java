package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

public class DashboardActivity extends AppCompatActivity {

    private LinearLayout btnNavEmployee, btnNavTimekeeping, btnNavDepartment, btnNavChat, btnNavTask, btnNavNotification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        btnNavEmployee = findViewById(R.id.btnNavEmployee);
        btnNavTimekeeping = findViewById(R.id.btnNavTimekeeping);
        btnNavDepartment = findViewById(R.id.btnNavDepartment);
        btnNavChat = findViewById(R.id.btnNavChat);
        btnNavTask = findViewById(R.id.btnNavTask);
        btnNavNotification = findViewById(R.id.btnNavNotification);

        btnNavEmployee.setOnClickListener(v -> startActivity(new Intent(this, EmployeeActivity.class)));
        btnNavDepartment.setOnClickListener(v -> startActivity(new Intent(this, DepartmentActivity.class)));
    }
}
