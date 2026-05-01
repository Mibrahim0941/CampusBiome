package com.example.campusbiome;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    public interface OnRegisterClickListener {
        void onRegisterClicked(SocietyEvent event, int position);
    }

    private final List<SocietyEvent>    events;
    private final String                currentUid;
    private final OnRegisterClickListener listener;

    public EventAdapter(List<SocietyEvent>      events,
                        String                  currentUid,
                        OnRegisterClickListener listener) {
        this.events     = events;
        this.currentUid = currentUid;
        this.listener   = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder h, int position) {
        SocietyEvent e = events.get(position);

        // Date
        h.tvDay.setText(e.getDay()   != null ? e.getDay()   : "--");
        h.tvMonth.setText(e.getMonth() != null ? e.getMonth() : "---");

        // Title & society
        h.tvTitle.setText(e.getTitle() != null ? e.getTitle() : "");
        if (e.getSocietyName() != null) {
            h.tvSocietyName.setText(e.getSocietyName());
            h.tvSocietyName.setVisibility(View.VISIBLE);
        } else {
            h.tvSocietyName.setVisibility(View.GONE);
        }

        // Description
        h.tvDescription.setText(e.getDescription() != null ? e.getDescription() : "");

        // Time
        String timeStr = e.getFormattedTime();
        if (timeStr != null) {
            h.tvTime.setText("🕐 " + timeStr);
            h.tvTime.setVisibility(View.VISIBLE);
        } else {
            h.tvTime.setVisibility(View.GONE);
        }

        // Venue
        if (e.getVenue() != null && !e.getVenue().isEmpty()) {
            h.tvVenue.setText("📍 " + e.getVenue());
            h.tvVenue.setVisibility(View.VISIBLE);
        } else {
            h.tvVenue.setVisibility(View.GONE);
        }

        // Registration count
        int count = e.getRegistrationCount();
        h.tvRegisteredCount.setText("👥 " + count + " registered");

        // Register / Registered button state
        boolean registered = currentUid != null && e.isRegistered(currentUid);
        if (registered) {
            h.btnRegister.setVisibility(View.GONE);
            h.btnRegistered.setVisibility(View.VISIBLE);
        } else {
            h.btnRegister.setVisibility(View.VISIBLE);
            h.btnRegistered.setVisibility(View.GONE);
        }

        h.btnRegister.setOnClickListener(v -> {
            if (listener != null) listener.onRegisterClicked(e, h.getAdapterPosition());
        });

        // Tapping "✓ Registered" unregisters
        h.btnRegistered.setOnClickListener(v -> {
            if (listener != null) listener.onRegisterClicked(e, h.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() { return events.size(); }


    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView       tvDay, tvMonth, tvTitle, tvSocietyName,
                tvDescription, tvTime, tvVenue, tvRegisteredCount;
        MaterialButton btnRegister, btnRegistered;

        EventViewHolder(@NonNull View v) {
            super(v);
            tvDay             = v.findViewById(R.id.tvEventDay);
            tvMonth           = v.findViewById(R.id.tvEventMonth);
            tvTitle           = v.findViewById(R.id.tvEventTitle);
            tvSocietyName     = v.findViewById(R.id.tvSocietyName);
            tvDescription     = v.findViewById(R.id.tvEventDescription);
            tvTime            = v.findViewById(R.id.tvEventTime);
            tvVenue           = v.findViewById(R.id.tvEventVenue);
            tvRegisteredCount = v.findViewById(R.id.tvRegisteredCount);
            btnRegister       = v.findViewById(R.id.btnRegister);
            btnRegistered     = v.findViewById(R.id.btnRegistered);
        }
    }
}