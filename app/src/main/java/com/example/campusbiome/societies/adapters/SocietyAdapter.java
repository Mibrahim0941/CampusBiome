package com.example.campusbiome.societies.adapters;

import android.content.Context;
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

public class SocietyAdapter extends RecyclerView.Adapter<SocietyAdapter.SocietyViewHolder> {

    public interface OnSocietyClickListener {
        void onSocietyClicked(Society society, String societyId);
    }

    // Distinct colors for avatar backgrounds
    private static final int[] AVATAR_COLORS = {
            0xFF6C63FF, 0xFF4CAF50, 0xFFFF7043,
            0xFF29B6F6, 0xFFAB47BC, 0xFFFFCA28
    };

    private final List<Society> societies;
    private final List<String>  societyIds;
    private final String        currentUid;
    private final List<String>  pendingIds;   // societyIds where user has pending request
    private final OnSocietyClickListener listener;

    public SocietyAdapter(List<Society> societies,
                          List<String>  societyIds,
                          String        currentUid,
                          List<String>  pendingIds,
                          OnSocietyClickListener listener) {
        this.societies  = societies;
        this.societyIds = societyIds;
        this.currentUid = currentUid;
        this.pendingIds = pendingIds;
        this.listener   = listener;
    }

    @NonNull
    @Override
    public SocietyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_society, parent, false);
        return new SocietyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SocietyViewHolder h, int position) {
        Society s  = societies.get(position);
        String  id = societyIds.get(position);

        // Initial avatar
        String name = s.getName() != null ? s.getName() : "?";
        h.txtInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
        setAvatarColor(h.txtInitial, position);

        h.txtName.setText(name);
        h.txtCategory.setText(s.getCategory() != null ? s.getCategory() : "Society");
        h.txtDescription.setText(s.getDescription() != null
                ? s.getDescription() : "No description available.");
        h.txtMemberCount.setText(s.getMemberCount() + " members");

        // Status badges
        boolean isMember = currentUid != null && s.isMember(currentUid);
        boolean isPending = pendingIds.contains(id);

        h.txtMemberBadge.setVisibility(isMember  ? View.VISIBLE : View.GONE);
        h.txtPendingBadge.setVisibility(isPending && !isMember ? View.VISIBLE : View.GONE);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSocietyClicked(s, id);
        });
    }

    @Override
    public int getItemCount() { return societies.size(); }

    private void setAvatarColor(TextView view, int position) {
        int color = AVATAR_COLORS[position % AVATAR_COLORS.length];
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(color);
        view.setBackground(bg);
    }

    static class SocietyViewHolder extends RecyclerView.ViewHolder {
        TextView txtInitial, txtName, txtCategory, txtDescription,
                txtMemberCount, txtMemberBadge, txtPendingBadge;

        SocietyViewHolder(@NonNull View v) {
            super(v);
            txtInitial     = v.findViewById(R.id.txtSocietyInitial);
            txtName        = v.findViewById(R.id.txtSocietyName);
            txtCategory    = v.findViewById(R.id.txtSocietyCategory);
            txtDescription = v.findViewById(R.id.txtSocietyDescription);
            txtMemberCount = v.findViewById(R.id.txtMemberCount);
            txtMemberBadge  = v.findViewById(R.id.txtMemberBadge);
            txtPendingBadge = v.findViewById(R.id.txtPendingBadge);
        }
    }
}