package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class TimekeepingActivity extends AppCompatActivity {

    private ImageView btnBackTimekeeping;
    private Button btnCheckIn, btnCheckOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timekeeping);

        btnBackTimekeeping = findViewById(R.id.btnBackTimekeeping);
        btnCheckIn = findViewById(R.id.btnCheckIn);
        btnCheckOut = findViewById(R.id.btnCheckOut);

        // Nút quay lại Dashboard
        btnBackTimekeeping.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Xử lý nút Chấm công vào
        btnCheckIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(TimekeepingActivity.this, "Chấm công vào thành công!", Toast.LENGTH_SHORT).show();
            }
        });

        // Xử lý nút Chấm công ra
        btnCheckOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(TimekeepingActivity.this, "Chưa đến giờ ra ca!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
