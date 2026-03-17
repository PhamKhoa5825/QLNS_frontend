package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
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
    
    private Long currentUserId;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        currentUserId = SharedPrefsManager.getInstance(this).getEmployeeId();
        apiService = RetrofitClient.getApiService(this);

        recyclerViewChat = findViewById(R.id.recyclerViewChat);
        btnBack = findViewById(R.id.btnBackChat);
        btnAddChat = findViewById(R.id.btnAddGroupChat); // Button to start new chat

        btnBack.setOnClickListener(v -> finish());
        btnAddChat.setOnClickListener(v -> {
            Intent intent = new Intent(ChatActivity.this, SelectEmployeeActivity.class);
            startActivity(intent);
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
                }
            }

            @Override
            public void onFailure(Call<List<ChatRoom>> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Lỗi tải phòng chat", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
