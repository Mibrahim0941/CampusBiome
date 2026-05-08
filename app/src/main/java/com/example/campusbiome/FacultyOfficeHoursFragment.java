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
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
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
    private ExtendedFloatingActionButton btnAddTiming;
    private SwitchMaterial switchAvailability;
    private TextView tvAvailabilityStatus;
    private FacultyOfficeHourAdapter adapter;
    private DatabaseReference dbRef;
    private String currentUid;
    
    private List<String> currentDays = new ArrayList<>();
    private java.util.Map<String, String> officeHoursMap = new java.util.HashMap<>();
    private final String[] ALL_DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_faculty_office_hours, container, false);

        rvOfficeHours = view.findViewById(R.id.rvOfficeHours);
        btnAddTiming = view.findViewById(R.id.btnAddTiming);
        switchAvailability = view.findViewById(R.id.switchAvailability);
        tvAvailabilityStatus = view.findViewById(R.id.tvAvailabilityStatus);
        
        rvOfficeHours.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new FacultyOfficeHourAdapter(currentDays, officeHoursMap, new FacultyOfficeHourAdapter.OnOfficeHourActionListener() {
            @Override
            public void onEdit(String day, String currentTime) {
                showEditTimingDialog(day, currentTime);
            }

            @Override
            public void onDelete(String day) {
                removeDay(day);
            }
        });
        rvOfficeHours.setAdapter(adapter);

        btnAddTiming.setOnClickListener(v -> showAddTimingDialog());

        switchAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateAvailability(isChecked);
        });

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            // Using "fac1" as per the data provided in the request
            dbRef = FirebaseDatabase.getInstance().getReference("Faculty").child("fac1");
            fetchData();
        }

        return view;
    }

    private void fetchData() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Update Availability
                    Boolean isAvailable = snapshot.child("Available").getValue(Boolean.class);
                    if (isAvailable != null) {
                        switchAvailability.setChecked(isAvailable);
                        tvAvailabilityStatus.setText("Currently: " + (isAvailable ? "Available" : "Unavailable"));
                        tvAvailabilityStatus.setTextColor(isAvailable ? 
                            getResources().getColor(R.color.success, null) : 
                            getResources().getColor(R.color.error, null));
                    }

                    // Update Office Hours
                    officeHoursMap.clear();
                    currentDays.clear();
                    DataSnapshot ohSnapshot = snapshot.child("officeHours");
                    for (DataSnapshot daySnap : ohSnapshot.getChildren()) {
                        String day = daySnap.getKey();
                        String time = daySnap.getValue(String.class);
                        if (day != null && time != null) {
                            officeHoursMap.put(day, time);
                            currentDays.add(day);
                        }
                    }
                    
                    // Sort days for consistent display
                    java.util.Collections.sort(currentDays, (d1, d2) -> {
                        List<String> daysOrder = Arrays.asList(ALL_DAYS);
                        return Integer.compare(daysOrder.indexOf(d1), daysOrder.indexOf(d2));
                    });

                    adapter.updateData(currentDays, officeHoursMap);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FacultyOfficeHours", "Failed to fetch data", error.toException());
            }
        });
    }

    private void updateAvailability(boolean isAvailable) {
        dbRef.child("Available").setValue(isAvailable);
    }

    private void showAddTimingDialog() {
        if (getContext() == null) return;

        List<String> availableDays = new ArrayList<>();
        for (String day : ALL_DAYS) {
            if (!officeHoursMap.containsKey(day)) {
                availableDays.add(day);
            }
        }
        if (availableDays.isEmpty()) {
            Toast.makeText(getContext(), "All days already have timings assigned", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_faculty_timing, null);
        
        android.widget.AutoCompleteTextView autoCompleteDay = dialogView.findViewById(R.id.autoCompleteDay);
        EditText etStartTime = dialogView.findViewById(R.id.etStartTime);
        EditText etEndTime = dialogView.findViewById(R.id.etEndTime);

        android.widget.ArrayAdapter<String> dayAdapter = new android.widget.ArrayAdapter<>(
                getContext(), android.R.layout.simple_dropdown_item_1line, availableDays);
        autoCompleteDay.setAdapter(dayAdapter);

        etStartTime.setOnClickListener(v -> showTimePickerDialog(etStartTime));
        etEndTime.setOnClickListener(v -> showTimePickerDialog(etEndTime));

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add New Timing");
        builder.setView(dialogView);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String selectedDay = autoCompleteDay.getText().toString();
            String startTime = etStartTime.getText().toString();
            String endTime = etEndTime.getText().toString();

            if (TextUtils.isEmpty(selectedDay) || TextUtils.isEmpty(startTime) || TextUtils.isEmpty(endTime)) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            String fullTime = startTime + " - " + endTime;
            dbRef.child("officeHours").child(selectedDay).setValue(fullTime);
        });
        builder.setNegativeButton("Cancel", null);
        
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
        }
        dialog.show();
    }

    private void showEditTimingDialog(String day, String currentTime) {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_faculty_timing, null);
        
        com.google.android.material.textfield.TextInputLayout tilDay = dialogView.findViewById(R.id.tilDay);
        android.widget.AutoCompleteTextView autoCompleteDay = dialogView.findViewById(R.id.autoCompleteDay);
        EditText etStartTime = dialogView.findViewById(R.id.etStartTime);
        EditText etEndTime = dialogView.findViewById(R.id.etEndTime);

        // Hide day selection or disable it for editing
        autoCompleteDay.setText(day);
        tilDay.setEnabled(false);

        String initialStart = "";
        String initialEnd = "";
        if (currentTime != null && currentTime.contains(" - ")) {
            String[] parts = currentTime.split(" - ");
            initialStart = parts[0];
            initialEnd = parts[1];
        }

        etStartTime.setText(initialStart);
        etEndTime.setText(initialEnd);
        
        etStartTime.setOnClickListener(v -> showTimePickerDialog(etStartTime));
        etEndTime.setOnClickListener(v -> showTimePickerDialog(etEndTime));

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Timing for " + day);
        builder.setView(dialogView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String startTime = etStartTime.getText().toString();
            String endTime = etEndTime.getText().toString();

            if (TextUtils.isEmpty(startTime) || TextUtils.isEmpty(endTime)) {
                Toast.makeText(getContext(), "Times cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            String fullTime = startTime + " - " + endTime;
            dbRef.child("officeHours").child(day).setValue(fullTime);
        });
        builder.setNegativeButton("Cancel", null);
        
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
        }
        dialog.show();
    }

    private void showTimePickerDialog(EditText editText) {
        java.util.Calendar mcurrentTime = java.util.Calendar.getInstance();
        int hour = mcurrentTime.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = mcurrentTime.get(java.util.Calendar.MINUTE);
        android.app.TimePickerDialog mTimePicker;
        mTimePicker = new android.app.TimePickerDialog(getContext(), (timePicker, selectedHour, selectedMinute) -> {
            String ampm = (selectedHour < 12) ? "AM" : "PM";
            int displayHour = (selectedHour > 12) ? selectedHour - 12 : (selectedHour == 0 ? 12 : selectedHour);
            editText.setText(String.format("%02d:%02d %s", displayHour, selectedMinute, ampm));
        }, hour, minute, false);
        mTimePicker.setTitle("Select Time");
        mTimePicker.show();
    }

    private void removeDay(String day) {
        dbRef.child("officeHours").child(day).removeValue();
    }
}
