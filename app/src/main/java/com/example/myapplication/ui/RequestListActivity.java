package com.example.myapplication.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.RequestAdapter;
import com.example.myapplication.model.Request;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.SharedPrefsManager;
import com.example.myapplication.viewmodel.RequestViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class RequestListActivity extends AppCompatActivity implements RequestAdapter.OnRequestActionListener {

    private static final String EXTRA_EMP_ID = "empId";
    private static final String EXTRA_REQUEST_ID = "requestId";
    private static final String EXTRA_REQUEST_TITLE = "requestTitle";
    private static final String EXTRA_REQUEST_DESCRIPTION = "requestDescription";

    private RecyclerView recyclerRequests;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private RequestAdapter requestAdapter;

    private RequestViewModel viewModel;
    private long employeeId = -1L;

    private final ActivityResultLauncher<Intent> requestFormLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadRequests();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_list);

        RetrofitClient.init(this);
        employeeId = resolveEmployeeId();
        viewModel = new ViewModelProvider(this).get(RequestViewModel.class);

        initViews();
        setupRecyclerView();
        setupActions();
        observeViewModel();
        loadRequests();
    }

    private void initViews() {
        recyclerRequests = findViewById(R.id.recyclerRequests);
        progressBar = findViewById(R.id.progressRequests);
        tvEmptyState = findViewById(R.id.tvEmptyRequests);
    }

    private void setupRecyclerView() {
        requestAdapter = new RequestAdapter(this, this);
        recyclerRequests.setLayoutManager(new LinearLayoutManager(this));
        recyclerRequests.setAdapter(requestAdapter);
    }

    private void setupActions() {
        View btnBack = findViewById(R.id.btnBack);
        FloatingActionButton fabAddRequest = findViewById(R.id.fabAddRequest);

        btnBack.setOnClickListener(v -> finish());
        fabAddRequest.setOnClickListener(v -> openCreateRequest());
    }

    private long resolveEmployeeId() {
        long fromIntent = getIntent().getLongExtra(EXTRA_EMP_ID, -1L);
        if (fromIntent > 0) {
            return fromIntent;
        }

        SharedPrefsManager prefsManager = SharedPrefsManager.getInstance(this);
        long employeeIdFromPrefs = prefsManager.getEmployeeId();
        if (employeeIdFromPrefs > 0) {
            return employeeIdFromPrefs;
        }

        return prefsManager.getUserId();
    }

    private void loadRequests() {
        if (employeeId <= 0) {
            showLoading(false);
            Toast.makeText(this, "Khong tim thay nhan vien. Vui long dang nhap lai.", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.loadRequests(employeeId);
    }

    private void observeViewModel() {
        viewModel.requests.observe(this, requests -> {
            List<Request> data = requests == null ? new ArrayList<>() : requests;
            requestAdapter.submitList(data);
            toggleEmptyState(data.isEmpty());
        });

        viewModel.isLoading.observe(this, isLoading -> showLoading(Boolean.TRUE.equals(isLoading)));

        viewModel.errorMessage.observe(this, message -> {
            if (message != null && !message.trim().isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                viewModel.clearError();
            }
        });

        viewModel.deleteSuccess.observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, "Da xoa don", Toast.LENGTH_SHORT).show();
                viewModel.clearDeleteSuccessEvent();
                loadRequests();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        recyclerRequests.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
    }

    private void toggleEmptyState(boolean isEmpty) {
        tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void openCreateRequest() {
        Intent intent = new Intent(this, RequestFormActivity.class);
        intent.putExtra(EXTRA_EMP_ID, employeeId);
        requestFormLauncher.launch(intent);
    }

    private void openEditRequest(Request request) {
        Intent intent = new Intent(this, RequestFormActivity.class);
        intent.putExtra(EXTRA_EMP_ID, employeeId);
        intent.putExtra(EXTRA_REQUEST_ID, request.getId());
        intent.putExtra(EXTRA_REQUEST_TITLE, request.getTitle());
        intent.putExtra(EXTRA_REQUEST_DESCRIPTION, request.getDescription());
        requestFormLauncher.launch(intent);
    }

    @Override
    public void onEditRequest(Request request) {
        openEditRequest(request);
    }

    @Override
    public void onDeleteRequest(Request request) {
        Long requestId = request.getId();
        if (requestId == null || requestId <= 0 || employeeId <= 0) {
            Toast.makeText(this, "Don khong hop le", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Xoa don")
                .setMessage("Bạn có chắc chắn muốn xoá đơn này?")
                .setNegativeButton("Khong", null)
                .setPositiveButton("Xoa", (dialog, which) -> deleteRequest(requestId))
                .show();
    }

    private void deleteRequest(Long requestId) {
        if (requestId == null) {
            return;
        }
        viewModel.deleteRequest(requestId, employeeId);
    }
}
