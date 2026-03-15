package com.example.myapplication.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.model.AttendanceModels;

import java.util.List;

public class AttendanceHistoryAdapter extends RecyclerView.Adapter<AttendanceHistoryAdapter.VH> {

    private List<AttendanceModels.AttendanceResponse> list;

    public AttendanceHistoryAdapter(List<AttendanceModels.AttendanceResponse> list) {
        this.list = list;
    }

    public void updateData(List<AttendanceModels.AttendanceResponse> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timekeeping_history, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AttendanceModels.AttendanceResponse att = list.get(position);

        h.tvDate.setText(att.date != null ? att.date : "--");
        h.tvTimeIn.setText(att.checkIn != null ? att.checkIn.substring(0, 5) : "--:--");
        h.tvTimeOut.setText(att.checkOut != null ? att.checkOut.substring(0, 5) : "--:--");

        switch (att.status != null ? att.status : "") {
            case "ON_TIME":
                h.tvStatus.setText("Đúng giờ");
                h.tvStatus.setBackgroundColor(Color.parseColor("#10B981"));
                break;
            case "LATE":
                h.tvStatus.setText("Đi muộn " + att.lateMinutes + " phút");
                h.tvStatus.setBackgroundColor(Color.parseColor("#EF4444"));
                break;
            case "ABSENT":
                h.tvStatus.setText("Vắng");
                h.tvStatus.setBackgroundColor(Color.parseColor("#6B7280"));
                break;
            default:
                h.tvStatus.setText(att.status);
                break;
        }
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDate, tvTimeIn, tvTimeOut, tvStatus;
        VH(View v) {
            super(v);
            tvDate    = v.findViewById(R.id.tvHistoryDate);
            tvTimeIn  = v.findViewById(R.id.tvTimeIn);
            tvTimeOut = v.findViewById(R.id.tvTimeOut);
            tvStatus  = v.findViewById(R.id.tvHistoryStatus);
        }
    }
}