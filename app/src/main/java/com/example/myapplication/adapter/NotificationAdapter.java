package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.model.NotificationModels;
import com.google.android.material.chip.Chip;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

    public interface OnActionListener {
        void onMarkRead(NotificationModels.NotificationResponse noti);
        void onEdit(NotificationModels.NotificationResponse noti);
        void onDelete(NotificationModels.NotificationResponse noti);
    }

    private List<NotificationModels.NotificationResponse> list;
    private final OnActionListener listener;
    private final boolean isAdmin;

    public NotificationAdapter(List<NotificationModels.NotificationResponse> list,
                               OnActionListener listener, boolean isAdmin) {
        this.list = list; this.listener = listener; this.isAdmin = isAdmin;
    }

    public void updateData(List<NotificationModels.NotificationResponse> newList) {
        this.list = newList; notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        NotificationModels.NotificationResponse noti = list.get(position);
        h.tvTitle.setText(noti.title);
        h.tvContent.setText(noti.content);
        h.tvTime.setText(formatTime(noti.createdAt));
        h.viewUnreadIndicator.setVisibility(noti.isRead ? View.GONE : View.VISIBLE);

        if (h.chipCategory != null) {
            String type = noti.targetType != null ? noti.targetType : "";
            switch (type) {
                case "DEPARTMENT":
                    h.chipCategory.setText(noti.departmentName != null ? noti.departmentName : "Phòng ban");
                    break;
                case "EMPLOYEE": h.chipCategory.setText("Cá nhân"); break;
                default: h.chipCategory.setText("Toàn công ty");
            }
        }

        h.itemView.setOnClickListener(v -> listener.onMarkRead(noti));

        if (h.btnMore != null) {
            if (isAdmin) {
                h.btnMore.setVisibility(View.VISIBLE);
                h.btnMore.setOnClickListener(v -> {
                    PopupMenu popup = new PopupMenu(v.getContext(), v);
                    popup.getMenu().add(0, 1, 0, "Sửa");
                    popup.getMenu().add(0, 2, 1, "Xóa");
                    popup.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == 1) { listener.onEdit(noti); return true; }
                        if (item.getItemId() == 2) { listener.onDelete(noti); return true; }
                        return false;
                    });
                    popup.show();
                });
            } else {
                h.btnMore.setVisibility(View.GONE);
            }
        }
    }

    private String formatTime(String iso) {
        if (iso == null) return "";
        try { return iso.length() >= 16 ? iso.substring(11, 16) : iso; }
        catch (Exception e) { return iso; }
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent, tvTime;
        View viewUnreadIndicator;
        ImageButton btnMore;
        Chip chipCategory;
        VH(View v) {
            super(v);
            tvTitle             = v.findViewById(R.id.tvTitle);
            tvContent           = v.findViewById(R.id.tvMessage);
            tvTime              = v.findViewById(R.id.tvTime);
            viewUnreadIndicator = v.findViewById(R.id.unreadIndicator);
            btnMore             = v.findViewById(R.id.btnMore);
            chipCategory        = v.findViewById(R.id.chipCategory);
        }
    }
}