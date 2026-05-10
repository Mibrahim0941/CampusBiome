package com.example.campusbiome.societies.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusbiome.R;
import com.example.campusbiome.societies.models.Society;

import java.util.List;

public class PendingSocietyAdapter
        extends RecyclerView.Adapter<PendingSocietyAdapter.VH> {

    private final List<Society> societies;

    public PendingSocietyAdapter(List<Society> societies) {
        this.societies = societies;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_society_pending, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Society s = societies.get(position);

        String name = s.getName() != null ? s.getName() : "?";
        h.txtInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
        h.txtName.setText(name);
        h.txtCategory.setText(s.getCategory() != null ? s.getCategory() : "Society");
        h.txtDescription.setText(s.getDescription() != null ? s.getDescription() : "");

        // Orange avatar for pending
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.parseColor("#FFB300"));
        h.txtInitial.setBackground(bg);
    }

    @Override
    public int getItemCount() { return societies.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView txtInitial, txtName, txtCategory, txtDescription;

        VH(@NonNull View v) {
            super(v);
            txtInitial   = v.findViewById(R.id.txtPendingInitial);
            txtName      = v.findViewById(R.id.txtPendingName);
            txtCategory  = v.findViewById(R.id.txtPendingCategory);
            txtDescription = v.findViewById(R.id.txtPendingDescription);
        }
    }
}