package com.example.campusbiome;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class FacultyAppointmentAdapter extends RecyclerView.Adapter<FacultyAppointmentAdapter.ViewHolder> {

    private List<FacultyAppointment> appointmentList;
    private OnAppointmentActionListener actionListener;

    public interface OnAppointmentActionListener {
        void onAccept(FacultyAppointment appointment);
        void onReject(FacultyAppointment appointment);
    }

    public FacultyAppointmentAdapter(List<FacultyAppointment> appointmentList, OnAppointmentActionListener actionListener) {
        this.appointmentList = appointmentList;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_faculty_appointment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FacultyAppointment appointment = appointmentList.get(position);

        if (appointment.getStudent() != null) {
            holder.tvStudentName.setText(appointment.getStudent().getName());
        } else {
            holder.tvStudentName.setText("Unknown Student");
        }

        // e.g. "• 3:00 pm - Assignment help"
        String desc = "• " + appointment.getCreatedAt() + " - " + appointment.getDescription();
        holder.tvDescription.setText(desc);
        
        holder.tvStatus.setText(appointment.getStatus() != null ? appointment.getStatus().substring(0, 1).toUpperCase() + appointment.getStatus().substring(1) : "Pending");

        if ("pending".equalsIgnoreCase(appointment.getStatus())) {
            holder.layoutActionButtons.setVisibility(View.VISIBLE);
        } else {
            holder.layoutActionButtons.setVisibility(View.GONE);
        }

        holder.btnAccept.setOnClickListener(v -> actionListener.onAccept(appointment));
        holder.btnReject.setOnClickListener(v -> actionListener.onReject(appointment));
    }

    @Override
    public int getItemCount() {
        return appointmentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvDescription, tvStatus;
        MaterialButton btnAccept, btnReject;
        LinearLayout layoutActionButtons;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
            layoutActionButtons = itemView.findViewById(R.id.layoutActionButtons);
        }
    }
}
