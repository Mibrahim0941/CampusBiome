package com.example.campusbiome;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SocietyAnnouncementAdapter extends RecyclerView.Adapter<SocietyAnnouncementAdapter.ViewHolder> {

    private List<SocietyAnnouncements> list;

    public SocietyAnnouncementAdapter(List<SocietyAnnouncements> list) {
        this.list = list;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay, tvMonth, tvTitle, tvDescription;

        public ViewHolder(View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tvEventDay);
            tvMonth = itemView.findViewById(R.id.tvEventMonth);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_society_announcement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        SocietyAnnouncements a = list.get(position);

        holder.tvTitle.setText(a.title);
        holder.tvDescription.setText(a.message);

        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Date date = input.parse(a.createdAt);

            SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());
            SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());

            holder.tvDay.setText(dayFormat.format(date));
            holder.tvMonth.setText(monthFormat.format(date).toUpperCase());

        } catch (Exception e) {
            holder.tvDay.setText("--");
            holder.tvMonth.setText("--");
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
