package com.example.myapplication.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.adapter.MessageAdapter;
import com.example.myapplication.model.ChatModels;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatRoomActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText edtMessage;
    private ImageView btnSend, btnBack;
    private TextView tvRoomName;

    private ApiService apiService;
    private SharedPreferences prefs;
    private Long userId, roomId;
    private String roomName;
    private MessageAdapter messageAdapter;
    private Handler pollingHandler = new Handler();
    private long lastMessageId = 0L;

    private static final int POLLING_INTERVAL = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        prefs    = getSharedPreferences("qlns_pref", MODE_PRIVATE);
        userId   = prefs.getLong("userId", -1);
        roomId   = getIntent().getLongExtra("roomId", -1);
        roomName = getIntent().getStringExtra("roomName");

        apiService = RetrofitClient.getClient().create(ApiService.class);

        recyclerView = findViewById(R.id.recyclerViewMessages);
        edtMessage   = findViewById(R.id.edtMessage);
        btnSend      = findViewById(R.id.btnSend);
        btnBack      = findViewById(R.id.btnBackRoom);
        tvRoomName   = findViewById(R.id.tvRoomName);

        tvRoomName.setText(roomName);
        btnBack.setOnClickListener(v -> finish());

        messageAdapter = new MessageAdapter(new ArrayList<>(), userId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);

        btnSend.setOnClickListener(v -> sendMessage());
        loadMessages();
    }

    private void loadMessages() {
        apiService.getMessages(roomId)
                .enqueue(new Callback<List<ChatModels.MessageResponse>>() {
                    @Override
                    public void onResponse(Call<List<ChatModels.MessageResponse>> call,
                                           Response<List<ChatModels.MessageResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<ChatModels.MessageResponse> msgs = response.body();
                            messageAdapter.updateData(msgs);
                            if (!msgs.isEmpty()) {
                                lastMessageId = msgs.get(msgs.size() - 1).id;
                                recyclerView.scrollToPosition(msgs.size() - 1);
                            }
                        }
                    }
                    @Override public void onFailure(Call<List<ChatModels.MessageResponse>> call, Throwable t) {}
                });
    }

    private final Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            if (lastMessageId > 0) {
                apiService.getNewMessages(roomId, lastMessageId)
                        .enqueue(new Callback<List<ChatModels.MessageResponse>>() {
                            @Override
                            public void onResponse(Call<List<ChatModels.MessageResponse>> call,
                                                   Response<List<ChatModels.MessageResponse>> response) {
                                if (response.isSuccessful() && response.body() != null
                                        && !response.body().isEmpty()) {
                                    List<ChatModels.MessageResponse> newMsgs = response.body();
                                    messageAdapter.appendMessages(newMsgs);
                                    lastMessageId = newMsgs.get(newMsgs.size() - 1).id;
                                    recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
                                }
                            }
                            @Override public void onFailure(Call<List<ChatModels.MessageResponse>> c, Throwable t) {}
                        });
            }
            pollingHandler.postDelayed(this, POLLING_INTERVAL);
        }
    };

    private void sendMessage() {
        String text = edtMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        edtMessage.setText("");

        apiService.sendMessage(new ChatModels.SendMessageBody(roomId, userId, text))
                .enqueue(new Callback<ChatModels.MessageResponse>() {
                    @Override
                    public void onResponse(Call<ChatModels.MessageResponse> call,
                                           Response<ChatModels.MessageResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ChatModels.MessageResponse msg = response.body();
                            messageAdapter.appendMessage(msg);
                            lastMessageId = msg.id;
                            recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
                        }
                    }
                    @Override public void onFailure(Call<ChatModels.MessageResponse> call, Throwable t) {
                        Toast.makeText(ChatRoomActivity.this, "Gửi thất bại", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        pollingHandler.postDelayed(pollingRunnable, POLLING_INTERVAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pollingHandler.removeCallbacks(pollingRunnable);
    }
}