package com.example.campusbiome.studyGroups;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.campusbiome.R;
import com.example.campusbiome.studyGroups.models.StudySession;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ScheduleSessionFragment extends Fragment {

    public static final String ARG_GROUP_ID   = "groupId";
    public static final String ARG_GROUP_NAME = "groupName";

    private TextInputEditText etTitle, etDate, etTime, etLocation;
    private TextInputLayout layoutTitle, layoutDate, layoutTime, layoutLocation;
    private MaterialButton btnSchedule;

    private String groupId;
    private String groupName;

    public static ScheduleSessionFragment newInstance(String groupId, String groupName) {
        ScheduleSessionFragment f = new ScheduleSessionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_GROUP_ID, groupId);
        args.putString(ARG_GROUP_NAME, groupName);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule_session, container, false);

        if (getArguments() != null) {
            groupId = getArguments().getString(ARG_GROUP_ID);
            groupName = getArguments().getString(ARG_GROUP_NAME);
        }

        initViews(view);
        setupListeners();

        return view;
    }

    private void initViews(View view) {
        // Toolbar
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        // Banner
        TextView txtForGroup = view.findViewById(R.id.txtSessionForGroup);
        if (groupName != null) txtForGroup.setText("Scheduling for: " + groupName);

        // Layouts
        layoutTitle = view.findViewById(R.id.layoutTitle);
        layoutDate = view.findViewById(R.id.layoutDate);
        layoutTime = view.findViewById(R.id.layoutTime);
        layoutLocation = view.findViewById(R.id.layoutLocation);

        // EditTexts
        etTitle = view.findViewById(R.id.etSessionTitle);
        etDate = view.findViewById(R.id.etDate);
        etTime = view.findViewById(R.id.etTime);
        etLocation = view.findViewById(R.id.etLocation);

        btnSchedule = view.findViewById(R.id.btnSchedule);
    }

    private void setupListeners() {
        // Show picker on click
        etDate.setOnClickListener(v -> showDatePicker());
        etTime.setOnClickListener(v -> showTimePicker());

        // Show picker on icon click (Targeting the TextInputLayouts)
        layoutDate.setEndIconOnClickListener(v -> showDatePicker());
        layoutTime.setEndIconOnClickListener(v -> showTimePicker());

        btnSchedule.setOnClickListener(v -> attemptSchedule());
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {

            // Format: 12 May 2026
            Calendar selectedCal = Calendar.getInstance();
            selectedCal.set(year, month, dayOfMonth);

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            etDate.setText(sdf.format(selectedCal.getTime()));

            // Clear error if set
            layoutDate.setError(null);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialog.show();
    }

    private void showTimePicker() {
        Calendar cal = Calendar.getInstance();
        new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {

            // University hours check: 08:30 - 18:30
            int totalMinutes = (hourOfDay * 60) + minute;
            int startLimit = (8 * 60) + 30;
            int endLimit = (18 * 60) + 30;

            if (totalMinutes < startLimit || totalMinutes > endLimit) {
                Toast.makeText(getContext(), "Select time between 8:30 AM and 6:30 PM", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if selected time is in the past (only if date is today)
            if (isDateToday(text(etDate))) {
                Calendar selectedTime = Calendar.getInstance();
                selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedTime.set(Calendar.MINUTE, minute);

                if (selectedTime.before(Calendar.getInstance())) {
                    Toast.makeText(getContext(), "Cannot select past time", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Format: 02:30 PM
            String amPm = (hourOfDay < 12) ? "AM" : "PM";
            int hour12 = (hourOfDay % 12 == 0) ? 12 : hourOfDay % 12;
            String formatted = String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, amPm);

            etTime.setText(formatted);
            layoutTime.setError(null);

        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
    }

    private boolean isDateToday(String dateStr) {
        if (dateStr.isEmpty()) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            Date pickedDate = sdf.parse(dateStr);
            Calendar pickedCal = Calendar.getInstance();
            pickedCal.setTime(pickedDate);
            Calendar now = Calendar.getInstance();
            return pickedCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    pickedCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);
        } catch (ParseException e) {
            return false;
        }
    }

    private void attemptSchedule() {
        String title = text(etTitle);
        String date = text(etDate);
        String time = text(etTime);
        String location = text(etLocation);

        layoutTitle.setError(null);
        layoutDate.setError(null);
        layoutTime.setError(null);
        layoutLocation.setError(null);

        if (title.isEmpty()) { layoutTitle.setError("Title required"); return; }
        if (date.isEmpty()) { layoutDate.setError("Select date"); return; }
        if (time.isEmpty()) { layoutTime.setError("Select time"); return; }
        if (location.isEmpty()) { layoutLocation.setError("Location required"); return; }

        saveSession(title, date, time, location);
    }

    private void saveSession(String title, String date, String time, String location) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("StudySessions").child(groupId);
        String sessionId = ref.push().getKey();

        if (sessionId == null) return;

        StudySession session = new StudySession(title, date, time, location, groupId);

        btnSchedule.setEnabled(false);
        btnSchedule.setText("Scheduling...");

        ref.child(sessionId).setValue(session)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), "Session scheduled!", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    btnSchedule.setEnabled(true);
                    btnSchedule.setText("Schedule Session");
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String text(TextInputEditText field) {
        return (field != null && field.getText() != null) ? field.getText().toString().trim() : "";
    }
}