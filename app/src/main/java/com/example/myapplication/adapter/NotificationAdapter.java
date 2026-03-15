package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.model.NotificationModels;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

    public interface OnReadListener {
        void onMarkRead(NotificationModels.NotificationResponse noti);
    }

    private List<NotificationModels.NotificationResponse> list;
    private final OnReadListener listener;

    public NotificationAdapter(List<NotificationModels.NotificationResponse> list,
                               OnReadListener listener) {
        this.list     = list;
        this.listener = listener;
    }

    public void updateData(List<NotificationModels.NotificationResponse> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        NotificationModels.NotificationResponse noti = list.get(position);

        h.tvTitle.setText(noti.title);
        h.tvContent.setText(noti.content);
        h.tvTime.setText(formatTime(noti.createdAt));

        // Unread indicator - khớp với item_notification.xml
        h.viewUnreadIndicator.setVisibility(noti.isRead ? View.GONE : View.VISIBLE);

        h.itemView.setOnClickListener(v -> listener.onMarkRead(noti));
    }

    private String formatTime(String iso) {
        if (iso == null) return "";
        try { return iso.substring(11, 16); } catch (Exception e) { return iso; }
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent, tvTime;
        View viewUnreadIndicator;
        VH(View v) {
            super(v);
            // ĐÃ SỬA: ID khớp chính xác với item_notification.xml
            tvTitle            = v.findViewById(R.id.tvTitle);
            tvContent          = v.findViewById(R.id.tvMessage);
            tvTime             = v.findViewById(R.id.tvTime);
            viewUnreadIndicator = v.findViewById(R.id.unreadIndicator);
        }
    }
}