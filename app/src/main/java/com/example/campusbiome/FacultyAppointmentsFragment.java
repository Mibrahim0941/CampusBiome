package com.example.campusbiome;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        // you can change this back to: dbRef.orderByChild("facultyId").equalTo(currentUid).addValueEventListener(...)
        dbRef.addValueEventListener(new ValueEventListener() {
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

    private void updateAppointmentStatus(FacultyAppointment appointment, String status) {
        if (appointment.getId() != null) {
            dbRef.child(appointment.getId()).child("status").setValue(status)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Appointment " + status, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update", Toast.LENGTH_SHORT).show());
        }
    }
}
