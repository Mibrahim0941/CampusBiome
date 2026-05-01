package com.example.campusbiome;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FacultyAnnouncementAdapter extends RecyclerView.Adapter<FacultyAnnouncementAdapter.ViewHolder> {

    private List<FacultyAnnouncement> announcementList;

    public FacultyAnnouncementAdapter(List<FacultyAnnouncement> announcementList) {
        this.announcementList = announcementList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_faculty_announcement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FacultyAnnouncement announcement = announcementList.get(position);

        holder.tvAnnTitle.setText(announcement.getTitle());
        holder.tvAnnDesc.setText(announcement.getDescription());
        
        String dateStr = announcement.getCreatedAt();
        if (dateStr != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                Date date = sdf.parse(dateStr);
                SimpleDateFormat outSdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                holder.tvDate.setText(outSdf.format(date));
            } catch (ParseException e) {
                holder.tvDate.setText(dateStr);
            }
        } else {
            holder.tvDate.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return announcementList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAnnTitle, tvDate, tvAnnDesc;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAnnTitle = itemView.findViewById(R.id.tvAnnTitle);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAnnDesc = itemView.findViewById(R.id.tvAnnDesc);
        }
    }
}
