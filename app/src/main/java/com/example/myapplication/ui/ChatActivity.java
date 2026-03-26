package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.RoomListAdapter;
import com.example.myapplication.model.ChatRoom;
import com.example.myapplication.repository.ChatRepository;
import com.example.myapplication.utils.SharedPrefsManager;
import com.example.myapplication.viewmodel.ChatViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ChatActivity extends AppCompatActivity implements CreateGroupDialog.OnGroupCreatedListener {

    private RecyclerView rvRooms;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private RoomListAdapter adapter;
    private ChatViewModel viewModel;
    private Long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initViews();
        setupRecyclerView();
        setupViewModel();
        setupActions();
        observeData();
        loadRooms();
    }

    private void initViews() {
        rvRooms = findViewById(R.id.rvRooms);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
    }

    private void setupRecyclerView() {
        rvRooms.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RoomListAdapter(room -> {
            Intent intent = new Intent(ChatActivity.this, MessageActivity.class);
            String roomName = resolveRoomName(room);

            intent.putExtra("roomId", room.getId());
            intent.putExtra("roomName", roomName);
            intent.putExtra("roomType", room.getType());
            intent.putExtra("otherParticipantName", room.getOtherParticipantName());

            // [Chat] Truyền thêm key cũ để tương thích với MessageActivity hiện tại.
            intent.putExtra("ROOM_ID", room.getId());
            intent.putExtra("ROOM_NAME", roomName);
            intent.putExtra("ROOM_TYPE", room.getType());

            startActivity(intent);
        });
        rvRooms.setAdapter(adapter);
    }

    private String resolveRoomName(ChatRoom room) {
        String type = room.getType() != null ? room.getType() : "";
        if ("PRIVATE".equalsIgnoreCase(type) && room.getOtherParticipantName() != null && !room.getOtherParticipantName().isEmpty()) {
            return room.getOtherParticipantName();
        }
        if (room.getName() != null && !room.getName().isEmpty()) {
            return room.getName();
        }
        return getString(R.string.chat_room_fallback, room.getId() != null ? room.getId() : 0L);
    }

    private void setupViewModel() {
        ChatRepository repository = new ChatRepository(this);
        ChatViewModel.Factory factory = new ChatViewModel.Factory(repository);
        viewModel = new ViewModelProvider(this, factory).get(ChatViewModel.class);
    }

    private void setupActions() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        FloatingActionButton fabAddRoom = findViewById(R.id.fabAddRoom);

        toolbar.setNavigationOnClickListener(v -> finish());

        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_create_group) {
                showCreateGroupDialog();
                return true;
            }
            return false;
        });

        fabAddRoom.setOnClickListener(v -> {
            // [Chat] FAB cũng mở ContactsActivity theo yêu cầu.
            startActivity(new Intent(ChatActivity.this, ContactsActivity.class));
        });
    }

    private void observeData() {
        // [Chat] Observe danh sách room để cập nhật RecyclerView.
        viewModel.getRooms().observe(this, chatRooms -> {
            if (chatRooms != null) {
                adapter.submitList(chatRooms);
                tvEmpty.setVisibility(chatRooms.isEmpty() ? View.VISIBLE : View.GONE);
            } else {
                adapter.submitList(null);
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText(getString(R.string.chat_load_rooms_failed));
                Toast.makeText(ChatActivity.this, getString(R.string.chat_load_rooms_failed), Toast.LENGTH_SHORT).show();
            }
        });

        // [Chat] Observe loading để hiển thị/ẩn ProgressBar.
        viewModel.getIsLoading().observe(this, loading -> {
            boolean showLoading = loading != null && loading;
            progressBar.setVisibility(showLoading ? View.VISIBLE : View.GONE);
        });

        // Theo dõi việc tạo nhóm ở Activity để điều hướng và reset state
        viewModel.getCreatedGroup().observe(this, room -> {
            if (room != null) {
                onGroupCreated(room);
                // Không reset ngay tại đây để Fragment kịp nhận sự kiện và tự đóng.
            }
        });
    }

    private void loadRooms() {
        userId = SharedPrefsManager.getInstance(this).getUserId();

        if (userId <= 0) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText(getString(R.string.chat_user_id_missing));
            Toast.makeText(this, getString(R.string.chat_user_id_missing), Toast.LENGTH_SHORT).show();
            return;
        }

        // [Chat] Trigger ViewModel gọi Repository -> API GET /api/chat/rooms/me.
        viewModel.loadRooms(userId);
    }

    private void showCreateGroupDialog() {
        FragmentManager fm = getSupportFragmentManager();
        CreateGroupDialog dialog = CreateGroupDialog.newInstance();
        dialog.show(fm, "CreateGroupDialog");
    }

    @Override
    public void onGroupCreated(ChatRoom room) {
        if (room == null) {
            return;
        }
        adapter.addRoomToTop(room);
        Intent intent = new Intent(this, MessageActivity.class);
        String roomName = resolveRoomName(room);
        intent.putExtra("ROOM_ID", room.getId());
        intent.putExtra("ROOM_NAME", roomName);
        intent.putExtra("ROOM_TYPE", room.getType());
        intent.putExtra("roomId", room.getId());
        intent.putExtra("roomName", roomName);
        intent.putExtra("roomType", room.getType());
        intent.putExtra("otherParticipantName", room.getOtherParticipantName());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Clear sự kiện createdGroup cũ để không tự động mở lại dialog khi quay về
        viewModel.resetCreatedGroup();
        // Reload phòng chat mỗi khi quay lại để thấy phòng mới tạo hoặc tin mới nhất
        loadRooms();
    }
}
