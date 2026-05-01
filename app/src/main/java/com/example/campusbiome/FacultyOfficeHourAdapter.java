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
    private String officeHours;
    private OnOfficeHourActionListener actionListener;

    public interface OnOfficeHourActionListener {
        void onEdit(String day);
        void onDelete(String day);
        void onAdd(String day);
    }

    public FacultyOfficeHourAdapter(List<String> days, String officeHours, OnOfficeHourActionListener actionListener) {
        this.days = days;
        this.officeHours = officeHours;
        this.actionListener = actionListener;
    }

    public void updateData(List<String> newDays, String newOfficeHours) {
        this.days = newDays;
        this.officeHours = newOfficeHours;
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
        String currentDay = days.get(position).trim();

        holder.tvDay.setText(currentDay);
        holder.tvTime.setText("• " + officeHours);
        holder.btnEdit.setText("EDIT");
        holder.btnDelete.setVisibility(View.VISIBLE);
        
        holder.btnEdit.setOnClickListener(v -> actionListener.onEdit(currentDay));
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
