package com.example.myapplication.ui;

import android.Manifest;
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
import com.example.myapplication.R;
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

    private static final String TAG = "GPSCheckInActivity";
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
    
    // Biến cho Marker vị trí người dùng (Ghim màu đỏ)
    private Marker userMarker;
    private boolean isFirstLocationUpdate = true;

    // Cấu hình vị trí: Trường Đại học Sư phạm Kỹ thuật TP.HCM (HCMUTE)
    private final LatLng officeLocation = new LatLng(10.8507, 106.7719);
    private final double checkInRadius = 500.0; // Bán kính 500 mét

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps_checkin);

        initViews();
        setupClock();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnBack.setOnClickListener(v -> finish());
        btnConfirmCheckIn.setOnClickListener(v -> Toast.makeText(GPSCheckInActivity.this, "Check-in thành công!", Toast.LENGTH_SHORT).show());
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
        
        // 1. Marker văn phòng (Màu xanh Cyan để phân biệt)
        mMap.addMarker(new MarkerOptions()
                .position(officeLocation)
                .title("ĐH Sư phạm Kỹ thuật TP.HCM")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

        // 2. Vòng tròn bán kính check-in
        mMap.addCircle(new CircleOptions()
                .center(officeLocation)
                .radius(checkInRadius)
                .strokeWidth(2)
                .strokeColor(Color.parseColor("#1A73E8"))
                .fillColor(Color.parseColor("#261A73E8")));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(officeLocation, 15f));
        mMap.getUiSettings().setZoomControlsEnabled(true);

        checkLocationPermission();
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
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

        // 1. Hiển thị Marker màu đỏ (ghim tọa độ) tại vị trí người dùng
        if (userMarker == null) {
            userMarker = mMap.addMarker(new MarkerOptions()
                    .position(userLatLng)
                    .title("Vị trí của bạn")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        } else {
            userMarker.setPosition(userLatLng);
        }

        // 2. Tự động di chuyển camera đến người dùng ở lần đầu tiên
        if (isFirstLocationUpdate) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 16f));
            isFirstLocationUpdate = false;
        }

        // 3. Tính khoảng cách
        float[] results = new float[1];
        Location.distanceBetween(userLatLng.latitude, userLatLng.longitude,
                officeLocation.latitude, officeLocation.longitude, results);
        float distanceInMeters = results[0];

        // 4. Cập nhật giao diện
        tvDistance.setText(String.format(Locale.getDefault(), "Khoảng cách: ~%.0fm", distanceInMeters));

        if (distanceInMeters <= checkInRadius) {
            tvStatusMessage.setText("Bạn đang nằm trong bán kính hợp lệ");
            tvStatusMessage.setTextColor(Color.parseColor("#15803D"));
            statusBox.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F0FDF4")));
            ivStatusIcon.setImageResource(R.drawable.ic_check_circle);
            btnConfirmCheckIn.setEnabled(true);
            btnConfirmCheckIn.setAlpha(1.0f);
        } else {
            tvStatusMessage.setText("Bạn đang nằm ngoài bán kính cho phép");
            tvStatusMessage.setTextColor(Color.parseColor("#B91C1C"));
            statusBox.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FEF2F2")));
            ivStatusIcon.setImageResource(R.drawable.ic_check_circle);
            btnConfirmCheckIn.setEnabled(false);
            btnConfirmCheckIn.setAlpha(0.5f);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Cần quyền truy cập vị trí để check-in", Toast.LENGTH_LONG).show();
            }
        }
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
        if (handler != null && updateTimeRunnable != null) {
            handler.removeCallbacks(updateTimeRunnable);
        }
    }
}
