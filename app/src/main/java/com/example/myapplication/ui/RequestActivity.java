package com.example.myapplication.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.adapter.RequestAdapter;
import com.example.myapplication.model.CreateRequestRequest;
import com.example.myapplication.model.Department;
import com.example.myapplication.model.Employee;
import com.example.myapplication.model.Request;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.BottomNavHelper;
import com.example.myapplication.utils.SharedPrefsManager;
import com.example.myapplication.utils.TopBarHelper;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RequestActivity extends AppCompatActivity {

    private RecyclerView recyclerViewRequest;
    private RequestAdapter adapter;
    private List<Request> allRequests = new ArrayList<>();
    private List<Request> requestList = new ArrayList<>();
    private EditText etSearchRequest;
    private TextView chipAll, chipPending, chipApproved, chipRejected, chipCancelled;
    private TextView tvLeaveBalance;
    private LinearLayout layoutLeaveBalance;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabAddRequest;

    // Current state
    private Long currentDeptId; 
    private Long currentEmployeeId;
    private String currentStatusFilter = "ALL";
    private String userRole;

    private LinearLayout btnMonthFilter, btnExtraFilter;
    private TextView tvSelectedMonth, tvSelectedExtra;
    private Integer selectedMonth, selectedYear;
    private Long selectedEmpId, selectedDeptId;
    private List<Employee> deptEmployees = new ArrayList<>();
    private List<Department> allDepts = new ArrayList<>();

    private static final int PICK_IMAGE_REQUEST = 1;
    private String selectedEvidencePath = null;
    private ImageView dialogIvPreview;
    private TextView dialogTvPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);
        BottomNavHelper.setupBottomNav(this, R.id.nav_request);
        TopBarHelper.setupTopBar(this);

        SharedPrefsManager prefs = SharedPrefsManager.getInstance(this);
        currentDeptId = prefs.getDepartmentId();
        currentEmployeeId = prefs.getEmployeeId();
        String rawRole = prefs.getRole();
        // Normalize role (remove ROLE_ prefix if present, e.g. ROLE_ADMIN -> ADMIN)
        userRole = (rawRole != null) ? rawRole.replace("ROLE_", "").toUpperCase() : "EMPLOYEE";
        
        android.util.Log.d("RequestActivity", "Normalized User Role: " + userRole);

        initViews();
        setupRecyclerView();
        setupListeners();
        
        fetchLeaveBalance();
        // Fetch initially
        fetchRequests("ALL");

        if (getIntent().getBooleanExtra("OPEN_CREATE_DIALOG", false)) {
            String preselect = getIntent().getStringExtra("PRESELECT_TYPE");
            showCreateRequestDialog(preselect);
        }
    }
 
    @Override
    protected void onResume() {
        super.onResume();
        TopBarHelper.setupTopBar(this);
    }

    private void initViews() {
        recyclerViewRequest = findViewById(R.id.recyclerViewRequest);
        etSearchRequest = findViewById(R.id.etSearchRequest);
        chipAll = findViewById(R.id.chipAll);
        chipPending = findViewById(R.id.chipPending);
        chipApproved = findViewById(R.id.chipApproved);
        chipRejected = findViewById(R.id.chipRejected);
        chipCancelled = findViewById(R.id.chipCancelled);
        
        tvLeaveBalance = findViewById(R.id.tvLeaveBalance);
        layoutLeaveBalance = findViewById(R.id.layoutLeaveBalance);
        
        fabAddRequest = findViewById(R.id.fabAddRequest);

        btnMonthFilter = findViewById(R.id.btnMonthFilter);
        tvSelectedMonth = findViewById(R.id.tvSelectedMonth);
        btnExtraFilter = findViewById(R.id.btnExtraFilter);
        tvSelectedExtra = findViewById(R.id.tvSelectedExtra);

        tvLeaveBalance = findViewById(R.id.tvLeaveBalance);

        // FAB tạo đơn: hiện cho tất cả nhân viên
        fabAddRequest.setVisibility(android.view.View.VISIBLE);

        // Default to current month/year
        java.util.Calendar cal = java.util.Calendar.getInstance();
        selectedMonth = cal.get(java.util.Calendar.MONTH) + 1;
        selectedYear = cal.get(java.util.Calendar.YEAR);
        tvSelectedMonth.setText("Tháng " + selectedMonth + "/" + selectedYear);

        setupFilters();
        loadFilterData();
        updateChipUI(chipAll);
    }

    private void setupRecyclerView() {
        recyclerViewRequest.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RequestAdapter(this, requestList, new RequestAdapter.OnRequestActionClickListener() {
            @Override
            public void onItemClick(Request request) {
                showRequestDetailDialog(request);
            }

            @Override
            public void onApprove(Request request) {
                updateRequestStatus(request.getId(), "APPROVED");
            }

            @Override
            public void onReject(Request request) {
                showRejectDialog(request);
            }

            @Override
            public void onCancel(Request request) {
                handleCancelRequest(request);
            }
        });
        recyclerViewRequest.setAdapter(adapter);
    }

    private void setupFilters() {
        btnMonthFilter.setOnClickListener(v -> showMonthPicker());
        btnExtraFilter.setOnClickListener(v -> showExtraFilterDialog());
    }

    private void loadFilterData() {
        ApiService apiService = RetrofitClient.getApiService(this);
        if ("MANAGER".equalsIgnoreCase(userRole)) {
            apiService.getEmployeesByDepartmentId(currentDeptId).enqueue(new Callback<List<Employee>>() {
                @Override public void onResponse(Call<List<Employee>> call, Response<List<Employee>> response) {
                    if (response.isSuccessful()) deptEmployees = response.body();
                }
                @Override public void onFailure(Call<List<Employee>> call, Throwable t) {}
            });
        } else if ("ADMIN".equalsIgnoreCase(userRole)) {
            apiService.getDepartments().enqueue(new Callback<List<Department>>() {
                @Override public void onResponse(Call<List<Department>> call, Response<List<Department>> response) {
                    if (response.isSuccessful()) allDepts = response.body();
                }
                @Override public void onFailure(Call<List<Department>> call, Throwable t) {}
            });
        } else {
            btnExtraFilter.setVisibility(View.GONE);
        }
    }

    private void showMonthPicker() {
        String[] months = new String[13];
        months[0] = "Xem toàn bộ thời gian";
        for (int i = 0; i < 12; i++) months[i+1] = "Tháng " + (i + 1);
        
        new android.app.AlertDialog.Builder(this)
            .setTitle("Chọn tháng")
            .setItems(months, (dialog, which) -> {
                if (which == 0) {
                    selectedMonth = 0; 
                    tvSelectedMonth.setText("Toàn thời gian");
                } else {
                    selectedMonth = which;
                    tvSelectedMonth.setText("Tháng " + selectedMonth + "/" + selectedYear);
                }
                fetchRequests(currentStatusFilter);
            })
            .show();
    }

    private void showExtraFilterDialog() {
        if ("MANAGER".equals(userRole)) {
            String[] names = new String[deptEmployees.size() + 1];
            names[0] = "Tất cả nhân viên";
            for (int i = 0; i < deptEmployees.size(); i++) names[i+1] = deptEmployees.get(i).getFullName();
            
            new android.app.AlertDialog.Builder(this)
                .setTitle("Chọn nhân viên")
                .setItems(names, (dialog, which) -> {
                    if (which == 0) {
                        selectedEmpId = null;
                        tvSelectedExtra.setText("Tất cả");
                    } else {
                        selectedEmpId = deptEmployees.get(which - 1).getId();
                        tvSelectedExtra.setText(deptEmployees.get(which - 1).getFullName());
                    }
                    fetchRequests(currentStatusFilter);
                })
                .show();
        } else if ("ADMIN".equals(userRole)) {
            String[] names = new String[allDepts.size() + 1];
            names[0] = "Tất cả phòng ban";
            for (int i = 0; i < allDepts.size(); i++) names[i+1] = allDepts.get(i).getName();

            new android.app.AlertDialog.Builder(this)
                .setTitle("Chọn phòng ban")
                .setItems(names, (dialog, which) -> {
                    selectedEmpId = null; // Reset employee when dept changes
                    if (which == 0) {
                        selectedDeptId = null;
                        tvSelectedExtra.setText("Tất cả");
                        fetchRequests(currentStatusFilter);
                    } else {
                        selectedDeptId = allDepts.get(which - 1).getId();
                        String deptName = allDepts.get(which - 1).getName();
                        tvSelectedExtra.setText(deptName);
                        // After selecting Dept, ask for Employee
                        loadDeptEmployeesAndShowDialog(selectedDeptId, deptName);
                    }
                })
                .show();
        }
    }

    private void loadDeptEmployeesAndShowDialog(Long deptId, String deptName) {
        ApiService apiService = RetrofitClient.getApiService(this);
        apiService.getEmployeesByDepartmentId(deptId).enqueue(new Callback<List<Employee>>() {
            @Override
            public void onResponse(Call<List<Employee>> call, Response<List<Employee>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Employee> emps = response.body();
                    String[] empNames = new String[emps.size() + 1];
                    empNames[0] = "Tất cả nhân viên (" + deptName + ")";
                    for (int i = 0; i < emps.size(); i++) empNames[i+1] = emps.get(i).getFullName();

                    new android.app.AlertDialog.Builder(RequestActivity.this)
                        .setTitle("Lọc tiếp theo nhân viên?")
                        .setItems(empNames, (dialog, which) -> {
                            if (which == 0) {
                                selectedEmpId = null;
                            } else {
                                selectedEmpId = emps.get(which - 1).getId();
                                tvSelectedExtra.setText(deptName + " - " + emps.get(which - 1).getFullName());
                            }
                            fetchRequests(currentStatusFilter);
                        })
                        .show();
                } else {
                    fetchRequests(currentStatusFilter);
                }
            }
            @Override public void onFailure(Call<List<Employee>> call, Throwable t) {
                fetchRequests(currentStatusFilter);
            }
        });
    }

    private void setupListeners() {
        chipAll.setOnClickListener(v -> {
            currentStatusFilter = "ALL";
            updateChipUI(chipAll);
            fetchRequests(currentStatusFilter);
        });

        chipPending.setOnClickListener(v -> {
            currentStatusFilter = "PENDING";
            updateChipUI(chipPending);
            fetchRequests(currentStatusFilter);
        });
        
        chipApproved.setOnClickListener(v -> {
            currentStatusFilter = "APPROVED";
            updateChipUI(chipApproved);
            fetchRequests(currentStatusFilter);
        });
        
        chipRejected.setOnClickListener(v -> {
            currentStatusFilter = "REJECTED";
            updateChipUI(chipRejected);
            fetchRequests(currentStatusFilter);
        });

        chipCancelled.setOnClickListener(v -> {
            currentStatusFilter = "CANCELLED";
            updateChipUI(chipCancelled);
            fetchRequests(currentStatusFilter);
        });

        etSearchRequest.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                filterList(s.toString());
            }
        });

        fabAddRequest.setOnClickListener(v -> showCreateRequestDialog());
    }

    private void updateChipUI(TextView selectedChip) {
        int unselectedBg = R.drawable.bg_chip_unselected;
        int selectedBg = R.drawable.bg_chip_selected;
        int unselectedTextColor = getResources().getColor(R.color.secondary);
        int selectedTextColor = android.graphics.Color.WHITE;

        chipAll.setBackgroundResource(unselectedBg);
        chipPending.setBackgroundResource(unselectedBg);
        chipApproved.setBackgroundResource(unselectedBg);
        chipRejected.setBackgroundResource(unselectedBg);
        chipCancelled.setBackgroundResource(unselectedBg);

        chipAll.setTextColor(unselectedTextColor);
        chipPending.setTextColor(unselectedTextColor);
        chipApproved.setTextColor(unselectedTextColor);
        chipRejected.setTextColor(unselectedTextColor);
        chipCancelled.setTextColor(unselectedTextColor);

        selectedChip.setBackgroundResource(selectedBg);
        selectedChip.setTextColor(selectedTextColor);
    }

    private void filterList(String query) {
        List<Request> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase().trim();

        for (Request r : allRequests) {
            String typeDisplay = getRequestTypeDisplay(r.getType());
            boolean matchesQuery = lowerQuery.isEmpty() || 
                                 (r.getEmployeeName() != null && r.getEmployeeName().toLowerCase().contains(lowerQuery)) ||
                                 (typeDisplay.toLowerCase().contains(lowerQuery));
            if (matchesQuery) {
                filtered.add(r);
            }
        }
        adapter.setRequestList(filtered);
    }

    private void fetchRequests(String status) {
        ApiService apiService = RetrofitClient.getApiService(this);
        etSearchRequest.setText(""); // Clear search when swapping filters
        
        // Clear current list immediately to show loading state/feedback
        allRequests.clear();
        requestList.clear();
        adapter.setRequestList(requestList);
        
        android.util.Log.d("RequestActivity", "Fetching requests for status: " + status + ", Role: " + userRole);

        // For Manager/Admin, we want to see BOTH:
        // 1. Requests they need to review (Subordinates for Manager, Managers for Admin)
        // 2. Their OWN requests (to monitor status)
        
        if ("EMPLOYEE".equalsIgnoreCase(userRole)) {
            fetchEmployeePersonalRequests(apiService, status);
        } else if ("MANAGER".equalsIgnoreCase(userRole)) {
            fetchManagerRequestsDual(apiService, status);
        } else if ("ADMIN".equalsIgnoreCase(userRole)) {
            fetchAdminRequestsDual(apiService, status);
        } else {
            // Fallback for unexpected roles
            fetchEmployeePersonalRequests(apiService, status);
        }
    }

    private void fetchEmployeePersonalRequests(ApiService apiService, String status) {
        Integer monthParam = (selectedMonth != null && selectedMonth > 0) ? selectedMonth : null;
        apiService.getMyRequests(currentEmployeeId, monthParam, selectedYear).enqueue(new Callback<List<Request>>() {
            @Override
            public void onResponse(Call<List<Request>> call, Response<List<Request>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Request> fetched = response.body();
                    allRequests = new ArrayList<>();
                    for (Request r : fetched) {
                        if ("ALL".equalsIgnoreCase(status) || status.equalsIgnoreCase(r.getStatus())) {
                            allRequests.add(r);
                        }
                    }
                    sortRequestsByDate(allRequests);
                    requestList = allRequests;
                    adapter.setRequestList(requestList);
                }
            }
            @Override public void onFailure(Call<List<Request>> call, Throwable t) {}
        });
    }

    private void fetchManagerRequestsDual(ApiService apiService, final String status) {
        Integer monthParam = (selectedMonth != null && selectedMonth > 0) ? selectedMonth : null;
        android.util.Log.d("RequestActivity", "fetchManagerRequestsDual - DeptId: " + currentDeptId + ", Status: " + status);

        // Fetch personal
        apiService.getMyRequests(currentEmployeeId, monthParam, selectedYear).enqueue(new Callback<List<Request>>() {
            @Override
            public void onResponse(Call<List<Request>> call, Response<List<Request>> responsePersonal) {
                final List<Request> personal = responsePersonal.isSuccessful() && responsePersonal.body() != null ? responsePersonal.body() : new ArrayList<>();
                
                Long deptIdToFetch = (selectedDeptId != null) ? selectedDeptId : currentDeptId;
                apiService.getRequestsByDepartmentAndStatus(deptIdToFetch, status, monthParam, selectedYear, selectedEmpId).enqueue(new Callback<List<Request>>() {
                    @Override
                    public void onResponse(Call<List<Request>> call, Response<List<Request>> responseDept) {
                        List<Request> dept = responseDept.isSuccessful() && responseDept.body() != null ? responseDept.body() : new ArrayList<>();
                        mergeAndDisplay(personal, dept, status);
                    }
                    @Override public void onFailure(Call<List<Request>> call, Throwable t) { mergeAndDisplay(personal, new ArrayList<>(), status); }
                });
            }
            @Override public void onFailure(Call<List<Request>> call, Throwable t) {}
        });
    }

    private void fetchAdminRequestsDual(ApiService apiService, final String status) {
        Integer monthParam = (selectedMonth != null && selectedMonth > 0) ? selectedMonth : null;

        apiService.getMyRequests(currentEmployeeId, monthParam, selectedYear).enqueue(new Callback<List<Request>>() {
            @Override
            public void onResponse(Call<List<Request>> call, Response<List<Request>> responsePersonal) {
                final List<Request> personal = responsePersonal.isSuccessful() && responsePersonal.body() != null ? responsePersonal.body() : new ArrayList<>();
                
                apiService.getAllRequestsByStatus(status, monthParam, selectedYear, selectedDeptId, selectedEmpId).enqueue(new Callback<List<Request>>() {
                    @Override
                    public void onResponse(Call<List<Request>> call, Response<List<Request>> responseAdmin) {
                        List<Request> adminList = responseAdmin.isSuccessful() && responseAdmin.body() != null ? responseAdmin.body() : new ArrayList<>();
                        mergeAndDisplay(personal, adminList, status);
                    }
                    @Override public void onFailure(Call<List<Request>> call, Throwable t) { mergeAndDisplay(personal, new ArrayList<>(), status); }
                });
            }
            @Override public void onFailure(Call<List<Request>> call, Throwable t) {}
        });
    }

    private void mergeAndDisplay(List<Request> personal, List<Request> reviewList, String filterStatus) {
        java.util.LinkedHashMap<Long, Request> map = new java.util.LinkedHashMap<>();
        
        // Add review items with local status and employee filtering
        for (Request r : reviewList) {
            boolean statusMatch = "ALL".equalsIgnoreCase(filterStatus) || filterStatus.equalsIgnoreCase(r.getStatus());
            boolean employeeMatch = (selectedEmpId == null) || (r.getEmployeeId() != null && r.getEmployeeId().equals(selectedEmpId));
            
            if (statusMatch && employeeMatch) {
                map.put(r.getId(), r);
            }
        }
        
        // Add personal items
        for (Request r : personal) {
            boolean statusMatch = "ALL".equalsIgnoreCase(filterStatus) || filterStatus.equalsIgnoreCase(r.getStatus());
            boolean employeeMatch = (selectedEmpId == null) || (r.getEmployeeId() != null && r.getEmployeeId().equals(selectedEmpId));
            
            if (statusMatch && employeeMatch) {
                map.put(r.getId(), r);
            }
        }
        
        allRequests = new ArrayList<>(map.values());
        sortRequestsByDate(allRequests);
        requestList = allRequests;
        adapter.setRequestList(requestList);
        android.util.Log.d("RequestActivity", "Merged list size: " + allRequests.size() + " for status: " + filterStatus + ", selectedEmpId: " + selectedEmpId);
    }

    private void fetchRequestsSimple(Call<List<Request>> call) {
        call.enqueue(new Callback<List<Request>>() {
            @Override
            public void onResponse(Call<List<Request>> call, Response<List<Request>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allRequests = response.body();
                    sortRequestsByDate(allRequests);
                    requestList = allRequests;
                    adapter.setRequestList(requestList);
                }
            }
            @Override public void onFailure(Call<List<Request>> call, Throwable t) {}
        });
    }

    private void sortRequestsByDate(List<Request> list) {
        if (list == null) return;
        java.util.Collections.sort(list, (r1, r2) -> {
            String d1 = r1.getCreatedAt() != null ? r1.getCreatedAt() : "";
            String d2 = r2.getCreatedAt() != null ? r2.getCreatedAt() : "";
            // Descending lexicographical sort for ISO date strings works for "Newest First"
            return d2.compareTo(d1);
        });
    }

    private void handleCancelRequest(Request request) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Hủy đơn")
                .setMessage("Bạn có chắc chắn muốn hủy đơn này không?")
                .setPositiveButton("Hủy đơn", (dialog, which) -> {
                    ApiService apiService = RetrofitClient.getApiService(this);
                    apiService.cancelRequest(request.getId(), currentEmployeeId).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(RequestActivity.this, "Đã hủy đơn thành công", Toast.LENGTH_SHORT).show();
                                fetchRequests(currentStatusFilter);
                                fetchLeaveBalance();
                                Toast.makeText(RequestActivity.this, getErrorMessage(response), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(RequestActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Quay lại", null)
                .show();
    }

    private void updateRequestStatus(Long id, String status) {
        ApiService apiService = RetrofitClient.getApiService(this);
        Call<Request> call = apiService.updateRequestStatus(id, status);
        call.enqueue(new Callback<Request>() {
            @Override
            public void onResponse(Call<Request> call, Response<Request> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(RequestActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    fetchRequests(currentStatusFilter); // Refresh list
                } else {
                    Toast.makeText(RequestActivity.this, getErrorMessage(response), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Request> call, Throwable t) {
                Toast.makeText(RequestActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRejectDialog(Request request) {
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
            updateRequestStatus(request.getId(), "REJECTED"); // Ideally send comment too, but backend status endpoint only takes status string right now based on our recent fix
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

    private void showRequestDetailDialog(Request request) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_request_detail);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        TextView tvDetailAvatar = dialog.findViewById(R.id.tvDetailAvatar);
        com.google.android.material.imageview.ShapeableImageView ivDetailAvatar = dialog.findViewById(R.id.ivDetailAvatar);
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
        tvDetailDept.setText(request.getDepartmentName() != null ? request.getDepartmentName() : "N/A");
        
        // Avatar logic
        if (tvDetailAvatar != null) {
            String trimmed = empName.trim();
            if (!trimmed.isEmpty()) {
                String[] parts = trimmed.split(" ");
                String lastWord = parts[parts.length - 1];
                tvDetailAvatar.setText(!lastWord.isEmpty() ? String.valueOf(lastWord.charAt(0)).toUpperCase() : "?");
            } else {
                tvDetailAvatar.setText("?");
            }
        }

        if (ivDetailAvatar != null) {
            String avatarUrl = request.getEmployeeAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                String fullUrl = avatarUrl.startsWith("http") ? avatarUrl : RetrofitClient.BASE_URL + avatarUrl;
                Glide.with(this).load(fullUrl).circleCrop().into(ivDetailAvatar);
                ivDetailAvatar.setVisibility(View.VISIBLE);
                if (tvDetailAvatar != null) tvDetailAvatar.setVisibility(View.GONE);
            } else {
                ivDetailAvatar.setVisibility(View.GONE);
                if (tvDetailAvatar != null) tvDetailAvatar.setVisibility(View.VISIBLE);
            }
        }

        // Type and Title
        String typeStr = "Khác";
        if (request.getType() != null) {
            switch (request.getType()) {
                case LEAVE_ANNUAL: typeStr = "Nghỉ phép năm"; break;
                case LEAVE_UNPAID: typeStr = "Nghỉ không lương"; break;
                case SICK_LEAVE: typeStr = "Nghỉ ốm (BHXH chi trả)"; break;
                case OVERTIME: typeStr = "Làm thêm giờ"; break;
                case BUSINESS_TRIP: typeStr = "Công tác"; break;
                case PUNCH_CORRECTION: typeStr = "Giải trình chấm công"; break;
                case RESIGNATION: typeStr = "Đơn xin thôi việc"; break;
            }
        }
        tvDetailTitle.setText(typeStr);

        // Status Badge
        String status = request.getStatus();
        if ("PENDING".equalsIgnoreCase(status)) {
            setBadge(tvDetailStatus, "CHỜ DUYỆT", "#F59E0B");
        } else if ("APPROVED".equalsIgnoreCase(status)) {
            setBadge(tvDetailStatus, "ĐÃ DUYỆT", "#10B981");
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            setBadge(tvDetailStatus, "TỪ CHỐI", "#EF4444");
        } else if ("CANCELLED".equalsIgnoreCase(status)) {
            setBadge(tvDetailStatus, "ĐÃ HỦY", "#6B7280");
        } else {
            setBadge(tvDetailStatus, status != null ? status.toUpperCase() : "UNK", "#6B7280");
        }

        // Description
        tvDetailDesc.setText(request.getDescription() != null ? request.getDescription() : "Không có lý do");

        // Dates Detail
        if (request.getDetails() != null && !request.getDetails().isEmpty()) {
            tvDetailDates.setVisibility(View.VISIBLE);
            StringBuilder datesBuilder = new StringBuilder();
            for (int i = 0; i < request.getDetails().size(); i++) {
                com.example.myapplication.model.RequestDetail d = request.getDetails().get(i);
                
                String dateStr = d.getSpecificDate() != null ? d.getSpecificDate() : "Chưa xác định";
                try {
                    // Try to format YYYY-MM-DD to DD/MM/YYYY
                    if (dateStr.length() >= 10 && dateStr.contains("-")) {
                        dateStr = dateStr.substring(8, 10) + "/" + dateStr.substring(5, 7) + "/" + dateStr.substring(0, 4);
                    }
                } catch (Exception ignored) {}

                datesBuilder.append("• ").append(dateStr);
                
                if ((request.getType() == com.example.myapplication.model.RequestType.LEAVE_ANNUAL
                        || request.getType() == com.example.myapplication.model.RequestType.LEAVE_UNPAID
                        || request.getType() == com.example.myapplication.model.RequestType.SICK_LEAVE) && d.getLeaveSession() != null) {
                    datesBuilder.append(" (");
                    switch (d.getLeaveSession()) {
                        case MORNING: datesBuilder.append("Sáng"); break;
                        case AFTERNOON: datesBuilder.append("Chiều"); break;
                        case ALL_DAY: datesBuilder.append("Cả ngày"); break;
                    }
                    datesBuilder.append(")");
                } else if (request.getType() == com.example.myapplication.model.RequestType.OVERTIME && d.getOvertimeHours() != null) {
                    datesBuilder.append(" (").append(d.getOvertimeHours()).append("h)");
                }
                if (i < request.getDetails().size() - 1) datesBuilder.append("\n");
            }
            tvDetailDates.setText(datesBuilder.toString());
        }

        // Evidence
        if (request.getFileUrl() != null && !request.getFileUrl().isEmpty()) {
            tvDetailEvidenceUrl.setVisibility(View.VISIBLE);
            tvDetailEvidenceUrl.setText("Đính kèm: " + (request.getFileName() != null ? request.getFileName() : "Xem tài liệu"));
            tvDetailEvidenceUrl.setOnClickListener(v -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(request.getFileUrl()));
                startActivity(browserIntent);
            });
        }

        // Creation Date
        String iso = request.getCreatedAt();
        if (iso != null) {
            try {
                String datePart = iso.length() >= 10 ? iso.substring(8, 10) + "/" + iso.substring(5, 7) + "/" + iso.substring(0, 4) : iso;
                String timePart = iso.length() >= 16 ? " " + iso.substring(11, 16) : "";
                tvDetailDate.setText("Gửi lúc: " + timePart.trim() + " - " + datePart);
            } catch (Exception e) {
                tvDetailDate.setText("Gửi lúc: " + iso);
            }
        }

        // Reviewer
        if (request.getReviewedByName() != null && !request.getReviewedByName().isEmpty()) {
            tvDetailReviewer.setVisibility(View.VISIBLE);
            tvDetailReviewer.setText("Người duyệt: " + request.getReviewedByName());
            
            String updateIso = request.getUpdatedAt();
            if (updateIso != null) {
                try {
                    String datePart = updateIso.length() >= 10 ? updateIso.substring(8, 10) + "/" + updateIso.substring(5, 7) + "/" + updateIso.substring(0, 4) : updateIso;
                    String timePart = updateIso.length() >= 16 ? " " + updateIso.substring(11, 16) : "";
                    tvDetailReviewer.setText("Người duyệt: " + request.getReviewedByName() + " lúc " + timePart.trim() + " - " + datePart);
                } catch (Exception ignored) {}
            }
        }

        // Rejection
        if ("REJECTED".equalsIgnoreCase(status) && request.getRejectionReason() != null) {
            tvDetailRejection.setVisibility(View.VISIBLE);
            tvDetailRejection.setText("Lý do từ chối: " + request.getRejectionReason());
        }

        // Action Buttons logic
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
            showRejectDialog(request);
            dialog.dismiss();
        });

        btnDetailClose.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    private void showCreateRequestDialog() {
        showCreateRequestDialog(null);
    }

    private void showCreateRequestDialog(String preselectType) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_request_create);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        com.example.myapplication.model.RequestType[] types = com.example.myapplication.model.RequestType.values();
        String[] typeNames = {"Nghỉ phép năm", "Nghỉ không lương", "Nghỉ ốm", "Làm thêm giờ", "Công tác", "Bổ sung công", "Thôi việc"};
        android.widget.Spinner spinnerType = dialog.findViewById(R.id.spinnerRequestType);
        android.widget.ArrayAdapter<String> typeAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, typeNames);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        // Pre-select if requested
        if (preselectType != null) {
            if ("OVERTIME".equals(preselectType)) spinnerType.setSelection(3);
            else if ("BUSINESS_TRIP".equals(preselectType)) spinnerType.setSelection(4);
        }

        EditText edtTitle = dialog.findViewById(R.id.edtRequestTitle);
        EditText edtDesc = dialog.findViewById(R.id.edtRequestDesc);
        android.widget.LinearLayout containerDateDetails = dialog.findViewById(R.id.containerDateDetails);
        TextView btnAddDate = dialog.findViewById(R.id.btnAddDate);
        Button btnCancel = dialog.findViewById(R.id.btnRequestCancel);
        Button btnSubmit = dialog.findViewById(R.id.btnRequestSubmit);
        ImageView btnModalClose = dialog.findViewById(R.id.btnModalClose);
        android.widget.RelativeLayout layoutDateSelection = dialog.findViewById(R.id.layoutDateSelection);
        
        if (btnModalClose != null) {
            btnModalClose.setOnClickListener(v -> dialog.dismiss());
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Evidence UI
        android.widget.LinearLayout layoutEvidence = dialog.findViewById(R.id.layoutEvidence);
        Button btnPickEvidence = dialog.findViewById(R.id.btnPickEvidence);
        dialogTvPath = dialog.findViewById(R.id.tvEvidencePath);
        dialogIvPreview = dialog.findViewById(R.id.ivEvidencePreview);
        selectedEvidencePath = null; // Reset for new dialog

        // Track current request type for dynamic UI in rows
        final com.example.myapplication.model.RequestType[] currentType = {com.example.myapplication.model.RequestType.LEAVE_ANNUAL};
        
        spinnerType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                currentType[0] = types[position];
                
                // Show/hide evidence section for SICK_LEAVE
                if (currentType[0] == com.example.myapplication.model.RequestType.SICK_LEAVE) {
                    layoutEvidence.setVisibility(View.VISIBLE);
                } else {
                    layoutEvidence.setVisibility(View.GONE);
                }

                // Show/hide suggest button for PUNCH_CORRECTION
                TextView btnSuggest = dialog.findViewById(R.id.btnSuggestDate);
                if (currentType[0] == com.example.myapplication.model.RequestType.PUNCH_CORRECTION) {
                    btnSuggest.setVisibility(View.VISIBLE);
                } else {
                    btnSuggest.setVisibility(View.GONE);
                }

                // Nếu là đơn THÔI VIỆC: Ẩn nút "Thêm ngày" và giữ lại duy nhất 1 dòng
                if (currentType[0] == com.example.myapplication.model.RequestType.RESIGNATION) {
                    btnAddDate.setVisibility(View.GONE);
                    if (layoutDateSelection != null) layoutDateSelection.setVisibility(View.GONE);
                    containerDateDetails.setVisibility(View.GONE);
                    // Nếu lỡ có nhiều hơn 1 dòng thì xóa bớt
                    while (containerDateDetails.getChildCount() > 1) {
                        containerDateDetails.removeViewAt(containerDateDetails.getChildCount() - 1);
                    }
                } else {
                    btnAddDate.setVisibility(View.VISIBLE);
                    if (layoutDateSelection != null) layoutDateSelection.setVisibility(View.VISIBLE);
                    containerDateDetails.setVisibility(View.VISIBLE);
                }

                // Update all existing rows
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
            // Giới hạn tối đa 3 ngày cho đơn nghỉ phép / nghỉ ốm
            if ((currentType[0] == com.example.myapplication.model.RequestType.LEAVE_ANNUAL
                    || currentType[0] == com.example.myapplication.model.RequestType.LEAVE_UNPAID
                    || currentType[0] == com.example.myapplication.model.RequestType.SICK_LEAVE)
                    && containerDateDetails.getChildCount() >= MAX_LEAVE_DAYS) {
                Toast.makeText(this, "Đơn nghỉ phép tối đa chỉ được " + MAX_LEAVE_DAYS + " ngày", Toast.LENGTH_SHORT).show();
                return;
            }
            addRequestDateRow(containerDateDetails, currentType[0], spinnerType, types);
        });

        // Add first row by default
        addRequestDateRow(containerDateDetails, currentType[0], spinnerType, types);

        TextView btnSuggestDate = dialog.findViewById(R.id.btnSuggestDate);
        btnSuggestDate.setOnClickListener(v -> fetchAndShowAttendanceSuggestions(containerDateDetails));

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSubmit.setOnClickListener(v -> {
            String title = edtTitle.getText().toString().trim();
            String desc = edtDesc.getText().toString().trim();

            if (title.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show();
                return;
            }

            List<com.example.myapplication.model.RequestDetail> details = new ArrayList<>();
            if (currentType[0] != com.example.myapplication.model.RequestType.RESIGNATION) {
                for (int i = 0; i < containerDateDetails.getChildCount(); i++) {
                    View row = containerDateDetails.getChildAt(i);
                    TextView tvDate = row.findViewById(R.id.tvSelectedDate);
                    String dateStr = tvDate.getText().toString();
                    if (dateStr.isEmpty() || dateStr.contains("Chọn ngày") || dateStr.contains("Ngày làm việc cuối cùng")) {
                        Toast.makeText(this, "Vui lòng chọn ngày cho tất cả các dòng", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    com.example.myapplication.model.RequestDetail detail = new com.example.myapplication.model.RequestDetail();
                    detail.setSpecificDate(dateStr);

                    if (currentType[0] == com.example.myapplication.model.RequestType.LEAVE_ANNUAL
                            || currentType[0] == com.example.myapplication.model.RequestType.LEAVE_UNPAID
                            || currentType[0] == com.example.myapplication.model.RequestType.SICK_LEAVE) {
                        android.widget.Spinner spinnerSession = row.findViewById(R.id.spinnerSession);
                        detail.setLeaveSession(com.example.myapplication.model.LeaveSession.values()[spinnerSession.getSelectedItemPosition()]);
                    } else if (currentType[0] == com.example.myapplication.model.RequestType.OVERTIME) {
                        EditText edtHours = row.findViewById(R.id.edtOvertimeHours);
                        String hoursStr = edtHours.getText().toString();
                        if (hoursStr.isEmpty()) {
                            Toast.makeText(this, "Vui lòng nhập số giờ làm thêm", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        detail.setOvertimeHours(Double.parseDouble(hoursStr));
                    } else if (currentType[0] == com.example.myapplication.model.RequestType.PUNCH_CORRECTION) {
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

            if (details.isEmpty() && currentType[0] != com.example.myapplication.model.RequestType.RESIGNATION) {
                Toast.makeText(this, "Vui lòng thêm ít nhất một ngày", Toast.LENGTH_SHORT).show();
                return;
            }

            // Frontend validation: max 3 ngày cho nghỉ phép/nghỉ ốm
            if ((currentType[0] == com.example.myapplication.model.RequestType.LEAVE_ANNUAL
                    || currentType[0] == com.example.myapplication.model.RequestType.LEAVE_UNPAID
                    || currentType[0] == com.example.myapplication.model.RequestType.SICK_LEAVE)
                    && details.size() > MAX_LEAVE_DAYS) {
                Toast.makeText(this, "Đơn nghỉ phép tối đa chỉ được " + MAX_LEAVE_DAYS + " ngày", Toast.LENGTH_SHORT).show();
                return;
            }

            // Frontend validation: ngày phải >= hôm nay (trừ nghỉ ốm và bổ sung công)
            if (currentType[0] != com.example.myapplication.model.RequestType.SICK_LEAVE 
                    && currentType[0] != com.example.myapplication.model.RequestType.PUNCH_CORRECTION) {
                java.util.Calendar todayCal = java.util.Calendar.getInstance();
                String todayStr = String.format("%04d-%02d-%02d",
                        todayCal.get(java.util.Calendar.YEAR),
                        todayCal.get(java.util.Calendar.MONTH) + 1,
                        todayCal.get(java.util.Calendar.DAY_OF_MONTH));
                for (com.example.myapplication.model.RequestDetail d : details) {
                    if (d.getSpecificDate() != null && d.getSpecificDate().compareTo(todayStr) < 0) {
                        Toast.makeText(this, "Ngày " + d.getSpecificDate() + " đã qua. Chỉ được chọn ngày hôm nay hoặc tương lai.", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            }

            com.example.myapplication.model.CreateRequestRequest createReq = 
                    new com.example.myapplication.model.CreateRequestRequest(title, currentType[0], details, desc);
            
            if (currentType[0] == com.example.myapplication.model.RequestType.SICK_LEAVE && selectedEvidencePath != null) {
                uploadImageThenCreate(createReq, Uri.parse(selectedEvidencePath), dialog);
            } else {
                performCreateRequest(createReq, dialog);
            }
        });

        dialog.show();
    }

    private void uploadImageThenCreate(com.example.myapplication.model.CreateRequestRequest createReq, Uri uri, Dialog dialog) {
        try {
            // Create temporary file from Uri
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
                        String serverUrl = response.body().get("fileUrl");
                        String serverFileName = response.body().get("fileName");
                        createReq.setFileUrl(serverUrl);
                        createReq.setFileName(serverFileName);
                        performCreateRequest(createReq, dialog);
                    } else {
                        Toast.makeText(RequestActivity.this, "Lỗi upload ảnh: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<Map<String, String>> call, Throwable t) {
                    Toast.makeText(RequestActivity.this, "Lỗi upload: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi xử lý file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void performCreateRequest(com.example.myapplication.model.CreateRequestRequest createReq, Dialog dialog) {
        ApiService apiService = RetrofitClient.getApiService(this);
        apiService.createRequest(currentEmployeeId, createReq).enqueue(new Callback<Request>() {
            @Override
            public void onResponse(Call<Request> call, Response<Request> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(RequestActivity.this, "Tạo đơn thành công", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    fetchRequests(currentStatusFilter);
                    fetchLeaveBalance();
                } else {
                    Toast.makeText(RequestActivity.this, getErrorMessage(response), Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<Request> call, Throwable t) {
                Toast.makeText(RequestActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addRequestDateRow(android.widget.LinearLayout container, com.example.myapplication.model.RequestType type, android.widget.Spinner spinnerType, com.example.myapplication.model.RequestType[] types) {
        View row = getLayoutInflater().inflate(R.layout.item_request_date, container, false);
        
        TextView tvDate = row.findViewById(R.id.tvSelectedDate);
        tvDate.setOnClickListener(v -> {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            if (!tvDate.getText().toString().contains("Chọn ngày")) {
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                    cal.setTime(sdf.parse(tvDate.getText().toString()));
                } catch (Exception e) {}
            }
            
            android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                java.util.Calendar selectedCal = java.util.Calendar.getInstance();
                selectedCal.set(year, month, dayOfMonth);
                int dayOfWeek = selectedCal.get(java.util.Calendar.DAY_OF_WEEK);

                // Fetch current type from dialog's spinner or the captured array
                com.example.myapplication.model.RequestType activeType = types[spinnerType.getSelectedItemPosition()];

                if (activeType == com.example.myapplication.model.RequestType.LEAVE_ANNUAL
                        || activeType == com.example.myapplication.model.RequestType.LEAVE_UNPAID
                        || activeType == com.example.myapplication.model.RequestType.SICK_LEAVE) {
                    if (dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY) {
                        Toast.makeText(this, "Không được chọn ngày nghỉ cuối tuần (Thứ 7, Chủ nhật)", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                String dateStr = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                tvDate.setText(dateStr);
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH));

            // Fetch current type for calendar restriction
            com.example.myapplication.model.RequestType activeType = types[spinnerType.getSelectedItemPosition()];
            if (activeType != com.example.myapplication.model.RequestType.SICK_LEAVE 
                    && activeType != com.example.myapplication.model.RequestType.PUNCH_CORRECTION) {
                datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            }
            datePickerDialog.show();
        });

        android.widget.Spinner spinnerSession = row.findViewById(R.id.spinnerSession);
        String[] sessions = {"Sáng", "Chiều", "Cả ngày"};
        android.widget.ArrayAdapter<String> sessionAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sessions);
        sessionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSession.setAdapter(sessionAdapter);
        spinnerSession.setSelection(2); // Default to ALL_DAY

        row.findViewById(R.id.btnRemoveDate).setOnClickListener(v -> {
            if (container.getChildCount() > 1) {
                container.removeView(row);
            } else {
                Toast.makeText(this, "Phải có ít nhất một ngày", Toast.LENGTH_SHORT).show();
            }
        });

        // Setup TimePickers for CheckIn/CheckOut
        TextView tvCheckIn = row.findViewById(R.id.tvCheckIn);
        TextView tvCheckOut = row.findViewById(R.id.tvCheckOut);
        tvCheckIn.setOnClickListener(v -> showTimePicker(tvCheckIn));
        tvCheckOut.setOnClickListener(v -> showTimePicker(tvCheckOut));

        updateRowVisibility(row, type);
        container.addView(row);
    }

    private void showTimePicker(TextView textView) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        new android.app.TimePickerDialog(this, (view, hourOfDay, minute) -> {
            textView.setText(String.format("%02d:%02d", hourOfDay, minute));
        }, cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), true).show();
    }

    private void fetchAndShowAttendanceSuggestions(android.widget.LinearLayout container) {
        ApiService apiService = RetrofitClient.getApiService(this);
        apiService.getAttendanceByMonth(currentEmployeeId, selectedMonth, selectedYear).enqueue(new Callback<List<com.example.myapplication.model.Attendance>>() {
            @Override
            public void onResponse(Call<List<com.example.myapplication.model.Attendance>> call, Response<List<com.example.myapplication.model.Attendance>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<com.example.myapplication.model.Attendance> existingList = response.body();
                    List<com.example.myapplication.model.Attendance> suggestions = new ArrayList<>();
                    
                    java.util.Calendar calendar = java.util.Calendar.getInstance();
                    calendar.set(selectedYear, selectedMonth - 1, 1);
                    int maxDay = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
                    
                    // Iterate through all days of the month
                    for (int day = 1; day <= maxDay; day++) {
                        calendar.set(selectedYear, selectedMonth - 1, day);
                        int dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK);
                        
                        // Skip future dates
                        if (calendar.getTimeInMillis() > System.currentTimeMillis()) break;
                        
                        // Skip weekends
                        if (dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY) continue;
                        
                        String dateStr = String.format("%04d-%02d-%02d", selectedYear, selectedMonth, day);
                        
                        // Find existing record
                        com.example.myapplication.model.Attendance record = null;
                        for (com.example.myapplication.model.Attendance a : existingList) {
                            if (dateStr.equals(a.getDate())) {
                                record = a;
                                break;
                            }
                        }
                        
                        if (record == null) {
                            // No record at all = COMPLETELY ABSENT (Grey in grid)
                            com.example.myapplication.model.Attendance mock = new com.example.myapplication.model.Attendance();
                            mock.setDate(dateStr);
                            mock.setStatus("ABSENT");
                            suggestions.add(mock);
                        } else if (record.getCheckIn() == null || record.getCheckOut() == null || "ABSENT".equals(record.getStatus())) {
                            // Has record but missing punch or status is ABSENT
                            suggestions.add(record);
                        }
                    }

                    if (suggestions.isEmpty()) {
                        Toast.makeText(RequestActivity.this, "Không tìm thấy ngày nào cần bổ sung công trong tháng này", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String[] items = new String[suggestions.size()];
                    for (int i = 0; i < suggestions.size(); i++) {
                        com.example.myapplication.model.Attendance a = suggestions.get(i);
                        String reason = "Nghỉ không phép";
                        if (a.getCheckIn() != null && a.getCheckOut() == null) reason = "Thiếu giờ ra";
                        else if (a.getCheckIn() == null && a.getCheckOut() != null) reason = "Thiếu giờ vào";
                        items[i] = a.getDate() + " (" + reason + ")";
                    }

                    new android.app.AlertDialog.Builder(RequestActivity.this)
                            .setTitle("Chọn ngày cần bổ sung công (" + selectedMonth + "/" + selectedYear + ")")
                            .setItems(items, (dialog, which) -> {
                                com.example.myapplication.model.Attendance selected = suggestions.get(which);
                                // Kiểm tra ngày này đã có trong danh sách chưa
                                boolean exists = false;
                                for (int i = 0; i < container.getChildCount(); i++) {
                                    TextView tvDate = container.getChildAt(i).findViewById(R.id.tvSelectedDate);
                                    if (selected.getDate().equals(tvDate.getText().toString())) {
                                        exists = true;
                                        break;
                                    }
                                }
                                
                                if (!exists) {
                                    // Xóa dòng mặc định nếu nó chưa chọn ngày
                                    if (container.getChildCount() == 1) {
                                        TextView tvDate = container.getChildAt(0).findViewById(R.id.tvSelectedDate);
                                        if (tvDate.getText().toString().contains("Chọn ngày")) {
                                            container.removeAllViews();
                                        }
                                    }
                                    
                                    addRequestDateWithData(container, selected.getDate());
                                } else {
                                    Toast.makeText(RequestActivity.this, "Ngày này đã được thêm", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("Đóng", null)
                            .show();
                }
            }

            @Override public void onFailure(Call<List<com.example.myapplication.model.Attendance>> call, Throwable t) {
                Toast.makeText(RequestActivity.this, "Lỗi khi lấy dữ liệu điểm danh", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addRequestDateWithData(android.widget.LinearLayout container, String date) {
        View row = getLayoutInflater().inflate(R.layout.item_request_date, container, false);
        TextView tvDate = row.findViewById(R.id.tvSelectedDate);
        tvDate.setText(date);
        
        // Vẫn cho phép nhấn để chọn lại ngày khác nếu muốn
        tvDate.setOnClickListener(v -> {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                cal.setTime(sdf.parse(date));
            } catch (Exception e) {}
            
            android.app.DatePickerDialog dp = new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                String newDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                tvDate.setText(newDate);
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH));
            
            // Luôn cho phép chọn ngày quá khứ vì đây là từ Gợi ý (thường là PUNCH_CORRECTION)
            dp.show();
        });

        row.findViewById(R.id.btnRemoveDate).setOnClickListener(v -> container.removeView(row));
        
        // Setup TimePickers
        TextView tvCheckIn = row.findViewById(R.id.tvCheckIn);
        TextView tvCheckOut = row.findViewById(R.id.tvCheckOut);
        tvCheckIn.setOnClickListener(v1 -> showTimePicker(tvCheckIn));
        tvCheckOut.setOnClickListener(v1 -> showTimePicker(tvCheckOut));

        updateRowVisibility(row, com.example.myapplication.model.RequestType.PUNCH_CORRECTION);
        container.addView(row);
    }

    private void updateRowVisibility(View row, com.example.myapplication.model.RequestType type) {
        View spinnerSession = row.findViewById(R.id.spinnerSession);
        View edtOvertime = row.findViewById(R.id.edtOvertimeHours);
        View tvCheckIn = row.findViewById(R.id.tvCheckIn);
        View tvCheckOut = row.findViewById(R.id.tvCheckOut);

        if (type == com.example.myapplication.model.RequestType.LEAVE_ANNUAL
                || type == com.example.myapplication.model.RequestType.LEAVE_UNPAID
                || type == com.example.myapplication.model.RequestType.SICK_LEAVE) {
            spinnerSession.setVisibility(View.VISIBLE);
            edtOvertime.setVisibility(View.GONE);
            tvCheckIn.setVisibility(View.GONE);
            tvCheckOut.setVisibility(View.GONE);
        } else if (type == com.example.myapplication.model.RequestType.OVERTIME) {
            spinnerSession.setVisibility(View.GONE);
            edtOvertime.setVisibility(View.VISIBLE);
            tvCheckIn.setVisibility(View.GONE);
            tvCheckOut.setVisibility(View.GONE);
        } else if (type == com.example.myapplication.model.RequestType.RESIGNATION) {
            spinnerSession.setVisibility(View.GONE);
            edtOvertime.setVisibility(View.GONE);
            tvCheckIn.setVisibility(View.GONE);
            tvCheckOut.setVisibility(View.GONE);
            
            // Đổi hint cho ngày của đơn thôi việc
            TextView tvDate = row.findViewById(R.id.tvSelectedDate);
            if (tvDate.getText().toString().isEmpty() || tvDate.getText().toString().equals("Chọn ngày")) {
                tvDate.setText("Ngày làm việc cuối cùng");
            }
        } else {
            spinnerSession.setVisibility(View.GONE);
            edtOvertime.setVisibility(View.GONE);
            tvCheckIn.setVisibility(View.GONE);
            tvCheckOut.setVisibility(View.GONE);
        }
    }

    private String getRequestTypeDisplay(com.example.myapplication.model.RequestType type) {
        if (type == null) return "";
        switch (type) {
            case LEAVE_ANNUAL: return "Nghỉ phép năm";
            case LEAVE_UNPAID: return "Nghỉ không lương";
            case SICK_LEAVE: return "Nghỉ ốm";
            case OVERTIME: return "Làm thêm giờ";
            case BUSINESS_TRIP: return "Công tác";
            case PUNCH_CORRECTION: return "Bổ sung công";
            case RESIGNATION: return "Thôi việc";
            default: return "Khác";
        }
    }

    private void fetchLeaveBalance() {
        if (currentEmployeeId == null) return;
        
        ApiService apiService = RetrofitClient.getApiService(this);
        apiService.getEmployeeById(currentEmployeeId).enqueue(new Callback<com.example.myapplication.model.Employee>() {
            @Override
            public void onResponse(Call<com.example.myapplication.model.Employee> call, Response<com.example.myapplication.model.Employee> response) {
                if (response.isSuccessful() && response.body() != null) {
                    com.example.myapplication.model.Employee emp = response.body();
                    double remaining = emp.getRemainingLeave();
                    tvLeaveBalance.setText(String.format("Quỹ phép: %.1f ngày", remaining));
                    layoutLeaveBalance.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onFailure(Call<com.example.myapplication.model.Employee> call, Throwable t) {
                // Ignore failure for balance
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            selectedEvidencePath = imageUri.toString(); 
            
            if (dialogTvPath != null) {
                dialogTvPath.setText("Đã chọn: " + imageUri.getLastPathSegment());
            }
            if (dialogIvPreview != null) {
                dialogIvPreview.setVisibility(View.VISIBLE);
                dialogIvPreview.setImageURI(imageUri);
            }
        }
    }

    private String getErrorMessage(retrofit2.Response<?> response) {
        try {
            if (response != null && response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                if (errorBody.contains("\"message\":\"")) {
                    return errorBody.split("\"message\":\"")[1].split("\"")[0];
                }
                return "Lỗi Server: " + response.code();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response != null ? "Mã lỗi: " + response.code() : "Lỗi không xác định";
    }
}
