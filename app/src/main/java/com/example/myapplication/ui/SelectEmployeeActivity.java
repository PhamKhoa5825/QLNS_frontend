package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.EmployeeAdapter;
import com.example.myapplication.model.ChatRoom;
import com.example.myapplication.model.Employee;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.SharedPrefsManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SelectEmployeeActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EmployeeAdapter adapter;
    private List<Employee> employeeList = new ArrayList<>();
    private Long currentUserId;
    private Long currentDeptId;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_employee);

        currentUserId = SharedPrefsManager.getInstance(this).getEmployeeId();
        currentDeptId = SharedPrefsManager.getInstance(this).getDepartmentId();
        apiService = RetrofitClient.getApiService();

        ImageView btnBack = findViewById(R.id.btnBackSelectEmp);
        btnBack.setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.rvSelectEmployee);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new EmployeeAdapter(employeeList, employee -> {
            onEmployeeSelected(employee);
        });
        recyclerView.setAdapter(adapter);

        fetchEmployees();
    }

    private void fetchEmployees() {
        apiService.getEmployeesByDepartmentId(currentDeptId).enqueue(new Callback<List<Employee>>() {
            @Override
            public void onResponse(Call<List<Employee>> call, Response<List<Employee>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    employeeList.clear();
                    for (Employee emp : response.body()) {
                        if (!emp.getId().equals(currentUserId)) {
                            employeeList.add(emp);
                        }
                    }
                    adapter.setData(employeeList);
                }
            }

            @Override
            public void onFailure(Call<List<Employee>> call, Throwable t) {
                Toast.makeText(SelectEmployeeActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onEmployeeSelected(Employee employee) {
        apiService.getOrCreatePrivateRoom(currentUserId, employee.getId()).enqueue(new Callback<ChatRoom>() {
            @Override
            public void onResponse(Call<ChatRoom> call, Response<ChatRoom> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Intent intent = new Intent(SelectEmployeeActivity.this, MessageActivity.class);
                    intent.putExtra("ROOM_ID", response.body().getId());
                    intent.putExtra("ROOM_NAME", employee.getFullName());
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(SelectEmployeeActivity.this, "Lỗi khởi tạo phòng chat", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ChatRoom> call, Throwable t) {
                Toast.makeText(SelectEmployeeActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
