package com.example.campusbiome;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.List;

public class SocietyTaskAdapter
        extends RecyclerView.Adapter<SocietyTaskAdapter.ViewHolder> {

    public interface OnTaskActionListener {
        /** Called when the manager taps "Mark Complete" or "Mark Pending" */
        void onToggleStatus(SocietyTask task, String taskId);
    }

    private final List<SocietyTask>    list;
    private final List<String>         taskIds;   // parallel Firebase key list
    private final OnTaskActionListener listener;  // null → read-only (no button)

    public SocietyTaskAdapter(List<SocietyTask>    list,
                              List<String>         taskIds,
                              OnTaskActionListener listener) {
        this.list     = list;
        this.taskIds  = taskIds;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_society_task, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        SocietyTask t   = list.get(pos);
        Context     ctx = h.itemView.getContext();

        // ── Title ─────────────────────────────────────────────────────────────
        if (h.tvTitle != null)
            h.tvTitle.setText(t.getTitle() != null ? t.getTitle() : "");

        // ── Description ───────────────────────────────────────────────────────
        if (h.tvDescription != null) {
            String desc = t.getDescription();
            if (desc != null && !desc.isEmpty()) {
                h.tvDescription.setText(desc);
                h.tvDescription.setVisibility(View.VISIBLE);
            } else {
                h.tvDescription.setVisibility(View.GONE);
            }
        }

        // ── Assigned to ───────────────────────────────────────────────────────
        if (h.tvAssignedTo != null) {
            String name = t.getAssignedToName();
            if (name != null && !name.isEmpty()) {
                h.tvAssignedTo.setText("👤 " + name);
                h.tvAssignedTo.setVisibility(View.VISIBLE);
            } else if (t.getAssignedTo() != null) {
                h.tvAssignedTo.setText("👤 " + t.getAssignedTo());
                h.tvAssignedTo.setVisibility(View.VISIBLE);
            } else {
                h.tvAssignedTo.setVisibility(View.GONE);
            }
        }

        // ── Due date ──────────────────────────────────────────────────────────
        if (h.tvDueDate != null) {
            String date = t.getFormattedDueDate();
            String time = t.getFormattedDueTime();
            String full = date;
            if (time != null) full += "  " + time;
            h.tvDueDate.setText("📅 " + full);
        }

        // ── Priority chip ─────────────────────────────────────────────────────
        if (h.chipPriority != null) {
            String priority = t.getPriority();
            if (priority != null && !priority.isEmpty()) {
                h.chipPriority.setText(priority);
                h.chipPriority.setVisibility(View.VISIBLE);
                // Colour by priority level
                int bgColor;
                switch (priority.toLowerCase()) {
                    case "high":
                        bgColor = 0xFFFFCDD2; // red-100
                        break;
                    case "medium":
                        bgColor = 0xFFFFF9C4; // yellow-100
                        break;
                    default:               // low
                        bgColor = 0xFFC8E6C9; // green-100
                        break;
                }
                h.chipPriority.setChipBackgroundColor(
                        android.content.res.ColorStateList.valueOf(bgColor));
            } else {
                h.chipPriority.setVisibility(View.GONE);
            }
        }

        // ── Status chip ───────────────────────────────────────────────────────
        if (h.chipStatus != null) {
            String status = t.getStatus();
            if (status == null) status = "pending";
            h.chipStatus.setText(statusLabel(status));
            int bgColor;
            switch (status.toLowerCase()) {
                case "completed":
                    bgColor = 0xFFC8E6C9; // green-100
                    break;
                case "in_progress":
                    bgColor = 0xFFBBDEFB; // blue-100
                    break;
                default:            // pending
                    bgColor = 0xFFE1BEE7; // purple-100
                    break;
            }
            h.chipStatus.setChipBackgroundColor(
                    android.content.res.ColorStateList.valueOf(bgColor));
        }

        // ── Toggle-status button ──────────────────────────────────────────────
        if (h.btnToggleStatus != null) {
            if (listener != null && taskIds != null && pos < taskIds.size()) {
                h.btnToggleStatus.setVisibility(View.VISIBLE);
                boolean done = t.isCompleted();
                h.btnToggleStatus.setText(done ? "Mark Pending" : "Mark Complete");
                String taskId = taskIds.get(pos);
                h.btnToggleStatus.setOnClickListener(v -> listener.onToggleStatus(t, taskId));
            } else {
                h.btnToggleStatus.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String statusLabel(String status) {
        switch (status.toLowerCase()) {
            case "completed":  return "Completed";
            case "in_progress": return "In Progress";
            default:           return "Pending";
        }
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView       tvTitle, tvDescription, tvAssignedTo, tvDueDate;
        Chip           chipPriority, chipStatus;
        MaterialButton btnToggleStatus;

        ViewHolder(View v) {
            super(v);
            tvTitle         = v.findViewById(R.id.tvTaskTitle);
            tvDescription   = v.findViewById(R.id.tvTaskDescription);
            tvAssignedTo    = v.findViewById(R.id.tvTaskAssignedTo);
            tvDueDate       = v.findViewById(R.id.tvTaskDueDate);
            chipPriority    = v.findViewById(R.id.chipTaskPriority);
            chipStatus      = v.findViewById(R.id.chipTaskStatus);
            btnToggleStatus = v.findViewById(R.id.btnToggleTaskStatus);
        }
    }
}