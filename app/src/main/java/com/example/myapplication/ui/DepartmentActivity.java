package com.example.myapplication.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.DepartmentAdapter;
import com.example.myapplication.viewmodel.DepartmentViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class DepartmentActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DepartmentAdapter adapter;
    private ProgressBar progressBar;
    private FloatingActionButton fabAdd;
    private DepartmentViewModel viewModel;

    private static final String PREF_NAME = "qlns_pref";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_department_demo);

        viewModel = new ViewModelProvider(this).get(DepartmentViewModel.class);

        initViews();
        observeViewModel();

        viewModel.loadDepartments();
    }

    private void initViews() {
        findViewById(R.id.btnBackDept).setOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressBar);
        fabAdd      = findViewById(R.id.fabAddDept);

        recyclerView = findViewById(R.id.recyclerViewDept);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DepartmentAdapter(new java.util.ArrayList<>(), dept -> {
            Toast.makeText(this, "Phòng: " + dept.getName(), Toast.LENGTH_SHORT).show();
        });
        recyclerView.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String role = prefs.getString("role", "EMPLOYEE");
        if ("ADMIN".equals(role)) {
            fabAdd.setVisibility(View.VISIBLE);
            fabAdd.setOnClickListener(v -> showAddDepartmentDialog());
        } else {
            fabAdd.setVisibility(View.GONE);
        }
    }

    private void observeViewModel() {
        viewModel.departments.observe(this, list -> {
            if (list != null) {
                adapter.setData(list);
            }
        });

        viewModel.isLoading.observe(this, isLoading -> {
            if (progressBar != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.errorMessage.observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddDepartmentDialog() {
        Toast.makeText(this, "Tính năng thêm phòng ban", Toast.LENGTH_SHORT).show();
    }
}
