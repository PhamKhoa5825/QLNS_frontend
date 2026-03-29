package com.example.myapplication.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.ContactCheckAdapter;
import com.example.myapplication.model.ChatRoom;
import com.example.myapplication.repository.ChatRepository;
import com.example.myapplication.viewmodel.ChatViewModel;
import com.example.myapplication.viewmodel.ContactViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CreateGroupDialog extends DialogFragment {

    public interface OnGroupCreatedListener {
        void onGroupCreated(ChatRoom room);
    }

    private OnGroupCreatedListener listener;

    private ContactViewModel contactViewModel;
    private ChatViewModel chatViewModel;

    private ContactCheckAdapter adapter;
    private TextInputEditText etGroupName;
    private RecyclerView rvContacts;
    private MaterialButton btnCreate;
    private TextView tvSelectedCount;

    public static CreateGroupDialog newInstance() {
        return new CreateGroupDialog();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnGroupCreatedListener) {
            listener = (OnGroupCreatedListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_create_group, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            Window window = getDialog().getWindow();
            window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setWindowAnimations(R.style.ChatDialogAnimation);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etGroupName = view.findViewById(R.id.etGroupName);
        rvContacts = view.findViewById(R.id.rvSelectContacts);
        btnCreate = view.findViewById(R.id.btnCreate);
        tvSelectedCount = view.findViewById(R.id.tvSelectedCount);

        setupViewModels();
        setupRecyclerView();
        observeContacts();
        observeCreateGroup();

        if (tvSelectedCount != null) {
            tvSelectedCount.setText(getString(R.string.chat_selected_count_zero));
        }
        btnCreate.setEnabled(false);

        contactViewModel.searchContacts("");

        btnCreate.setOnClickListener(v -> {
            String name = etGroupName.getText() != null ? etGroupName.getText().toString().trim() : "";
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(getContext(), getString(R.string.chat_group_name_required), Toast.LENGTH_SHORT).show();
                return;
            }
            Set<Long> selected = adapter.getSelectedIds();
            if (selected == null || selected.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.chat_select_member_required), Toast.LENGTH_SHORT).show();
                return;
            }

            btnCreate.setEnabled(false);
            chatViewModel.createGroupRoom(name, new ArrayList<>(selected));
        });
    }

    private void setupViewModels() {
        FragmentActivity activity = requireActivity();
        ChatRepository repository = new ChatRepository(activity);
        ContactViewModel.Factory contactFactory = new ContactViewModel.Factory(repository);
        contactViewModel = new ViewModelProvider(activity, contactFactory).get(ContactViewModel.class);

        ChatViewModel.Factory chatFactory = new ChatViewModel.Factory(repository);
        chatViewModel = new ViewModelProvider(activity, chatFactory).get(ChatViewModel.class);
    }

    private void setupRecyclerView() {
        adapter = new ContactCheckAdapter(selectedIds -> {
            int count = selectedIds != null ? selectedIds.size() : 0;
            if (tvSelectedCount != null) {
                tvSelectedCount.setText(getString(R.string.chat_selected_count_label, count));
            }
            btnCreate.setEnabled(count > 0);
        });
        rvContacts.setLayoutManager(new LinearLayoutManager(getContext()));
        rvContacts.setAdapter(adapter);
    }

    private void observeContacts() {
        contactViewModel.getContacts().observe(getViewLifecycleOwner(), contacts -> {
            if (contacts != null) {
                adapter.submitList(contacts);
            }
        });
    }

    private void observeCreateGroup() {
        chatViewModel.getCreatedGroup().observe(getViewLifecycleOwner(), room -> {
            if (room != null) {
                // Tạo thành công: Activity sẽ lo việc chuyển màn hình và reset
                chatViewModel.resetCreatedGroup();
                dismiss();
            } else {
                btnCreate.setEnabled(true);
            }
        });
    }
}
