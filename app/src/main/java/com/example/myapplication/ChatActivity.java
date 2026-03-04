package com.example.myapplication;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private List<Chat> chatList;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        btnBack = findViewById(R.id.btnBackChat);
        btnBack.setOnClickListener(v -> finish()); // Quay lại

        recyclerView = findViewById(R.id.recyclerViewChat);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Nạp dữ liệu giả lập giống y hệt thiết kế
        chatList = new ArrayList<>();
        chatList.add(new Chat("Nguyễn Văn A", "Báo cáo đã gửi cho anh rồi nhé", "10:30", "IT", 2, true));
        chatList.add(new Chat("Trần Thị B", "Cuộc họp lúc 2h chiều nhé", "09:15", "Marketing", 0, true));
        chatList.add(new Chat("Lê Văn C", "Ok, mình đã nhận được file", "Hôm qua", "Kế toán", 0, false));
        chatList.add(new Chat("Phạm Thị D", "Cảm ơn bạn nhiều!", "Hôm qua", "Nhân sự", 1, false));
        chatList.add(new Chat("Nhóm IT Team", "Mai họp sprint planning nhé", "08:00", "Nhóm", 5, true));

        adapter = new ChatAdapter(chatList);
        recyclerView.setAdapter(adapter);
    }
}
