package com.example.campusbiome;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment to manage and view faculty appointments.
 */
public class FacultyAppointmentsFragment extends Fragment {

    private RecyclerView rvAppointments;
    private FacultyAppointmentAdapter adapter;
    private List<FacultyAppointment> appointmentList;
    private DatabaseReference dbRef;
    private String currentUid;

    private View layoutEmptyState, viewGlow;
    private TextView tvEmptyGreeting, tvAppointmentStats;
    private MaterialButton btnQuickCheck;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_faculty_appointments, container, false);

        rvAppointments = view.findViewById(R.id.rvAppointments);
        rvAppointments.setLayoutManager(new LinearLayoutManager(getContext()));
        appointmentList = new ArrayList<>();
        
        adapter = new FacultyAppointmentAdapter(appointmentList, new FacultyAppointmentAdapter.OnAppointmentActionListener() {
            @Override
            public void onAccept(com.example.campusbiome.FacultyAppointment appointment) {
                updateAppointmentStatus(appointment, "approved", null);
            }

            @Override
            public void onReject(com.example.campusbiome.FacultyAppointment appointment) {
                showRejectionDialog(appointment);
            }

            @Override
            public void onClick(com.example.campusbiome.FacultyAppointment appointment) {
                showAppointmentDetailsDialog(appointment);
            }
        });
        rvAppointments.setAdapter(adapter);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            dbRef = FirebaseDatabase.getInstance().getReference("FacultyAppointment");
            
            layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
            viewGlow = view.findViewById(R.id.viewGlow);
            tvEmptyGreeting = view.findViewById(R.id.tvEmptyGreeting);
            tvAppointmentStats = view.findViewById(R.id.tvAppointmentStats);
            btnQuickCheck = view.findViewById(R.id.btnQuickCheck);

            setupEmptyStateAnimations();
            fetchAppointments();
        }

        return view;
    }

    private void fetchAppointments() {
        dbRef.orderByChild("facultyId").equalTo(currentUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                appointmentList.clear();
                long totalCount = snapshot.getChildrenCount();
                
                for (DataSnapshot ds : snapshot.getChildren()) {
                    FacultyAppointment appointment = ds.getValue(FacultyAppointment.class);
                    if (appointment != null) {
                        appointment.setId(ds.getKey());
                        appointmentList.add(appointment);
                    }
                }
                adapter.notifyDataSetChanged();
                updateEmptyState(appointmentList.isEmpty(), (int) totalCount);
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
        MaterialButton btnClose = dialogView.findViewById(R.id.btnClose);

        // Setup items
        View detailName = dialogView.findViewById(R.id.detailStudentName);
        View detailId = dialogView.findViewById(R.id.detailStudentId);
        View detailEmail = dialogView.findViewById(R.id.detailStudentEmail);
        View detailProgram = dialogView.findViewById(R.id.detailStudentProgram);
        View detailDateTime = dialogView.findViewById(R.id.detailDateTime);

        FacultyAppointment.StudentInfo student = appointment.getStudent();
        setupDetailItem(detailName, "Student Name", student != null ? student.getName() : "N/A", R.drawable.ic_profile);
        setupDetailItem(detailId, "Student ID", student != null ? student.getUid() : "N/A", R.drawable.ic_id);
        setupDetailItem(detailEmail, "Email Address", student != null ? student.getEmail() : "N/A", R.drawable.ic_email);
        setupDetailItem(detailProgram, "Program & Section", 
            student != null ? (student.getProgram() + " - " + student.getSection()) : "N/A", 
            R.drawable.ic_department);
        
        setupDetailItem(detailDateTime, "Date & Time", appointment.getCreatedAt(), R.drawable.ic_office_hours);

        tvDescription.setText(appointment.getDescription());

        AlertDialog dialog = new AlertDialog.Builder(getContext())
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

    private void showRejectionDialog(FacultyAppointment appointment) {
        if (getContext() == null) return;

        EditText etReason = new EditText(getContext());
        etReason.setHint("Enter reason for rejection");
        etReason.setPadding(40, 40, 40, 40);

        new AlertDialog.Builder(getContext())
                .setTitle("Reject Appointment")
                .setView(etReason)
                .setPositiveButton("Reject", (dialog, which) -> {
                    String reason = etReason.getText().toString().trim();
                    if (reason.isEmpty()) {
                        reason = "No reason provided.";
                    }
                    updateAppointmentStatus(appointment, "rejected", reason);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void updateAppointmentStatus(FacultyAppointment appointment, String status, String rejectionReason) {
        if (appointment.getId() != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", status);
            if (rejectionReason != null) {
                updates.put("rejectionReason", rejectionReason);
            }

            dbRef.child(appointment.getId()).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Appointment " + status, Toast.LENGTH_SHORT).show();
                    }
                    
                    FacultyAppointment.StudentInfo student = appointment.getStudent();
                    if (student != null) {
                        String studentUid = student.getUid();
                        String day = appointment.getDay();
                        String startTime = appointment.getStartTime();
                        
                        String title;
                        String message;
                        
                        if ("approved".equals(status)) {
                            title = "Appointment Approved!";
                            message = "Your meeting for " + (day != null ? day : "") + " at " + (startTime != null ? startTime : "") + " has been approved.";
                        } else if ("rejected".equals(status)) {
                            title = "Appointment Rejected";
                            message = "Your meeting for " + (day != null ? day : "") + " at " + (startTime != null ? startTime : "") + " was rejected. Reason: " + rejectionReason;
                        } else {
                            return;
                        }
                        
                        DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference()
                                .child("Notifications").child(studentUid).push();
                                
                        HashMap<String, Object> notifData = new HashMap<>();
                        notifData.put("title", title);
                        notifData.put("message", message);
                        notifData.put("type", "alert");
                        notifData.put("timestamp", System.currentTimeMillis());
                        notifData.put("isRead", false);
                        notifRef.setValue(notifData);
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to update", Toast.LENGTH_SHORT).show();
                    }
                });
        }
    }

    private void updateEmptyState(boolean isEmpty, int totalCount) {
        if (isEmpty) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvAppointments.setVisibility(View.GONE);
            
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            String greeting;
            if (hour < 12) greeting = "Good Morning, Professor";
            else if (hour < 17) greeting = "Good Afternoon, Professor";
            else greeting = "Good Evening, Professor";
            
            tvEmptyGreeting.setText(greeting);
            tvAppointmentStats.setText("You have " + totalCount + " total appointments on record");

            btnQuickCheck.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new FacultyProfileFragment())
                        .commit();
                }
            });
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvAppointments.setVisibility(View.VISIBLE);
        }
    }

    private void setupEmptyStateAnimations() {
        ObjectAnimator glowScaleX = ObjectAnimator.ofFloat(viewGlow, "scaleX", 1f, 1.4f);
        ObjectAnimator glowScaleY = ObjectAnimator.ofFloat(viewGlow, "scaleY", 1f, 1.4f);
        ObjectAnimator glowAlpha = ObjectAnimator.ofFloat(viewGlow, "alpha", 0.1f, 0.3f);

        glowScaleX.setRepeatMode(ValueAnimator.REVERSE);
        glowScaleX.setRepeatCount(ValueAnimator.INFINITE);
        glowScaleY.setRepeatMode(ValueAnimator.REVERSE);
        glowScaleY.setRepeatCount(ValueAnimator.INFINITE);
        glowAlpha.setRepeatMode(ValueAnimator.REVERSE);
        glowAlpha.setRepeatCount(ValueAnimator.INFINITE);

        AnimatorSet glowSet = new AnimatorSet();
        glowSet.playTogether(glowScaleX, glowScaleY, glowAlpha);
        glowSet.setDuration(4000);
        glowSet.start();
    }
}
