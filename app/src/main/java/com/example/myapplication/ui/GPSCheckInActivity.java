package com.example.myapplication.ui;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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

    // Cấu hình vị trí văn phòng (HCMUTE)
    private final LatLng officeLocation = new LatLng(10.8507, 106.7719);
    private final double checkInRadius = 500.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps_checkin);

        isCheckInAction = getIntent().getBooleanExtra("isCheckInAction", true);
        attendanceViewModel = new ViewModelProvider(this).get(AttendanceViewModel.class);

        initViews();
        setupClock();
        observeViewModel();

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
            btnConfirmCheckIn.setEnabled(!isLoading);
            btnConfirmCheckIn.setAlpha(isLoading ? 0.5f : 1.0f);
        });
    }

    private void handleAttendanceAction() {
        SharedPreferences prefs = getSharedPreferences("qlns_pref", Context.MODE_PRIVATE);
        long userId = prefs.getLong("userId", -1);

        if (userId == -1 || currentLocation == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("employeeId", userId);
        
        if (isCheckInAction) {
            data.put("latitude", currentLocation.getLatitude());
            data.put("longitude", currentLocation.getLongitude());
            attendanceViewModel.checkIn(data);
        } else {
            attendanceViewModel.checkOut(data);
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

    // ... (Các phần Clock, Map, Location updates giữ nguyên logic UI) ...
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
        mMap.addMarker(new MarkerOptions().position(officeLocation).title("Văn phòng").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        mMap.addCircle(new CircleOptions().center(officeLocation).radius(checkInRadius).strokeWidth(2).strokeColor(Color.parseColor("#1A73E8")).fillColor(Color.parseColor("#261A73E8")));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(officeLocation, 15f));
        checkLocationPermission();
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build();
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    currentLocation = location;
                    updateUIWithLocation(location);
                }
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void updateUIWithLocation(Location location) {
        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (userMarker == null) {
            userMarker = mMap.addMarker(new MarkerOptions().position(userLatLng).title("Bạn").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        } else userMarker.setPosition(userLatLng);
        if (isFirstLocationUpdate) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 16f));
            isFirstLocationUpdate = false;
        }
        float[] results = new float[1];
        Location.distanceBetween(userLatLng.latitude, userLatLng.longitude, officeLocation.latitude, officeLocation.longitude, results);
        float distance = results[0];
        tvDistance.setText(String.format(Locale.getDefault(), "Khoảng cách: ~%.0fm", distance));
        boolean inRadius = distance <= checkInRadius;
        tvStatusMessage.setText(inRadius ? "Vị trí hợp lệ" : "Quá xa văn phòng");
        tvStatusMessage.setTextColor(inRadius ? Color.parseColor("#15803D") : Color.parseColor("#B91C1C"));
        statusBox.setBackgroundTintList(ColorStateList.valueOf(inRadius ? Color.parseColor("#F0FDF4") : Color.parseColor("#FEF2F2")));
        btnConfirmCheckIn.setEnabled(inRadius);
        btnConfirmCheckIn.setAlpha(inRadius ? 1.0f : 0.5f);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fusedLocationClient != null) fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateTimeRunnable);
    }
}
