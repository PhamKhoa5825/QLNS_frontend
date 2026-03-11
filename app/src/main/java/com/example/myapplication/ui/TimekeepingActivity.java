package com.example.myapplication.ui;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.AttendanceAdapter;
import com.example.myapplication.model.Attendance;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.SharedPrefsManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TimekeepingActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvCurrentTime, tvCurrentDate, tabDepartment;
    
    // We reuse a generic RecyclerView here just for demonstration if we want.
    // In actual XML we didn't add the RecyclerView yet, but let's assume we replace the history section with it dynamically
    // For now, let's just show a Toast that we are fetching Department Attendance.
    
    private Long currentDeptId; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timekeeping);

        currentDeptId = SharedPrefsManager.getInstance(this).getDepartmentId();

        btnBack = findViewById(R.id.btnBackTimekeeping);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        tabDepartment = findViewById(R.id.tabDepartment);

        btnBack.setOnClickListener(v -> finish());
        
        // Update Time
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        String date = new SimpleDateFormat("EEEE, dd MMMM, yyyy", new Locale("vi", "VN")).format(new Date());
        tvCurrentTime.setText(time);
        tvCurrentDate.setText(date);
        
        tabDepartment.setOnClickListener(v -> {
            fetchDepartmentAttendance();
        });
    }
    
    private void fetchDepartmentAttendance() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        ApiService apiService = RetrofitClient.getApiService(this);
        Call<List<Attendance>> call = apiService.getDepartmentAttendanceByDate(currentDeptId, today);
        call.enqueue(new Callback<List<Attendance>>() {
            @Override
            public void onResponse(Call<List<Attendance>> call, Response<List<Attendance>> response) {
                if(response.isSuccessful() && response.body() != null) {
                    List<Attendance> list = response.body();
                    Toast.makeText(TimekeepingActivity.this, "Đã tải " + list.size() + " người chấm công hôm nay", Toast.LENGTH_SHORT).show();
                    // Setup Adapter to a RecyclerView if it exists
                } else {
                    Toast.makeText(TimekeepingActivity.this, "Không có dữ liệu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Attendance>> call, Throwable t) {
                Toast.makeText(TimekeepingActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
