package com.example.myapplication.ui;

import android.content.Intent;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.adapter.MessageAdapter;
import com.example.myapplication.adapter.MessageAdapter.OnRecallRequestListener;
import com.example.myapplication.model.Message;
import com.example.myapplication.model.ChatRoom;
import android.graphics.Typeface;
import com.example.myapplication.repository.ChatRepository;
import com.example.myapplication.service.ChatForegroundService;
import com.example.myapplication.utils.SharedPrefsManager;
import com.example.myapplication.utils.ImagePickerHelper;
import com.example.myapplication.viewmodel.MessageViewModel;
import com.example.myapplication.viewmodel.ChatRoomStore;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;
import androidx.core.content.ContextCompat;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;

public class MessageActivity extends AppCompatActivity implements OnRecallRequestListener, MessageAdapter.OnReplyRequestListener {

    private Long roomId;
    private String roomName;
    private Long myId;
    private String token;
    private String roomType;
    private String otherParticipantName;

    private MessageViewModel viewModel;
    private final ChatRoomStore roomStore = ChatRoomStore.getInstance();

    private RecyclerView rvMessages;
    private ProgressBar progressBar;
    private EditText etMessage;
    private ImageButton btnAttach;
    private ImageButton btnSend;
    private MessageAdapter adapter;

    private LinearLayout llReplyBox;
    private TextView tvReplyName;
    private TextView tvReplyContent;
    private ImageButton btnCancelReply;

    private LinearLayout llAttachmentPreview;
    private ImageView ivAttachmentPreview;
    private TextView tvAttachmentName;
    private TextView tvAttachmentMeta;
    private ImageButton btnCancelAttachment;

    private Uri pendingAttachmentUri;
    private String pendingAttachmentType;

    private ImagePickerHelper imagePickerHelper;

    private ProgressBar progressUpload;

    private LinearLayoutManager layoutManager;
    private boolean historyRenderedOnce = false;
    private boolean refreshOnResume = false;
    private boolean subscribed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        parseIntentData();
        initViews();
        setupToolbar();
        setupRecyclerView();
        setupViewModel();
        setupPickers();
        observeData();

        viewModel.loadMessages(roomId);
        viewModel.loadMembers(roomId);
        refreshOnResume = true;
        viewModel.connectAndSubscribe(roomId);
        subscribed = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        ChatForegroundService.currentOpenRoomId = roomId;
        if (refreshOnResume) {
            historyRenderedOnce = false; 
            viewModel.loadMessages(roomId);
            viewModel.loadMembers(roomId);
        }
        if (!subscribed) {
            viewModel.connectAndSubscribe(roomId);
            subscribed = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        ChatForegroundService.currentOpenRoomId = null;
    }

    private void parseIntentData() {
        roomId = getIntent().getLongExtra("roomId", -1L);
        if (roomId <= 0) roomId = getIntent().getLongExtra("ROOM_ID", -1L);

        roomName = getIntent().getStringExtra("roomName");
        if (roomName == null || roomName.isEmpty()) roomName = getIntent().getStringExtra("ROOM_NAME");
        
        roomType = getIntent().getStringExtra("roomType");
        if (roomType == null || roomType.isEmpty()) {
            roomType = getIntent().getStringExtra("ROOM_TYPE");
        }
        otherParticipantName = getIntent().getStringExtra("otherParticipantName");

        SharedPrefsManager prefs = SharedPrefsManager.getInstance(this);
        myId = prefs.getUserId();
        token = prefs.getToken();

        if (roomName == null || roomName.isEmpty()) {
            roomName = resolveDisplayName();
        }
    }

    private String resolveDisplayName() {
        if (roomType != null && "PRIVATE".equalsIgnoreCase(roomType) && otherParticipantName != null && !otherParticipantName.isEmpty()) {
            return otherParticipantName;
        }
        if (roomName != null && !roomName.isEmpty()) {
            return roomName;
        }
        return getString(R.string.chat_message_room_fallback);
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rvMessages);
        progressBar = findViewById(R.id.progressBar);
        etMessage = findViewById(R.id.etMessage);
        btnAttach = findViewById(R.id.btnAttach);
        btnSend = findViewById(R.id.btnSend);
        progressUpload = findViewById(R.id.progressUpload);
        llReplyBox = findViewById(R.id.llReplyBox);
        tvReplyName = findViewById(R.id.tvReplyName);
        tvReplyContent = findViewById(R.id.tvReplyContent);
        btnCancelReply = findViewById(R.id.btnCancelReply);

