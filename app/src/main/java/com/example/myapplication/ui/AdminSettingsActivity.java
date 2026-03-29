package com.example.myapplication.ui;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.example.myapplication.model.CompanySettings;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminSettingsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_CODE = 3001;

    private TextInputEditText etCompanyName, etBaseLat, etBaseLng, etAllowedRadius;
    private TextInputEditText etWorkStart, etMorningEnd, etAfternoonStart, etWorkEnd;
    private MaterialButton btnSave;
    private FloatingActionButton fabMyLocation;
    private ProgressBar progressBar;

    private ApiService apiService;
    private GoogleMap mMap;
    private Marker companyMarker;
    private Circle radiusCircle;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    // Default coordinates (HCMC)
    private double selectedLat = 10.7769;
    private double selectedLng = 106.7009;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_admin_settings);

        // Adjust topBar for status bar height
        View topBar = findViewById(R.id.topBar);
        ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int dp16 = (int) (16 * getResources().getDisplayMetrics().density);
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) v.getLayoutParams();
            params.topMargin = dp16 + statusBarHeight;
            v.setLayoutParams(params);
            return insets;
        });

        apiService = RetrofitClient.getApiService(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        bindViews();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapSettings);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        loadSettings();
    }

    private void bindViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        etCompanyName = findViewById(R.id.etCompanyName);
        etBaseLat = findViewById(R.id.etBaseLat);
        etBaseLng = findViewById(R.id.etBaseLng);
        etAllowedRadius = findViewById(R.id.etAllowedRadius);
        etWorkStart = findViewById(R.id.etWorkStart);
        etMorningEnd = findViewById(R.id.etMorningEnd);
        etAfternoonStart = findViewById(R.id.etAfternoonStart);
        etWorkEnd = findViewById(R.id.etWorkEnd);
        progressBar = findViewById(R.id.progressBar);
        btnSave = findViewById(R.id.btnSave);
        fabMyLocation = findViewById(R.id.fabMyLocation);

        etWorkStart.setOnClickListener(v -> showTimePicker(etWorkStart));
        etMorningEnd.setOnClickListener(v -> showTimePicker(etMorningEnd));
        etAfternoonStart.setOnClickListener(v -> showTimePicker(etAfternoonStart));
        etWorkEnd.setOnClickListener(v -> showTimePicker(etWorkEnd));
        
        btnSave.setOnClickListener(v -> doSave());
        fabMyLocation.setOnClickListener(v -> goToMyLocation());
    }

    // ── GOOGLE MAPS ──────────────────────────────────────────────

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        mMap.setOnMapClickListener(latLng -> {
            selectedLat = latLng.latitude;
            selectedLng = latLng.longitude;
            updateMapMarker(latLng);
            fillLatLng(selectedLat, selectedLng);
        });

        // Move to current selection
        LatLng initial = new LatLng(selectedLat, selectedLng);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initial, 15f));
    }

    private void updateMapMarker(LatLng latLng) {
        if (companyMarker != null) companyMarker.remove();
        if (radiusCircle != null) radiusCircle.remove();

        companyMarker = mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Vị trí công ty")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        int radius = 1000;
        try {
            String rText = etAllowedRadius.getText().toString().trim();
            if (!rText.isEmpty()) radius = Integer.parseInt(rText);
        } catch (Exception ignored) {}

        radiusCircle = mMap.addCircle(new CircleOptions()
                .center(latLng)
                .radius(radius)
                .strokeWidth(2)
                .strokeColor(Color.parseColor("#1A73E8"))
                .fillColor(Color.parseColor("#261A73E8")));

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
    }

    private void fillLatLng(double lat, double lng) {
        etBaseLat.setText(String.format(Locale.US, "%.6f", lat));
        etBaseLng.setText(String.format(Locale.US, "%.6f", lng));
    }

    // ── GPS ──────────────────────────────────────────────────────

    private void goToMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
                    LOCATION_PERMISSION_CODE);
            return;
        }

        Toast.makeText(this, "Đang định vị...", Toast.LENGTH_SHORT).show();

        LocationRequest req = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMaxUpdates(1)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                fusedLocationClient.removeLocationUpdates(this);
                if (result.getLastLocation() != null) {
                    selectedLat = result.getLastLocation().getLatitude();
                    selectedLng = result.getLastLocation().getLongitude();
                    LatLng myPos = new LatLng(selectedLat, selectedLng);
                    updateMapMarker(myPos);
                    fillLatLng(selectedLat, selectedLng);
                    Toast.makeText(AdminSettingsActivity.this, "Đã cập nhật vị trí hiện tại", Toast.LENGTH_SHORT).show();
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(req, locationCallback, Looper.getMainLooper());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE && grantResults.length > 0 && 
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            goToMyLocation();
        }
    }

    // ── TIME PICKER ──────────────────────────────────────────────

    private void showTimePicker(TextInputEditText target) {
        int hour = 8, minute = 0;
        try {
            String[] parts = target.getText().toString().split(":");
            hour = Integer.parseInt(parts[0]);
            minute = Integer.parseInt(parts[1]);
        } catch (Exception ignored) {}

        new TimePickerDialog(this, (view, h, m) ->
                target.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m)),
                hour, minute, true).show();
    }

    // ── API LOAD/SAVE ────────────────────────────────────────────

    private void loadSettings() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getCompanySettings().enqueue(new Callback<CompanySettings>() {
            @Override
            public void onResponse(Call<CompanySettings> call, Response<CompanySettings> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    CompanySettings s = response.body();
                    etCompanyName.setText(s.companyName);
                    etAllowedRadius.setText(s.allowedRadius != null ? String.valueOf(s.allowedRadius) : "1000");
                    etWorkStart.setText(s.workStartTime != null ? s.workStartTime : "08:00");
                    etMorningEnd.setText(s.morningEndTime != null ? s.morningEndTime : "12:00");
                    etAfternoonStart.setText(s.afternoonStartTime != null ? s.afternoonStartTime : "13:00");
                    etWorkEnd.setText(s.workEndTime != null ? s.workEndTime : "17:30");

                    if (s.baseLat != null && s.baseLng != null) {
                        selectedLat = s.baseLat;
                        selectedLng = s.baseLng;
                        fillLatLng(selectedLat, selectedLng);
                        if (mMap != null) {
                            updateMapMarker(new LatLng(selectedLat, selectedLng));
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<CompanySettings> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminSettingsActivity.this, "Lỗi khi tải cấu hình", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void doSave() {
        CompanySettings req = new CompanySettings();
        req.companyName = etCompanyName.getText().toString().trim();
        req.workStartTime = etWorkStart.getText().toString().trim();
        req.morningEndTime = etMorningEnd.getText().toString().trim();
        req.afternoonStartTime = etAfternoonStart.getText().toString().trim();
        req.workEndTime = etWorkEnd.getText().toString().trim();

        try {
            req.baseLat = Double.parseDouble(etBaseLat.getText().toString());
            req.baseLng = Double.parseDouble(etBaseLng.getText().toString());
            req.allowedRadius = Integer.parseInt(etAllowedRadius.getText().toString());
        } catch (Exception ignored) {}

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        apiService.updateCompanySettings(req).enqueue(new Callback<CompanySettings>() {
            @Override
            public void onResponse(Call<CompanySettings> call, Response<CompanySettings> response) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(AdminSettingsActivity.this, "✅ Đã lưu cấu hình thành công!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AdminSettingsActivity.this, "Lỗi khi lưu (Code: " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CompanySettings> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(AdminSettingsActivity.this, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
