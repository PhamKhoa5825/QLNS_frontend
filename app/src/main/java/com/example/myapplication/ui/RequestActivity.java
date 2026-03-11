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
    
    // Current manager's department
    private Long currentDeptId; 
    private String currentStatusFilter = "PENDING";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);

        currentDeptId = SharedPrefsManager.getInstance(this).getDepartmentId();

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
        Call<List<Request>> call = apiService.getRequestsByDepartmentAndStatus(currentDeptId, status);
        
        call.enqueue(new Callback<List<Request>>() {
            @Override
            public void onResponse(Call<List<Request>> call, Response<List<Request>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    requestList = response.body();
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
        // Pending
        apiService.getRequestsByDepartmentAndStatus(currentDeptId, "PENDING").enqueue(new Callback<List<Request>>() {
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
        apiService.getRequestsByDepartmentAndStatus(currentDeptId, "APPROVED").enqueue(new Callback<List<Request>>() {
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
        apiService.getRequestsByDepartmentAndStatus(currentDeptId, "REJECTED").enqueue(new Callback<List<Request>>() {
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
}
