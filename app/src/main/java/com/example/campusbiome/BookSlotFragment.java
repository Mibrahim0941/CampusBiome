package com.example.campusbiome;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BookSlotFragment extends Fragment {

    private String facultyId;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    private TextView tvName, tvPost, tvDept, tvAvailability, tvSelectedDayHours, tvAvailableDaysText;
    private ImageView btnBack;
    private TextInputEditText etSelectDate, etStartTime, etDuration, etReason;
    private MaterialButton btnConfirmBooking;

    private Map<String, String> officeHoursMap = new HashMap<>();
    private String selectedDateStr = null;
    private String selectedDayOfWeek = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book_slot, container, false);

        if (getArguments() != null) {
            facultyId = getArguments().getString("facultyId");
        }

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        tvName = view.findViewById(R.id.tvName);
        tvPost = view.findViewById(R.id.tvPost);
        tvDept = view.findViewById(R.id.tvDept);
        tvAvailability = view.findViewById(R.id.tvAvailability);
        tvSelectedDayHours = view.findViewById(R.id.tvSelectedDayHours);
        tvAvailableDaysText = view.findViewById(R.id.tvAvailableDaysText);
        btnBack = view.findViewById(R.id.btnBack);
        etSelectDate = view.findViewById(R.id.etSelectDate);
        etStartTime = view.findViewById(R.id.etStartTime);
        etDuration = view.findViewById(R.id.etDuration);
        etReason = view.findViewById(R.id.etReason);
        btnConfirmBooking = view.findViewById(R.id.btnConfirmBooking);

        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        btnConfirmBooking.setOnClickListener(v -> validateAndConfirmBooking());

        etSelectDate.setOnClickListener(v -> showDatePickerDialog());

        if (facultyId != null) {
            fetchProfessorDetails();
        }

        return view;
    }

    private void showDatePickerDialog() {
        if (getContext() == null || officeHoursMap.isEmpty()) {
            Toast.makeText(getContext(), "Office hours not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar calendar = Calendar.getInstance();
        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCal = Calendar.getInstance();
                    selectedCal.set(year, month, dayOfMonth);
                    
                    SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.US);
                    String dayOfWeek = dayFormat.format(selectedCal.getTime());
                    
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    String formattedDate = dateFormat.format(selectedCal.getTime());

                    // Check if selected day of week is in office hours map (case-insensitive check)
                    String matchedDay = null;
                    for (String availableDay : officeHoursMap.keySet()) {
                        if (availableDay.equalsIgnoreCase(dayOfWeek) || availableDay.toLowerCase().startsWith(dayOfWeek.toLowerCase().substring(0, 3))) {
                            matchedDay = availableDay;
                            break;
                        }
                    }

                    if (matchedDay != null) {
                        selectedDateStr = formattedDate;
                        selectedDayOfWeek = matchedDay;
                        etSelectDate.setText(formattedDate + " (" + dayOfWeek + ")");
                        tvSelectedDayHours.setVisibility(View.VISIBLE);
                        tvSelectedDayHours.setText("Available hours: " + officeHoursMap.get(matchedDay));
                    } else {
                        selectedDateStr = null;
                        selectedDayOfWeek = null;
                        etSelectDate.setText("");
                        tvSelectedDayHours.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Dr. " + tvName.getText() + " is not available on " + dayOfWeek + "s. Please select a different date.", Toast.LENGTH_LONG).show();
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        
        // Prevent picking past dates
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void fetchProfessorDetails() {
        mDatabase.child("Faculty").child(facultyId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || !snapshot.exists()) return;

                String name = getString(snapshot, "name");
                String post = getString(snapshot, "position", "post");
                String dept = getString(snapshot, "department");
                Boolean isAvailable = snapshot.child("Available").getValue(Boolean.class);

                tvName.setText(name);
                tvPost.setText(post);
                tvDept.setText("Dept. of " + dept);

                if (isAvailable != null && isAvailable) {
                    tvAvailability.setText("Available");
                    tvAvailability.setTextColor(Color.parseColor("#006B5E"));
                } else {
                    tvAvailability.setText("Busy");
                    tvAvailability.setTextColor(Color.parseColor("#D32F2F"));
                }

                DataSnapshot officeHoursSnapshot = snapshot.child("officeHours");
                if (officeHoursSnapshot.exists() && officeHoursSnapshot.hasChildren()) {
                    StringBuilder daysStr = new StringBuilder("Available Days: ");
                    int count = 0;
                    for (DataSnapshot daySnap : officeHoursSnapshot.getChildren()) {
                        String day = daySnap.getKey();
                        String timeRange = daySnap.getValue(String.class);
                        if (day != null && timeRange != null) {
                            officeHoursMap.put(day, timeRange);
                            if (count > 0) daysStr.append(", ");
                            daysStr.append(day);
                            count++;
                        }
                    }
                    tvAvailableDaysText.setText(daysStr.toString());
                } else {
                    tvAvailableDaysText.setText("No office hours specified.");
                    btnConfirmBooking.setEnabled(false);
                    btnConfirmBooking.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#8F9B99")));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) Toast.makeText(getContext(), "Failed to load details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void validateAndConfirmBooking() {
        if (officeHoursMap.isEmpty()) {
            Toast.makeText(getContext(), "Cannot book: No office hours available.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDateStr == null || selectedDayOfWeek == null) {
            Toast.makeText(getContext(), "Please select a valid date", Toast.LENGTH_SHORT).show();
            return;
        }

        String startTimeStr = etStartTime.getText().toString().trim();
        String durationStr = etDuration.getText().toString().trim();
        String reasonStr = etReason.getText().toString().trim();

        if (startTimeStr.isEmpty() || durationStr.isEmpty() || reasonStr.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int durationMins;
        try {
            durationMins = Integer.parseInt(durationStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Duration must be a valid number", Toast.LENGTH_SHORT).show();
            return;
        }

        String officeHoursRange = officeHoursMap.get(selectedDayOfWeek); // e.g. "11:00 AM - 01:30 PM"
        if (officeHoursRange == null || !officeHoursRange.contains("-")) {
            Toast.makeText(getContext(), "Invalid office hours format", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] parts = officeHoursRange.split("-");
        if (parts.length != 2) {
            Toast.makeText(getContext(), "Invalid office hours format", Toast.LENGTH_SHORT).show();
            return;
        }

        String ohStartStr = parts[0].trim();
        String ohEndStr = parts[1].trim();

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
        try {
            Date ohStartDate = sdf.parse(ohStartStr);
            Date ohEndDate = sdf.parse(ohEndStr);
            Date reqStartDate = sdf.parse(startTimeStr);

            if (ohStartDate == null || ohEndDate == null || reqStartDate == null) {
                throw new ParseException("Null date parsed", 0);
            }

            Calendar reqEndCal = Calendar.getInstance();
            reqEndCal.setTime(reqStartDate);
            reqEndCal.add(Calendar.MINUTE, durationMins);
            Date reqEndDate = reqEndCal.getTime();

            if (reqStartDate.before(ohStartDate) || reqEndDate.after(ohEndDate)) {
                Toast.makeText(getContext(), "Requested time is outside office hours", Toast.LENGTH_LONG).show();
                return;
            }

        } catch (ParseException e) {
            Toast.makeText(getContext(), "Invalid time format. Use hh:mm AM/PM", Toast.LENGTH_LONG).show();
            return;
        }

        showConfirmationDialog(startTimeStr, durationStr, reasonStr);
    }

    private void showConfirmationDialog(String startTimeStr, String durationStr, String reasonStr) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Booking Confirmation");
        builder.setMessage("Are you sure you want to book this appointment for " + selectedDateStr + "?");
        builder.setPositiveButton("Confirm", (dialog, which) -> processBooking(startTimeStr, durationStr, reasonStr));
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
        }
        dialog.show();
    }

    private void processBooking(String startTimeStr, String durationStr, String reasonStr) {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        mDatabase.child("Users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                FacultyAppointment.StudentInfo studentInfo = new FacultyAppointment.StudentInfo();
                studentInfo.setUid(uid);
                studentInfo.setName(snapshot.child("name").getValue(String.class));
                studentInfo.setEmail(snapshot.child("email").getValue(String.class));
                studentInfo.setProgram(snapshot.child("Program").getValue(String.class));
                studentInfo.setSection(snapshot.child("Section").getValue(String.class));
                Integer semester = snapshot.child("Semester").getValue(Integer.class);
                if (semester != null) {
                    studentInfo.setSemester(semester);
                }

                DatabaseReference appointmentsRef = mDatabase.child("FacultyAppointment");
                String pushId = appointmentsRef.push().getKey();
                if (pushId == null) return;

                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
                String createdAt = df.format(new Date());

                FacultyAppointment appointment = new FacultyAppointment(
                        pushId,
                        facultyId,
                        createdAt,
                        reasonStr,
                        "pending",
                        studentInfo,
                        selectedDateStr, // using the specific date here instead of just 'Monday'
                        startTimeStr,
                        durationStr + " mins"
                );

                appointmentsRef.child(pushId).setValue(appointment).addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Appointment Booked Successfully", Toast.LENGTH_SHORT).show();
                    if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                        getParentFragmentManager().popBackStack();
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to book appointment", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private String getString(DataSnapshot snapshot, String... keys) {
        for (String key : keys) {
            Object val = snapshot.child(key).getValue();
            if (val != null) return String.valueOf(val);
        }
        return "";
    }
}
