package com.example.myapplication.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.SearchResultAdapter;
import com.example.myapplication.repository.ChatRepository;
import com.example.myapplication.viewmodel.SearchViewModel;

public class SearchMessageActivity extends AppCompatActivity {

    private Long roomId;
    private SearchViewModel viewModel;
    private SearchResultAdapter adapter;
    private SearchView searchView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private RecyclerView rvResults;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;
    private String latestKeyword = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_message);

        roomId = getIntent().getLongExtra("roomId", -1L);
        initViews();
        setupViewModel();
        setupList();
        setupSearch();
        observeData();
    }

    private void initViews() {
        searchView = findViewById(R.id.searchView);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        rvResults = findViewById(R.id.rvResults);
    }

    private void setupViewModel() {
        ChatRepository repository = new ChatRepository(this);
        SearchViewModel.Factory factory = new SearchViewModel.Factory(repository);
        viewModel = new androidx.lifecycle.ViewModelProvider(this, factory).get(SearchViewModel.class);
    }

    private void setupList() {
        adapter = new SearchResultAdapter(this);
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        rvResults.setAdapter(adapter);
    }

    private void setupSearch() {
        searchView.setIconifiedByDefault(false);
        searchView.setQueryHint(getString(R.string.chat_search_in_conversation));
        searchView.requestFocus();

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
        latestKeyword = keyword != null ? keyword.trim() : "";
        if (pendingSearch != null) {
            handler.removeCallbacks(pendingSearch);
        }

        if (android.text.TextUtils.isEmpty(latestKeyword)) {
            pendingSearch = null;
            adapter.submit(null, "");
            tvEmpty.setVisibility(android.view.View.GONE);
            progressBar.setVisibility(android.view.View.GONE);
            return;
        }

        pendingSearch = () -> {
            progressBar.setVisibility(android.view.View.VISIBLE);
            viewModel.searchMessages(roomId, latestKeyword);
        };
        handler.postDelayed(pendingSearch, 300);
    }

    private void observeData() {
        viewModel.getResults().observe(this, list -> {
            progressBar.setVisibility(android.view.View.GONE);
            adapter.submit(list, latestKeyword);
        });

        viewModel.getIsEmpty().observe(this, empty -> {
            boolean show = Boolean.TRUE.equals(empty);
            tvEmpty.setVisibility(show ? android.view.View.VISIBLE : android.view.View.GONE);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pendingSearch != null) {
            handler.removeCallbacks(pendingSearch);
        }
    }
}

