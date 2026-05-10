package com.example.campusbiome;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.widget.ImageView;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.view.animation.LinearInterpolator;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminDashboardFragment extends Fragment {

    private TextView tvTotalStudents, tvDefaulters;
    private TextView tvTotalStaff, tvLecturers, tvProfessors;
    private TextView tvTotalSocieties, tvPendingSocieties;
    private MaterialCardView btnShowStudentsCard, btnManageFacultyCard, btnManageSocietiesCard;
    private MaterialCardView btnNewAnnouncement, btnManageEvents, btnSeeCampusMap;
    
    private DatabaseReference mDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_dashboard, container, false);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        tvTotalStudents = view.findViewById(R.id.tvTotalStudents);
        tvDefaulters = view.findViewById(R.id.tvDefaulters);

        
        tvTotalStaff = view.findViewById(R.id.tvTotalStaff);
        tvLecturers = view.findViewById(R.id.tvLecturers);
        tvProfessors = view.findViewById(R.id.tvProfessors);
        
        tvTotalSocieties = view.findViewById(R.id.tvTotalSocieties);
        tvPendingSocieties = view.findViewById(R.id.tvPendingSocieties);

        btnShowStudentsCard = view.findViewById(R.id.btnShowStudentsCard);
        btnManageFacultyCard = view.findViewById(R.id.btnManageFacultyCard);
        btnManageSocietiesCard = view.findViewById(R.id.btnManageSocietiesCard);
        
        btnNewAnnouncement = view.findViewById(R.id.btnNewAnnouncement);
        btnManageEvents = view.findViewById(R.id.btnManageEvents);
        btnSeeCampusMap = view.findViewById(R.id.btnSeeCampusMap);

        btnShowStudentsCard.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AdminStudentsActivity.class);
            startActivity(intent);
        });

        btnManageFacultyCard.setOnClickListener(v -> {
            if (getActivity() instanceof AdminDashboardActivity) {
                ((AdminDashboardActivity) getActivity()).switchToProfessors();
            }
        });

        btnManageSocietiesCard.setOnClickListener(v -> {
            if (getActivity() instanceof AdminDashboardActivity) {
                ((AdminDashboardActivity) getActivity()).switchToSocieties();
            }
        });

        btnNewAnnouncement.setOnClickListener(v -> showAnnouncementDialog());
        btnManageEvents.setOnClickListener(v -> {
            if (getActivity() instanceof AdminDashboardActivity) {
                ((AdminDashboardActivity) getActivity()).switchToManageEvents();
            }
        });
        btnSeeCampusMap.setOnClickListener(v -> {
             if (getActivity() instanceof AdminDashboardActivity) {
                 ((AdminDashboardActivity) getActivity()).switchToCampusMap();
             }
        });

        startMapAnimation(view);

        fetchStats();

        return view;
    }

    private void startMapAnimation(View view) {
        ImageView ivMapBackground = view.findViewById(R.id.ivMapBackground);
        if (ivMapBackground == null) return;

        // Slow Ken Burns Effect: Zoom and Pan
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.2f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.2f);
        PropertyValuesHolder transX = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0f, -30f);
        PropertyValuesHolder transY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, -20f);

        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(ivMapBackground, scaleX, scaleY, transX, transY);
        animator.setDuration(15000); // 15 seconds for a very slow, elegant movement
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setInterpolator(new LinearInterpolator());
        animator.start();
    }

    private void fetchStats() {
        // 1. Fetch Students & Defaulters from Users node
        mDatabase.child("Users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                
                long totalStudents = 0;
                long defaulters = 0;
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String role = userSnap.child("role").getValue(String.class);
                    if ("student".equals(role)) {
                        String status = userSnap.child("accountStatus").getValue(String.class);
                        if ("disabled".equals(status)) {
                            defaulters++;
                        } else {
                            totalStudents++;
                        }
                    }
                }
                tvTotalStudents.setText(String.valueOf(totalStudents));
                tvDefaulters.setText(String.valueOf(defaulters));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Fetch Faculty from Faculty node
        mDatabase.child("Faculty").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                int faculty = 0;
                int lecturers = 0;
                int professors = 0;

                for (DataSnapshot facultySnapshot : snapshot.getChildren()) {
                    faculty++;
                    String post = facultySnapshot.child("post").getValue(String.class);
                    if (post == null) post = facultySnapshot.child("position").getValue(String.class);
                    
                    if (post != null) {
                        post = post.trim();
                        if ("Lecturer".equalsIgnoreCase(post)) {
                            lecturers++;
                        } else if ("Professor".equalsIgnoreCase(post)) {
                            professors++;
                        }
                    }
                }
                tvTotalStaff.setText(String.valueOf(faculty));
                tvLecturers.setText(String.valueOf(lecturers));
                tvProfessors.setText(String.valueOf(professors));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Error loading faculty: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        mDatabase.child("Societies").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                
                int active = 0;
                int pending = 0;
                for (DataSnapshot societySnapshot : snapshot.getChildren()) {
                    String status = societySnapshot.child("status").getValue(String.class);
                    if ("approved".equals(status)) {
                        active++;
                    } else if ("pending".equals(status)) {
                        pending++;
                    }
                }
                tvTotalSocieties.setText(String.valueOf(active));
                tvPendingSocieties.setText(String.valueOf(pending));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Error loading societies: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showAnnouncementDialog() {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_admin_announcement, null);
        com.google.android.material.textfield.TextInputEditText etTitle = dialogView.findViewById(R.id.etTitle);
        com.google.android.material.textfield.TextInputEditText etContent = dialogView.findViewById(R.id.etContent);
        android.widget.RadioGroup rgTarget = dialogView.findViewById(R.id.rgTarget);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
        }

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btnPost).setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String content = etContent.getText().toString().trim();
            String target = "all";

            int checkedId = rgTarget.getCheckedRadioButtonId();
            if (checkedId == R.id.rbStudents) target = "students";
            else if (checkedId == R.id.rbFaculty) target = "faculty";

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            String id = mDatabase.child("Announcements").push().getKey();
            java.util.Map<String, Object> announcement = new java.util.HashMap<>();
            announcement.put("id", id);
            announcement.put("title", title);
            announcement.put("content", content);
            announcement.put("target", target);
            announcement.put("postedBy", "Admin");
            announcement.put("timestamp", System.currentTimeMillis());

            if (id != null) {
                mDatabase.child("Announcements").child(id).setValue(announcement)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Announcement Posted", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });

        dialog.show();
    }
}
