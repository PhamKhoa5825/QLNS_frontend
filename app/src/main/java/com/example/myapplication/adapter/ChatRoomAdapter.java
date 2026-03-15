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

public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.VH> {

    public interface OnRoomClick { void onClick(ChatModels.ChatRoomResponse room); }

    private List<ChatModels.ChatRoomResponse> list;
    private final OnRoomClick listener;

    public ChatRoomAdapter(List<ChatModels.ChatRoomResponse> list, OnRoomClick listener) {
        this.list     = list;
        this.listener = listener;
    }

    public void updateData(List<ChatModels.ChatRoomResponse> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_internal_message, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ChatModels.ChatRoomResponse room = list.get(position);

        h.tvName.setText(room.name != null ? room.name : "Phòng chat");
        h.tvLastMessage.setText(room.lastMessage != null ? room.lastMessage : "");
        h.tvTime.setText(room.lastMessageTime != null ? formatTime(room.lastMessageTime) : "");

        if (room.name != null && !room.name.isEmpty()) {
            h.tvAvatar.setText(String.valueOf(room.name.charAt(0)).toUpperCase());
        }

        if (room.unreadCount > 0) {
            h.tvUnread.setVisibility(View.VISIBLE);
            h.tvUnread.setText(String.valueOf(room.unreadCount));
        } else {
            h.tvUnread.setVisibility(View.GONE);
        }

        h.viewOnlineStatus.setVisibility(View.VISIBLE);
        h.itemView.setOnClickListener(v -> listener.onClick(room));
    }

    private String formatTime(String isoTime) {
        try {
            if (isoTime.contains("T")) {
                return isoTime.substring(isoTime.indexOf("T") + 1, isoTime.indexOf("T") + 6);
            }
            return isoTime;
        } catch (Exception e) { return ""; }
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvLastMessage, tvTime, tvUnread;
        View viewOnlineStatus;
        VH(View v) {
            super(v);
            tvAvatar         = v.findViewById(R.id.tvAvatarText);
            tvName           = v.findViewById(R.id.tvSenderName);
            tvLastMessage    = v.findViewById(R.id.tvLastMessage);
            tvTime           = v.findViewById(R.id.tvTime);
            tvUnread         = v.findViewById(R.id.tvUnreadCount);
            viewOnlineStatus = v.findViewById(R.id.viewOnlineStatus);
        }
    }
}