package com.example.campusbiome;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProfessorAdapter extends RecyclerView.Adapter<ProfessorAdapter.ProfessorViewHolder> {

    private List<DataSnapshot> facultyList;
    private OnProfessorClickListener listener;

    public interface OnProfessorClickListener {
        void onViewDetails(DataSnapshot faculty);
        void onBookSlot(DataSnapshot faculty);
    }

    public ProfessorAdapter(List<DataSnapshot> facultyList, OnProfessorClickListener listener) {
        this.facultyList = facultyList;
        this.listener = listener;
    }

    public void updateData(List<DataSnapshot> newList) {
        this.facultyList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProfessorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_professor, parent, false);
        return new ProfessorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfessorViewHolder holder, int position) {
        DataSnapshot faculty = facultyList.get(position);

        String name = getString(faculty, "name");
        String post = getString(faculty, "position", "post");
        Boolean isAvailable = faculty.child("Available").getValue(Boolean.class);

        holder.tvName.setText(name);
        holder.tvPost.setText(post);

        if (isAvailable != null && isAvailable) {
            holder.tvAvailability.setText("Available");
            holder.tvAvailability.setTextColor(Color.parseColor("#006B5E"));
            // Set a soft green background if possible, or just rely on outline
        } else {
            holder.tvAvailability.setText("Busy");
            holder.tvAvailability.setTextColor(Color.parseColor("#D32F2F"));
        }

        holder.btnViewDetails.setOnClickListener(v -> {
            if (listener != null) listener.onViewDetails(faculty);
        });

        holder.btnBookSlot.setOnClickListener(v -> {
            if (listener != null) listener.onBookSlot(faculty);
        });
    }

    @Override
    public int getItemCount() {
        return facultyList != null ? facultyList.size() : 0;
    }

    private String getString(DataSnapshot snapshot, String... keys) {
        for (String key : keys) {
            Object val = snapshot.child(key).getValue();
            if (val != null) return String.valueOf(val);
        }
        return "";
    }

    static class ProfessorViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPost, tvAvailability;
        MaterialButton btnViewDetails, btnBookSlot;

        public ProfessorViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvPost = itemView.findViewById(R.id.tvPost);
            tvAvailability = itemView.findViewById(R.id.tvAvailability);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnBookSlot = itemView.findViewById(R.id.btnBookSlot);
        }
    }
}
