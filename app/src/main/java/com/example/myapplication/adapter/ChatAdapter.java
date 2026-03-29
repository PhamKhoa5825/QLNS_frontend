package com.example.myapplication.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.ChatRoom;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private Context context;
    private List<ChatRoom> roomList;
    private OnRoomClickListener listener;
    private Long currentUserId;

    public interface OnRoomClickListener {
        void onRoomSelected(ChatRoom room);
    }

    public ChatAdapter(Context context, List<ChatRoom> roomList, Long currentUserId, OnRoomClickListener listener) {
        this.context = context;
        this.roomList = roomList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void setRoomList(List<ChatRoom> roomList) {
        this.roomList = roomList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatRoom room = roomList.get(position);
        
        String name = room.getName();
        if ("PRIVATE".equals(room.getType()) && room.getOtherParticipantName() != null) {
            name = room.getOtherParticipantName();
        } else if (name == null) {
            name = "Phòng chat " + room.getId();
        }
        
        holder.tvChatName.setText(name);
        
        // Last message & Time
        if (room.getLastMessage() != null) {
            holder.tvChatMessage.setText(room.getLastMessage());
        } else {
            holder.tvChatMessage.setText("Chưa có tin nhắn");
        }

        if (room.getLastMessageTime() != null) {
            String timeStr = room.getLastMessageTime();
            if (timeStr.contains("T")) {
                int tIndex = timeStr.indexOf("T");
                int colonIndex = timeStr.lastIndexOf(":");
                if (colonIndex > tIndex) {
                    timeStr = timeStr.substring(tIndex + 1, colonIndex);
                } else {
                    timeStr = timeStr.substring(tIndex + 1);
                }
            }
            holder.tvChatTime.setText(timeStr);
        } else {
            holder.tvChatTime.setText("");
        }
        
        // Unread Count
        if (room.getUnreadCount() > 0) {
            holder.tvUnreadCount.setVisibility(View.VISIBLE);
            holder.tvUnreadCount.setText(String.valueOf(room.getUnreadCount()));
            holder.ivReadReceipt.setVisibility(View.GONE);
        } else {
            holder.tvUnreadCount.setVisibility(View.GONE);
            // Show read receipt if no unread messages (mock logic)
            holder.ivReadReceipt.setVisibility(View.VISIBLE);
        }

        // Online Dot Logic (Mocked for UI)
        holder.viewOnlineDot.setVisibility(position == 0 ? View.VISIBLE : View.GONE);

        // Avatar logic
        String firstChar = name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase();
        holder.tvAvatarPlaceholder.setText(firstChar);
        holder.tvAvatarPlaceholder.setVisibility(View.VISIBLE); // Always show placeholder for now
        holder.ivAvatar.setVisibility(View.GONE); // Image loading not implemented

        holder.itemView.setOnClickListener(v -> listener.onRoomSelected(room));
    }

    @Override
    public int getItemCount() {
        return roomList == null ? 0 : roomList.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        android.widget.ImageView ivAvatar, ivReadReceipt;
        TextView tvChatName, tvChatMessage, tvChatTime, tvUnreadCount, tvAvatarPlaceholder;
        View viewOnlineDot;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivChatAvatar);
            ivReadReceipt = itemView.findViewById(R.id.ivReadReceipt);
            tvChatName = itemView.findViewById(R.id.tvChatName);
            tvChatMessage = itemView.findViewById(R.id.tvChatMessage);
            tvChatTime = itemView.findViewById(R.id.tvChatTime);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
            tvAvatarPlaceholder = itemView.findViewById(R.id.tvChatAvatarPlaceholder);
            viewOnlineDot = itemView.findViewById(R.id.viewOnlineDot);
        }
    }
}
