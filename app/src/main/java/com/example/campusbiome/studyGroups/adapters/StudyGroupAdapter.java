package com.example.campusbiome.studyGroups.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusbiome.R;
import com.example.campusbiome.studyGroups.models.StudyGroup;

import java.util.List;

public class StudyGroupAdapter
        extends RecyclerView.Adapter<StudyGroupAdapter.GroupViewHolder> {

    public interface OnGroupActionListener {
        void onActionClicked(StudyGroup group, String groupId);
    }

    private final List<StudyGroup>      groups;
    private final List<String>          groupIds;
    private final List<String>          joinedIds;
    private final OnGroupActionListener listener;

    public StudyGroupAdapter(List<StudyGroup>      groups,
                             List<String>          groupIds,
                             List<String>          joinedIds,
                             OnGroupActionListener listener) {
        this.groups    = groups;
        this.groupIds  = groupIds;
        this.joinedIds = joinedIds;
        this.listener  = listener;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_study_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder h, int position) {
        StudyGroup group   = groups.get(position);
        String     groupId = groupIds.get(position);
        boolean    joined  = joinedIds != null && joinedIds.contains(groupId);

        h.txtName.setText(group.getName());
        h.txtCourse.setText(group.getCourse());
        h.txtMembers.setText(group.getCurrentMembers() + "/" + group.getMaxMembers());

        if (group.getDescription() != null && !group.getDescription().isEmpty()) {
            h.txtDescription.setText(group.getDescription());
            h.txtDescription.setVisibility(View.VISIBLE);
        } else {
            h.txtDescription.setVisibility(View.GONE);
        }

        // Tags
        h.tagsContainer.removeAllViews();
        if (group.getTags() != null && !group.getTags().isEmpty()) {
            for (String tag : group.getTags()) {
                h.tagsContainer.addView(makeTagChip(h.itemView.getContext(), tag));
            }
            h.tagsContainer.setVisibility(View.VISIBLE);
        } else {
            h.tagsContainer.setVisibility(View.GONE);
        }

        // Whole card tap → always open details (join/leave lives inside details screen)
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onActionClicked(group, groupId);
        });
    }

    @Override
    public int getItemCount() { return groups.size(); }

    private TextView makeTagChip(Context ctx, String label) {
        TextView chip = new TextView(ctx);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMarginEnd(8);
        chip.setLayoutParams(p);
        chip.setText(label);
        chip.setTextSize(11f);
        chip.setTextColor(Color.parseColor("#6C63FF"));
        chip.setPadding(20, 6, 20, 6);
        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        bg.setCornerRadius(30f);
        bg.setColor(Color.parseColor("#1A6C63FF"));
        chip.setBackground(bg);
        return chip;
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView     txtName, txtCourse, txtMembers, txtDescription;
        LinearLayout tagsContainer;
        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName        = itemView.findViewById(R.id.txtGroupName);
            txtCourse      = itemView.findViewById(R.id.txtCourse);
            txtMembers     = itemView.findViewById(R.id.txtMembers);
            txtDescription = itemView.findViewById(R.id.txtDescription);
            tagsContainer  = itemView.findViewById(R.id.tagsContainer);
        }
    }
}