package com.example.myapplication.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.adapter.ChatRoomAdapter;
import com.example.myapplication.model.ChatModels;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText edtSearch;
    private TextView tvUnreadCount;
    private ApiService apiService;
    private SharedPreferences prefs;
    private Long userId;
    private ChatRoomAdapter adapter;
    private List<ChatModels.ChatRoomResponse> allRooms = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_internal_message);

        prefs  = getSharedPreferences("qlns_pref", MODE_PRIVATE);
        userId = prefs.getLong("userId", -1);
        apiService = RetrofitClient.getClient().create(ApiService.class);

        recyclerView  = findViewById(R.id.recyclerViewChat);
        edtSearch     = findViewById(R.id.edtSearch);
        tvUnreadCount = findViewById(R.id.tvUnreadCountHeader);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatRoomAdapter(new ArrayList<>(), this::openChatRoom);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.fabNewMessage).setOnClickListener(v ->
                Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show());

        setupSearch();
        loadRooms();
    }

    private void setupSearch() {
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                String q = s.toString().toLowerCase();
                List<ChatModels.ChatRoomResponse> filtered = allRooms.stream()
                        .filter(r -> r.name != null && r.name.toLowerCase().contains(q))
                        .collect(Collectors.toList());
                adapter.updateData(filtered);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadRooms() {
        apiService.getChatRooms(userId)
                .enqueue(new Callback<List<ChatModels.ChatRoomResponse>>() {
                    @Override
                    public void onResponse(Call<List<ChatModels.ChatRoomResponse>> call,
                                           Response<List<ChatModels.ChatRoomResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            allRooms = response.body();
                            adapter.updateData(allRooms);
                            long totalUnread = allRooms.stream().mapToLong(r -> r.unreadCount).sum();
                            tvUnreadCount.setText(totalUnread + " tin nhắn chưa đọc");
                        }
                    }
                    @Override public void onFailure(Call<List<ChatModels.ChatRoomResponse>> call, Throwable t) {
                        Toast.makeText(ChatListActivity.this, "Lỗi tải danh sách chat", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openChatRoom(ChatModels.ChatRoomResponse room) {
        Intent intent = new Intent(this, ChatRoomActivity.class);
        intent.putExtra("roomId",   room.id);
        intent.putExtra("roomName", room.name);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRooms();
    }
}