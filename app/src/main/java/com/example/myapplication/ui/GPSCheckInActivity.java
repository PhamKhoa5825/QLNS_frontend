package com.example.myapplication.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.R;
import com.example.myapplication.model.TimekeepingRequest;
import com.example.myapplication.utils.SharedPrefsManager;
import com.example.myapplication.viewmodel.AttendanceViewModel;
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
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class GPSCheckInActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private TextView tvCurrentTime, tvCurrentDate, tvDistance, tvStatusMessage;
    private ImageView ivStatusIcon;
    private LinearLayout statusBox;
    private MaterialButton btnConfirmCheckIn;
    private ImageButton btnBack;
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateTimeRunnable;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    
    private Marker userMarker;
    private boolean isFirstLocationUpdate = true;
    private Location currentLocation;

    private AttendanceViewModel attendanceViewModel;
    private boolean isCheckInAction = true;

    // Dynamic Office Location Config
    private LatLng officeLocation;
    private double checkInRadius = 500.0; // Default fallback

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps_checkin);

        isCheckInAction = getIntent().getBooleanExtra("isCheckInAction", true);
        attendanceViewModel = new ViewModelProvider(this).get(AttendanceViewModel.class);

        initViews();
        setupClock();
        observeViewModel();
        
        // Fetch office location from backend
        attendanceViewModel.fetchCompanySettings(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        btnBack.setOnClickListener(v -> finish());
        btnConfirmCheckIn.setOnClickListener(v -> handleAttendanceAction());
    }

    private void observeViewModel() {
        attendanceViewModel.attendanceActionResponse.observe(this, response -> {
            if (response != null) {
                String action = isCheckInAction ? "Check-in" : "Check-out";
                Toast.makeText(this, action + " thành công!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        attendanceViewModel.errorMessage.observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                btnConfirmCheckIn.setEnabled(true);
            }
        });

        attendanceViewModel.isLoading.observe(this, isLoading -> {
            btnConfirmCheckIn.setEnabled(!isLoading && officeLocation != null);
            btnConfirmCheckIn.setAlpha((isLoading || officeLocation == null) ? 0.5f : 1.0f);
        });

        attendanceViewModel.companySettings.observe(this, settings -> {
            if (settings != null) {
                officeLocation = new LatLng(settings.baseLat, settings.baseLng);
                checkInRadius = settings.allowedRadius != null ? settings.allowedRadius.doubleValue() : 500.0;
                
                // Update map markers/circle if map is ready
                if (mMap != null) {
                    refreshMapMarkers();
                }
                
                // Trình cập nhật UI nếu đã có vị trí hiện tại
                if (currentLocation != null) {
                    updateUIWithLocation(currentLocation);
                }
            }
        });
    }

    private void refreshMapMarkers() {
        if (mMap == null || officeLocation == null) return;
        mMap.clear();
        mMap.addMarker(new MarkerOptions()
                .position(officeLocation)
                .title("Văn phòng")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                
        mMap.addCircle(new CircleOptions()
                .center(officeLocation)
                .radius(checkInRadius)
                .strokeWidth(2)
                .strokeColor(Color.parseColor("#1A73E8"))
                .fillColor(Color.parseColor("#261A73E8")));
                
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(officeLocation, 15f));
        
        // Re-add user marker if exists
        if (currentLocation != null) {
            LatLng userLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            userMarker = mMap.addMarker(new MarkerOptions()
                    .position(userLatLng)
                    .title("Bạn")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        }
    }

    private void handleAttendanceAction() {
        Long empId = SharedPrefsManager.getInstance(this).getEmployeeId();

        if (empId == -1L || currentLocation == null) {
            Toast.makeText(this, "Không thể xác định thông tin nhân viên hoặc vị trí", Toast.LENGTH_SHORT).show();
            return;
        }

        TimekeepingRequest request = new TimekeepingRequest(
                empId,
                currentLocation.getLatitude(),
                currentLocation.getLongitude()
        );
        
        if (isCheckInAction) {
            attendanceViewModel.checkIn(request);
        } else {
            attendanceViewModel.checkOut(request);
        }
    }

    private void initViews() {
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        tvDistance = findViewById(R.id.tvDistance);
        tvStatusMessage = findViewById(R.id.tvStatusMessage);
        ivStatusIcon = findViewById(R.id.ivStatusIcon);
        statusBox = findViewById(R.id.statusBox);
        btnConfirmCheckIn = findViewById(R.id.btnConfirmCheckIn);
        btnBack = findViewById(R.id.btnBack);

        btnConfirmCheckIn.setText(isCheckInAction ? "Xác nhận Check-in" : "Xác nhận Check-out");
    }

    private void setupClock() {
        updateTimeRunnable = new Runnable() {
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                tvCurrentTime.setText(timeFormat.format(calendar.getTime()));
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d MMMM, yyyy", new Locale("vi", "VN"));
                tvCurrentDate.setText(dateFormat.format(calendar.getTime()));
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateTimeRunnable);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (officeLocation != null) {
            refreshMapMarkers();
        }
        checkLocationPermission();
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Ứng dụng cần quyền vị trí để chấm công", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startLocationUpdates() {
        if (tvDistance != null && currentLocation == null) {
            tvDistance.setText("Đang xác định vị trí...");
            tvDistance.setTextColor(Color.GRAY);
        }

        // Check if GPS is enabled
        android.location.LocationManager lm = (android.location.LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = false;
        try {
            gpsEnabled = lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
        } catch (Exception e) {}

        if (!gpsEnabled) {
            showLocationSettingsDialog();
        }

        // Request Last Known Location for immediate feedback
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null && currentLocation == null) {
                    currentLocation = location;
                    updateUIWithLocation(location);
                }
            });
        }

        // More frequent updates initially (2s) to get a fix faster
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setMinUpdateIntervalMillis(1000)
                .build();
                
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    currentLocation = location;
                    updateUIWithLocation(location);
                }
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void showLocationSettingsDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Chưa bật vị trí")
                .setMessage("Ứng dụng cần GPS để chấm công. Vui lòng bật vị trí trong cài đặt hệ thống.")
                .setPositiveButton("Cài đặt", (dialog, which) -> {
                    startActivity(new android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                })
                .setNegativeButton("Huỷ", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    private void updateUIWithLocation(Location location) {
        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (userMarker == null) {
            userMarker = mMap.addMarker(new MarkerOptions()
                    .position(userLatLng)
                    .title("Bạn")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        } else {
            userMarker.setPosition(userLatLng);
        }

        if (isFirstLocationUpdate) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 16f));
            isFirstLocationUpdate = false;
        }

        float distance = 10000; // Default far
        if (officeLocation != null) {
            float[] results = new float[1];
            Location.distanceBetween(userLatLng.latitude, userLatLng.longitude, officeLocation.latitude, officeLocation.longitude, results);
            distance = results[0];
            tvDistance.setText(String.format(Locale.getDefault(), "Khoảng cách: ~%.0fm", distance));
            tvDistance.setTextColor(Color.parseColor("#1A73E8")); // Restore primary blue
        } else {
            tvDistance.setText("Đang tải dữ liệu văn phòng...");
        }

        boolean inRadius = officeLocation != null && distance <= checkInRadius;
        tvStatusMessage.setText(inRadius ? "Vị trí hợp lệ" : "Quá xa văn phòng");
        tvStatusMessage.setTextColor(inRadius ? Color.parseColor("#15803D") : Color.parseColor("#B91C1C"));
        statusBox.setBackgroundTintList(ColorStateList.valueOf(inRadius ? Color.parseColor("#F0FDF4") : Color.parseColor("#FEF2F2")));
        ivStatusIcon.setImageResource(inRadius ? R.drawable.ic_check_circle : R.drawable.ic_nav_approve); 
        
        btnConfirmCheckIn.setEnabled(inRadius);
        btnConfirmCheckIn.setAlpha(inRadius ? 1.0f : 0.5f);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateTimeRunnable);
    }
}
