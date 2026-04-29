package com.example.campusbiome.studyGroups.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusbiome.R;
import com.example.campusbiome.studyGroups.models.StudyGroup;

import java.util.List;

public class StudyGroupAdapter
        extends RecyclerView.Adapter<StudyGroupAdapter.GroupViewHolder> {

    // ── Callback interface ──────────────────────────────────────────────────
    public interface OnGroupActionListener {
        void onActionClicked(StudyGroup group, String groupId);
    }

    // ── Fields ──────────────────────────────────────────────────────────────
    private final List<StudyGroup>        groups;
    private final List<String>            groupIds;
    private final List<String>            joinedIds;   // IDs the current user has joined
    private final OnGroupActionListener   listener;

    // ── Constructor matching StudyGroupsFragment call ───────────────────────
    public StudyGroupAdapter(List<StudyGroup>      groups,
                             List<String>          groupIds,
                             List<String>          joinedIds,
                             OnGroupActionListener listener) {
        this.groups    = groups;
        this.groupIds  = groupIds;
        this.joinedIds = joinedIds;
        this.listener  = listener;
    }

    // ── RecyclerView boilerplate ─────────────────────────────────────────────
    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_study_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        StudyGroup group   = groups.get(position);
        String     groupId = groupIds.get(position);
        boolean    joined  = joinedIds.contains(groupId);

        // Basic text
        holder.txtName.setText(group.getName());
        holder.txtCourse.setText(group.getCourse());
        holder.txtMembers.setText(group.getCurrentMembers() + "/" + group.getMaxMembers() + " members");

        if (group.getDescription() != null) {
            holder.txtDescription.setText(group.getDescription());
            holder.txtDescription.setVisibility(View.VISIBLE);
        } else {
            holder.txtDescription.setVisibility(View.GONE);
        }

        // Tags
        holder.tagsContainer.removeAllViews();
        if (group.getTags() != null) {
            for (String tag : group.getTags()) {
                holder.tagsContainer.addView(makeTagChip(holder.itemView.getContext(), tag));
            }
        }

        // Joined badge
        if (joined) {
            holder.txtJoinedBadge.setVisibility(View.VISIBLE);
            holder.btnAction.setText("View Group");
            holder.btnAction.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        } else {
            holder.txtJoinedBadge.setVisibility(View.GONE);
            holder.btnAction.setText("Join");
            holder.btnAction.setBackgroundTintList(null); // revert to @color/primary from XML
        }

        // Click
        holder.btnAction.setOnClickListener(v -> {
            if (listener != null) listener.onActionClicked(group, groupId);
        });

        // Full card click also triggers action
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onActionClicked(group, groupId);
        });
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    // ── Tag chip helper ──────────────────────────────────────────────────────
    private TextView makeTagChip(Context ctx, String label) {
        TextView chip = new TextView(ctx);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMarginEnd(8);
        chip.setLayoutParams(params);

        chip.setText(label);
        chip.setTextSize(11f);
        chip.setTextColor(Color.parseColor("#6C63FF"));
        chip.setPadding(20, 6, 20, 6);
        chip.setBackground(makeTagBackground(ctx));
        return chip;
    }

    private android.graphics.drawable.GradientDrawable makeTagBackground(Context ctx) {
        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        bg.setCornerRadius(30f);
        bg.setColor(Color.parseColor("#1A6C63FF"));
        return bg;
    }

    // ── ViewHolder ───────────────────────────────────────────────────────────
    static class GroupViewHolder extends RecyclerView.ViewHolder {

        TextView     txtName, txtCourse, txtMembers, txtDescription, txtJoinedBadge;
        LinearLayout tagsContainer;
        Button       btnAction;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName        = itemView.findViewById(R.id.txtGroupName);
            txtCourse      = itemView.findViewById(R.id.txtCourse);
            txtMembers     = itemView.findViewById(R.id.txtMembers);
            txtDescription = itemView.findViewById(R.id.txtDescription);
            txtJoinedBadge = itemView.findViewById(R.id.txtJoinedBadge);
            tagsContainer  = itemView.findViewById(R.id.tagsContainer);
            btnAction      = itemView.findViewById(R.id.btnAction);
        }
    }
}