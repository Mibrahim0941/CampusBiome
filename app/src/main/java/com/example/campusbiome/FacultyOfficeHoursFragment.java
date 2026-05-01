package com.example.campusbiome;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import java.util.Arrays;
import java.util.List;

public class FacultyOfficeHoursFragment extends Fragment {

    private RecyclerView rvOfficeHours;
    private MaterialButton btnAddTiming;
    private FacultyOfficeHourAdapter adapter;
    private DatabaseReference dbRef;
    private String currentUid;
    
    private List<String> currentDays = new ArrayList<>();
    private String currentOfficeHours = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_faculty_office_hours, container, false);

        rvOfficeHours = view.findViewById(R.id.rvOfficeHours);
        btnAddTiming = view.findViewById(R.id.btnAddTiming);
        
        rvOfficeHours.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new FacultyOfficeHourAdapter(currentDays, currentOfficeHours, new FacultyOfficeHourAdapter.OnOfficeHourActionListener() {
            @Override
            public void onEdit(String day) {
                showEditDialog();
            }

            @Override
            public void onDelete(String day) {
                removeDay(day);
            }

            @Override
            public void onAdd(String day) {
                showEditDialog();
            }
        });
        rvOfficeHours.setAdapter(adapter);

        btnAddTiming.setOnClickListener(v -> showEditDialog());

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            // Fetching from fac1 explicitly so you can see the data as requested.
            // When real auth links are done, change "fac1" back to currentUid.
            dbRef = FirebaseDatabase.getInstance().getReference("Faculty").child("fac1");
            fetchOfficeHours();
        }

        return view;
    }

    private void fetchOfficeHours() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String daysStr = snapshot.child("days").getValue(String.class);
                    String hoursStr = snapshot.child("officeHours").getValue(String.class);
                    
                    if (daysStr != null) {
                        currentDays = new ArrayList<>(Arrays.asList(daysStr.split("\\s*,\\s*")));
                    } else {
                        currentDays = new ArrayList<>();
                    }
                    
                    if (hoursStr != null) {
                        currentOfficeHours = hoursStr;
                    } else {
                        currentOfficeHours = "";
                    }
                    
                    adapter.updateData(currentDays, currentOfficeHours);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FacultyOfficeHours", "Failed to fetch data", error.toException());
            }
        });
    }

    private void removeDay(String dayToRemove) {
        currentDays.removeIf(d -> d.trim().equalsIgnoreCase(dayToRemove));
        updateFirebase();
    }

    private void addDay(String dayToAdd) {
        boolean exists = false;
        for (String d : currentDays) {
            if (d.trim().equalsIgnoreCase(dayToAdd)) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            currentDays.add(dayToAdd);
            updateFirebase();
        }
    }

    private void showEditDialog() {
        if (getContext() == null) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Office Hours");

        View view = LayoutInflater.from(getContext()).inflate(R.layout.activity_generic_login, null); 
        // Re-using a simple layout is tricky. Let's create a programmatic layout instead.
        
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        
        final EditText etDays = new EditText(getContext());
        etDays.setHint("Days (e.g. Monday, Wednesday)");
        etDays.setText(TextUtils.join(", ", currentDays));
        layout.addView(etDays);
        
        final EditText etHours = new EditText(getContext());
        etHours.setHint("Hours (e.g. 2:00 PM - 4:00 PM)");
        etHours.setText(currentOfficeHours);
        layout.addView(etHours);
        
        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newDays = etDays.getText().toString();
            String newHours = etHours.getText().toString();
            
            currentDays = new ArrayList<>(Arrays.asList(newDays.split("\\s*,\\s*")));
            currentOfficeHours = newHours;
            updateFirebase();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateFirebase() {
        String daysStr = TextUtils.join(", ", currentDays);
        dbRef.child("days").setValue(daysStr);
        dbRef.child("officeHours").setValue(currentOfficeHours)
            .addOnSuccessListener(aVoid -> {
                if (getContext() != null) Toast.makeText(getContext(), "Office hours updated", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                if (getContext() != null) Toast.makeText(getContext(), "Update failed", Toast.LENGTH_SHORT).show();
            });
    }
}
