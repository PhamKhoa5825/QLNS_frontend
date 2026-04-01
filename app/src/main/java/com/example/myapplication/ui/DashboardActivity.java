package com.example.myapplication.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.R;
import com.example.myapplication.model.Attendance;
import com.example.myapplication.model.CompanySettings;
import com.example.myapplication.model.DepartmentDashboardDTO;
import com.example.myapplication.model.Employee;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.service.ChatForegroundService;
import com.example.myapplication.utils.BottomNavHelper;
import com.example.myapplication.utils.SharedPrefsManager;
import com.example.myapplication.utils.TopBarHelper;
import com.example.myapplication.viewmodel.AttendanceViewModel;
import com.example.myapplication.model.Request;
import com.bumptech.glide.Glide;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.app.Dialog;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import com.example.myapplication.model.CreateRequestRequest;
import com.example.myapplication.model.LeaveSession;
import com.example.myapplication.model.Request;
import com.example.myapplication.model.RequestDetail;
import com.example.myapplication.model.RequestType;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Date;
import java.text.SimpleDateFormat;

public class DashboardActivity extends AppCompatActivity {

    private View btnCheckInGPS;
    private TextView tvCheckInStatus, tvTimekeepingOnTime, tvTimekeepingLate;
    private View layoutCompanySettingsCard, layoutAdminTools, layoutAdminStats;
    private View btnAdminAddEmployee, btnAdminAccounts, btnAdminLogs, btnAdminBackup, btnAdminPayroll;
    private TextView tvCompanyName, tvWorkHours, tvRadius, tvLocation;
    private TextView tvTotalEmployees, tvTotalDepartments, tvPendingRequests, tvOpenTasks, tvTodayAttendanceAdmin;
    
    // Employee Dashboard Widgets
    private TextView tvRemainingLeave, tvWorkStats, btnQuickRequestLeave, btnViewAttendanceHistory;
    private View cardLeaveBalance, cardAdminEmployees, cardAdminDepartments, cardAdminRequests, cardAdminTasks, cardAdminAttendance;
    private View layoutTodayTasks;
    private View cardTodayTask1, cardTodayTask2;
    private TextView tvTodayTaskTitle1, tvTodayTaskDeadline1;
    private TextView tvTodayTaskTitle2, tvTodayTaskDeadline2;
    private TextView tvNoTasksToday;
    private View cardAttendanceRate;
    private View layoutManagerRequests;
    private View cardManagerRequest1, cardManagerRequest2;
    private TextView tvManagerRequestAvatar1, tvManagerRequestName1, tvManagerRequestType1, tvManagerRequestDate1;
    private ImageView ivManagerRequestAvatar1;
    private TextView tvManagerRequestAvatar2, tvManagerRequestName2, tvManagerRequestType2, tvManagerRequestDate2;
    private ImageView ivManagerRequestAvatar2;
    private TextView tvTasksDone, tvTasksTotal;
    private TextView tvStatsHeader;
    private View layoutStatsCards, layoutQuickAccess;
    private View indicatorAttendance, indicatorTasks;
    private AttendanceViewModel attendanceViewModel;
    private boolean isCheckInAction = true;

    private Long currentDeptId;
    private static final int NOTIFICATION_PERMISSION_CODE = 123;
    private static final int PICK_IMAGE_REQUEST = 100;

    private Long currentEmployeeId;
    private String selectedEvidencePath;
    private TextView dialogTvPath;
    private ImageView dialogIvPreview;
    private int selectedMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
    private int selectedYear = Calendar.getInstance().get(Calendar.YEAR);
    
    private android.content.BroadcastReceiver systemNotificationReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        checkNotificationPermission();
        startBackgroundService();

        currentDeptId = SharedPrefsManager.getInstance(this).getDepartmentId();

        attendanceViewModel = new ViewModelProvider(this).get(AttendanceViewModel.class);

        currentEmployeeId = SharedPrefsManager.getInstance(this).getEmployeeId();

