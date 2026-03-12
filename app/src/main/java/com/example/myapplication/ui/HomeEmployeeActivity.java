package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.R;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.viewmodel.EmployeeViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeEmployeeActivity extends AppCompatActivity {

    private TextView tvUserName, tvAvatarInitials, tvCurrentTime, tvCurrentDate;
    private EmployeeViewModel viewModel;
    private final Handler timeHandler = new Handler(Looper.getMainLooper());
    private Runnable timeRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 1. Khởi tạo môi trường mạng
        RetrofitClient.init(this);

        // 2. Lấy ViewModel ngay lập tức
        viewModel = new ViewModelProvider(this).get(EmployeeViewModel.class);

        // 3. Đăng ký các "đài quan sát" (Observers)
        observeViewModel();

        // 4. Ra lệnh cho ViewModel kiểm tra quyền truy cập
//        viewModel.checkAuth();
        //Tạm thời chạy thẳng vào home
        setupUI();
    }

    private void setupUI() {
        setContentView(R.layout.activity_home_employee);
        initViews();
        startClock();
        
        // Chỉ khi UI đã sẵn sàng mới tải profile
        viewModel.loadMyProfile();
    }

    private void observeViewModel() {
        // Lắng nghe trạng thái đăng nhập từ ViewModel
        viewModel.isAuthorized.observe(this, isAuthorized -> {
            if (Boolean.FALSE.equals(isAuthorized)) {
                redirectToLogin();
            } else if (Boolean.TRUE.equals(isAuthorized)) {
                setupUI();
            }
        });

        // Lắng nghe dữ liệu profile
        viewModel.userProfile.observe(this, user -> {
            if (user != null && tvUserName != null) {
                tvUserName.setText(user.getFullName());
                tvAvatarInitials.setText(user.getAvatarText());
            }
        });

        // Lắng nghe thông báo lỗi
        viewModel.errorMessage.observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void redirectToLogin() {
        // Intent intent = new Intent(this, LoginActivity.class);
        // startActivity(intent);
        // finish();
        Toast.makeText(this, "Vui lòng đăng nhập để tiếp tục", Toast.LENGTH_SHORT).show();
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tvUserName);
        tvAvatarInitials = findViewById(R.id.tvAvatarInitials);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);

        // Nút Check-in GPS
        View btnCheckIn = findViewById(R.id.btnCheckIn);
        if (btnCheckIn != null) {
            btnCheckIn.setOnClickListener(v -> {
                Intent intent = new Intent(HomeEmployeeActivity.this, GPSCheckInActivity.class);
                startActivity(intent);
            });
        }

        // Nút Thông báo (Bell icon)
        View btnNotification = findViewById(R.id.btnNotification);
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> {
                Intent intent = new Intent(HomeEmployeeActivity.this, NotificationCenterActivity.class);
                startActivity(intent);
            });
        }

        // Xử lý các card trong menuGrid
        ViewGroup menuGrid = findViewById(R.id.menuGrid);
        if (menuGrid != null) {
            // Card 1: Xin nghỉ phép (Chưa có activity nên tạm thời để Toast hoặc trỏ tới LeaveApplicationActivity nếu có)
             View leaveCard = menuGrid.getChildAt(0);
             if (leaveCard != null) {
                 leaveCard.setOnClickListener(v -> {
                     Intent intent = new Intent(HomeEmployeeActivity.this, LeaveApplicationActivity.class);
                     startActivity(intent);
                 });
             }

             // Card 2: Công việc -> TaskManagementActivity
             View taskCard = menuGrid.getChildAt(1);
             if (taskCard != null) {
                 taskCard.setOnClickListener(v -> {
                     Intent intent = new Intent(HomeEmployeeActivity.this, TaskManagementActivity.class);
                     startActivity(intent);
                 });
             }

             // Card 3: Thông báo -> InternalMessageActivity
             View messageCard = menuGrid.getChildAt(2);
             if (messageCard != null) {
                 messageCard.setOnClickListener(v -> {
                     Intent intent = new Intent(HomeEmployeeActivity.this, InternalMessageActivity.class);
                     startActivity(intent);
                 });
             }
        }

        // Click vào Avatar để vào Hồ sơ cá nhân
        View imgAvatar = findViewById(R.id.imgAvatar);
        if (imgAvatar != null) {
            imgAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(HomeEmployeeActivity.this, ProfileActivity.class);
                startActivity(intent);
            });
        }

        // Xử lý Bottom Navigation
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home); // Mặc định là Trang chủ
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    return true;
                } else if (itemId == R.id.nav_work) {
                    Intent intent = new Intent(HomeEmployeeActivity.this, TaskManagementActivity.class);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_message) {
                    Intent intent = new Intent(HomeEmployeeActivity.this, InternalMessageActivity.class);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    Intent intent = new Intent(HomeEmployeeActivity.this, ProfileActivity.class);
                    startActivity(intent);
                    return true;
                }
                return false;
            });
        }
    }

    private void startClock() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd 'tháng' M, yyyy", new Locale("vi", "VN"));

        timeRunnable = new Runnable() {
            @Override
            public void run() {
                Date now = new Date();
                if (tvCurrentTime != null) tvCurrentTime.setText(timeFormat.format(now));
                if (tvCurrentDate != null) tvCurrentDate.setText(dateFormat.format(now));
                timeHandler.postDelayed(this, 1000);
            }
        };
        timeHandler.post(timeRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timeHandler != null && timeRunnable != null) {
            timeHandler.removeCallbacks(timeRunnable);
        }
    }
}
