package com.example.campusbiome;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;

public class TimetableDayAdapter extends RecyclerView.Adapter<TimetableDayAdapter.ViewHolder> {
    private String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
    private int selectedPosition = 0;
    private OnDaySelectedListener listener;

    public interface OnDaySelectedListener {
        void onDaySelected(int position, String dayShortName);
    }

    public TimetableDayAdapter(OnDaySelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timetable_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvDayName.setText(days[position]);
        if (selectedPosition == position) {
            holder.cardDayPill.setCardBackgroundColor(android.graphics.Color.parseColor("#E6F2ED"));
            holder.tvDayName.setTextColor(android.graphics.Color.parseColor("#006B5E"));
        } else {
            holder.cardDayPill.setCardBackgroundColor(android.graphics.Color.TRANSPARENT);
            holder.tvDayName.setTextColor(android.graphics.Color.parseColor("#8F9B99"));
        }

        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);
            String shortName = days[selectedPosition].substring(0, 3);
            if (listener != null) listener.onDaySelected(selectedPosition, shortName);
        });
    }

    @Override
    public int getItemCount() {
        return days.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardDayPill;
        TextView tvDayName;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardDayPill = itemView.findViewById(R.id.cardDayPill);
            tvDayName = itemView.findViewById(R.id.tvDayName);
        }
    }
}
