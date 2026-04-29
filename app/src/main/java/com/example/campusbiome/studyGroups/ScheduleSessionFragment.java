package com.example.campusbiome.studyGroups;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.campusbiome.R;
import com.example.campusbiome.studyGroups.models.StudySession;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.Locale;

public class ScheduleSessionFragment extends Fragment {

    // ── Argument keys ────────────────────────────────────────────────────────
    public static final String ARG_GROUP_ID   = "groupId";
    public static final String ARG_GROUP_NAME = "groupName";

    // ── Views ────────────────────────────────────────────────────────────────
    private TextInputEditText etTitle, etDate, etTime, etLocation;
    private MaterialButton    btnSchedule;

    // ── Data ─────────────────────────────────────────────────────────────────
    private String groupId;
    private String groupName;

    // ── Factory method ───────────────────────────────────────────────────────
    public static ScheduleSessionFragment newInstance(String groupId, String groupName) {
        ScheduleSessionFragment f = new ScheduleSessionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_GROUP_ID, groupId);
        args.putString(ARG_GROUP_NAME, groupName);
        f.setArguments(args);
        return f;
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_schedule_session, container, false);

        if (getArguments() != null) {
            groupId   = getArguments().getString(ARG_GROUP_ID);
            groupName = getArguments().getString(ARG_GROUP_NAME);
        }

        // Toolbar
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // Group name banner
        android.widget.TextView txtForGroup = view.findViewById(R.id.txtSessionForGroup);
        if (txtForGroup != null && groupName != null) {
            txtForGroup.setText("Scheduling for: " + groupName);
        }

        // Wire fields
        etTitle    = view.findViewById(R.id.etSessionTitle);
        etDate     = view.findViewById(R.id.etDate);
        etTime     = view.findViewById(R.id.etTime);
        etLocation = view.findViewById(R.id.etLocation);
        btnSchedule = view.findViewById(R.id.btnSchedule);

        // Date picker — opens when field is clicked
        etDate.setOnClickListener(v -> showDatePicker());

        // Also open picker when the end icon (calendar icon) is tapped
        View dateLayout = view.findViewById(R.id.etDate);
        if (dateLayout.getParent() instanceof com.google.android.material.textfield.TextInputLayout) {
            ((com.google.android.material.textfield.TextInputLayout) dateLayout.getParent())
                    .setEndIconOnClickListener(v -> showDatePicker());
        }

        // Time picker
        etTime.setOnClickListener(v -> showTimePicker());

        btnSchedule.setOnClickListener(v -> attemptSchedule());

        return view;
    }

    // ── Date Picker ──────────────────────────────────────────────────────────
    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    // Format: "15 Jan 2025"
                    String[] months = {"Jan","Feb","Mar","Apr","May","Jun",
                            "Jul","Aug","Sep","Oct","Nov","Dec"};
                    String formatted = String.format(Locale.getDefault(),
                            "%02d %s %d", dayOfMonth, months[month], year);
                    etDate.setText(formatted);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    // ── Time Picker ──────────────────────────────────────────────────────────
    private void showTimePicker() {
        Calendar cal = Calendar.getInstance();
        new TimePickerDialog(requireContext(),
                (view, hourOfDay, minute) -> {
                    // Format: "02:30 PM"
                    String amPm = hourOfDay < 12 ? "AM" : "PM";
                    int hour12  = hourOfDay % 12;
                    if (hour12 == 0) hour12 = 12;
                    String formatted = String.format(Locale.getDefault(),
                            "%02d:%02d %s", hour12, minute, amPm);
                    etTime.setText(formatted);
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                false
        ).show();
    }

    // ── Validation & Submit ──────────────────────────────────────────────────
    private void attemptSchedule() {
        String title    = text(etTitle);
        String date     = text(etDate);
        String time     = text(etTime);
        String location = text(etLocation);

        if (title.isEmpty()) {
            etTitle.setError("Session title is required");
            etTitle.requestFocus();
            return;
        }
        if (date.isEmpty()) {
            etDate.setError("Please select a date");
            etDate.requestFocus();
            return;
        }
        if (time.isEmpty()) {
            etTime.setError("Please select a time");
            etTime.requestFocus();
            return;
        }
        if (location.isEmpty()) {
            etLocation.setError("Location is required");
            etLocation.requestFocus();
            return;
        }

        saveSession(title, date, time, location);
    }

    private void saveSession(String title, String date, String time, String location) {
        // Sessions stored at: StudySessions/{groupId}/{sessionId}
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("StudySessions").child(groupId);

        String sessionId = ref.push().getKey();
        if (sessionId == null) {
            Toast.makeText(getContext(), "Failed to create session ID", Toast.LENGTH_SHORT).show();
            return;
        }

        StudySession session = new StudySession(title, date, time, location, groupId);

        btnSchedule.setEnabled(false);
        btnSchedule.setText("Scheduling…");

        ref.child(sessionId).setValue(session)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), "Session scheduled!", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    btnSchedule.setEnabled(true);
                    btnSchedule.setText("Schedule Session");
                    Toast.makeText(getContext(),
                            "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String text(TextInputEditText field) {
        if (field == null || field.getText() == null) return "";
        return field.getText().toString().trim();
    }
}