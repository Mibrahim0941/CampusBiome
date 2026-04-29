package com.example.campusbiome.studyGroups.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusbiome.R;
import com.example.campusbiome.studyGroups.models.StudySession;

import java.util.List;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {

    private final List<StudySession> sessions;

    public SessionAdapter(List<StudySession> sessions) {
        this.sessions = sessions;
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        StudySession session = sessions.get(position);

        holder.txtTitle.setText(session.getTitle());
        holder.txtTime.setText(session.getTime());
        holder.txtLocation.setText(session.getLocation());

        // Parse date string — stored as "dd MMM yyyy" e.g. "15 Jan 2025"
        // Split into day and month for the badge
        String date = session.getDate();
        if (date != null && date.contains(" ")) {
            String[] parts = date.split(" ");
            holder.txtDay.setText(parts[0]);                          // "15"
            holder.txtMonth.setText(parts.length > 1 ? parts[1] : ""); // "Jan"
        } else {
            holder.txtDay.setText(date != null ? date : "");
            holder.txtMonth.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    static class SessionViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtDay, txtMonth, txtTime, txtLocation;

        SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle    = itemView.findViewById(R.id.txtSessionTitle);
            txtDay      = itemView.findViewById(R.id.txtSessionDay);
            txtMonth    = itemView.findViewById(R.id.txtSessionMonth);
            txtTime     = itemView.findViewById(R.id.txtSessionTime);
            txtLocation = itemView.findViewById(R.id.txtSessionLocation);
        }
    }
}