package com.example.campusbiome;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TimetableClassAdapter extends RecyclerView.Adapter<TimetableClassAdapter.ViewHolder> {
    private List<TimetableSlot> classList;

    public TimetableClassAdapter(List<TimetableSlot> classList) {
        this.classList = classList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timetable_class, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TimetableSlot slot = classList.get(position);
        holder.tvCourseTitle.setText(slot.courseTitle);
        holder.tvInstructor.setText("Prof: " + slot.instructor);
        
        // Calculate end time and append to start time, or AM/PM
        String endTime = calculateEndTime(slot.timeSlot, slot.durationMinutes);
        holder.tvTime.setText(formatTimeAmPm(slot.timeSlot) + " - " + formatTimeAmPm(endTime));
        holder.tvVenue.setText("Room: " + slot.venue);
    }

    @Override
    public int getItemCount() {
        return classList.size();
    }

    public void updateData(List<TimetableSlot> newData) {
        this.classList.clear();
        this.classList.addAll(newData);
        notifyDataSetChanged();
    }

    private String formatTimeAmPm(String time24) {
        try {
            String[] parts = time24.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            String amPm = hour >= 12 ? "pm" : "am";
            int hour12 = hour % 12;
            if (hour12 == 0) hour12 = 12;
            return String.format("%d:%02d %s", hour12, minute, amPm);
        } catch (Exception e) {
            return time24;
        }
    }

    private String calculateEndTime(String startTime, int durationMinutes) {
        try {
            String[] parts = startTime.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            int totalMinutes = hour * 60 + minute + durationMinutes;
            int endHour = totalMinutes / 60;
            int endMinute = totalMinutes % 60;
            return String.format("%02d:%02d", endHour, endMinute);
        } catch (Exception e) {
            return startTime; // Fallback
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCourseTitle, tvInstructor, tvTime, tvVenue;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCourseTitle = itemView.findViewById(R.id.tvCourseTitle);
            tvInstructor = itemView.findViewById(R.id.tvInstructor);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvVenue = itemView.findViewById(R.id.tvVenue);
        }
    }
}
