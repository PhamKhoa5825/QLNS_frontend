package com.example.myapplication.ui;

import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerViewChat;
    private ChatAdapter adapter;
    private List<ChatRoom> roomList = new ArrayList<>();
    private ImageView btnBack, btnAddGroupChat;
    
    private Long currentDeptId = 1L;
    private Long currentEmployeeId = 1L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerViewChat = findViewById(R.id.recyclerViewChat);
        btnBack = findViewById(R.id.btnBackChat);
        btnAddGroupChat = findViewById(R.id.btnAddGroupChat);

        btnBack.setOnClickListener(v -> finish());
        btnAddGroupChat.setOnClickListener(v -> createGroupChat());

        recyclerViewChat.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter(this, roomList);
        recyclerViewChat.setAdapter(adapter);
    }
    
    private void createGroupChat() {
        ApiService apiService = RetrofitClient.getApiService(this);
        Call<ChatRoom> call = apiService.createDepartmentGroupChat("Nhóm Phòng Ban", currentDeptId, currentEmployeeId);
        call.enqueue(new Callback<ChatRoom>() {
            @Override
            public void onResponse(Call<ChatRoom> call, Response<ChatRoom> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(ChatActivity.this, "Đã tạo nhóm: " + response.body().getName(), Toast.LENGTH_SHORT).show();
                    roomList.add(response.body());
                    adapter.setRoomList(roomList);
                } else {
                    Toast.makeText(ChatActivity.this, "Lỗi tạo nhóm", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ChatRoom> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
