package com.example.myapplication.adapter;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Message;

import java.util.ArrayList;
import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.SearchViewHolder> {

    private final List<Message> data = new ArrayList<>();
    private final Context context;
    private String keyword = "";

    public SearchResultAdapter(Context context) {
        this.context = context;
    }

    public void submit(List<Message> list, String keyword) {
        data.clear();
        if (list != null) {
            data.addAll(list);
        }
        this.keyword = keyword != null ? keyword : "";
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result, parent, false);
        return new SearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
        holder.bind(data.get(position), keyword);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    class SearchViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSenderName;
        private final TextView tvMessage;
        private final TextView tvTime;

        SearchViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        void bind(Message msg, String keyword) {
            tvSenderName.setText(msg.getSenderName() != null ? msg.getSenderName() : context.getString(R.string.chat_sender_anonymous));
            tvTime.setText(msg.getCreatedAt() != null ? msg.getCreatedAt() : "");
            tvMessage.setText(applyHighlight(msg.getMessage(), keyword));
        }

        private CharSequence applyHighlight(String text, String keyword) {
            if (TextUtils.isEmpty(text) || TextUtils.isEmpty(keyword)) {
                return text != null ? text : "";
            }
            String lower = text.toLowerCase();
            String keyLower = keyword.toLowerCase();
            SpannableString span = new SpannableString(text);
            int start = lower.indexOf(keyLower);
            int color = ContextCompat.getColor(context, R.color.colorPrimary);
            while (start >= 0) {
                int end = start + keyword.length();
                span.setSpan(new ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                start = lower.indexOf(keyLower, end);
            }
            return span;
        }
    }
}
