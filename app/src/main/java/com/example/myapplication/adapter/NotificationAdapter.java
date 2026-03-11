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
import com.example.myapplication.model.Notification;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotiViewHolder> {

    private Context context;
    private List<Notification> notiList;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification noti);
    }

    public NotificationAdapter(Context context, List<Notification> notiList, OnNotificationClickListener listener) {
        this.context = context;
        this.notiList = notiList;
        this.listener = listener;
    }

    public void setNotiList(List<Notification> notiList) {
        this.notiList = notiList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotiViewHolder holder, int position) {
        Notification noti = notiList.get(position);
        holder.tvTitle.setText(noti.getTitle());
        holder.tvContent.setText(noti.getContent());
        
        // Target Type Tag
        if ("COMPANY".equals(noti.getTargetType())) {
            holder.tvTag.setText("Công ty");
            holder.tvTag.setBackgroundResource(R.drawable.bg_tag_rose); // Actually use a blue one if we had it, but keeping rose for consistent theme
            holder.tvTag.setTextColor(Color.parseColor("#E11D48"));
        } else {
            holder.tvTag.setText("Phòng ban");
            holder.tvTag.setBackgroundResource(R.drawable.bg_tag_rose);
            holder.tvTag.setTextColor(Color.parseColor("#E11D48"));
        }

        // Read Status
        if (!noti.isRead()) {
            holder.viewReadStatus.setVisibility(View.VISIBLE);
            holder.tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            holder.viewReadStatus.setVisibility(View.GONE);
            holder.tvTitle.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        
        // Date (simple display for now)
        if (noti.getCreatedAt() != null) {
            String time = noti.getCreatedAt();
            if (time.contains("T")) time = time.split("T")[0];
            holder.tvTime.setText(time);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNotificationClick(noti);
        });
    }

    @Override
    public int getItemCount() {
        return notiList == null ? 0 : notiList.size();
    }

    public static class NotiViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent, tvTag, tvTime;
        View viewReadStatus;

        public NotiViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotiTitle);
            tvContent = itemView.findViewById(R.id.tvNotiContent);
            tvTag = itemView.findViewById(R.id.tvNotiTag);
            tvTime = itemView.findViewById(R.id.tvNotiTime);
            viewReadStatus = itemView.findViewById(R.id.viewReadStatus);
        }
    }
}
