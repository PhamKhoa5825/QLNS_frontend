package com.example.myapplication;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<Notification> notiList;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        btnBack = findViewById(R.id.btnBackNoti);
        btnBack.setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerViewNoti);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Dữ liệu giả lập theo đúng thiết kế
        notiList = new ArrayList<>();
        notiList.add(new Notification("Công việc mới được giao", "Bạn có 1 công việc mới: \"Hoàn thành báo cáo tháng 1\"", "5 phút trước", "Xem công việc \u2192", Notification.TYPE_TASK, true));
        notiList.add(new Notification("Tin nhắn mới", "Nguyễn Văn A đã gửi tin nhắn cho bạn", "10 phút trước", "Xem tin nhắn \u2192", Notification.TYPE_MESSAGE, true));
        notiList.add(new Notification("Nhắc nhở cuộc họp", "Cuộc họp \"Sprint Planning\" sẽ bắt đầu trong 30 phút", "15 phút trước", "Xem lịch \u2192", Notification.TYPE_MEETING, true));
        notiList.add(new Notification("Chấm công thành công", "Bạn đã chấm công vào lúc 08:25 hôm nay", "2 giờ trước", "", Notification.TYPE_CHECKIN, false));
        notiList.add(new Notification("Thông báo từ HR", "Lương tháng 1 sẽ được chuyển vào ngày 25/01/2026", "1 ngày trước", "", Notification.TYPE_HR, false));
        notiList.add(new Notification("Hợp đồng sắp hết hạn", "Hợp đồng lao động của bạn sẽ hết hạn vào 01/06/2026", "2 ngày trước", "Xem chi tiết \u2192", Notification.TYPE_WARNING, false));

        adapter = new NotificationAdapter(notiList);
        recyclerView.setAdapter(adapter);
    }
}