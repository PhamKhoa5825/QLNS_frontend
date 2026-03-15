package com.example.myapplication.ui;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.adapter.AttendanceHistoryAdapter;
import com.example.myapplication.model.AttendanceModels;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.appbar.MaterialToolbar;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TimekeepingActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private TextView tvCurrentTime, tvCurrentDate;
    private MaterialButton btnCheckIn, btnCheckOut;
    private View layoutChamCong, layoutLichSu;
    private RecyclerView recyclerViewHistory;
    private TabLayout tabLayout;
    private MaterialToolbar topAppBar;

    private FusedLocationProviderClient fusedLocationClient;
    private ApiService apiService;
    private SharedPreferences prefs;
    private Long employeeId;
    private Handler timeHandler = new Handler();
    private AttendanceHistoryAdapter historyAdapter;

    private enum PendingAction { NONE, CHECK_IN, CHECK_OUT }
    private PendingAction pendingAction = PendingAction.NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timekeeping);

        prefs = getSharedPreferences("qlns_pref", MODE_PRIVATE);
        employeeId = prefs.getLong("employeeId", -1);

        apiService = RetrofitClient.getClient().create(ApiService.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        bindViews();
        setupTabs();
        startClock();
        loadHistory();
    }

    private void bindViews() {
        tvCurrentTime       = findViewById(R.id.tvCurrentTime);
        tvCurrentDate       = findViewById(R.id.tvCurrentDate);
        btnCheckIn          = findViewById(R.id.btnCheckIn);
        btnCheckOut         = findViewById(R.id.btnCheckOut);
        layoutChamCong      = findViewById(R.id.layoutChamCong);
        layoutLichSu        = findViewById(R.id.layoutLichSu);
        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        tabLayout           = findViewById(R.id.tabLayoutTimekeeping);
        topAppBar           = findViewById(R.id.topAppBar);

        if (topAppBar != null) {
            topAppBar.setNavigationOnClickListener(v -> finish());
        }

        btnCheckIn.setOnClickListener(v -> requestLocationAndCheckIn());
        btnCheckOut.setOnClickListener(v -> requestLocationAndCheckOut());

        historyAdapter = new AttendanceHistoryAdapter(new ArrayList<>());
        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewHistory.setAdapter(historyAdapter);
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    layoutChamCong.setVisibility(View.VISIBLE);
                    layoutLichSu.setVisibility(View.GONE);
                } else {
                    layoutChamCong.setVisibility(View.GONE);
                    layoutLichSu.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void startClock() {
        timeHandler.post(new Runnable() {
            @Override
            public void run() {
                String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                String date = new SimpleDateFormat("EEEE, dd 'tháng' MM, yyyy",
                        new Locale("vi")).format(new Date());
                if (tvCurrentTime != null) tvCurrentTime.setText(time);
                if (tvCurrentDate != null) {
                    String cap = date.substring(0,1).toUpperCase() + date.substring(1);
                    tvCurrentDate.setText(cap);
                }
                timeHandler.postDelayed(this, 30_000);
            }
        });
    }

    private void requestLocationAndCheckIn() {
        pendingAction = PendingAction.CHECK_IN;
        checkLocationPermission();
    }

    private void requestLocationAndCheckOut() {
        pendingAction = PendingAction.CHECK_OUT;
        checkLocationPermission();
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        } else {
            executeWithLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                executeWithLocation();
            } else {
                Toast.makeText(this, "Cần quyền vị trí để chấm công", Toast.LENGTH_SHORT).show();
                pendingAction = PendingAction.NONE;
            }
        }
    }

    private void executeWithLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            double lat = 10.7765, lng = 106.7005;
            if (location != null) {
                lat = location.getLatitude();
                lng = location.getLongitude();
            }

            if (pendingAction == PendingAction.CHECK_IN) {
                doCheckIn(lat, lng);
            } else if (pendingAction == PendingAction.CHECK_OUT) {
                doCheckOut();
            }
            pendingAction = PendingAction.NONE;
        });
    }

    private void doCheckIn(double lat, double lng) {
        btnCheckIn.setEnabled(false);
        btnCheckIn.setText("Đang xử lý...");

        apiService.checkIn(new AttendanceModels.CheckInRequest(employeeId, lat, lng))
                .enqueue(new Callback<AttendanceModels.AttendanceResponse>() {
                    @Override
                    public void onResponse(Call<AttendanceModels.AttendanceResponse> call,
                                           Response<AttendanceModels.AttendanceResponse> response) {
                        btnCheckIn.setEnabled(true);
                        btnCheckIn.setText("Chấm công vào");
                        if (response.isSuccessful() && response.body() != null) {
                            AttendanceModels.AttendanceResponse att = response.body();
                            String msg = "Đã chấm công vào lúc " + att.checkIn;
                            if ("LATE".equals(att.status)) {
                                msg += "\n⚠️ Đi muộn " + att.lateMinutes + " phút";
                            }
                            Toast.makeText(TimekeepingActivity.this, msg, Toast.LENGTH_LONG).show();
                            loadHistory();
                        } else {
                            String err = response.code() == 400 ? "Đã chấm công hôm nay rồi" : "Lỗi chấm công";
                            Toast.makeText(TimekeepingActivity.this, err, Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<AttendanceModels.AttendanceResponse> call, Throwable t) {
                        btnCheckIn.setEnabled(true);
                        btnCheckIn.setText("Chấm công vào");
                        Toast.makeText(TimekeepingActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void doCheckOut() {
        btnCheckOut.setEnabled(false);
        btnCheckOut.setText("Đang xử lý...");

        apiService.checkOut(new AttendanceModels.CheckOutRequest(employeeId))
                .enqueue(new Callback<AttendanceModels.AttendanceResponse>() {
                    @Override
                    public void onResponse(Call<AttendanceModels.AttendanceResponse> call,
                                           Response<AttendanceModels.AttendanceResponse> response) {
                        btnCheckOut.setEnabled(true);
                        btnCheckOut.setText("Chấm công ra");
                        if (response.isSuccessful() && response.body() != null) {
                            AttendanceModels.AttendanceResponse att = response.body();
                            String hours = att.workHours != null
                                    ? String.format(Locale.getDefault(), "%.1f", att.workHours) : "?";
                            Toast.makeText(TimekeepingActivity.this,
                                    "Đã chấm công ra. Tổng: " + hours + " giờ", Toast.LENGTH_LONG).show();
                            loadHistory();
                        } else {
                            String err = response.code() == 400
                                    ? "Chưa đến giờ ra ca hoặc chưa chấm vào" : "Lỗi chấm công ra";
                            Toast.makeText(TimekeepingActivity.this, err, Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<AttendanceModels.AttendanceResponse> call, Throwable t) {
                        btnCheckOut.setEnabled(true);
                        btnCheckOut.setText("Chấm công ra");
                        Toast.makeText(TimekeepingActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadHistory() {
        int month = Integer.parseInt(new SimpleDateFormat("M", Locale.getDefault()).format(new Date()));
        int year  = Integer.parseInt(new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date()));

        apiService.getAttendanceByMonth(employeeId, month, year)
                .enqueue(new Callback<List<AttendanceModels.AttendanceResponse>>() {
                    @Override
                    public void onResponse(Call<List<AttendanceModels.AttendanceResponse>> call,
                                           Response<List<AttendanceModels.AttendanceResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            historyAdapter.updateData(response.body());
                        }
                    }
                    @Override public void onFailure(Call<List<AttendanceModels.AttendanceResponse>> call, Throwable t) {}
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timeHandler.removeCallbacksAndMessages(null);
    }
}