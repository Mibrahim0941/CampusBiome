package com.example.campusbiome;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class FacultyOfficeHourAdapter extends RecyclerView.Adapter<FacultyOfficeHourAdapter.ViewHolder> {

    private List<String> days;
    private java.util.Map<String, String> officeHoursMap;
    private OnOfficeHourActionListener actionListener;

    public interface OnOfficeHourActionListener {
        void onEdit(String day, String time);
        void onDelete(String day);
    }

    public FacultyOfficeHourAdapter(List<String> days, java.util.Map<String, String> officeHoursMap, OnOfficeHourActionListener actionListener) {
        this.days = days;
        this.officeHoursMap = officeHoursMap;
        this.actionListener = actionListener;
    }

    public void updateData(List<String> newDays, java.util.Map<String, String> newOfficeHoursMap) {
        this.days = newDays;
        this.officeHoursMap = newOfficeHoursMap;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_faculty_office_hour, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (days == null || days.isEmpty()) return;
        String currentDay = days.get(position);
        String currentTime = officeHoursMap.get(currentDay);

        holder.tvDay.setText(currentDay);
        holder.tvTime.setText("• " + (currentTime != null ? currentTime : "Not set"));
        
        holder.btnEdit.setOnClickListener(v -> actionListener.onEdit(currentDay, currentTime));
        holder.btnDelete.setOnClickListener(v -> actionListener.onDelete(currentDay));
    }

    @Override
    public int getItemCount() {
        return days == null ? 0 : days.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay, tvTime;
        MaterialButton btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tvDay);
            tvTime = itemView.findViewById(R.id.tvTime);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
