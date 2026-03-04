package com.example.myapplication;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private List<Chat> list;

    public ChatAdapter(List<Chat> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Chat chat = list.get(position);

        holder.tvChatAvatar.setText(chat.getAvatarText());
        holder.tvChatName.setText(chat.getName());
        holder.tvChatMessage.setText(chat.getMessage());
        holder.tvChatTime.setText(chat.getTime());
        holder.tvChatDepartment.setText(chat.getDepartment());

        // Xử lý hiển thị chấm Online
        if (chat.isOnline()) {
            holder.viewOnlineDot.setVisibility(View.VISIBLE);
        } else {
            holder.viewOnlineDot.setVisibility(View.GONE);
        }

        // Xử lý hiển thị số tin nhắn chưa đọc
        if (chat.getUnreadCount() > 0) {
            holder.tvUnreadCount.setVisibility(View.VISIBLE);
            holder.tvUnreadCount.setText(String.valueOf(chat.getUnreadCount()));
            holder.tvChatMessage.setTextColor(Color.parseColor("#111827")); // Chữ đậm hơn nếu chưa đọc
            holder.tvChatMessage.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            holder.tvUnreadCount.setVisibility(View.GONE);
            holder.tvChatMessage.setTextColor(Color.parseColor("#6B7280")); // Chữ xám nếu đã đọc
            holder.tvChatMessage.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvChatAvatar, tvChatName, tvChatMessage, tvChatTime, tvChatDepartment, tvUnreadCount;
        View viewOnlineDot;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChatAvatar = itemView.findViewById(R.id.tvChatAvatar);
            tvChatName = itemView.findViewById(R.id.tvChatName);
            tvChatMessage = itemView.findViewById(R.id.tvChatMessage);
            tvChatTime = itemView.findViewById(R.id.tvChatTime);
            tvChatDepartment = itemView.findViewById(R.id.tvChatDepartment);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
            viewOnlineDot = itemView.findViewById(R.id.viewOnlineDot);
        }
    }
}