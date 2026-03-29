package com.example.myapplication.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.MemberAdapter;
import com.example.myapplication.adapter.SelectContactAdapter;
import com.example.myapplication.model.RoomMember;
import com.example.myapplication.repository.ChatRepository;
import com.example.myapplication.utils.SharedPrefsManager;
import com.example.myapplication.viewmodel.ChatRoomStore;
import com.example.myapplication.viewmodel.RoomInfoViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.example.myapplication.model.Contact;
import java.util.List;

public class RoomInfoActivity extends AppCompatActivity {

    private Long roomId;
    private Long myUserId;
    private String roomName;
    private String roomType;
    private String otherParticipantName;
    private boolean isPrivate;
    private boolean isDepartment;
    private boolean allowManageMembers = true;

    private RoomInfoViewModel viewModel;
    private MemberAdapter adapter;

    private RecyclerView rvMembers;
    private MaterialButton btnAddMember;
    private MaterialButton btnRename;
    private MaterialButton btnLeave;
    private View cardRoomHeader;
    private TextView tvRoomNameView;

    private boolean leavePending = false;

    private final ChatRoomStore roomStore = ChatRoomStore.getInstance();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_info);

        parseIntent();
        initViews();
        setupToolbar();
        setupRecyclerView();
        setupViewModel();
        observeData();

        // [Chat] Tải danh sách thành viên ban đầu.
        viewModel.loadMembers(roomId, myUserId);
    }

    private void parseIntent() {
        Intent intent = getIntent();
        roomId = intent.getLongExtra("roomId", -1L);
        if (roomId <= 0) roomId = intent.getLongExtra("ROOM_ID", -1L);
        myUserId = intent.getLongExtra("myUserId", -1L);
        if (myUserId <= 0) myUserId = SharedPrefsManager.getInstance(this).getUserId();
        roomName = intent.getStringExtra("roomName");
        roomType = intent.getStringExtra("roomType");
        otherParticipantName = intent.getStringExtra("otherParticipantName");
        isPrivate = roomType != null && "PRIVATE".equalsIgnoreCase(roomType);
        isDepartment = roomType != null && "DEPARTMENT".equalsIgnoreCase(roomType);
    }

    private void initViews() {
        rvMembers = findViewById(R.id.rvMembers);
        btnAddMember = findViewById(R.id.btnAddMember);
        btnRename = findViewById(R.id.btnRename);
        btnLeave = findViewById(R.id.btnLeave);
        cardRoomHeader = findViewById(R.id.cardRoomHeader);
        tvRoomNameView = findViewById(R.id.tvRoomName);
        if (tvRoomNameView != null) {
            tvRoomNameView.setText(resolveDisplayName());
        }
        applyRoomTypeRules();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(resolveDisplayName());
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new MemberAdapter(member -> confirmRemove(member));
        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        rvMembers.setAdapter(adapter);
    }

    private void setupViewModel() {
        ChatRepository repository = new ChatRepository(this);
        RoomInfoViewModel.Factory factory = new RoomInfoViewModel.Factory(repository);
        viewModel = new androidx.lifecycle.ViewModelProvider(this, factory).get(RoomInfoViewModel.class);
    }

    private void observeData() {
        viewModel.getMembers().observe(this, members -> {
            if (members != null) {
                String role = viewModel.getMyRole().getValue() != null ? viewModel.getMyRole().getValue() : "MEMBER";
                adapter.submitList(members, role, allowManageMembers);
                updateRoleUi(role);
            } else {
                Toast.makeText(this, getString(R.string.chat_load_members_failed), Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getMyRole().observe(this, this::updateRoleUi);

        viewModel.getRenamedRoom().observe(this, room -> {
            if (room != null && room.getName() != null) {
                roomName = room.getName();
                if (tvRoomNameView != null) {
                    tvRoomNameView.setText(roomName);
                }
                MaterialToolbar toolbar = findViewById(R.id.toolbar);
                if (toolbar != null) {
                    toolbar.setTitle(roomName);
                }
                roomStore.updateRoomName(roomId, roomName);
            }
        });

        viewModel.getActionSuccess().observe(this, success -> {
            if (success != null && success) {
                if (leavePending) {
                    finish();
                } else {
                    viewModel.loadMembers(roomId, myUserId);
                }
            } else if (success != null) {
                Toast.makeText(this, getString(R.string.chat_action_failed), Toast.LENGTH_SHORT).show();
            }
            leavePending = false;
        });
    }

    private void applyRoomTypeRules() {
        if (isDepartment || isPrivate) {
            // Ẩn khung header/rename cho phòng không phải GROUP
            if (cardRoomHeader != null) cardRoomHeader.setVisibility(View.GONE);
            btnRename.setVisibility(View.GONE);
            // Phòng DEPARTMENT: cho phép thêm/xóa tùy role (không ẩn cứng), vẫn không cho rời nhóm
            if (isPrivate) {
                btnAddMember.setVisibility(View.GONE);
                btnLeave.setVisibility(View.GONE);
            } else if (isDepartment) {
                btnLeave.setVisibility(View.GONE);
            }
        }
    }

    private void updateRoleUi(String role) {
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        boolean allowRename = false;
        boolean allowAddRemove = false;
        boolean allowLeave = false;

        if (isDepartment) {
            allowRename = false;
            allowAddRemove = isAdmin; // DEPARTMENT: admin được thêm/xóa thành viên
            allowLeave = false;
        } else if (isPrivate) {
            allowRename = false;
            allowAddRemove = false;
            allowLeave = false;
        } else { // GROUP
            allowRename = isAdmin;
            allowAddRemove = isAdmin;
            allowLeave = true;
        }

        allowManageMembers = allowAddRemove;

        if (cardRoomHeader != null) {
            cardRoomHeader.setVisibility((isPrivate || isDepartment) ? View.GONE : View.VISIBLE);
        }
        if (tvRoomNameView != null && !isPrivate && !isDepartment) {
            tvRoomNameView.setText(resolveDisplayName());
        }
        btnRename.setVisibility(allowRename ? View.VISIBLE : View.GONE);
        btnAddMember.setVisibility(allowAddRemove ? View.VISIBLE : View.GONE);
        btnLeave.setVisibility(allowLeave ? View.VISIBLE : View.GONE);

        List<RoomMember> current = viewModel.getMembers().getValue();
        if (current != null) {
            adapter.submitList(current, role != null ? role : "MEMBER", allowManageMembers);
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

    private void confirmRemove(RoomMember member) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.chat_remove_member_title))
                .setMessage(getString(R.string.chat_remove_member_message, member.getDisplayName()))
                .setPositiveButton(getString(R.string.chat_remove_member_action), (dialog, which) -> viewModel.removeMember(roomId, member.getUserId()))
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showRenameDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_rename_room, null, false);
        TextInputEditText etName = dialogView.findViewById(R.id.etNewName);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.chat_rename_group_title))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.chat_save_action), (dialog, which) -> {
                    String newName = etName.getText() != null ? etName.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(newName)) {
                        Toast.makeText(this, getString(R.string.chat_group_name_empty), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    viewModel.renameRoom(roomId, newName);
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showAddMembersDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_members, null, false);
        EditText etKeyword = dialogView.findViewById(R.id.etKeyword);
        ProgressBar progressContacts = dialogView.findViewById(R.id.progressContacts);
        RecyclerView rvContacts = dialogView.findViewById(R.id.rvContacts);
        TextView tvEmptyContacts = dialogView.findViewById(R.id.tvEmptyContacts);
        Button btnAddDialog = dialogView.findViewById(R.id.btnAdd);
        Button btnCancelDialog = dialogView.findViewById(R.id.btnCancel);
        TextView tvSelected = dialogView.findViewById(R.id.tvSelected);

        SelectContactAdapter contactAdapter = new SelectContactAdapter();
        rvContacts.setLayoutManager(new LinearLayoutManager(this));
        rvContacts.setAdapter(contactAdapter);

        contactAdapter.setOnSelectionChangeListener(count -> {
            tvSelected.setText(getString(R.string.chat_selected_count_label, count));
            btnAddDialog.setEnabled(count > 0);
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.chat_add_members_title)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        Handler handler = new Handler(Looper.getMainLooper());
        final Runnable[] searchTask = new Runnable[1];

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                if (searchTask[0] != null) handler.removeCallbacks(searchTask[0]);
                searchTask[0] = () -> performSearch(etKeyword.getText().toString().trim(), progressContacts);
                handler.postDelayed(searchTask[0], 300);
            }
        };
        etKeyword.addTextChangedListener(watcher);

        androidx.lifecycle.Observer<List<Contact>> contactsObserver = contacts -> {
            progressContacts.setVisibility(View.GONE);
            contactAdapter.submitList(contacts);
            boolean empty = contacts == null || contacts.isEmpty();
            tvEmptyContacts.setVisibility(empty ? View.VISIBLE : View.GONE);
            rvContacts.setVisibility(empty ? View.GONE : View.VISIBLE);
        };
        viewModel.getContacts().observe(this, contactsObserver);

        btnAddDialog.setOnClickListener(v -> {
            List<Long> selectedIds = contactAdapter.getSelectedIds();
            if (selectedIds.isEmpty()) {
                Toast.makeText(this, getString(R.string.chat_pick_at_least_one), Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.addMembers(roomId, selectedIds);
            dialog.dismiss();
        });

        btnCancelDialog.setOnClickListener(v -> dialog.dismiss());

        dialog.setOnDismissListener(d -> {
            etKeyword.removeTextChangedListener(watcher);
            viewModel.getContacts().removeObserver(contactsObserver);
        });

        dialog.setOnShowListener(d -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.94f), ViewGroup.LayoutParams.WRAP_CONTENT);
                window.setWindowAnimations(R.style.ChatDialogAnimation);
            }
        });

        dialog.show();
        performSearch("", progressContacts);
    }

    private void performSearch(String keyword, ProgressBar progressContacts) {
        if (progressContacts != null) progressContacts.setVisibility(View.VISIBLE);
        viewModel.searchContacts(keyword, roomId);
    }

    private void confirmLeaveRoom() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.chat_leave_group_title))
                .setMessage(getString(R.string.chat_leave_group_message))
                .setPositiveButton(getString(R.string.chat_leave_group_action), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        leavePending = true;
                        viewModel.removeMember(roomId, myUserId);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void setupActions() {
        btnRename.setOnClickListener(v -> {
            if (isPrivate || isDepartment) return;
            showRenameDialog();
        });
        btnAddMember.setOnClickListener(v -> {
            // PRIVATE không có thêm thành viên; DEPARTMENT được phép theo role (đã kiểm soát bằng updateRoleUi)
            if (isPrivate) return;
            showAddMembersDialog();
        });
        btnLeave.setOnClickListener(v -> {
            if (isDepartment || isPrivate) return;
            confirmLeaveRoom();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        setupActions();
    }
}
