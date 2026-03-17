package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.ChatAdapter;
import com.example.myapplication.model.ChatRoom;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.SharedPrefsManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerViewChat;
    private ChatAdapter adapter;
    private List<ChatRoom> roomList = new ArrayList<>();
    private ImageView btnBack, btnAddChat;
    
    private TextView tvUnreadSummary;
    private Long currentUserId;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        currentUserId = SharedPrefsManager.getInstance(this).getEmployeeId();
        apiService = RetrofitClient.getApiService();

        recyclerViewChat = findViewById(R.id.recyclerViewChat);
        btnBack = findViewById(R.id.btnBackChat);
        btnAddChat = findViewById(R.id.btnAddGroupChat);
        tvUnreadSummary = findViewById(R.id.tvUnreadSummary);
        EditText edtSearch = findViewById(R.id.edtSearchChat);

        btnBack.setOnClickListener(v -> finish());
        btnAddChat.setOnClickListener(v -> {
            Intent intent = new Intent(ChatActivity.this, SelectEmployeeActivity.class);
            startActivity(intent);
        });

        edtSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRooms(s.toString());
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        recyclerViewChat.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter(this, roomList, currentUserId, room -> {
            Intent intent = new Intent(ChatActivity.this, MessageActivity.class);
            intent.putExtra("ROOM_ID", room.getId());
            
            String displayName = room.getName();
            if ("PRIVATE".equals(room.getType()) && room.getOtherParticipantName() != null) {
                displayName = room.getOtherParticipantName();
            } else if (displayName == null) {
                displayName = "Phòng chat " + room.getId();
            }
            
            intent.putExtra("ROOM_NAME", displayName);
            startActivity(intent);
        });
        recyclerViewChat.setAdapter(adapter);

        fetchChatRooms();
    }

    private void fetchChatRooms() {
        apiService.getChatRooms(currentUserId).enqueue(new Callback<List<ChatRoom>>() {
            @Override
            public void onResponse(Call<List<ChatRoom>> call, Response<List<ChatRoom>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    roomList.clear();
                    roomList.addAll(response.body());
                    adapter.setRoomList(roomList);
                    updateUnreadSummary();
                }
            }

            @Override
            public void onFailure(Call<List<ChatRoom>> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Lỗi tải phòng chat", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUnreadSummary() {
        int totalUnread = 0;
        for (ChatRoom room : roomList) {
            totalUnread += room.getUnreadCount();
        }
        if (tvUnreadSummary != null) {
            tvUnreadSummary.setText(totalUnread + " tin nhắn chưa đọc");
        }
    }

    private void filterRooms(String query) {
        if (query.isEmpty()) {
            adapter.setRoomList(roomList);
            return;
        }
        List<ChatRoom> filteredList = new ArrayList<>();
        for (ChatRoom room : roomList) {
            String name = room.getName();
            if ("PRIVATE".equals(room.getType()) && room.getOtherParticipantName() != null) {
                name = room.getOtherParticipantName();
            }
            if (name != null && name.toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(room);
            }
        }
        adapter.setRoomList(filteredList);
    }
}
