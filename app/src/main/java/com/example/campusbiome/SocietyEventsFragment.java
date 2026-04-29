package com.example.campusbiome;

import android.app.AlertDialog;
import android.os.Bundle;
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

    private RecyclerView rvEvents;
    private FloatingActionButton btnAddEvent;
    private List<SocietyEvent> eventList = new ArrayList<>();
    private SocietyEventAdapter adapter;
    private DatabaseReference dbRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_society_events, container, false);

        rvEvents = view.findViewById(R.id.rvEvents);
        btnAddEvent = view.findViewById(R.id.btnAddEvent);

        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SocietyEventAdapter(eventList);
        rvEvents.setAdapter(adapter);

        dbRef = FirebaseDatabase.getInstance().getReference("Events");

        loadEvents();

        btnAddEvent.setOnClickListener(v -> showAddEventDialog());

        return view;
    }

    private void loadEvents() {
        dbRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                eventList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    SocietyEvent event = snap.getValue(SocietyEvent.class);
                    if (event != null) {
                        eventList.add(event);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load events", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void showAddEventDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_event, null);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        EditText etTitle = dialogView.findViewById(R.id.etTitle);
        EditText etDescription = dialogView.findViewById(R.id.etDescription);
        EditText etVenue = dialogView.findViewById(R.id.etVenue);
        EditText etTime = dialogView.findViewById(R.id.etTime);

        Spinner spDay = dialogView.findViewById(R.id.spDay);
        Spinner spMonth = dialogView.findViewById(R.id.spMonth);
        Spinner spYear = dialogView.findViewById(R.id.spYear);
        Spinner spAmPm = dialogView.findViewById(R.id.spAmPm);

        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnPublish = dialogView.findViewById(R.id.btnPublish);

        // Populate dropdowns
        setupSpinners(spDay, spMonth, spYear, spAmPm);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnPublish.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String desc = etDescription.getText().toString().trim();
            String venue = etVenue.getText().toString().trim();
            String time = etTime.getText().toString().trim();
            String day = spDay.getSelectedItem().toString();
            String month = spMonth.getSelectedItem().toString();

            if (title.isEmpty() || desc.isEmpty()) {
                Toast.makeText(getContext(), "Fill required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            addEventToFirebase(title, desc, day, month);
            dialog.dismiss();
        });

        dialog.show();
    }
    private void setupSpinners(Spinner spDay, Spinner spMonth, Spinner spYear, Spinner spAmPm) {

        List<String> days = new ArrayList<>();
        for (int i = 1; i <= 31; i++) days.add(String.valueOf(i));

        List<String> months = Arrays.asList("JAN","FEB","MAR","APR","MAY","JUN",
                "JUL","AUG","SEP","OCT","NOV","DEC");

        List<String> years = Arrays.asList("2024","2025","2026","2027");
        List<String> ampm = Arrays.asList("AM","PM");

        spDay.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, days));
        spMonth.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, months));
        spYear.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, years));
        spAmPm.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, ampm));
    }

    private void addEventToFirebase(String title, String desc, String day, String month) {

        String eventId = dbRef.push().getKey(); // auto ID

        SocietyEvent event = new SocietyEvent(day, month, title, desc);

        dbRef.child(eventId).setValue(event)
                .addOnSuccessListener(unused ->
                        Toast.makeText(getContext(), "Event Added!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}

