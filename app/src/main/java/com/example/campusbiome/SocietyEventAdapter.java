package com.example.campusbiome;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class SocietyEventAdapter
        extends RecyclerView.Adapter<SocietyEventAdapter.ViewHolder> {

    public interface OnViewRegistrationsListener {
        void onViewRegistrations(SocietyEvent event, String eventId);
    }

    private final List<SocietyEvent>          list;
    private final List<String>                eventIds;
    private final OnViewRegistrationsListener listener;
    private final String                      buttonLabel; // e.g. "View Registrations" or "View Tasks"

    // ── Constructor with custom button label ──────────────────────────────────
    public SocietyEventAdapter(List<SocietyEvent>          list,
                               List<String>                eventIds,
                               OnViewRegistrationsListener listener,
                               String                      buttonLabel) {
        this.list        = list;
        this.eventIds    = eventIds;
        this.listener    = listener;
        this.buttonLabel = buttonLabel;
    }

    // ── Convenience constructor (keeps old call sites working) ────────────────
    public SocietyEventAdapter(List<SocietyEvent>          list,
                               List<String>                eventIds,
                               OnViewRegistrationsListener listener) {
        this(list, eventIds, listener, "View Registrations");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (listener == null)
                ? R.layout.society_dashboard_event  // dashboard — no button
                : R.layout.society_event;           // events/tasks fragment — has button
        View v = LayoutInflater.from(parent.getContext())
                .inflate(layout, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        SocietyEvent e = list.get(pos);

        if (h.tvDay   != null) h.tvDay.setText(e.getDay()    != null ? e.getDay()    : "--");
        if (h.tvMonth != null) h.tvMonth.setText(e.getMonth() != null ? e.getMonth() : "---");
        if (h.tvTitle != null) h.tvTitle.setText(e.getTitle() != null ? e.getTitle() : "");
        if (h.tvDesc  != null) h.tvDesc.setText(e.getDescription() != null ? e.getDescription() : "");

        if (h.tvVenue != null) {
            if (e.getVenue() != null && !e.getVenue().isEmpty()) {
                h.tvVenue.setText("📍 " + e.getVenue());
                h.tvVenue.setVisibility(View.VISIBLE);
            } else {
                h.tvVenue.setVisibility(View.GONE);
            }
        }

        if (h.tvRegistrationCount != null) {
            h.tvRegistrationCount.setText(e.getRegistrationCount() + " registered");
        }

        if (h.btnViewRegistrations != null) {
            if (listener != null && eventIds != null && pos < eventIds.size()) {
                // Set the label based on which screen is using the adapter
                h.btnViewRegistrations.setText(buttonLabel);
                h.btnViewRegistrations.setVisibility(View.VISIBLE);
                String id = eventIds.get(pos);
                h.btnViewRegistrations.setOnClickListener(v -> listener.onViewRegistrations(e, id));
            } else {
                h.btnViewRegistrations.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView       tvDay, tvMonth, tvTitle, tvDesc, tvVenue, tvRegistrationCount;
        MaterialButton btnViewRegistrations;

        ViewHolder(View v) {
            super(v);
            tvDay                = v.findViewById(R.id.tvEventDay);
            tvMonth              = v.findViewById(R.id.tvEventMonth);
            tvTitle              = v.findViewById(R.id.tvEventTitle);
            tvDesc               = v.findViewById(R.id.tvEventDescription);
            tvVenue              = v.findViewById(R.id.tvEventVenue);
            tvRegistrationCount  = v.findViewById(R.id.tvRegistrationCount);
            btnViewRegistrations = v.findViewById(R.id.btnViewRegistrations);
        }
    }
}