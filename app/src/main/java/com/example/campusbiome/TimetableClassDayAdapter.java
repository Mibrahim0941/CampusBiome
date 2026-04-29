package com.example.campusbiome;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TimetableClassDayAdapter extends RecyclerView.Adapter<TimetableClassDayAdapter.ViewHolder> {
    private List<TimetableSlot> classList;

    public TimetableClassDayAdapter(List<TimetableSlot> classList) {
        this.classList = classList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timetable_class_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TimetableSlot slot = classList.get(position);
        holder.tvCourseTitle.setText(slot.courseTitle);
        holder.tvCourseCode.setText(slot.courseCode + " - Lecture");
        holder.tvInstructor.setText("Prof: " + slot.instructor);
        holder.tvVenue.setText("Room: " + slot.venue);
        
        String endTime = calculateEndTime(slot.timeSlot, slot.durationMinutes);
        holder.tvTimeRange.setText(formatTimeAmPm(slot.timeSlot) + " - " + formatTimeAmPm(endTime));

        // Format side time
        try {
            String[] parts = slot.timeSlot.split(":");
            int hour = Integer.parseInt(parts[0]);
            int min = Integer.parseInt(parts[1]);
            String amPm = hour >= 12 ? "PM" : "AM";
            int hour12 = hour % 12;
            if (hour12 == 0) hour12 = 12;
            
            if (min == 0) {
                holder.tvSideTimeValue.setText(String.valueOf(hour12));
            } else {
                holder.tvSideTimeValue.setText(hour12 + ":" + String.format("%02d", min));
            }
            holder.tvSideTimeAmPm.setText(amPm);
        } catch (Exception e) {
            holder.tvSideTimeValue.setText(slot.timeSlot);
            holder.tvSideTimeAmPm.setText("");
        }
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
        TextView tvSideTimeValue, tvSideTimeAmPm;
        TextView tvCourseTitle, tvCourseCode, tvInstructor, tvVenue, tvTimeRange;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSideTimeValue = itemView.findViewById(R.id.tvSideTimeValue);
            tvSideTimeAmPm = itemView.findViewById(R.id.tvSideTimeAmPm);
            tvCourseTitle = itemView.findViewById(R.id.tvCourseTitle);
            tvCourseCode = itemView.findViewById(R.id.tvCourseCode);
            tvInstructor = itemView.findViewById(R.id.tvInstructor);
            tvVenue = itemView.findViewById(R.id.tvVenue);
            tvTimeRange = itemView.findViewById(R.id.tvTimeRange);
        }
    }
}
