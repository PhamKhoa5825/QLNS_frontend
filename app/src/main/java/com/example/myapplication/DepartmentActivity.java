package com.example.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class DepartmentActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DepartmentAdapter adapter;
    private List<Department> departmentList;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_department_demo);

        btnBack = findViewById(R.id.btnBackDept);
        btnBack.setOnClickListener(v -> finish()); // Nút Back

        recyclerView = findViewById(R.id.recyclerViewDept);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Nạp dữ liệu giả lập (Demo theo ảnh thiết kế)
        departmentList = new ArrayList<>();
        departmentList.add(new Department("Công nghệ thông tin", "Nguyễn Văn A", 25, 92, Color.parseColor("#2563EB"))); // Xanh dương
        departmentList.add(new Department("Marketing", "Trần Thị B", 18, 88, Color.parseColor("#EC4899"))); // Hồng
        departmentList.add(new Department("Kế toán", "Lê Văn C", 12, 95, Color.parseColor("#10B981"))); // Xanh lá
        departmentList.add(new Department("Nhân sự", "Phạm Thị D", 10, 90, Color.parseColor("#A855F7"))); // Tím

        adapter = new DepartmentAdapter(departmentList);
        recyclerView.setAdapter(adapter);
    }
}