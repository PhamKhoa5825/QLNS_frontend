package com.example.myapplication.ui;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.MessageAdapter;
import com.example.myapplication.model.Message;
import com.example.myapplication.model.SendMessageRequest;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.SharedPrefsManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageActivity extends AppCompatActivity {

    private Long roomId;
    private String roomName;
    private Long currentUserId;
    private ApiService apiService;
    
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private List<Message> messageList = new ArrayList<>();
    
    private EditText edtMessage;
    private View btnSend;
    private ImageView btnBack;
    private TextView tvRoomName;

    private Handler handler = new Handler();
    private Runnable pollingRunnable;
    private static final int POLLING_INTERVAL = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        roomId = getIntent().getLongExtra("ROOM_ID", -1);
        roomName = getIntent().getStringExtra("ROOM_NAME");
        currentUserId = SharedPrefsManager.getInstance(this).getEmployeeId();
        apiService = RetrofitClient.getApiService();

        initViews();
        fetchMessages();
        startPolling();
    }

    private void initViews() {
        tvRoomName = findViewById(R.id.tvRoomName);
        tvRoomName.setText(roomName);
        
        btnBack = findViewById(R.id.btnBackMessage);
        btnBack.setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerViewMessages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MessageAdapter(this, messageList, currentUserId);
        recyclerView.setAdapter(adapter);

        edtMessage = findViewById(R.id.edtMessage);
        btnSend = findViewById(R.id.btnSendMessage);
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void fetchMessages() {
        apiService.getMessages(roomId).enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    messageList.clear();
                    messageList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    recyclerView.scrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                Toast.makeText(MessageActivity.this, "Lỗi tải tin nhắn", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String content = edtMessage.getText().toString().trim();
        if (content.isEmpty()) return;

        SendMessageRequest request = new SendMessageRequest(roomId, currentUserId, content);
        apiService.sendMessage(request).enqueue(new Callback<Message>() {
            @Override
            public void onResponse(Call<Message> call, Response<Message> response) {
                if (response.isSuccessful() && response.body() != null) {
                    edtMessage.setText("");
                    messageList.add(response.body());
                    adapter.notifyDataSetChanged();
                    recyclerView.scrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onFailure(Call<Message> call, Throwable t) {
                Toast.makeText(MessageActivity.this, "Lỗi gửi tin nhắn", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startPolling() {
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                fetchMessages();
                handler.postDelayed(this, POLLING_INTERVAL);
            }
        };
        handler.postDelayed(pollingRunnable, POLLING_INTERVAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && pollingRunnable != null) {
            handler.removeCallbacks(pollingRunnable);
        }
    }
}
