package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.ContactAdapter;
import com.example.myapplication.model.Contact;
import com.example.myapplication.repository.ChatRepository;
import com.example.myapplication.viewmodel.ContactViewModel;

import java.util.List;

public class ContactsActivity extends AppCompatActivity {

    private ContactViewModel viewModel;
    private ContactAdapter adapter;

    private SearchView searchView;
    private RecyclerView rvContacts;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;
    private String pendingContactName;
    private Long pendingContactId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        initViews();
        setupRecyclerView();
        setupViewModel();
        observeData();
        setupSearch();

        // Gọi API ngay khi vào màn hình để load toàn bộ danh bạ
        viewModel.searchContacts("");
    }

    private void initViews() {
        searchView = findViewById(R.id.searchView);
        rvContacts = findViewById(R.id.rvContacts);

        int searchTextId = androidx.appcompat.R.id.search_src_text;
        EditText searchEditText = searchView.findViewById(searchTextId);
        if (searchEditText != null) {
            searchEditText.setTextColor(getResources().getColor(R.color.text_primary));
            searchEditText.setHintTextColor(getResources().getColor(R.color.text_secondary));
            searchEditText.setPadding(searchEditText.getPaddingLeft() + 6, searchEditText.getPaddingTop(), searchEditText.getPaddingRight(), searchEditText.getPaddingBottom());
            searchEditText.setTextSize(14f);
        }
    }

    private void setupRecyclerView() {
        rvContacts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ContactAdapter(contact -> {
            pendingContactName = contact.getFullName();
            pendingContactId = contact.getUserId();
            viewModel.createPrivateRoom(contact.getUserId());
        });
        rvContacts.setAdapter(adapter);
    }

    private void setupViewModel() {
        ChatRepository repository = new ChatRepository(this);
        ContactViewModel.Factory factory = new ContactViewModel.Factory(repository);
        viewModel = new ViewModelProvider(this, factory).get(ContactViewModel.class);
    }

    private void observeData() {
        viewModel.getContacts().observe(this, this::renderContacts);
        viewModel.getCreatedRoom().observe(this, room -> {
            if (room == null) {
                Toast.makeText(this, getString(R.string.chat_create_room_failed), Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, MessageActivity.class);
            String partnerName = room.getOtherParticipantName() != null && !room.getOtherParticipantName().isEmpty()
                    ? room.getOtherParticipantName()
                    : pendingContactName;
            intent.putExtra("ROOM_ID", room.getId());
            intent.putExtra("ROOM_NAME", partnerName);
            intent.putExtra("ROOM_TYPE", room.getType());
            intent.putExtra("roomId", room.getId());
            intent.putExtra("roomName", partnerName);
            intent.putExtra("roomType", room.getType());
            intent.putExtra("otherParticipantName", partnerName);
            startActivity(intent);
        });
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                triggerSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                triggerSearch(newText);
                return true;
            }
        });
    }

    private void triggerSearch(String keyword) {
        if (pendingSearch != null) {
            searchHandler.removeCallbacks(pendingSearch);
        }
        pendingSearch = () -> viewModel.searchContacts(keyword != null ? keyword : "");
        searchHandler.postDelayed(pendingSearch, 300);
    }

    private void renderContacts(List<Contact> contacts) {
        if (contacts == null) {
            adapter.submitList(null);
            Toast.makeText(this, getString(R.string.chat_search_no_result), Toast.LENGTH_SHORT).show();
            return;
        }
        adapter.submitList(contacts);
    }
}
