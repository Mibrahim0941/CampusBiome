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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SocietyEventsFragment extends Fragment {

    private RecyclerView           rvEvents;
    private FloatingActionButton   btnAddEvent;

    private final List<SocietyEvent> eventList = new ArrayList<>();
    private final List<String>       eventIds  = new ArrayList<>(); // parallel keys list
    private SocietyEventAdapter adapter;

    private DatabaseReference dbRef;
    private String societyId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_society_events, container, false);

        rvEvents    = view.findViewById(R.id.rvEvents);
        btnAddEvent = view.findViewById(R.id.btnAddEvent);

        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));

        if (getArguments() != null) {
            societyId = getArguments().getString("societyId");
        }

        if (societyId == null) {
            Toast.makeText(getContext(), "Society not found", Toast.LENGTH_SHORT).show();
            return view;
        }

        // Pass eventIds and listener to adapter
        adapter = new SocietyEventAdapter(eventList, eventIds, this::openRegistrations);
        rvEvents.setAdapter(adapter);

        dbRef = FirebaseDatabase.getInstance()
                .getReference("Societies")
                .child(societyId)
                .child("events");

        loadEvents();

        btnAddEvent.setOnClickListener(v -> showAddEventDialog());

        return view;
    }

    private void loadEvents() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                eventList.clear();
                eventIds.clear();

                for (DataSnapshot snap : snapshot.getChildren()) {
                    SocietyEvent event = snap.getValue(SocietyEvent.class);
                    if (event != null) {
                        event.setId(snap.getKey());
                        eventList.add(0, event);    // newest first
                        eventIds.add(0, snap.getKey());
                    }
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Open registrations list for this event ────────────────────────────────
    private void openRegistrations(SocietyEvent event, String eventId) {
        EventRegistrationsFragment frag =
                EventRegistrationsFragment.newInstance(societyId, eventId, event.getTitle());
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, frag)
                .addToBackStack(null)
                .commit();
    }

    // ── Add event dialog (unchanged from original) ────────────────────────────
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
            String title  = etTitle.getText().toString().trim();
            String desc   = etDescription.getText().toString().trim();
            String day    = spDay.getSelectedItem().toString();
            String month  = spMonth.getSelectedItem().toString();
            String year   = spYear.getSelectedItem().toString();
            String time   = etTime.getText().toString().trim();
            String ampm   = spAmPm.getSelectedItem().toString();
            String venue  = etVenue.getText().toString().trim();

            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(desc)) {
                Toast.makeText(getContext(), "Fill required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            addEvent(title, desc, day, month, year, time, ampm, venue);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void setupSpinners(Spinner spDay, Spinner spMonth,
                               Spinner spYear, Spinner spAmPm) {
        List<String> days = new ArrayList<>();
        for (int i = 1; i <= 31; i++) days.add(String.valueOf(i));

        List<String> months = Arrays.asList(
                "JAN","FEB","MAR","APR","MAY","JUN",
                "JUL","AUG","SEP","OCT","NOV","DEC");
        List<String> years = Arrays.asList("2025","2026","2027");
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

    private void addEvent(String title, String desc, String day, String month,
                          String year, String time, String ampm, String venue) {

        String id = dbRef.push().getKey();
        if (id == null) return;

        SocietyEvent event = new SocietyEvent(day, month, year, title, desc, time, ampm, venue);

        // Save under society events
        dbRef.child(id).setValue(event);

        // Also mirror to global Events table (for home screen)
        FirebaseDatabase.getInstance()
                .getReference("Events")
                .child(id)
                .setValue(event)
                .addOnSuccessListener(unused ->
                        Toast.makeText(getContext(), "Event Added!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}