        initViews();
        setupObservers();
        updateTopBar();
        fetchDashboardStats();
        BottomNavHelper.setupBottomNav(this, R.id.nav_home);
        setupNotificationReceiver();
    }

    private void setupNotificationReceiver() {
        systemNotificationReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String title = intent.getStringExtra("title");
                if (title != null) {
                    Toast.makeText(DashboardActivity.this, "Hệ thống: " + title, Toast.LENGTH_LONG).show();
                    fetchDashboardStats();
                }
            }
        };
        android.content.IntentFilter filter = new android.content.IntentFilter("com.example.myapplication.SYSTEM_NOTIFICATION");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(systemNotificationReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(systemNotificationReceiver, filter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAttendanceStatus();
        updateTopBar();
        
        String role = SharedPrefsManager.getInstance(this).getRole();
        if ("EMPLOYEE".equals(role) || "MANAGER".equals(role)) {
            fetchEmployeeDashboardData();
        }
    }

    private void refreshAttendanceStatus() {
        Long empId = SharedPrefsManager.getInstance(this).getEmployeeId();
        if (empId != -1L) {
            attendanceViewModel.fetchTodayAttendance(empId);
        }
    }

    private void setupObservers() {
        attendanceViewModel.todayAttendance.observe(this, attendance -> {
            if (attendance == null) {
                isCheckInAction = true;
                if (tvCheckInStatus != null) tvCheckInStatus.setText("Bấm để Check-in");
                if (btnCheckInGPS != null) btnCheckInGPS.setEnabled(true);
            } else if (attendance.getCheckIn() != null && attendance.getCheckOut() == null) {
                isCheckInAction = false;
                if (tvCheckInStatus != null) tvCheckInStatus.setText("Bấm để Check-out");
                if (btnCheckInGPS != null) btnCheckInGPS.setEnabled(true);
            } else if (attendance.getCheckOut() != null) {
                if (tvCheckInStatus != null) tvCheckInStatus.setText("Đã hoàn thành chấm công");
                if (btnCheckInGPS != null) btnCheckInGPS.setEnabled(false); // Hoặc giữ để xem lại
            }
        });
    }

    private void initViews() {
        btnCheckInGPS = findViewById(R.id.btnCheckInGPS);
        tvCheckInStatus = findViewById(R.id.tvCheckInStatus);
        tvTimekeepingOnTime = findViewById(R.id.tvTimekeepingOnTime);
        tvTimekeepingLate = findViewById(R.id.tvTimekeepingLate);

        layoutCompanySettingsCard = findViewById(R.id.layoutCompanySettingsCard);
        tvCompanyName = findViewById(R.id.tvCompanyName);
        tvWorkHours = findViewById(R.id.tvWorkHours);
        tvRadius = findViewById(R.id.tvRadius);
        tvLocation = findViewById(R.id.tvLocation);
        View cardCompany = findViewById(R.id.cardCompany);
        
        layoutAdminTools = findViewById(R.id.layoutAdminTools);
        btnAdminAddEmployee = findViewById(R.id.btnAdminAddEmployee);
        btnAdminAccounts = findViewById(R.id.btnAdminAccounts);
        btnAdminLogs = findViewById(R.id.btnAdminLogs);
        btnAdminBackup = findViewById(R.id.btnAdminBackup);
        btnAdminPayroll = findViewById(R.id.btnAdminPayroll);

        layoutAdminStats = findViewById(R.id.layoutAdminStats);
        cardAdminEmployees = findViewById(R.id.cardAdminEmployees);
        cardAdminDepartments = findViewById(R.id.cardAdminDepartments);
        cardAdminRequests = findViewById(R.id.cardAdminRequests);
        cardAdminTasks = findViewById(R.id.cardAdminTasks);
        cardAdminAttendance = findViewById(R.id.cardAdminAttendance);

        tvTotalEmployees = findViewById(R.id.tvTotalEmployees);
        tvTotalDepartments = findViewById(R.id.tvTotalDepartments);
        tvPendingRequests = findViewById(R.id.tvPendingRequests);
        tvOpenTasks = findViewById(R.id.tvOpenTasks);
        tvTodayAttendanceAdmin = findViewById(R.id.tvTodayAttendanceAdmin);

        cardAttendanceRate = findViewById(R.id.cardAttendanceRate);
        tvTasksDone = findViewById(R.id.tvTasksDone);
        tvTasksTotal = findViewById(R.id.tvTasksTotal);
        tvStatsHeader = findViewById(R.id.tvStatsHeader);
        layoutStatsCards = findViewById(R.id.layoutStatsCards);
        layoutQuickAccess = findViewById(R.id.layoutQuickAccess);
        indicatorAttendance = findViewById(R.id.indicatorAttendance);
        indicatorTasks = findViewById(R.id.indicatorTasks);
        
        String dName = SharedPrefsManager.getInstance(this).getDepartmentName();
        if (dName == null || dName.isEmpty()) dName = "Phòng Ban";
        if (tvStatsHeader != null) tvStatsHeader.setText("Thống kê - " + dName);
        cardLeaveBalance = findViewById(R.id.cardLeaveBalance);
        tvRemainingLeave = findViewById(R.id.tvRemainingLeave);
        tvWorkStats = findViewById(R.id.tvWorkStats);
        btnQuickRequestLeave = findViewById(R.id.btnQuickRequestLeave);
        btnViewAttendanceHistory = findViewById(R.id.btnViewAttendanceHistory);
        layoutTodayTasks = findViewById(R.id.layoutTodayTasks);
        cardTodayTask1 = findViewById(R.id.cardTodayTask1);
        cardTodayTask2 = findViewById(R.id.cardTodayTask2);
        tvTodayTaskTitle1 = findViewById(R.id.tvTodayTaskTitle1);
        tvTodayTaskDeadline1 = findViewById(R.id.tvTodayTaskDeadline1);
        tvTodayTaskTitle2 = findViewById(R.id.tvTodayTaskTitle2);
        tvTodayTaskDeadline2 = findViewById(R.id.tvTodayTaskDeadline2);
        tvNoTasksToday = findViewById(R.id.tvNoTasksToday);

        layoutManagerRequests = findViewById(R.id.layoutManagerRequests);
        cardManagerRequest1 = findViewById(R.id.cardManagerRequest1);
        cardManagerRequest2 = findViewById(R.id.cardManagerRequest2);
        tvManagerRequestAvatar1 = findViewById(R.id.tvManagerRequestAvatar1);
        ivManagerRequestAvatar1 = findViewById(R.id.ivManagerRequestAvatar1);
        tvManagerRequestName1 = findViewById(R.id.tvManagerRequestName1);
        tvManagerRequestType1 = findViewById(R.id.tvManagerRequestType1);
        tvManagerRequestDate1 = findViewById(R.id.tvManagerRequestDate1);
        tvManagerRequestAvatar2 = findViewById(R.id.tvManagerRequestAvatar2);
        ivManagerRequestAvatar2 = findViewById(R.id.ivManagerRequestAvatar2);
        tvManagerRequestName2 = findViewById(R.id.tvManagerRequestName2);
        tvManagerRequestType2 = findViewById(R.id.tvManagerRequestType2);
        tvManagerRequestDate2 = findViewById(R.id.tvManagerRequestDate2);

        if (btnQuickRequestLeave != null) {
            btnQuickRequestLeave.setOnClickListener(v -> showCreateRequestDialog());
        }

        View btnQuickOT = findViewById(R.id.btnQuickOT);
        if (btnQuickOT != null) {
            btnQuickOT.setOnClickListener(v -> showCreateRequestDialog("OVERTIME"));
        }

        View btnQuickTrip = findViewById(R.id.btnQuickTrip);
        if (btnQuickTrip != null) {
            btnQuickTrip.setOnClickListener(v -> showCreateRequestDialog("BUSINESS_TRIP"));
        }

        View btnQuickSick = findViewById(R.id.btnQuickSick);
        if (btnQuickSick != null) {
            btnQuickSick.setOnClickListener(v -> showCreateRequestDialog("SICK_LEAVE"));
        }

        View btnQuickCorrection = findViewById(R.id.btnQuickCorrection);
        if (btnQuickCorrection != null) {
            btnQuickCorrection.setOnClickListener(v -> showCreateRequestDialog("PUNCH_CORRECTION"));
        }

        View btnQuickResign = findViewById(R.id.btnQuickResign);
        if (btnQuickResign != null) {
            btnQuickResign.setOnClickListener(v -> showCreateRequestDialog("RESIGNATION"));
        }
        if (btnViewAttendanceHistory != null) {
            btnViewAttendanceHistory.setOnClickListener(v -> startActivity(new Intent(this, AttendanceActivity.class)));
        }

        if (cardCompany != null) {
            cardCompany.setOnClickListener(v -> startActivity(new Intent(this, AdminSettingsActivity.class)));
        }
        if (btnAdminAddEmployee != null) btnAdminAddEmployee.setOnClickListener(v -> startActivity(new Intent(this, AddEditEmployeeActivity.class)));
        if (btnAdminAccounts != null) btnAdminAccounts.setOnClickListener(v -> startActivity(new Intent(this, AccountManagementActivity.class)));
        if (btnAdminLogs != null) btnAdminLogs.setOnClickListener(v -> startActivity(new Intent(this, SystemLogActivity.class)));
        if (btnAdminBackup != null) btnAdminBackup.setOnClickListener(v -> startActivity(new Intent(this, BackupActivity.class)));
        if (btnAdminPayroll != null) btnAdminPayroll.setOnClickListener(v -> startActivity(new Intent(this, AdminPayrollActivity.class)));

        if (cardAdminEmployees != null) cardAdminEmployees.setOnClickListener(v -> startActivity(new Intent(this, EmployeeActivity.class)));
        if (cardAdminDepartments != null) cardAdminDepartments.setOnClickListener(v -> startActivity(new Intent(this, DepartmentActivity.class)));
        if (cardAdminRequests != null) cardAdminRequests.setOnClickListener(v -> startActivity(new Intent(this, RequestActivity.class)));
        if (cardAdminTasks != null) cardAdminTasks.setOnClickListener(v -> startActivity(new Intent(this, TaskActivity.class)));
        if (cardAdminAttendance != null) cardAdminAttendance.setOnClickListener(v -> startActivity(new Intent(this, AttendanceActivity.class)));

        if (btnCheckInGPS != null) {
            btnCheckInGPS.setOnClickListener(v -> {
                Intent intent = new Intent(this, GPSCheckInActivity.class);
                intent.putExtra("isCheckInAction", isCheckInAction);
                startActivity(intent);
            });
        }
        
        // Hide items based on role
        String role = SharedPrefsManager.getInstance(this).getRole();
        if ("EMPLOYEE".equals(role)) {
            if (tvStatsHeader != null) tvStatsHeader.setVisibility(View.GONE);
            if (layoutStatsCards != null) layoutStatsCards.setVisibility(View.GONE);
        }
        if ("EMPLOYEE".equals(role) || "MANAGER".equals(role)) {
            if (layoutTodayTasks != null) layoutTodayTasks.setVisibility(View.VISIBLE);
        }
        if ("MANAGER".equals(role) || "ADMIN".equals(role)) {
            if (layoutManagerRequests != null) layoutManagerRequests.setVisibility(View.VISIBLE);
            fetchManagerRequests();
        }
        if ("ADMIN".equals(role)) {
            if (layoutCompanySettingsCard != null) {
                layoutCompanySettingsCard.setVisibility(View.VISIBLE);
                fetchCompanySettings();
            }
            if (layoutAdminStats != null) {
                layoutAdminStats.setVisibility(View.VISIBLE);
                fetchAdminStats();
            }
            if (layoutAdminTools != null) layoutAdminTools.setVisibility(View.VISIBLE);
            if (cardLeaveBalance != null) cardLeaveBalance.setVisibility(View.GONE);
            if (layoutQuickAccess != null) layoutQuickAccess.setVisibility(View.GONE);
            
            // Hide department stats for Admin as requested
            if (tvStatsHeader != null) tvStatsHeader.setVisibility(View.GONE);
            if (layoutStatsCards != null) layoutStatsCards.setVisibility(View.GONE);
        }
    }

    private void fetchEmployeeDashboardData() {
        Long empId = SharedPrefsManager.getInstance(this).getEmployeeId();
        if (empId == -1L) return;

        ApiService apiService = RetrofitClient.getApiService(this);
        
        // Fetch Leave Balance
        apiService.getEmployeeDetail(empId).enqueue(new Callback<Employee>() {
            @Override
            public void onResponse(Call<Employee> call, Response<Employee> response) {
                if(response.isSuccessful() && response.body() != null) {
                    Double used = response.body().getLeaveDaysUsed();
                    Double quota = response.body().getAnnualLeaveQuota();
                    
                    double usedVal = used != null ? used : 0.0;
                    double quotaVal = quota != null ? quota : 12.0;

                    if(tvRemainingLeave != null) {
                        tvRemainingLeave.setText(String.format(java.util.Locale.US, "%.1f / %.0f", usedVal, quotaVal));
                    }
                }
            }
            @Override public void onFailure(Call<Employee> call, Throwable t) {}
        });

        // Fetch Attendance Summary for Current Month
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int month = cal.get(java.util.Calendar.MONTH) + 1;
        int year = cal.get(java.util.Calendar.YEAR);

        apiService.getAttendanceSummary(empId, month, year).enqueue(new Callback<com.example.myapplication.model.AttendanceMonthlyResponse>() {
            @Override
            public void onResponse(Call<com.example.myapplication.model.AttendanceMonthlyResponse> call, Response<com.example.myapplication.model.AttendanceMonthlyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    double worked = 0;
                    if (response.body().getDays() != null) {
                        for (com.example.myapplication.model.AttendanceSummary s : response.body().getDays()) {
                            String status = s.getStatus();
                            if ("PRESENT".equals(status)) worked += 1.0;
                            else if ("PRESENT_PARTIAL".equals(status)) worked += 0.5;
                            else if ("LATE".equals(status)) worked += 1.0;
                            else if ("LATE_PARTIAL".equals(status)) worked += 0.5;
                            else if ("TRIP".equals(status)) worked += 1.0;
                        }
                    }
                    if (tvWorkStats != null) {
                        tvWorkStats.setText(String.format(java.util.Locale.US, "%.1f / %d", worked, response.body().getStandardWorkingDays()));
                    }
                }
            }
            @Override public void onFailure(Call<com.example.myapplication.model.AttendanceMonthlyResponse> call, Throwable t) {}
        });

        // Fetch Today's Tasks
        apiService.getMyTasks(empId).enqueue(new Callback<java.util.List<com.example.myapplication.model.Task>>() {
            @Override
            public void onResponse(Call<java.util.List<com.example.myapplication.model.Task>> call, Response<java.util.List<com.example.myapplication.model.Task>> response) {
                if(response.isSuccessful() && response.body() != null) {
                    java.util.List<com.example.myapplication.model.Task> tasks = response.body();
                    java.util.List<com.example.myapplication.model.Task> activeTasks = new java.util.ArrayList<>();
                    for(com.example.myapplication.model.Task t : tasks) {
                        String status = t.getStatus();
                        if(!"COMPLETED".equals(status) && !"DONE".equals(status) && 
                           !"UNDER_REVIEW".equals(status) && !"CANCELLED".equals(status)) {
                            activeTasks.add(t);
                        }
                    }
                    
                    int totalTasks = tasks.size();
                    int doneTasks = 0;
                    for (com.example.myapplication.model.Task t : tasks) {
                        String s = t.getStatus();
                        if ("DONE".equals(s) || "COMPLETED".equals(s)) {
                            doneTasks++;
                        }
                    }

                    if (tvTasksDone != null) tvTasksDone.setText(String.valueOf(doneTasks));
                    if (tvTasksTotal != null) tvTasksTotal.setText(String.valueOf(totalTasks));
                    updateIndicator(indicatorTasks, doneTasks, totalTasks);

                    if(activeTasks.isEmpty()) {
                        if(tvNoTasksToday != null) tvNoTasksToday.setVisibility(View.VISIBLE);
                        if(cardTodayTask1 != null) cardTodayTask1.setVisibility(View.GONE);
                        if(cardTodayTask2 != null) cardTodayTask2.setVisibility(View.GONE);
                    } else {
                        if(tvNoTasksToday != null) tvNoTasksToday.setVisibility(View.GONE);
                        
                        if(cardTodayTask1 != null) {
                            cardTodayTask1.setVisibility(View.VISIBLE);
                            com.example.myapplication.model.Task t1 = activeTasks.get(0);
                            if(tvTodayTaskTitle1 != null) tvTodayTaskTitle1.setText(t1.getTitle());
                            updateDeadlineBadge(tvTodayTaskDeadline1, t1.getDeadline());
                            cardTodayTask1.setOnClickListener(v -> showTaskDetailDialog(t1));
                        }
                        
                        if(activeTasks.size() > 1 && cardTodayTask2 != null) {
                            cardTodayTask2.setVisibility(View.VISIBLE);
                            com.example.myapplication.model.Task t2 = activeTasks.get(1);
                            if(tvTodayTaskTitle2 != null) tvTodayTaskTitle2.setText(t2.getTitle());
                            updateDeadlineBadge(tvTodayTaskDeadline2, t2.getDeadline());
                            cardTodayTask2.setOnClickListener(v -> showTaskDetailDialog(t2));
                        } else if(cardTodayTask2 != null) {
                            cardTodayTask2.setVisibility(View.GONE);
                        }
                    }
                }
            }
            @Override public void onFailure(Call<java.util.List<com.example.myapplication.model.Task>> call, Throwable t) {}
        });
    }

    private void showTaskDetailDialog(com.example.myapplication.model.Task task) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_task_detail);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView tvAvatar = dialog.findViewById(R.id.tvDetailAvatar);
        TextView tvEmployeeName = dialog.findViewById(R.id.tvDetailEmployeeName);
        TextView tvDept = dialog.findViewById(R.id.tvDetailDept);
        String name = task.getAssignedToName() != null ? task.getAssignedToName() : "N/A";
        tvEmployeeName.setText(name);
        tvAvatar.setText(name.substring(0, 1).toUpperCase());
        tvDept.setText("Nhiệm vụ cá nhân");

        TextView tvTitle = dialog.findViewById(R.id.tvDetailTitle);
        TextView tvStatus = dialog.findViewById(R.id.tvDetailStatus);
        TextView tvPriority = dialog.findViewById(R.id.tvDetailPriority);
        tvTitle.setText(task.getTitle());
        
        String priority = task.getPriority() != null ? task.getPriority() : "MEDIUM";
        String priorityTag = "TRUNG BÌNH";
        if ("HIGH".equals(priority)) priorityTag = "CAO";
        else if ("LOW".equals(priority)) priorityTag = "THẤP";
        tvPriority.setText(priorityTag);

        TextView tvDeadline = dialog.findViewById(R.id.tvDetailDeadline);
        TextView tvCreator = dialog.findViewById(R.id.tvDetailCreator);
        TextView tvDesc = dialog.findViewById(R.id.tvDetailDesc);
        tvDeadline.setText(formatDateTime(task.getDeadline()));
        tvCreator.setText(task.getAssignedByName() != null ? task.getAssignedByName() : "Hệ thống");
        tvDesc.setText(task.getDescription());

        int statusColor = 0xFF3B82F6; int statusBg = 0xFFDBEAFE;
        String status = task.getStatus() != null ? task.getStatus() : "PENDING";
        boolean overdue = isOverdue(task.getDeadline());
        boolean isFinished = "DONE".equals(status) || "COMPLETED".equals(status);
        boolean isSubmitted = "UNDER_REVIEW".equals(status);

        String statusTag = status;
        if (!isFinished && !isSubmitted && overdue) {
            statusTag = "QUÁ HẠN";
            statusColor = 0xFFEF4444; statusBg = 0xFFFEE2E2;
        } else if (isFinished) {
            statusTag = "HOÀN THÀNH";
            statusColor = 0xFF10B981; statusBg = 0xFFD1FAE5;
        } else if ("REJECTED".equals(status)) {
            statusTag = "CẦN LÀM LẠI";
            statusColor = 0xFFEF4444; statusBg = 0xFFFEE2E2;
        } else if ("UNDER_REVIEW".equals(status)) {
            statusTag = "CHỜ DUYỆT";
            statusColor = 0xFFF59E0B; statusBg = 0xFFFEF3C7;
        } else if ("IN_PROGRESS".equals(status) || "ACCEPTED".equals(status)) {
            statusTag = "ĐANG LÀM";
            statusColor = 0xFF8B5CF6; statusBg = 0xFFEDE9FE;
        } else {
            statusTag = "CHỜ NHẬN";
        }
        tvStatus.setText(statusTag);
        tvStatus.setTextColor(statusColor);
        tvStatus.getBackground().setTint(statusBg);

        if ("HIGH".equals(task.getPriority())) {
            tvPriority.setTextColor(0xFFEF4444); tvPriority.getBackground().setTint(0xFFFEE2E2);
        } else if ("LOW".equals(task.getPriority())) {
            tvPriority.setTextColor(0xFF6B7280); tvPriority.getBackground().setTint(0xFFF3F4F6);
        }

        View layoutInput = dialog.findViewById(R.id.layoutDetailActionInput);
        TextView tvLabelInput = dialog.findViewById(R.id.tvLabelActionNote);
        EditText edtNote = dialog.findViewById(R.id.edtDetailNote);
        Button btnPrimary = dialog.findViewById(R.id.btnDetailPrimaryAction);
        Button btnSecondary = dialog.findViewById(R.id.btnDetailSecondaryAction);
        String role = SharedPrefsManager.getInstance(this).getRole();
        boolean isEmp = role != null && role.toUpperCase().contains("EMPLOYEE");
        boolean isMan = role != null && (role.toUpperCase().contains("MANAGER") || role.toUpperCase().contains("ADMIN"));

        if (isEmp) {
            if ("PENDING".equals(status)) {
                btnPrimary.setVisibility(View.VISIBLE); btnPrimary.setText("Nhận nhiệm vụ");
                btnPrimary.setOnClickListener(v -> { dialog.dismiss(); acceptTaskAPI_Dash(task); });
            } else if ("ACCEPTED".equals(status) || "IN_PROGRESS".equals(status) || "REJECTED".equals(status)) {
                layoutInput.setVisibility(View.VISIBLE); tvLabelInput.setText("Báo cáo hoàn thành:");
                btnPrimary.setVisibility(View.VISIBLE); btnPrimary.setText("Gửi duyệt");
                btnPrimary.setOnClickListener(v -> updateTaskStatusAPI_Dash(task, "DONE", edtNote.getText().toString().trim(), dialog));
            }
        } else if (isMan) {
            if ("UNDER_REVIEW".equals(status)) {
                layoutInput.setVisibility(View.VISIBLE); tvLabelInput.setText("Nhận xét:");
                btnPrimary.setVisibility(View.VISIBLE); btnPrimary.setText("Duyệt");
                btnPrimary.setOnClickListener(v -> updateTaskStatusAPI_Dash(task, "DONE", edtNote.getText().toString().trim(), dialog));
                btnSecondary.setVisibility(View.VISIBLE); btnSecondary.setText("Yêu cầu sửa lại");
                btnSecondary.setOnClickListener(v -> updateTaskStatusAPI_Dash(task, "REJECTED", edtNote.getText().toString().trim(), dialog));
            }
        }
        dialog.findViewById(R.id.btnDetailClose).setOnClickListener(v -> dialog.dismiss());
        View btnCloseX = dialog.findViewById(R.id.btnModalClose);
        if (btnCloseX != null) btnCloseX.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void fetchManagerRequests() {
        ApiService apiService = RetrofitClient.getApiService(this);
        String role = SharedPrefsManager.getInstance(this).getRole();
        Call<List<Request>> call;
        
        if ("ADMIN".equals(role)) {
            call = apiService.getAllRequestsByStatus("PENDING", null, null, null, null);
        } else {
            if (currentDeptId == null || currentDeptId == -1L) return;
            call = apiService.getRequestsByDepartmentAndStatus(currentDeptId, "PENDING", null, null, null);
        }

        call.enqueue(new Callback<List<Request>>() {
            @Override
            public void onResponse(Call<List<Request>> call, Response<List<Request>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Request> rawList = response.body();
                    List<Request> pendingList = new ArrayList<>();
                    for (Request r : rawList) {
                        if ("PENDING".equalsIgnoreCase(r.getStatus())) {
                            pendingList.add(r);
                        }
                    }

                    if (pendingList.isEmpty()) {
                        if (layoutManagerRequests != null) layoutManagerRequests.setVisibility(View.GONE);
                        return;
                    }
                    if (layoutManagerRequests != null) layoutManagerRequests.setVisibility(View.VISIBLE);

                    // Bind Item 1
                    Request r1 = pendingList.get(0);
                    if (cardManagerRequest1 != null) {
                        cardManagerRequest1.setVisibility(View.VISIBLE);
                        if (tvManagerRequestName1 != null) tvManagerRequestName1.setText(r1.getEmployeeName() != null ? r1.getEmployeeName() : "NV");
                        
                        // Avatar logic for Item 1
                        String empName1 = r1.getEmployeeName();
                        if (tvManagerRequestAvatar1 != null) {
                            tvManagerRequestAvatar1.setText((empName1 != null ? empName1 : "A").substring(0,1).toUpperCase());
                        }
                        if (ivManagerRequestAvatar1 != null) {
                            String avatarUrl = r1.getEmployeeAvatarUrl();
                            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                String fullUrl = avatarUrl.startsWith("http") ? avatarUrl : RetrofitClient.BASE_URL + avatarUrl;
                                Glide.with(DashboardActivity.this).load(fullUrl).circleCrop().into(ivManagerRequestAvatar1);
                                ivManagerRequestAvatar1.setVisibility(View.VISIBLE);
                                if (tvManagerRequestAvatar1 != null) tvManagerRequestAvatar1.setVisibility(View.GONE);
                            } else {
                                ivManagerRequestAvatar1.setVisibility(View.GONE);
                                if (tvManagerRequestAvatar1 != null) tvManagerRequestAvatar1.setVisibility(View.VISIBLE);
                            }
                        }

                        if (tvManagerRequestType1 != null) tvManagerRequestType1.setText(translateRequestType(r1.getType() != null ? r1.getType().name() : null));
                        if (tvManagerRequestDate1 != null) tvManagerRequestDate1.setText(formatShortDate(r1.getCreatedAt()));
                        cardManagerRequest1.setOnClickListener(v -> showRequestDetailDialog(r1));
                    }

                    // Bind Item 2
                    if (pendingList.size() > 1 && cardManagerRequest2 != null) {
                        Request r2 = pendingList.get(1);
                        cardManagerRequest2.setVisibility(View.VISIBLE);
                        if (tvManagerRequestName2 != null) tvManagerRequestName2.setText(r2.getEmployeeName() != null ? r2.getEmployeeName() : "NV");
                        
                        // Avatar logic for Item 2
                        String empName2 = r2.getEmployeeName();
                        if (tvManagerRequestAvatar2 != null) {
                            tvManagerRequestAvatar2.setText((empName2 != null ? empName2 : "B").substring(0,1).toUpperCase());
                        }
                        if (ivManagerRequestAvatar2 != null) {
                            String avatarUrl = r2.getEmployeeAvatarUrl();
                            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                String fullUrl = avatarUrl.startsWith("http") ? avatarUrl : RetrofitClient.BASE_URL + avatarUrl;
                                Glide.with(DashboardActivity.this).load(fullUrl).circleCrop().into(ivManagerRequestAvatar2);
                                ivManagerRequestAvatar2.setVisibility(View.VISIBLE);
                                if (tvManagerRequestAvatar2 != null) tvManagerRequestAvatar2.setVisibility(View.GONE);
                            } else {
                                ivManagerRequestAvatar2.setVisibility(View.GONE);
                                if (tvManagerRequestAvatar2 != null) tvManagerRequestAvatar2.setVisibility(View.VISIBLE);
                            }
                        }

                        if (tvManagerRequestType2 != null) tvManagerRequestType2.setText(translateRequestType(r2.getType() != null ? r2.getType().name() : null));
                        if (tvManagerRequestDate2 != null) tvManagerRequestDate2.setText(formatShortDate(r2.getCreatedAt()));
                        cardManagerRequest2.setOnClickListener(v -> showRequestDetailDialog(r2));
                    } else if (cardManagerRequest2 != null) {
                        cardManagerRequest2.setVisibility(View.GONE);
                    }
                }
            }
            @Override public void onFailure(Call<List<Request>> call, Throwable t) {}
        });
    }

    private void updateIndicator(View indicator, int part, int total) {
        if (indicator == null || total <= 0) return;
        float ratio = (float) part / total;
        if (ratio > 1) ratio = 1;
        
        final float finalRatio = ratio;
        indicator.post(() -> {
            ViewGroup.LayoutParams lp = indicator.getLayoutParams();
            View parent = (View) indicator.getParent();
            if (parent != null) {
                int maxWidth = parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight();
                lp.width = (int) (maxWidth * finalRatio);
                indicator.setLayoutParams(lp);
            }
        });
    }

    private String translateRequestType(String type) {
        if (type == null) return "Yêu cầu";
        switch (type) {
            case "LEAVE_ANNUAL": return "Nghỉ phép năm";
            case "LEAVE_UNPAID": return "Nghỉ không lương";
            case "OVERTIME": return "Tăng ca";
            case "BUSINESS_TRIP": return "Công tác";
            case "SICK_LEAVE": return "Nghỉ ốm";
            case "PUNCH_CORRECTION": return "Bổ sung công";
            case "RESIGNATION": return "Thôi việc";
            default: return type;
        }
    }

    private String formatShortDate(String raw) {
        if (raw == null || raw.length() < 10) return "--/--";
        try {
            // "yyyy-MM-dd..." -> "dd/MM"
            String[] parts = raw.substring(0, 10).split("-");
            return parts[2] + "/" + parts[1];
        } catch (Exception e) { return "--/--"; }
    }

    private void acceptTaskAPI_Dash(com.example.myapplication.model.Task task) {
        ApiService apiService = RetrofitClient.getApiService(this);
        java.util.Map<String, Long> body = new java.util.HashMap<>();
        body.put("employeeId", SharedPrefsManager.getInstance(this).getEmployeeId());
        apiService.acceptTask(task.getId(), body).enqueue(new Callback<com.example.myapplication.model.Task>() {
            @Override public void onResponse(Call<com.example.myapplication.model.Task> call, Response<com.example.myapplication.model.Task> response) {
                if (response.isSuccessful()) { fetchEmployeeDashboardData(); }
            }
            @Override public void onFailure(Call<com.example.myapplication.model.Task> call, Throwable t) {}
        });
    }

    private void updateTaskStatusAPI_Dash(com.example.myapplication.model.Task task, String status, String note, Dialog dialog) {
        ApiService apiService = RetrofitClient.getApiService(this);
        com.example.myapplication.model.UpdateTaskStatusRequest req = new com.example.myapplication.model.UpdateTaskStatusRequest(status, note);
        Long empId = SharedPrefsManager.getInstance(this).getEmployeeId();
        apiService.updateTaskStatus(task.getId(), req, empId).enqueue(new Callback<com.example.myapplication.model.Task>() {
            @Override public void onResponse(Call<com.example.myapplication.model.Task> call, Response<com.example.myapplication.model.Task> response) {
                if (response.isSuccessful()) { if (dialog != null) dialog.dismiss(); fetchEmployeeDashboardData(); }
            }
            @Override public void onFailure(Call<com.example.myapplication.model.Task> call, Throwable t) {}
        });
    }

    private boolean isOverdue(String deadlineStr) {
        if (deadlineStr == null || deadlineStr.isEmpty()) return false;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            if (deadlineStr.contains("T")) sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", java.util.Locale.US);
            java.util.Date deadline = sdf.parse(deadlineStr.substring(0, Math.min(deadlineStr.length(), 16)));
            return deadline != null && deadline.before(new java.util.Date());
        } catch (Exception e) { return false; }
    }

    @SuppressWarnings("unchecked")
    private void fetchAdminStats() {
        ApiService apiService = RetrofitClient.getApiService(this);
        apiService.getAdminDashboardStats().enqueue(new Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(Call<java.util.Map<String, Object>> call, Response<java.util.Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    java.util.Map<String, Object> stats = response.body();
                    
                    // employees.total
                    if (tvTotalEmployees != null && stats.containsKey("employees")) {
                        java.util.Map<String, Object> empirical = (java.util.Map<String, Object>) stats.get("employees");
                        Object val = empirical != null ? empirical.get("total") : "0";
                        tvTotalEmployees.setText(String.valueOf(val != null ? val : "0"));
                    }
                    // departments.total
                    if (tvTotalDepartments != null && stats.containsKey("departments")) {
                        java.util.Map<String, Object> dData = (java.util.Map<String, Object>) stats.get("departments");
                        Object val = dData != null ? dData.get("total") : "0";
                        tvTotalDepartments.setText(String.valueOf(val != null ? val : "0"));
                    }
                    // requests.pending
                    if (tvPendingRequests != null && stats.containsKey("requests")) {
                        java.util.Map<String, Object> rData = (java.util.Map<String, Object>) stats.get("requests");
                        Object val = rData != null ? rData.get("pending") : "0";
                        tvPendingRequests.setText(String.valueOf(val != null ? val : "0"));
                    }
                    // tasks.pending
                    if (tvOpenTasks != null && stats.containsKey("tasks")) {
                        java.util.Map<String, Object> tData = (java.util.Map<String, Object>) stats.get("tasks");
                        Object val = tData != null ? tData.get("pending") : "0";
                        tvOpenTasks.setText(String.valueOf(val != null ? val : "0"));
                    }
                    // attendanceToday.totalCheckedIn
                    if (tvTodayAttendanceAdmin != null && stats.containsKey("attendanceToday")) {
                        java.util.Map<String, Object> aData = (java.util.Map<String, Object>) stats.get("attendanceToday");
                        Object val = aData != null ? aData.get("totalCheckedIn") : "0";
                        tvTodayAttendanceAdmin.setText(String.valueOf(val != null ? val : "0"));
                    }
                }
            }
            @Override public void onFailure(Call<java.util.Map<String, Object>> call, Throwable t) {}
        });
    }

    private void fetchCompanySettings() {
        ApiService apiService = RetrofitClient.getApiService(this);
        apiService.getCompanySettings().enqueue(new Callback<com.example.myapplication.model.CompanySettings>() {
            @Override
            public void onResponse(Call<com.example.myapplication.model.CompanySettings> call, Response<com.example.myapplication.model.CompanySettings> response) {
                if (response.isSuccessful() && response.body() != null) {
                    com.example.myapplication.model.CompanySettings s = response.body();
                    if (tvCompanyName != null) tvCompanyName.setText(s.companyName != null ? s.companyName : "--");
                    if (tvWorkHours != null) {
                        String time = (s.workStartTime != null ? s.workStartTime : "--") + " → " + 
                                     (s.workEndTime != null ? s.workEndTime : "--");
                        tvWorkHours.setText(time);
                    }
                    if (tvRadius != null) tvRadius.setText(s.allowedRadius != null ? (s.allowedRadius + " m") : "-- m");
                    if (tvLocation != null) {
                        if (s.baseLat != null && s.baseLng != null) {
                            tvLocation.setText(String.format(java.util.Locale.US, "%.5f, %.5f", s.baseLat, s.baseLng));
                        } else {
                            tvLocation.setText("Chưa thiết lập");
                        }
                    }
                }
            }
            @Override public void onFailure(Call<com.example.myapplication.model.CompanySettings> call, Throwable t) {}
        });
    }

    private void fetchDashboardStats() {
        if (currentDeptId == null || currentDeptId == -1L) {
            return;
        }
        ApiService apiService = RetrofitClient.getApiService(this);
        Call<DepartmentDashboardDTO> call = apiService.getDepartmentDashboard(currentDeptId);
        
        call.enqueue(new Callback<DepartmentDashboardDTO>() {
            @Override
            public void onResponse(Call<DepartmentDashboardDTO> call, Response<DepartmentDashboardDTO> response) {
                if(response.isSuccessful() && response.body() != null) {
                    DepartmentDashboardDTO dto = response.body();
                    if (tvTimekeepingOnTime != null) tvTimekeepingOnTime.setText(String.valueOf(dto.getPresentToday()));
                    if (tvTimekeepingLate != null) tvTimekeepingLate.setText(String.valueOf(dto.getLateToday()));

                    // Task Stats
                    int done = dto.getDoneTasks();
                    int total = dto.getDoneTasks() + dto.getInProgressTasks() + dto.getPendingTasks();
                    if (tvTasksDone != null) tvTasksDone.setText(String.valueOf(done));
                    if (tvTasksTotal != null) tvTasksTotal.setText(String.valueOf(total));

                    // Optional: Update indicator widths
                    updateIndicator(indicatorAttendance, dto.getPresentToday(), dto.getPresentToday() + dto.getLateToday());
                    updateIndicator(indicatorTasks, done, total);
                }
            }

            @Override
            public void onFailure(Call<DepartmentDashboardDTO> call, Throwable t) {
                Toast.makeText(DashboardActivity.this, "Lỗi fetch dashboard stats", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateDeadlineBadge(TextView tv, String raw) {
        if (tv == null || raw == null || raw.isEmpty()) {
            if(tv != null) tv.setVisibility(View.GONE);
            return;
        }
        try {
            java.text.SimpleDateFormat inFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", java.util.Locale.getDefault());
            java.util.Date targetDate = inFormat.parse(raw.length() > 16 ? raw.substring(0, 16) : raw);
            if (targetDate == null) {
                tv.setVisibility(View.GONE);
                return;
            }

            long diffMillis = targetDate.getTime() - System.currentTimeMillis();
            boolean isOverdue = diffMillis < 0;
            long absDiff = Math.abs(diffMillis);

            long minutes = absDiff / (1000 * 60);
            long hours = minutes / 60;
            long days = hours / 24;

            String text;
            int bgColorAttr, textColorAttr;

            if (isOverdue) {
                text = (days > 0 ? "Trễ " + days + " ngày" : (hours > 0 ? "Trễ " + hours + "h" : "Trễ " + minutes + "m"));
                bgColorAttr = R.color.error_container; // Mờ đỏ
                textColorAttr = R.color.error; // Đỏ đậm
            } else {
                text = (days > 0 ? "Còn " + days + " ngày" : (hours > 0 ? "Còn " + hours + "h" : "Còn " + minutes + "m"));
                bgColorAttr = R.color.surface_container_high; // Xám nhạt
                textColorAttr = R.color.secondary; // Xám đậm
            }

            tv.setText(text);
            tv.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, bgColorAttr)));
            tv.setTextColor(ContextCompat.getColor(this, textColorAttr));
            tv.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            tv.setVisibility(View.GONE);
        }
    }

    private String formatRelativeTimeOnly(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        try {
            java.text.SimpleDateFormat inFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", java.util.Locale.getDefault());
            java.util.Date targetDate = inFormat.parse(raw.length() > 16 ? raw.substring(0, 16) : raw);
            if (targetDate == null) return "";

            long diffMillis = targetDate.getTime() - System.currentTimeMillis();
            boolean isOverdue = diffMillis < 0;
            long absDiff = Math.abs(diffMillis);

            long minutes = absDiff / (1000 * 60);
            long hours = minutes / 60;
            long days = hours / 24;

            if (isOverdue) {
                if (days > 0) return "Trễ " + days + " ngày";
                if (hours > 0) return "Trễ " + hours + "h";
                return "Trễ " + minutes + "m";
            } else {
                if (days > 0) return "Còn " + days + " ngày";
                if (hours > 0) return "Còn " + hours + "h";
                return "Còn " + minutes + "m";
            }
        } catch (Exception e) {
            return "";
        }
    }

    private String formatDateTime(String raw) {
        if (raw == null || raw.isEmpty()) return "Chưa cập nhật";
        try {
            java.text.SimpleDateFormat inFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", java.util.Locale.getDefault());
            java.text.SimpleDateFormat outFormat = new java.text.SimpleDateFormat("HH:mm, dd/MM/yyyy", java.util.Locale.getDefault());
            java.util.Date targetDate = inFormat.parse(raw.length() > 16 ? raw.substring(0, 16) : raw);
            if (targetDate == null) return raw;

            long diffMillis = targetDate.getTime() - System.currentTimeMillis();
            boolean isOverdue = diffMillis < 0;
            long absoluteDiff = Math.abs(diffMillis);

            long minutes = absoluteDiff / (1000 * 60);
            long hours = minutes / 60;
            long days = hours / 24;

            String relativeTime;
            if (days > 0) {
                relativeTime = days + " ngày";
            } else if (hours > 0) {
                relativeTime = hours + " giờ";
            } else if (minutes > 0) {
                relativeTime = minutes + " phút";
            } else {
                relativeTime = "vài giây";
            }

            String statusText = isOverdue ? " (Quá hạn " + relativeTime + ")" : " (Còn " + relativeTime + ")";
            return outFormat.format(targetDate) + statusText;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return raw.replace("T", ", lúc ");
    }

    private void updateTopBar() {
        SharedPrefsManager prefs = SharedPrefsManager.getInstance(this);
        String fullName = prefs.getFullName();
        String deptName = prefs.getDepartmentName();
        Long employeeId = prefs.getEmployeeId();

        if (fullName.isEmpty() && employeeId != -1L) {
            fetchEmployeeDetails(employeeId);
        } else {
            TopBarHelper.setupTopBar(this);
            updateGreeting(fullName.isEmpty() ? prefs.getUsername() : fullName);
        }
    }

    private void updateGreeting(String name) {
        TextView tvUserName = findViewById(R.id.tvUserName);
        if (tvUserName != null) {
            String greeting;
            int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
            
            if (hour >= 5 && hour < 12) {
                greeting = "Chào Buổi Sáng";
            } else if (hour >= 12 && hour < 18) {
                greeting = "Chào Buổi Chiều";
            } else {
                greeting = "Chào Buổi Tối";
            }
            
            tvUserName.setText(greeting);
        }
    }

    private void fetchEmployeeDetails(Long employeeId) {
        ApiService apiService = RetrofitClient.getApiService(this);
        apiService.getEmployeeById(employeeId).enqueue(new Callback<Employee>() {
            @Override
            public void onResponse(Call<Employee> call, Response<Employee> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Employee emp = response.body();
                    SharedPrefsManager prefs = SharedPrefsManager.getInstance(DashboardActivity.this);
                    prefs.setFullName(emp.getFullName());
                    prefs.setDepartmentName(emp.getDepartment());
                    prefs.setAvatarUrl(emp.getAvatarUrl());
                    TopBarHelper.setupTopBar(DashboardActivity.this);
                    updateGreeting(emp.getFullName());
                }
            }

            @Override
            public void onFailure(Call<Employee> call, Throwable t) {
                TopBarHelper.setupTopBar(DashboardActivity.this);
            }
        });
    }


    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    private void startBackgroundService() {
        Intent serviceIntent = new Intent(this, ChatForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    protected void onDestroy() {
        if (systemNotificationReceiver != null) {
            unregisterReceiver(systemNotificationReceiver);
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Đã bật thông báo hệ thống", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Thông báo bị tắt, bạn có thể không nhận được tin nhắn mới", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showCreateRequestDialog() {
        showCreateRequestDialog(null);
    }

    private void showCreateRequestDialog(String preselectType) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_request_create);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        RequestType[] types = RequestType.values();
        String[] typeNames = {"Nghỉ phép năm", "Nghỉ không lương", "Nghỉ ốm", "Làm thêm giờ", "Công tác", "Bổ sung công", "Thôi việc"};
        Spinner spinnerType = dialog.findViewById(R.id.spinnerRequestType);
        android.widget.ArrayAdapter<String> typeAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, typeNames);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        // Pre-select if requested
        if (preselectType != null) {
            if ("SICK_LEAVE".equals(preselectType)) spinnerType.setSelection(2);
            else if ("OVERTIME".equals(preselectType)) spinnerType.setSelection(3);
            else if ("BUSINESS_TRIP".equals(preselectType)) spinnerType.setSelection(4);
            else if ("PUNCH_CORRECTION".equals(preselectType)) spinnerType.setSelection(5);
            else if ("RESIGNATION".equals(preselectType)) spinnerType.setSelection(6);
        }

        EditText edtTitle = dialog.findViewById(R.id.edtRequestTitle);
        EditText edtDesc = dialog.findViewById(R.id.edtRequestDesc);
        LinearLayout containerDateDetails = dialog.findViewById(R.id.containerDateDetails);
        TextView btnAddDate = dialog.findViewById(R.id.btnAddDate);
        Button btnCancel = dialog.findViewById(R.id.btnRequestCancel);
        Button btnSubmit = dialog.findViewById(R.id.btnRequestSubmit);
        ImageView btnModalClose = dialog.findViewById(R.id.btnModalClose);
        android.widget.RelativeLayout layoutDateSelection = dialog.findViewById(R.id.layoutDateSelection);
        
        if (btnModalClose != null) {
            btnModalClose.setOnClickListener(v -> dialog.dismiss());
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Evidence UI (Sick Leave)
        LinearLayout layoutEvidence = dialog.findViewById(R.id.layoutEvidence);
        Button btnPickEvidence = dialog.findViewById(R.id.btnPickEvidence);
        dialogTvPath = dialog.findViewById(R.id.tvEvidencePath);
        dialogIvPreview = dialog.findViewById(R.id.ivEvidencePreview);
        selectedEvidencePath = null; 

        final RequestType[] currentType = {RequestType.LEAVE_ANNUAL};
        
        spinnerType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                currentType[0] = types[position];
                
                if (currentType[0] == RequestType.SICK_LEAVE) {
                    layoutEvidence.setVisibility(View.VISIBLE);
                } else {
                    layoutEvidence.setVisibility(View.GONE);
                }

                TextView btnSuggest = dialog.findViewById(R.id.btnSuggestDate);
                if (currentType[0] == RequestType.PUNCH_CORRECTION) {
                    btnSuggest.setVisibility(View.VISIBLE);
                } else {
                    btnSuggest.setVisibility(View.GONE);
                }

                if (currentType[0] == RequestType.RESIGNATION) {
                    btnAddDate.setVisibility(View.GONE);
                    if (layoutDateSelection != null) layoutDateSelection.setVisibility(View.GONE);
                    containerDateDetails.setVisibility(View.GONE);
                    while (containerDateDetails.getChildCount() > 1) {
                        containerDateDetails.removeViewAt(containerDateDetails.getChildCount() - 1);
                    }
                } else {
                    btnAddDate.setVisibility(View.VISIBLE);
                    if (layoutDateSelection != null) layoutDateSelection.setVisibility(View.VISIBLE);
                    containerDateDetails.setVisibility(View.VISIBLE);
                }

                for (int i = 0; i < containerDateDetails.getChildCount(); i++) {
                    updateRowVisibility(containerDateDetails.getChildAt(i), currentType[0]);
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btnPickEvidence.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        final int MAX_LEAVE_DAYS = 3;
        btnAddDate.setOnClickListener(v -> {
            if ((currentType[0] == RequestType.LEAVE_ANNUAL
                    || currentType[0] == RequestType.LEAVE_UNPAID
                    || currentType[0] == RequestType.SICK_LEAVE)
                    && containerDateDetails.getChildCount() >= MAX_LEAVE_DAYS) {
                Toast.makeText(this, "Đơn nghỉ phép tối đa chỉ được " + MAX_LEAVE_DAYS + " ngày", Toast.LENGTH_SHORT).show();
                return;
            }
            addRequestDateRow(containerDateDetails, currentType[0], spinnerType, types);
        });

        addRequestDateRow(containerDateDetails, currentType[0], spinnerType, types);

        TextView btnSuggestDate = dialog.findViewById(R.id.btnSuggestDate);
        btnSuggestDate.setOnClickListener(v -> fetchAndShowAttendanceSuggestions(containerDateDetails));

        btnSubmit.setOnClickListener(v -> {
            String title = edtTitle.getText().toString().trim();
            String desc = edtDesc.getText().toString().trim();

            if (title.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show();
                return;
            }

            List<RequestDetail> details = new ArrayList<>();
            if (currentType[0] != RequestType.RESIGNATION) {
                for (int i = 0; i < containerDateDetails.getChildCount(); i++) {
                    View row = containerDateDetails.getChildAt(i);
                    TextView tvDate = row.findViewById(R.id.tvSelectedDate);
                    String dateStr = tvDate.getText().toString();
                    if (dateStr.isEmpty() || dateStr.contains("Chọn ngày") || dateStr.contains("Ngày làm việc cuối cùng")) {
                        Toast.makeText(this, "Vui lòng chọn ngày cho tất cả các dòng", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    RequestDetail detail = new RequestDetail();
                    detail.setSpecificDate(dateStr);

                    if (currentType[0] == RequestType.LEAVE_ANNUAL
                            || currentType[0] == RequestType.LEAVE_UNPAID
                            || currentType[0] == RequestType.SICK_LEAVE) {
                        Spinner spinnerSession = row.findViewById(R.id.spinnerSession);
                        detail.setLeaveSession(LeaveSession.values()[spinnerSession.getSelectedItemPosition()]);
                    } else if (currentType[0] == RequestType.OVERTIME) {
                        EditText edtHours = row.findViewById(R.id.edtOvertimeHours);
                        String hoursStr = edtHours.getText().toString();
                        if (hoursStr.isEmpty()) {
                            Toast.makeText(this, "Vui lòng nhập số giờ làm thêm", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        detail.setOvertimeHours(Double.parseDouble(hoursStr));
                    } else if (currentType[0] == RequestType.PUNCH_CORRECTION) {
                        TextView tvIn = row.findViewById(R.id.tvCheckIn);
                        TextView tvOut = row.findViewById(R.id.tvCheckOut);
                        String inTime = tvIn.getText().toString();
                        String outTime = tvOut.getText().toString();
                        if (inTime.isEmpty() || inTime.contains("Vào") || outTime.isEmpty() || outTime.contains("Ra")) {
                            Toast.makeText(this, "Vui lòng chọn đầy đủ giờ vào và giờ ra", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        detail.setCheckIn(inTime);
                        detail.setCheckOut(outTime);
                    }
                    details.add(detail);
                }
            }

            if (details.isEmpty() && currentType[0] != RequestType.RESIGNATION) {
                Toast.makeText(this, "Vui lòng thêm ít nhất một ngày", Toast.LENGTH_SHORT).show();
                return;
            }

            if ((currentType[0] == RequestType.LEAVE_ANNUAL || currentType[0] == RequestType.LEAVE_UNPAID || currentType[0] == RequestType.SICK_LEAVE)
                    && details.size() > MAX_LEAVE_DAYS) {
                Toast.makeText(this, "Đơn nghỉ phép tối đa chỉ được " + MAX_LEAVE_DAYS + " ngày", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentType[0] != RequestType.SICK_LEAVE && currentType[0] != RequestType.PUNCH_CORRECTION) {
                Calendar todayCal = Calendar.getInstance();
                String todayStr = String.format("%04d-%02d-%02d", todayCal.get(Calendar.YEAR), todayCal.get(Calendar.MONTH) + 1, todayCal.get(Calendar.DAY_OF_MONTH));
                for (RequestDetail d : details) {
                    if (d.getSpecificDate() != null && d.getSpecificDate().compareTo(todayStr) < 0) {
                        Toast.makeText(this, "Ngày " + d.getSpecificDate() + " đã qua. Chỉ được chọn ngày hôm nay hoặc tương lai.", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            }

            CreateRequestRequest createReq = new CreateRequestRequest(title, currentType[0], details, desc);
            if (currentType[0] == RequestType.SICK_LEAVE && selectedEvidencePath != null) {
                uploadImageThenCreate(createReq, Uri.parse(selectedEvidencePath), dialog);
            } else {
                performCreateRequest(createReq, dialog);
            }
        });

        dialog.show();
    }

    private void uploadImageThenCreate(CreateRequestRequest createReq, Uri uri, Dialog dialog) {
        try {
            String fileName = "upload_" + System.currentTimeMillis() + ".jpg";
            File tempFile = new File(getCacheDir(), fileName);
            java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
            java.io.OutputStream outputStream = new java.io.FileOutputStream(tempFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) outputStream.write(buf, 0, len);
            outputStream.close();
            inputStream.close();

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), tempFile);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", tempFile.getName(), requestFile);

            ApiService apiService = RetrofitClient.getApiService(this);
            apiService.uploadImage(body).enqueue(new Callback<Map<String, String>>() {
                @Override
                public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        createReq.setFileUrl(response.body().get("fileUrl"));
                        createReq.setFileName(response.body().get("fileName"));
                        performCreateRequest(createReq, dialog);
                    } else {
                        Toast.makeText(DashboardActivity.this, "Lỗi upload ảnh", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override public void onFailure(Call<Map<String, String>> call, Throwable t) {
                    Toast.makeText(DashboardActivity.this, "Lỗi upload: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi xử lý file", Toast.LENGTH_SHORT).show();
        }
    }

    private void performCreateRequest(CreateRequestRequest createReq, Dialog dialog) {
        ApiService apiService = RetrofitClient.getApiService(this);
        apiService.createRequest(currentEmployeeId, createReq).enqueue(new Callback<Request>() {
            @Override
            public void onResponse(Call<Request> call, Response<Request> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(DashboardActivity.this, "Tạo đơn thành công", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    fetchDashboardStats();
                } else {
                    Toast.makeText(DashboardActivity.this, getErrorMessage(response), Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onFailure(Call<Request> call, Throwable t) {
                Toast.makeText(DashboardActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addRequestDateRow(LinearLayout container, RequestType type, Spinner spinnerType, RequestType[] types) {
        View row = getLayoutInflater().inflate(R.layout.item_request_date, container, false);
        TextView tvDate = row.findViewById(R.id.tvSelectedDate);
        tvDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            if (!tvDate.getText().toString().contains("Chọn ngày")) {
                try { cal.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(tvDate.getText().toString())); } catch (Exception e) {}
            }
            new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar selectedCal = Calendar.getInstance();
                selectedCal.set(year, month, dayOfMonth);
                RequestType activeType = types[spinnerType.getSelectedItemPosition()];
                if ((activeType == RequestType.LEAVE_ANNUAL || activeType == RequestType.LEAVE_UNPAID || activeType == RequestType.SICK_LEAVE)
                        && (selectedCal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || selectedCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)) {
                    Toast.makeText(this, "Không được chọn ngày nghỉ cuối tuần", Toast.LENGTH_SHORT).show();
                    return;
                }
                tvDate.setText(String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        Spinner spinnerSession = row.findViewById(R.id.spinnerSession);
        android.widget.ArrayAdapter<String> sessionAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Sáng", "Chiều", "Cả ngày"});
        sessionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSession.setAdapter(sessionAdapter);
        spinnerSession.setSelection(2); 

        row.findViewById(R.id.btnRemoveDate).setOnClickListener(v -> {
            if (container.getChildCount() > 1) container.removeView(row);
            else Toast.makeText(this, "Phải có ít nhất một ngày", Toast.LENGTH_SHORT).show();
        });

        TextView tvCheckIn = row.findViewById(R.id.tvCheckIn);
        TextView tvCheckOut = row.findViewById(R.id.tvCheckOut);
        tvCheckIn.setOnClickListener(v -> showTimePicker(tvCheckIn));
        tvCheckOut.setOnClickListener(v -> showTimePicker(tvCheckOut));

        updateRowVisibility(row, type);
        container.addView(row);
    }

    private void showTimePicker(TextView textView) {
        Calendar cal = Calendar.getInstance();
        new android.app.TimePickerDialog(this, (view, hourOfDay, minute) -> textView.setText(String.format("%02d:%02d", hourOfDay, minute)), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
    }

    private void updateRowVisibility(View row, RequestType type) {
        View spinnerSession = row.findViewById(R.id.spinnerSession);
        View edtOvertime = row.findViewById(R.id.edtOvertimeHours);
        View tvCheckIn = row.findViewById(R.id.tvCheckIn);
        View tvCheckOut = row.findViewById(R.id.tvCheckOut);
        if (type == RequestType.LEAVE_ANNUAL || type == RequestType.LEAVE_UNPAID || type == RequestType.SICK_LEAVE) {
            spinnerSession.setVisibility(View.VISIBLE); edtOvertime.setVisibility(View.GONE); tvCheckIn.setVisibility(View.GONE); tvCheckOut.setVisibility(View.GONE);
        } else if (type == RequestType.OVERTIME) {
            spinnerSession.setVisibility(View.GONE); edtOvertime.setVisibility(View.VISIBLE); tvCheckIn.setVisibility(View.GONE); tvCheckOut.setVisibility(View.GONE);
        } else if (type == RequestType.RESIGNATION) {
            spinnerSession.setVisibility(View.GONE); edtOvertime.setVisibility(View.GONE); tvCheckIn.setVisibility(View.GONE); tvCheckOut.setVisibility(View.GONE);
            TextView tvDate = row.findViewById(R.id.tvSelectedDate);
            if (tvDate.getText().toString().isEmpty() || tvDate.getText().toString().equals("Chọn ngày")) tvDate.setText("Ngày làm việc cuối cùng");
        } else {
            spinnerSession.setVisibility(View.GONE); edtOvertime.setVisibility(View.GONE); tvCheckIn.setVisibility(View.GONE); tvCheckOut.setVisibility(View.GONE);
        }
    }

    private void fetchAndShowAttendanceSuggestions(LinearLayout container) {
        ApiService apiService = RetrofitClient.getApiService(this);
        apiService.getAttendanceByMonth(currentEmployeeId, selectedMonth, selectedYear).enqueue(new Callback<List<Attendance>>() {
            @Override
            public void onResponse(Call<List<Attendance>> call, Response<List<Attendance>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Attendance> suggestions = new ArrayList<>();
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(selectedYear, selectedMonth - 1, 1);
                    int maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                    for (int day = 1; day <= maxDay; day++) {
                        calendar.set(selectedYear, selectedMonth - 1, day);
                        if (calendar.getTimeInMillis() > System.currentTimeMillis()) break;
                        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) continue;
                        String dateStr = String.format("%04d-%02d-%02d", selectedYear, selectedMonth, day);
                        Attendance record = null;
                        for (Attendance a : response.body()) { if (dateStr.equals(a.getDate())) { record = a; break; } }
                        if (record == null) { Attendance mock = new Attendance(); mock.setDate(dateStr); mock.setStatus("ABSENT"); suggestions.add(mock); }
                        else if (record.getCheckIn() == null || record.getCheckOut() == null || "ABSENT".equals(record.getStatus())) suggestions.add(record);
                    }
                    if (suggestions.isEmpty()) { Toast.makeText(DashboardActivity.this, "Không tìm thấy ngày nào cần bổ sung công", Toast.LENGTH_SHORT).show(); return; }
                    String[] items = new String[suggestions.size()];
                    for (int i = 0; i < suggestions.size(); i++) {
                        Attendance a = suggestions.get(i);
                        String reason = (a.getCheckIn() != null && a.getCheckOut() == null) ? "Thiếu giờ ra" : (a.getCheckIn() == null && a.getCheckOut() != null ? "Thiếu giờ vào" : "Nghỉ không phép");
                        items[i] = a.getDate() + " (" + reason + ")";
                    }
                    new AlertDialog.Builder(DashboardActivity.this).setTitle("Chọn ngày cần bổ sung công").setItems(items, (dialog, which) -> {
                                Attendance selected = suggestions.get(which);
                                boolean exists = false;
                                for (int i = 0; i < container.getChildCount(); i++) { if (selected.getDate().equals(((TextView)container.getChildAt(i).findViewById(R.id.tvSelectedDate)).getText().toString())) { exists = true; break; } }
                                if (!exists) {
                                    if (container.getChildCount() == 1 && ((TextView)container.getChildAt(0).findViewById(R.id.tvSelectedDate)).getText().toString().contains("Chọn ngày")) container.removeAllViews();
                                    addRequestDateWithData(container, selected.getDate());
                                } else Toast.makeText(DashboardActivity.this, "Ngày này đã được thêm", Toast.LENGTH_SHORT).show();
                            }).setNegativeButton("Đóng", null).show();
                }
            }
            @Override public void onFailure(Call<List<Attendance>> call, Throwable t) {}
        });
    }

    private void addRequestDateWithData(LinearLayout container, String date) {
        View row = getLayoutInflater().inflate(R.layout.item_request_date, container, false);
        TextView tvDate = row.findViewById(R.id.tvSelectedDate); tvDate.setText(date);
        tvDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance(); try { cal.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)); } catch (Exception e) {}
            new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> tvDate.setText(String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)), cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });
        row.findViewById(R.id.btnRemoveDate).setOnClickListener(v -> container.removeView(row));
        TextView tvCheckIn = row.findViewById(R.id.tvCheckIn); TextView tvCheckOut = row.findViewById(R.id.tvCheckOut);
        tvCheckIn.setOnClickListener(v1 -> showTimePicker(tvCheckIn)); tvCheckOut.setOnClickListener(v1 -> showTimePicker(tvCheckOut));
        updateRowVisibility(row, RequestType.PUNCH_CORRECTION); container.addView(row);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            selectedEvidencePath = imageUri.toString(); 
            if (dialogTvPath != null) dialogTvPath.setText("Đã chọn: " + imageUri.getLastPathSegment());
            if (dialogIvPreview != null) {
                dialogIvPreview.setVisibility(View.VISIBLE);
                dialogIvPreview.setImageURI(imageUri);
            }
        }
    }

    private String getErrorMessage(Response<?> response) {
        try {
            if (response != null && response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                if (errorBody.contains("\"message\":\"")) return errorBody.split("\"message\":\"")[1].split("\"")[0];
            }
        } catch (Exception e) {}
        return response != null ? "Mã lỗi: " + response.code() : "Lỗi không xác định";
    }

    private void showRequestDetailDialog(Request request) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_request_detail);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        TextView tvDetailAvatar = dialog.findViewById(R.id.tvDetailAvatar);
        TextView tvDetailEmployeeName = dialog.findViewById(R.id.tvDetailEmployeeName);
        TextView tvDetailDept = dialog.findViewById(R.id.tvDetailDept);
        TextView tvDetailTitle = dialog.findViewById(R.id.tvDetailTitle);
        TextView tvDetailStatus = dialog.findViewById(R.id.tvDetailStatus);
        TextView tvDetailDates = dialog.findViewById(R.id.tvDetailDates);
        TextView tvDetailDesc = dialog.findViewById(R.id.tvDetailDesc);
        TextView tvDetailEvidenceUrl = dialog.findViewById(R.id.tvDetailEvidenceUrl);
        TextView tvDetailDate = dialog.findViewById(R.id.tvDetailDate);
        TextView tvDetailReviewer = dialog.findViewById(R.id.tvDetailReviewer);
        TextView tvDetailRejection = dialog.findViewById(R.id.tvDetailRejection);
        Button btnDetailClose = dialog.findViewById(R.id.btnDetailClose);

        LinearLayout layoutDetailActions = dialog.findViewById(R.id.layoutDetailActions);
        Button btnDetailApprove = dialog.findViewById(R.id.btnDetailApprove);
        Button btnDetailReject = dialog.findViewById(R.id.btnDetailReject);

        // Header Info
        String empName = request.getEmployeeName() != null ? request.getEmployeeName() : "Unknown";
        tvDetailEmployeeName.setText(empName);
        tvDetailAvatar.setText(empName.substring(0, 1).toUpperCase());

        com.google.android.material.imageview.ShapeableImageView ivDetailAvatar = dialog.findViewById(R.id.ivDetailAvatar);
        if (ivDetailAvatar != null) {
            String avatarUrl = request.getEmployeeAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                String fullUrl = avatarUrl.startsWith("http") ? avatarUrl : RetrofitClient.BASE_URL + avatarUrl;
                Glide.with(this).load(fullUrl).circleCrop().into(ivDetailAvatar);
                ivDetailAvatar.setVisibility(View.VISIBLE);
                tvDetailAvatar.setVisibility(View.GONE);
            } else {
                ivDetailAvatar.setVisibility(View.GONE);
                tvDetailAvatar.setVisibility(View.VISIBLE);
            }
        }

        tvDetailDept.setText(request.getDepartmentName() != null ? request.getDepartmentName() : "Phòng ban: N/A");

        // Type & Status
        String typeStr = translateRequestType(request.getType() != null ? request.getType().name() : null);
        tvDetailTitle.setText(typeStr);

        String status = request.getStatus();
        if ("PENDING".equalsIgnoreCase(status)) {
            setBadge(tvDetailStatus, "CHỜ DUYỆT", "#F59E0B");
        } else if ("APPROVED".equalsIgnoreCase(status)) {
            setBadge(tvDetailStatus, "ĐÃ DUYỆT", "#10B981");
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            setBadge(tvDetailStatus, "TỪ CHỐI", "#EF4444");
        } else {
            setBadge(tvDetailStatus, status != null ? status.toUpperCase() : "UNK", "#6B7280");
        }

        // Dates
        if (request.getDetails() != null && !request.getDetails().isEmpty()) {
            tvDetailDates.setVisibility(View.VISIBLE);
            StringBuilder sb = new StringBuilder();
            for (RequestDetail d : request.getDetails()) {
                sb.append("• ").append(d.getSpecificDate());
                if (d.getLeaveSession() != null) sb.append(" (").append(translateSession(d.getLeaveSession().name())).append(")");
                else if (d.getOvertimeHours() != null) sb.append(" (").append(d.getOvertimeHours()).append(" giờ)");
                else if (d.getCheckIn() != null) sb.append(" (").append(d.getCheckIn()).append("-").append(d.getCheckOut()).append(")");
                sb.append("\n");
            }
            tvDetailDates.setText(sb.toString().trim());
        }

        // Desc
        tvDetailDesc.setText(request.getDescription() != null ? request.getDescription() : "Không có mô tả");

        // Evidence
        if (request.getFileUrl() != null && !request.getFileUrl().isEmpty()) {
            tvDetailEvidenceUrl.setVisibility(View.VISIBLE);
            tvDetailEvidenceUrl.setText("Xem minh chứng: " + request.getFileName());
            tvDetailEvidenceUrl.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(request.getFileUrl()));
                startActivity(intent);
            });
        }

        // Footer info
        tvDetailDate.setText("Ngày tạo: " + formatDateTime(request.getCreatedAt()));
        if (request.getReviewedByName() != null) {
            tvDetailReviewer.setVisibility(View.VISIBLE);
            tvDetailReviewer.setText("Người duyệt: " + request.getReviewedByName());
        }
        if ("REJECTED".equalsIgnoreCase(status) && request.getRejectionReason() != null) {
            tvDetailRejection.setVisibility(View.VISIBLE);
            tvDetailRejection.setText("Lý do từ chối: " + request.getRejectionReason());
        }

        // Action Buttons logic
        String userRole = SharedPrefsManager.getInstance(this).getRole();
        boolean isOwnRequest = (request.getEmployeeId() != null && request.getEmployeeId().equals(currentEmployeeId));
        if ("PENDING".equalsIgnoreCase(status) && !isOwnRequest && ("MANAGER".equalsIgnoreCase(userRole) || "ADMIN".equalsIgnoreCase(userRole))) {
            layoutDetailActions.setVisibility(View.VISIBLE);
        } else {
            layoutDetailActions.setVisibility(View.GONE);
        }

        btnDetailApprove.setOnClickListener(v -> {
            updateRequestStatus(request.getId(), "APPROVED");
            dialog.dismiss();
        });

        btnDetailReject.setOnClickListener(v -> {
            showQuickRejectDialog(request);
            dialog.dismiss();
        });

        btnDetailClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void updateRequestStatus(Long id, String status) {
        ApiService apiService = RetrofitClient.getApiService(this);
        Call<Request> call = apiService.updateRequestStatus(id, status);
        call.enqueue(new Callback<Request>() {
            @Override
            public void onResponse(Call<Request> call, Response<Request> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(DashboardActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    fetchDashboardStats();
                    fetchManagerRequests(); // Refresh list on dashboard
                } else {
                    Toast.makeText(DashboardActivity.this, getErrorMessage(response), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Request> call, Throwable t) {
                Toast.makeText(DashboardActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showQuickRejectDialog(Request request) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_request_review);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        EditText edtComment = dialog.findViewById(R.id.edtReviewComment);
        Button btnCancel = dialog.findViewById(R.id.btnReviewCancel);
        Button btnSubmit = dialog.findViewById(R.id.btnReviewSubmit);
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSubmit.setOnClickListener(v -> {
            String comment = edtComment.getText().toString().trim();
            if (comment.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập lý do", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            updateRequestStatus(request.getId(), "REJECTED");
        });
        dialog.show();
    }

    private void setBadge(TextView tv, String text, String color) {
        tv.setText(text);
        tv.setTextColor(android.graphics.Color.WHITE);
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(android.graphics.Color.parseColor(color));
        gd.setCornerRadius(40f);
        tv.setBackground(gd);
    }

    private String translateSession(String session) {
        if (session == null) return "Cả ngày";
        switch (session) {
            case "MORNING": return "Sáng";
            case "AFTERNOON": return "Chiều";
            case "ALL_DAY": return "Cả ngày";
            default: return session;
        }
    }
}