        llAttachmentPreview = findViewById(R.id.llAttachmentPreview);
        ivAttachmentPreview = findViewById(R.id.ivAttachmentPreview);
        tvAttachmentName = findViewById(R.id.tvAttachmentName);
        tvAttachmentMeta = findViewById(R.id.tvAttachmentMeta);
        btnCancelAttachment = findViewById(R.id.btnCancelAttachment);

        btnAttach.setOnClickListener(v -> showAttachSheet());
        btnCancelAttachment.setOnClickListener(v -> clearAttachmentPreview());
        btnCancelReply.setOnClickListener(v -> {
            viewModel.clearReplyTarget();
            updateReplyBox(null);
        });

        btnSend.setOnClickListener(v -> {
            if (pendingAttachmentUri != null && pendingAttachmentType != null) {
                viewModel.uploadFile(roomId, pendingAttachmentUri, pendingAttachmentType, this);
                return;
            }

            String content = etMessage.getText().toString().trim();
            if (content.isEmpty()) return;
            viewModel.sendMessage(roomId, content);
            etMessage.setText("");
            viewModel.clearReplyTarget();
            updateReplyBox(null);
        });
    }

    private void setupPickers() {
        imagePickerHelper = new ImagePickerHelper(this, new ImagePickerHelper.Listener() {
            @Override
            public void onImagePicked(@NonNull Uri uri) {
                setAttachmentPreview(uri, "IMAGE");
            }

            @Override
            public void onFilePicked(@NonNull Uri uri) {
                setAttachmentPreview(uri, "FILE");
            }

            @Override
            public void onPermissionDenied() {
                Toast.makeText(MessageActivity.this, getString(R.string.chat_permission_required), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAttachSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 24, 24, 24);

        TextView pickImage = new TextView(this);
        pickImage.setText(getString(R.string.chat_choose_image));
        pickImage.setTextSize(16);
        pickImage.setPadding(12, 12, 12, 12);
        pickImage.setOnClickListener(v -> {
            sheet.dismiss();
            imagePickerHelper.pickImage();
        });

        TextView pickFile = new TextView(this);
        pickFile.setText(getString(R.string.chat_choose_file));
        pickFile.setTextSize(16);
        pickFile.setPadding(12, 12, 12, 12);
        pickFile.setOnClickListener(v -> {
            sheet.dismiss();
            imagePickerHelper.pickFile();
        });

        layout.addView(pickImage);
        layout.addView(pickFile);
        sheet.setContentView(layout);
        sheet.show();
    }

    private void setAttachmentPreview(@NonNull Uri uri, @NonNull String type) {
        pendingAttachmentUri = uri;
        pendingAttachmentType = type;
        llAttachmentPreview.setVisibility(View.VISIBLE);

        String name = resolveDisplayName(uri);
        long size = resolveFileSize(uri);
        tvAttachmentName.setText(name);
        tvAttachmentMeta.setText(size > 0 ? formatSize(size) : "");

        if ("IMAGE".equals(type)) {
            ivAttachmentPreview.setVisibility(View.VISIBLE);
            Glide.with(this).load(uri).centerCrop().into(ivAttachmentPreview);
        } else {
            ivAttachmentPreview.setVisibility(View.VISIBLE);
            ivAttachmentPreview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            ivAttachmentPreview.setImageResource(android.R.drawable.ic_menu_save);
        }
    }

    private void clearAttachmentPreview() {
        pendingAttachmentUri = null;
        pendingAttachmentType = null;
        llAttachmentPreview.setVisibility(View.GONE);
        ivAttachmentPreview.setImageDrawable(null);
        tvAttachmentName.setText("");
        tvAttachmentMeta.setText("");
    }

    private String resolveDisplayName(@NonNull Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String value = cursor.getString(index);
                    if (value != null && !value.trim().isEmpty()) {
                        return value;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return getString(R.string.chat_attachment_default);
    }

    private long resolveFileSize(@NonNull Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (index >= 0) {
                    return cursor.getLong(index);
                }
            }
        } catch (Exception ignored) {
        }
        return -1L;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(java.util.Locale.getDefault(), "%.1f KB", kb);
        return String.format(java.util.Locale.getDefault(), "%.1f MB", kb / 1024.0);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(resolveDisplayName());
        toolbar.setNavigationOnClickListener(v -> finish());

        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_search) {
                Intent intent = new Intent(MessageActivity.this, SearchMessageActivity.class);
                intent.putExtra("roomId", roomId);
                startActivity(intent);
                return true;
            }
            if (item.getItemId() == R.id.action_info) {
                Intent intent = new Intent(MessageActivity.this, RoomInfoActivity.class);
                intent.putExtra("roomId", roomId);
                intent.putExtra("myId", myId);
                intent.putExtra("roomName", resolveDisplayName());
                intent.putExtra("roomType", roomType);
                intent.putExtra("otherParticipantName", otherParticipantName);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    private void setupRecyclerView() {
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); 
        rvMessages.setLayoutManager(layoutManager);
        adapter = new MessageAdapter(this, myId, this, this);
        rvMessages.setAdapter(adapter);

        rvMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!recyclerView.canScrollVertically(1)) {
                    Long lastId = getLastMessageId();
                    if (lastId != null) viewModel.markRoomAsSeen(roomId, lastId);
                }
            }
        });
    }

    private void setupViewModel() {
        ChatRepository repository = new ChatRepository(this);
        MessageViewModel.Factory factory = new MessageViewModel.Factory(repository, token, myId);
        viewModel = new ViewModelProvider(this, factory).get(MessageViewModel.class);
    }

    private void observeData() {
        viewModel.getMessages().observe(this, this::renderMessages);
        viewModel.getIsLoading().observe(this, loading ->
                progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));

        viewModel.getNewMessage().observe(this, message -> {
            if (message == null) return;
            adapter.addMessage(message);
            rvMessages.smoothScrollToPosition(adapter.getItemCount() - 1);

            if (isAtBottom()) {
                Long lastId = message.getId();
                if (lastId != null) viewModel.markRoomAsSeen(roomId, lastId);
            }
        });

        viewModel.getRecalledMessage().observe(this, message -> {
            if (message != null) adapter.updateRecalled(message);
        });

        viewModel.getIsUploading().observe(this, uploading -> {
            boolean show = Boolean.TRUE.equals(uploading);
            progressUpload.setVisibility(show ? View.VISIBLE : View.GONE);
            btnSend.setEnabled(!show);
            btnAttach.setEnabled(!show);
            btnCancelAttachment.setEnabled(!show);
        });

        viewModel.getUploadResult().observe(this, result -> {
            if (result == null) return;
            if (result.isSuccess()) {
                clearAttachmentPreview();
                Toast.makeText(this, getString(R.string.chat_upload_success), Toast.LENGTH_SHORT).show();
                return;
            }

            int code = result.getHttpCode();
            if (code == 401) {
                Toast.makeText(this, getString(R.string.chat_error_401), Toast.LENGTH_SHORT).show();
            } else if (code == 413) {
                Toast.makeText(this, getString(R.string.chat_error_413), Toast.LENGTH_SHORT).show();
            } else if (code == 400) {
                Toast.makeText(this, getString(R.string.chat_error_400), Toast.LENGTH_SHORT).show();
            } else {
                String message = result.getErrorMessage() != null ? result.getErrorMessage() : getString(R.string.chat_upload_failed);
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getSelectedReplyMessage().observe(this, this::updateReplyBox);
        viewModel.getMembers().observe(this, adapter::setMembers);
    }

    private boolean isPrivateRoom() {
        return roomType != null && "PRIVATE".equalsIgnoreCase(roomType);
    }

    private void renderMessages(List<Message> messages) {
        if (messages == null) {
            adapter.submitList(null);
            return;
        }

        adapter.submitList(messages);

        if (adapter.getItemCount() > 0 && !historyRenderedOnce) {
            rvMessages.scrollToPosition(adapter.getItemCount() - 1);

            Message last = adapter.getLastMessage();
            if (last != null && last.getId() != null) {
                viewModel.markRoomAsSeen(roomId, last.getId());
            }
            historyRenderedOnce = true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (viewModel != null) viewModel.disconnectRoom();
        subscribed = false;
    }

    @Override
    public void onRecallRequested(Long messageId) {
        if (messageId != null) viewModel.recallMessage(messageId);
    }

    @Override
    public void onReplyRequested(Message message) {
        viewModel.setReplyTarget(message);
        updateReplyBox(message);
    }

    private void updateReplyBox(Message message) {
        if (message == null) {
            llReplyBox.setVisibility(View.GONE);
            return;
        }
        llReplyBox.setVisibility(View.VISIBLE);
        tvReplyName.setText(message.getSenderName() != null ? message.getSenderName() : "");
        String preview = message.getMessage();
        if (preview == null || preview.isEmpty()) preview = getString(R.string.chat_reply_no_content);
        tvReplyContent.setText(preview);
    }

    private boolean isAtBottom() {
        if (layoutManager == null) return false;
        int lastPos = layoutManager.findLastCompletelyVisibleItemPosition();
        return lastPos >= adapter.getItemCount() - 1;
    }

    private Long getLastMessageId() {
        Message last = adapter != null ? adapter.getLastMessage() : null;
        return last != null ? last.getId() : null;
    }
}
