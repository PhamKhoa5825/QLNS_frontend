package com.example.myapplication.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.RequestAdapter;
import com.example.myapplication.model.CreateRequestRequest;
import com.example.myapplication.model.Request;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.SharedPrefsManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RequestActivity extends AppCompatActivity {

    private RecyclerView recyclerViewRequest;
    private RequestAdapter adapter;
    private List<Request> requestList = new ArrayList<>();
    
    private TextView tabPending, tabApproved, tabRejected;
    private TextView tvStatPending, tvStatApproved, tvStatRejected;
    private ImageView btnBackRequest;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabAddRequest;
    
    // Current manager's department
    private Long currentDeptId; 
    private Long currentEmployeeId;
    private String currentStatusFilter = "PENDING";
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);

        SharedPrefsManager prefs = SharedPrefsManager.getInstance(this);
        currentDeptId = prefs.getDepartmentId();
        currentEmployeeId = prefs.getEmployeeId();
        userRole = prefs.getRole();

        initViews();
        setupRecyclerView();
        setupListeners();
        
        // Fetch initially
        fetchRequests("PENDING");
        fetchStatsForHeader();
    }

    private void initViews() {
        recyclerViewRequest = findViewById(R.id.recyclerViewRequest);
        tabPending = findViewById(R.id.tabPending);
        tabApproved = findViewById(R.id.tabApproved);
        tabRejected = findViewById(R.id.tabRejected);
        
        tvStatPending = findViewById(R.id.tvStatPending);
        tvStatApproved = findViewById(R.id.tvStatApproved);
        tvStatRejected = findViewById(R.id.tvStatRejected);
        
        btnBackRequest = findViewById(R.id.btnBackRequest);
        fabAddRequest = findViewById(R.id.fabAddRequest);

        if ("EMPLOYEE".equalsIgnoreCase(userRole)) {
            fabAddRequest.setVisibility(android.view.View.VISIBLE);
        }
    }

    private void setupRecyclerView() {
        recyclerViewRequest.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RequestAdapter(this, requestList, new RequestAdapter.OnRequestActionClickListener() {
            @Override
            public void onApprove(Request request) {
                updateRequestStatus(request.getId(), "APPROVED");
            }

            @Override
            public void onReject(Request request) {
                showRejectDialog(request);
            }
        });
        recyclerViewRequest.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBackRequest.setOnClickListener(v -> finish());
        
        tabPending.setOnClickListener(v -> {
            currentStatusFilter = "PENDING";
            updateTabUI(tabPending);
            fetchRequests(currentStatusFilter);
        });
        
        tabApproved.setOnClickListener(v -> {
            currentStatusFilter = "APPROVED";
            updateTabUI(tabApproved);
            fetchRequests(currentStatusFilter);
        });
        
        tabRejected.setOnClickListener(v -> {
            currentStatusFilter = "REJECTED";
            updateTabUI(tabRejected);
            fetchRequests(currentStatusFilter);
        });

        fabAddRequest.setOnClickListener(v -> showCreateRequestDialog());
    }

    private void updateTabUI(TextView selectedTab) {
        tabPending.setBackground(null);
        tabApproved.setBackground(null);
        tabRejected.setBackground(null);
        
        tabPending.setTextColor(getResources().getColor(R.color.textColorSecondary));
        tabApproved.setTextColor(getResources().getColor(R.color.textColorSecondary));
        tabRejected.setTextColor(getResources().getColor(R.color.textColorSecondary));
        
        selectedTab.setBackgroundResource(R.drawable.bg_tab_selected);
        selectedTab.setTextColor(getResources().getColor(R.color.textColorPrimary));
    }

    private void fetchRequests(String status) {
        ApiService apiService = RetrofitClient.getApiService(this);
        
        Call<List<Request>> call;
        if ("ADMIN".equals(userRole)) {
            call = apiService.getAllRequestsByStatus(status);
        } else if ("MANAGER".equals(userRole)) {
            call = apiService.getRequestsByDepartmentAndStatus(currentDeptId, status);
        } else {
            // EMPLOYEE
            call = apiService.getMyRequests(currentEmployeeId);
        }
        
        call.enqueue(new Callback<List<Request>>() {
            @Override
            public void onResponse(Call<List<Request>> call, Response<List<Request>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Request> allMyRequests = response.body();
                    if ("EMPLOYEE".equals(userRole)) {
                        // Filter by status locally for employee if needed, but backend returns all orders?
                        // Let's filter locally for consistency with UI tabs
                        requestList = new ArrayList<>();
                        for (Request r : allMyRequests) {
                            if (status.equalsIgnoreCase(r.getStatus())) {
                                requestList.add(r);
                            }
                        }
                    } else {
                        requestList = allMyRequests;
                    }
                    adapter.setRequestList(requestList);
                } else {
                    Toast.makeText(RequestActivity.this, "Failed to load requests", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Request>> call, Throwable t) {
                Toast.makeText(RequestActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void fetchStatsForHeader() {
        ApiService apiService = RetrofitClient.getApiService(this);
        
        if ("EMPLOYEE".equals(userRole)) {
            apiService.getMyRequests(currentEmployeeId).enqueue(new Callback<List<Request>>() {
                @Override
                public void onResponse(Call<List<Request>> call, Response<List<Request>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        int p = 0, a = 0, r = 0;
                        for (Request req : response.body()) {
                            if ("PENDING".equalsIgnoreCase(req.getStatus())) p++;
                            else if ("APPROVED".equalsIgnoreCase(req.getStatus())) a++;
                            else if ("REJECTED".equalsIgnoreCase(req.getStatus())) r++;
                        }
                        tvStatPending.setText(String.valueOf(p));
                        tvStatApproved.setText(String.valueOf(a));
                        tvStatRejected.setText(String.valueOf(r));
                    }
                }
                @Override
                public void onFailure(Call<List<Request>> call, Throwable t) {}
            });
            return;
        }

        boolean isAdmin = "ADMIN".equals(userRole);
        
        // Pending
        Call<List<Request>> pendingCall = isAdmin 
            ? apiService.getAllRequestsByStatus("PENDING")
            : apiService.getRequestsByDepartmentAndStatus(currentDeptId, "PENDING");
        pendingCall.enqueue(new Callback<List<Request>>() {
            @Override
            public void onResponse(Call<List<Request>> call, Response<List<Request>> response) {
                if(response.isSuccessful() && response.body() != null) {
                    tvStatPending.setText(String.valueOf(response.body().size()));
                }
            }
            @Override
            public void onFailure(Call<List<Request>> call, Throwable t) {}
        });
        
        // Approved
        Call<List<Request>> approvedCall = isAdmin 
            ? apiService.getAllRequestsByStatus("APPROVED")
            : apiService.getRequestsByDepartmentAndStatus(currentDeptId, "APPROVED");
        approvedCall.enqueue(new Callback<List<Request>>() {
            @Override
            public void onResponse(Call<List<Request>> call, Response<List<Request>> response) {
                if(response.isSuccessful() && response.body() != null) {
                    tvStatApproved.setText(String.valueOf(response.body().size()));
                }
            }
            @Override
            public void onFailure(Call<List<Request>> call, Throwable t) {}
        });
        
        // Rejected
        Call<List<Request>> rejectedCall = isAdmin 
            ? apiService.getAllRequestsByStatus("REJECTED")
            : apiService.getRequestsByDepartmentAndStatus(currentDeptId, "REJECTED");
        rejectedCall.enqueue(new Callback<List<Request>>() {
            @Override
            public void onResponse(Call<List<Request>> call, Response<List<Request>> response) {
                if(response.isSuccessful() && response.body() != null) {
                    tvStatRejected.setText(String.valueOf(response.body().size()));
                }
            }
            @Override
            public void onFailure(Call<List<Request>> call, Throwable t) {}
        });
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
                    fetchStatsForHeader();
                } else {
                    Toast.makeText(RequestActivity.this, "Lỗi cập nhật", Toast.LENGTH_SHORT).show();
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

    private void showCreateRequestDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_request_create);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        EditText edtTitle = dialog.findViewById(R.id.edtRequestTitle);
        EditText edtDesc = dialog.findViewById(R.id.edtRequestDesc);
        Button btnCancel = dialog.findViewById(R.id.btnRequestCancel);
        Button btnSubmit = dialog.findViewById(R.id.btnRequestSubmit);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSubmit.setOnClickListener(v -> {
            String title = edtTitle.getText().toString().trim();
            String desc = edtDesc.getText().toString().trim();

            if (title.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show();
                return;
            }

            CreateRequestRequest req = new CreateRequestRequest(title, desc);
            ApiService apiService = RetrofitClient.getApiService(this);
            apiService.createRequest(currentEmployeeId, req).enqueue(new Callback<Request>() {
                @Override
                public void onResponse(Call<Request> call, Response<Request> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(RequestActivity.this, "Tạo đơn thành công", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        fetchRequests(currentStatusFilter);
                        fetchStatsForHeader();
                    } else {
                        Toast.makeText(RequestActivity.this, "Lỗi tạo đơn", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Request> call, Throwable t) {
                    Toast.makeText(RequestActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }
}
