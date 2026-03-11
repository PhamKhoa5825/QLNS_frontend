package com.example.myapplication.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.model.ChatRoom;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private Context context;
    private List<ChatRoom> roomList;

    public ChatAdapter(Context context, List<ChatRoom> roomList) {
        this.context = context;
        this.roomList = roomList;
    }

    public void setRoomList(List<ChatRoom> roomList) {
        this.roomList = roomList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatRoom room = roomList.get(position);
        holder.tvTitle.setText(room.getName());
        
        holder.tvDetails.setText("Loại: " + room.getType());
        holder.tvDetails.setTextColor(Color.parseColor("#6B7280"));
    }

    @Override
    public int getItemCount() {
        return roomList == null ? 0 : roomList.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDetails;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(android.R.id.text1);
            tvDetails = itemView.findViewById(android.R.id.text2);
            tvTitle.setTextSize(16f);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            tvDetails.setTextSize(14f);
        }
    }
}
