package com.example.myapplication.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.AttendanceSummary;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class AttendanceSummaryAdapter extends RecyclerView.Adapter<AttendanceSummaryAdapter.ViewHolder> {

    private Context context;
    private List<AttendanceSummary> summaryList;

    public AttendanceSummaryAdapter(Context context, List<AttendanceSummary> summaryList) {
        this.context = context;
        this.summaryList = summaryList;
    }

    public void setSummaryList(List<AttendanceSummary> summaryList) {
        this.summaryList = summaryList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_attendance_summary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendanceSummary summary = summaryList.get(position);
        
        // Reset cell state
        holder.viewTop.setVisibility(View.GONE);
        holder.viewMiddle.setVisibility(View.GONE);
        holder.viewBottom.setVisibility(View.GONE);
        holder.cardDay.setCardBackgroundColor(Color.WHITE);
        holder.tvDayNumber.setTextColor(Color.BLACK);
        holder.cardDay.setStrokeWidth(0); 

        if (summary == null) {
            holder.tvDayNumber.setText("");
            holder.cardDay.setVisibility(View.INVISIBLE);
            return;
        }

        holder.cardDay.setVisibility(View.VISIBLE);
        String[] parts = summary.getDate().split("-");
        holder.tvDayNumber.setText(parts[parts.length - 1]);

        try {
            String top = summary.getTopColor();
            String mid = summary.getMiddleColor();
            String bot = summary.getBottomColor();
            String base = summary.getColorCode();

            if (top != null || mid != null || bot != null) {
                // Split Mode
                holder.cardDay.setCardBackgroundColor(Color.TRANSPARENT);
                holder.tvDayNumber.setTextColor(Color.WHITE);
                
                if (top != null && mid != null && bot != null) {
                    holder.viewTop.setVisibility(View.VISIBLE);
                    holder.viewMiddle.setVisibility(View.VISIBLE);
                    holder.viewBottom.setVisibility(View.VISIBLE);
                    holder.viewTop.setBackgroundColor(Color.parseColor(top));
                    holder.viewMiddle.setBackgroundColor(Color.parseColor(mid));
                    holder.viewBottom.setBackgroundColor(Color.parseColor(bot));
                } else if (top != null && bot != null) {
                    holder.viewTop.setVisibility(View.VISIBLE);
                    holder.viewBottom.setVisibility(View.VISIBLE);
                    holder.viewTop.setBackgroundColor(Color.parseColor(top));
                    holder.viewBottom.setBackgroundColor(Color.parseColor(bot));
                }
            } else if (base != null) {
                // Solid Mode
                int color = Color.parseColor(base);
                holder.cardDay.setCardBackgroundColor(color);
                // If colored solid background, use white text (except for very light colors like white/weekend grey)
                if (!base.equalsIgnoreCase("#FFFFFF") && !base.equalsIgnoreCase("#BDBDBD")) {
                    holder.tvDayNumber.setTextColor(Color.WHITE);
                }
            }

            // --- Update: Border Logic from Backend (Handles Today and OT priority) ---
            String border = summary.getBorderColor();
            boolean isBold = summary.getIsBold();
            
            if (border != null) {
                int borderColor = Color.parseColor(border);
                holder.cardDay.setStrokeColor(android.content.res.ColorStateList.valueOf(borderColor));
                holder.cardDay.setStrokeWidth(dpToPx(context, 3));
            } else {
                holder.cardDay.setStrokeWidth(0);
            }

            if (isBold) {
                holder.tvDayNumber.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                holder.tvDayNumber.setTypeface(null, android.graphics.Typeface.NORMAL);
            }

        } catch (Exception e) {
            holder.cardDay.setCardBackgroundColor(Color.WHITE);
            holder.tvDayNumber.setTextColor(Color.BLACK);
            holder.cardDay.setStrokeWidth(0);
            holder.tvDayNumber.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        holder.itemView.setOnClickListener(v -> {
            if (!summary.getDescription().isEmpty()) {
                Toast.makeText(context, summary.getDate() + ": " + summary.getDescription(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    @Override
    public int getItemCount() {
        return summaryList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayNumber;
        MaterialCardView cardDay;
        View viewTop, viewMiddle, viewBottom;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayNumber = itemView.findViewById(R.id.tvDayNumber);
            cardDay = itemView.findViewById(R.id.cardDay);
            viewTop = itemView.findViewById(R.id.viewTop);
            viewMiddle = itemView.findViewById(R.id.viewMiddle);
            viewBottom = itemView.findViewById(R.id.viewBottom);
        }
    }
}
