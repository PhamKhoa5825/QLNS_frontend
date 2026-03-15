package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.model.ChatModels;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.VH> {

    private static final int VIEW_TYPE_MINE  = 1;
    private static final int VIEW_TYPE_OTHER = 2;

    private List<ChatModels.MessageResponse> list;
    private final Long currentUserId;

    public MessageAdapter(List<ChatModels.MessageResponse> list, Long currentUserId) {
        this.list          = list;
        this.currentUserId = currentUserId;
    }

    public void updateData(List<ChatModels.MessageResponse> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    public void appendMessage(ChatModels.MessageResponse msg) {
        list.add(msg);
        notifyItemInserted(list.size() - 1);
    }

    public void appendMessages(List<ChatModels.MessageResponse> msgs) {
        int start = list.size();
        list.addAll(msgs);
        notifyItemRangeInserted(start, msgs.size());
    }

    @Override
    public int getItemViewType(int position) {
        ChatModels.MessageResponse msg = list.get(position);
        return (msg.senderId != null && msg.senderId.equals(currentUserId))
                ? VIEW_TYPE_MINE : VIEW_TYPE_OTHER;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == VIEW_TYPE_MINE
                ? R.layout.item_message_mine
                : R.layout.item_message_other;
        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ChatModels.MessageResponse msg = list.get(position);
        h.tvMessage.setText(msg.message);
        if (h.tvSender != null) {
            h.tvSender.setText(msg.senderName != null ? msg.senderName : "");
        }
        if (h.tvTime != null) {
            h.tvTime.setText(msg.createdAt != null ? formatTime(msg.createdAt) : "");
        }
    }

    private String formatTime(String iso) {
        try { return iso.substring(11, 16); } catch (Exception e) { return ""; }
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvMessage, tvSender, tvTime;
        VH(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tvMessage);
            tvSender  = v.findViewById(R.id.tvSenderName);
            tvTime    = v.findViewById(R.id.tvMessageTime);
        }
    }
}