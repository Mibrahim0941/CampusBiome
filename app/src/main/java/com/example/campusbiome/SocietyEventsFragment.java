package com.example.campusbiome;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SocietyEventsFragment extends Fragment {

    private RecyclerView         rvEvents;
    private FloatingActionButton btnAddEvent;

    private final List<SocietyEvent> eventList   = new ArrayList<>();
    private final List<String>       eventIds    = new ArrayList<>();
    private SocietyEventAdapter adapter;

    private final List<SocietyEvent> pendingList = new ArrayList<>();
    private final List<String>       pendingIds  = new ArrayList<>();
    private SocietyEventAdapter pendingAdapter;

    private RecyclerView rvPendingEvents;
    private View         pendingSection;

    private DatabaseReference dbRef;
    private String societyId;

    // Month name → Calendar int (handles both "JAN" and "January" styles)
    private static int monthToInt(String month) {
        if (month == null || month.length() < 3) return -1;
        switch (month.trim().toUpperCase().substring(0, 3)) {
            case "JAN": return Calendar.JANUARY;
            case "FEB": return Calendar.FEBRUARY;
            case "MAR": return Calendar.MARCH;
            case "APR": return Calendar.APRIL;
            case "MAY": return Calendar.MAY;
            case "JUN": return Calendar.JUNE;
            case "JUL": return Calendar.JULY;
            case "AUG": return Calendar.AUGUST;
            case "SEP": return Calendar.SEPTEMBER;
            case "OCT": return Calendar.OCTOBER;
            case "NOV": return Calendar.NOVEMBER;
            case "DEC": return Calendar.DECEMBER;
            default:    return -1;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_society_events, container, false);

        rvEvents        = view.findViewById(R.id.rvEvents);
        btnAddEvent     = view.findViewById(R.id.btnAddEvent);
        rvPendingEvents = view.findViewById(R.id.rvPendingEvents);
        pendingSection  = view.findViewById(R.id.pendingSection);

        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));

        if (getArguments() != null) {
            societyId = getArguments().getString("societyId");
        }

        if (societyId == null) {
            Toast.makeText(getContext(), "Society not found", Toast.LENGTH_SHORT).show();
            return view;
        }

        adapter = new SocietyEventAdapter(eventList, eventIds, this::openRegistrations);
        rvEvents.setAdapter(adapter);

        if (rvPendingEvents != null) {
            rvPendingEvents.setLayoutManager(new LinearLayoutManager(getContext()));
            pendingAdapter = new SocietyEventAdapter(pendingList, pendingIds, null);
            rvPendingEvents.setAdapter(pendingAdapter);
        }

        dbRef = FirebaseDatabase.getInstance()
                .getReference("Societies")
                .child(societyId)
                .child("events");

        loadEvents();
        btnAddEvent.setOnClickListener(v -> showAddEventDialog());

        return view;
    }

    // ── Load & split by status ────────────────────────────────────────────────
    private void loadEvents() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                eventList.clear();   eventIds.clear();
                pendingList.clear(); pendingIds.clear();

                for (DataSnapshot snap : snapshot.getChildren()) {
                    SocietyEvent event = snap.getValue(SocietyEvent.class);
                    if (event == null) continue;

                    event.setId(snap.getKey());
                    String status = event.getStatus();

                    if ("approved".equalsIgnoreCase(status)) {
                        eventList.add(0, event);
                        eventIds.add(0, snap.getKey());
                    } else if ("pending".equalsIgnoreCase(status) || status == null) {
                        pendingList.add(0, event);
                        pendingIds.add(0, snap.getKey());
                    }
                    // "rejected" silently ignored
                }

                adapter.notifyDataSetChanged();

                if (pendingSection != null)
                    pendingSection.setVisibility(pendingList.isEmpty() ? View.GONE : View.VISIBLE);
                if (pendingAdapter != null)
                    pendingAdapter.notifyDataSetChanged();
            }

            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null && com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
                    Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // ── Open registrations ────────────────────────────────────────────────────
    private void openRegistrations(SocietyEvent event, String eventId) {
        EventRegistrationsFragment frag =
                EventRegistrationsFragment.newInstance(societyId, eventId, event.getTitle());
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, frag)
                .addToBackStack(null)
                .commit();
    }

    // ── Add event dialog ──────────────────────────────────────────────────────
    private void showAddEventDialog() {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_add_event, null);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        EditText etTitle       = dialogView.findViewById(R.id.etTitle);
        EditText etDescription = dialogView.findViewById(R.id.etDescription);
        EditText etTime        = dialogView.findViewById(R.id.etTime);
        EditText etVenue       = dialogView.findViewById(R.id.etVenue);
        Spinner  spDay         = dialogView.findViewById(R.id.spDay);
        Spinner  spMonth       = dialogView.findViewById(R.id.spMonth);
        Spinner  spYear        = dialogView.findViewById(R.id.spYear);
        Spinner  spAmPm        = dialogView.findViewById(R.id.spAmPm);
        Button   btnCancel     = dialogView.findViewById(R.id.btnCancel);
        Button   btnPublish    = dialogView.findViewById(R.id.btnPublish);

        setupSpinners(spDay, spMonth, spYear, spAmPm);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnPublish.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String desc  = etDescription.getText().toString().trim();
            String day   = spDay.getSelectedItem().toString();
            String month = spMonth.getSelectedItem().toString();
            String year  = spYear.getSelectedItem().toString();
            String time  = etTime.getText().toString().trim();
            String ampm  = spAmPm.getSelectedItem().toString();
            String venue = etVenue.getText().toString().trim();

            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(desc)) {
                Toast.makeText(getContext(), "Fill required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // ── Date validation: event must not be in the past ────────────────
            if (!isEventDateValid(day, month, year, time, ampm)) {
                Toast.makeText(getContext(),
                        "Event date/time cannot be in the past. Please select a future date.",
                        Toast.LENGTH_LONG).show();
                return;   // ← stop here; dialog stays open so user can fix it
            }

            addEvent(title, desc, day, month, year, time, ampm, venue);
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Returns true if the selected date+time is today or in the future.
     * If time is blank we compare date-only (day granularity).
     */
    private boolean isEventDateValid(String day, String month, String year,
                                     String time, String ampm) {
        try {
            int dayInt  = Integer.parseInt(day.trim());
            int yearInt = Integer.parseInt(year.trim());
            int monInt  = monthToInt(month);
            if (monInt == -1) return true; // can't parse → allow through

            // Build event Calendar
            Calendar eventCal = Calendar.getInstance();
            eventCal.set(yearInt, monInt, dayInt, 0, 0, 0);
            eventCal.set(Calendar.MILLISECOND, 0);

            // If time was provided, also set hours and minutes
            if (!time.isEmpty()) {
                String[] parts = time.split(":");
                if (parts.length == 2) {
                    int hour   = Integer.parseInt(parts[0].trim());
                    int minute = Integer.parseInt(parts[1].trim());

                    // Convert 12-hour → 24-hour
                    if ("PM".equalsIgnoreCase(ampm) && hour != 12) hour += 12;
                    if ("AM".equalsIgnoreCase(ampm) && hour == 12) hour  = 0;

                    eventCal.set(Calendar.HOUR_OF_DAY, hour);
                    eventCal.set(Calendar.MINUTE, minute);
                }
            }

            // Today at midnight for date-only comparison (when no time given)
            Calendar now = Calendar.getInstance();

            // Event must be >= now
            return !eventCal.before(now);

        } catch (NumberFormatException e) {
            return true; // can't parse → allow through
        }
    }

    // ── Spinners ──────────────────────────────────────────────────────────────
    private void setupSpinners(Spinner spDay, Spinner spMonth,
                               Spinner spYear, Spinner spAmPm) {
        List<String> days = new ArrayList<>();
        for (int i = 1; i <= 31; i++) days.add(String.valueOf(i));

        List<String> months = Arrays.asList(
                "JAN","FEB","MAR","APR","MAY","JUN",
                "JUL","AUG","SEP","OCT","NOV","DEC");
        List<String> years = Arrays.asList("2026","2027");
        List<String> ampm  = Arrays.asList("AM","PM");

        spDay.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, days));
        spMonth.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, months));
        spYear.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, years));
        spAmPm.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, ampm));
    }

    // ── Write to Firebase ─────────────────────────────────────────────────────
    private void addEvent(String title, String desc, String day, String month,
                          String year, String time, String ampm, String venue) {

        String id = dbRef.push().getKey();
        if (id == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "unknown";
        String createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(new Date());

        SocietyEvent event = new SocietyEvent(day, month, year, title, desc, time, ampm, venue);
        event.setCreatedBy(uid);
        event.setCreatedAt(createdAt);
        // status = "pending" set automatically by constructor

        dbRef.child(id).setValue(event)
                .addOnSuccessListener(unused ->
                        Toast.makeText(getContext(),
                                "Event submitted for approval! ⏳",
                                Toast.LENGTH_LONG).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}