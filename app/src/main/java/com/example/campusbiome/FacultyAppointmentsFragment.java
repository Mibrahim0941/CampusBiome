package com.example.campusbiome;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FacultyAppointmentsFragment extends Fragment {

    private RecyclerView rvAppointments;
    private FacultyAppointmentAdapter adapter;
    private List<FacultyAppointment> appointmentList;
    private DatabaseReference dbRef;
    private String currentUid;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_faculty_appointments, container, false);

        rvAppointments = view.findViewById(R.id.rvAppointments);
        rvAppointments.setLayoutManager(new LinearLayoutManager(getContext()));
        appointmentList = new ArrayList<>();
        
        adapter = new FacultyAppointmentAdapter(appointmentList, new FacultyAppointmentAdapter.OnAppointmentActionListener() {
            @Override
            public void onAccept(FacultyAppointment appointment) {
                updateAppointmentStatus(appointment, "approved");
            }

            @Override
            public void onReject(FacultyAppointment appointment) {
                updateAppointmentStatus(appointment, "rejected");
            }

            @Override
            public void onClick(FacultyAppointment appointment) {
                showAppointmentDetailsDialog(appointment);
            }
        });
        rvAppointments.setAdapter(adapter);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            dbRef = FirebaseDatabase.getInstance().getReference("FacultyAppointment");
            fetchAppointments();
        }

        return view;
    }

    private void fetchAppointments() {
        // We fetch all appointments here for presentation/testing so they show up. 
        // If your database gets updated with correct 'facultyId' in FacultyAppointment, 
        dbRef.orderByChild("facultyId").equalTo(currentUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                appointmentList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    FacultyAppointment appointment = ds.getValue(FacultyAppointment.class);
                    if (appointment != null) {
                        appointment.setId(ds.getKey());
                        appointmentList.add(appointment);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FacultyAppointments", "Failed to load appointments", error.toException());
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load appointments", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showAppointmentDetailsDialog(FacultyAppointment appointment) {
        if (getContext() == null || appointment == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_appointment_details, null);
        
        TextView tvDescription = dialogView.findViewById(R.id.tvDetailDescription);
        com.google.android.material.button.MaterialButton btnClose = dialogView.findViewById(R.id.btnClose);

        // Setup items
        View detailName = dialogView.findViewById(R.id.detailStudentName);
        View detailId = dialogView.findViewById(R.id.detailStudentId);
        View detailEmail = dialogView.findViewById(R.id.detailStudentEmail);
        View detailProgram = dialogView.findViewById(R.id.detailStudentProgram);
        View detailDateTime = dialogView.findViewById(R.id.detailDateTime);

        setupDetailItem(detailName, "Student Name", appointment.getStudent() != null ? appointment.getStudent().getName() : "N/A", R.drawable.ic_profile);
        setupDetailItem(detailId, "Student ID", appointment.getStudent() != null ? appointment.getStudent().getUid() : "N/A", R.drawable.ic_id);
        setupDetailItem(detailEmail, "Email Address", appointment.getStudent() != null ? appointment.getStudent().getEmail() : "N/A", R.drawable.ic_email);
        setupDetailItem(detailProgram, "Program & Section", 
            appointment.getStudent() != null ? (appointment.getStudent().getProgram() + " - " + appointment.getStudent().getSection()) : "N/A", 
            R.drawable.ic_department);
        
        setupDetailItem(detailDateTime, "Date & Time", appointment.getCreatedAt(), R.drawable.ic_office_hours);

        tvDescription.setText(appointment.getDescription());

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(getContext())
            .setView(dialogView)
            .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void setupDetailItem(View itemView, String label, String value, int iconRes) {
        ((TextView) itemView.findViewById(R.id.tvLabel)).setText(label);
        ((TextView) itemView.findViewById(R.id.tvValue)).setText(value);
        ((android.widget.ImageView) itemView.findViewById(R.id.ivIcon)).setImageResource(iconRes);
    }

    private void updateAppointmentStatus(FacultyAppointment appointment, String status) {
        if (appointment.getId() != null) {
            dbRef.child(appointment.getId()).child("status").setValue(status)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Appointment " + status, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update", Toast.LENGTH_SHORT).show());
        }
    }
}
