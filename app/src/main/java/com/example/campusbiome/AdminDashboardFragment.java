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

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminDashboardFragment extends Fragment {

    private TextView tvTotalStudents, tvDefaulters, tvOffenders;
    private TextView tvTotalStaff, tvVisiting, tvLabInstructors;
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
        tvOffenders = view.findViewById(R.id.tvOffenders);
        
        tvTotalStaff = view.findViewById(R.id.tvTotalStaff);
        tvVisiting = view.findViewById(R.id.tvVisiting);
        tvLabInstructors = view.findViewById(R.id.tvLabInstructors);
        
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

        btnNewAnnouncement.setOnClickListener(v -> Toast.makeText(getContext(), "New Announcement Feature", Toast.LENGTH_SHORT).show());
        btnManageEvents.setOnClickListener(v -> Toast.makeText(getContext(), "Manage Events Feature", Toast.LENGTH_SHORT).show());
        btnSeeCampusMap.setOnClickListener(v -> {
             Toast.makeText(getContext(), "See Campus Map Feature", Toast.LENGTH_SHORT).show();
        });

        fetchStats();

        return view;
    }

    private void fetchStats() {
        mDatabase.child("Users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                
                int students = 0;
                int defaulters = 0;
                int offenders = 0;
                int faculty = 0;
                int visiting = 0;
                int labInstructors = 0;

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String role = userSnapshot.child("role").getValue(String.class);
                    
                    if ("student".equals(role)) {
                        students++;
                        
                        Boolean isDefaulter = userSnapshot.child("isDefaulter").getValue(Boolean.class);
                        if (isDefaulter != null && isDefaulter) defaulters++;
                        
                        Boolean isOffender = userSnapshot.child("isOffender").getValue(Boolean.class);
                        if (isOffender != null && isOffender) offenders++;
                        
                    } else if ("faculty".equals(role)) {
                        faculty++;
                        
                        String post = userSnapshot.child("post").getValue(String.class);
                        if ("Visiting".equalsIgnoreCase(post)) {
                            visiting++;
                        } else if ("Lab Instructor".equalsIgnoreCase(post)) {
                            labInstructors++;
                        }
                    }
                }
                
                tvTotalStudents.setText(String.valueOf(students));
                tvDefaulters.setText(String.valueOf(defaulters));
                tvOffenders.setText(String.valueOf(offenders));
                
                tvTotalStaff.setText(String.valueOf(faculty));
                tvVisiting.setText(String.valueOf(visiting));
                tvLabInstructors.setText(String.valueOf(labInstructors));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
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
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
