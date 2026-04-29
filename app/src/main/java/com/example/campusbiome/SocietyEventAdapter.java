package com.example.campusbiome;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * Binds DashboardEvent (from Firebase Events node) to item_dashboard_event.xml.
 * Card style mirrors the student dashboard event cards exactly.
 */
public class SocietyEventAdapter extends RecyclerView.Adapter<SocietyEventAdapter.ViewHolder> {

    private final List<SocietyEvent> list;

    public SocietyEventAdapter(List<SocietyEvent> list) { this.list = list; }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dashboard_event, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        SocietyEvent e = list.get(pos);
        h.tvDay.setText(e.getDay()         != null ? e.getDay()         : "--");
        h.tvMonth.setText(e.getMonth()     != null ? e.getMonth()       : "---");
        h.tvTitle.setText(e.getTitle()     != null ? e.getTitle()       : "");
        h.tvDesc.setText(e.getDescription()!= null ? e.getDescription() : "");
    }

    @Override public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay, tvMonth, tvTitle, tvDesc;
        ViewHolder(View v) {
            super(v);
            tvDay   = v.findViewById(R.id.tvEventDay);
            tvMonth = v.findViewById(R.id.tvEventMonth);
            tvTitle = v.findViewById(R.id.tvEventTitle);
            tvDesc  = v.findViewById(R.id.tvEventDescription);
        }
    }
}
