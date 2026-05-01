package com.example.campusbiome;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class EventsFragment extends Fragment {

    private static final String TAG = "EventsFragment";

    // ── Views ─────────────────────────────────────────────────────────────────
    private RecyclerView      recyclerRegistered, recyclerBrowse;
    private TextView          txtNoRegistered, txtRegisteredCount;
    private TextView          txtNoBrowse, txtBrowseCount;
    private TextInputEditText etSearch;

    // ── Data ──────────────────────────────────────────────────────────────────
    // All events collected from all approved societies
    private final List<SocietyEvent> allEvents = new ArrayList<>();
    private String currentUid = null;

    // Firebase
    private DatabaseReference  societiesRef;
    private ValueEventListener societiesListener;

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_events, container, false);

        recyclerRegistered = view.findViewById(R.id.recyclerRegistered);
        recyclerBrowse     = view.findViewById(R.id.recyclerBrowse);
        txtNoRegistered    = view.findViewById(R.id.txtNoRegistered);
        txtRegisteredCount = view.findViewById(R.id.txtRegisteredCount);
        txtNoBrowse        = view.findViewById(R.id.txtNoBrowse);
        txtBrowseCount     = view.findViewById(R.id.txtBrowseCount);
        etSearch           = view.findViewById(R.id.etSearchEvent);

        recyclerRegistered.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerRegistered.setNestedScrollingEnabled(false);
        recyclerBrowse.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerBrowse.setNestedScrollingEnabled(false);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) currentUid = user.getUid();

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                public void afterTextChanged(Editable s) {}
                public void onTextChanged(CharSequence s, int a, int b, int c) {
                    renderLists(s.toString().trim().toLowerCase());
                }
            });
        }

        loadAllEvents();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (societiesRef != null && societiesListener != null)
            societiesRef.removeEventListener(societiesListener);
    }

    // ── Firebase: read all events from all approved societies ─────────────────
    private void loadAllEvents() {
        societiesRef = FirebaseDatabase.getInstance().getReference("Societies");

        societiesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allEvents.clear();

                for (DataSnapshot societySnap : snapshot.getChildren()) {

                    // Only include approved societies
                    String status = societySnap.child("status").getValue(String.class);
                    if (!"approved".equals(status)) continue;

                    String societyName = societySnap.child("name").getValue(String.class);
                    String societyId   = societySnap.getKey();

                    DataSnapshot eventsSnap = societySnap.child("events");
                    for (DataSnapshot eventSnap : eventsSnap.getChildren()) {
                        SocietyEvent event = eventSnap.getValue(SocietyEvent.class);
                        if (event == null) continue;

                        // Set transient fields manually
                        event.setId(eventSnap.getKey());
                        event.setSocietyId(societyId);
                        event.setSocietyName(societyName != null ? societyName : "Society");

                        allEvents.add(event);
                        Log.d(TAG, "Loaded event: " + event.getTitle() + " from " + societyName);
                    }
                }

                renderLists(currentSearchQuery());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase error: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to load events.", Toast.LENGTH_SHORT).show();
            }
        };

        societiesRef.addValueEventListener(societiesListener);
    }

    // ── Render both lists ─────────────────────────────────────────────────────
    private void renderLists(String query) {
        List<SocietyEvent> registered = new ArrayList<>();
        List<SocietyEvent> browse     = new ArrayList<>();

        for (SocietyEvent e : allEvents) {
            if (!query.isEmpty() && !matchesQuery(e, query)) continue;

            if (currentUid != null && e.isRegistered(currentUid)) {
                registered.add(e);
            } else {
                browse.add(e);
            }
        }

        // Registered section
        txtRegisteredCount.setText(String.valueOf(registered.size()));
        if (registered.isEmpty()) {
            txtNoRegistered.setVisibility(View.VISIBLE);
            recyclerRegistered.setVisibility(View.GONE);
        } else {
            txtNoRegistered.setVisibility(View.GONE);
            recyclerRegistered.setVisibility(View.VISIBLE);
            recyclerRegistered.setAdapter(new EventAdapter(
                    registered, currentUid, this::onRegisterClicked));
        }

        // Browse section
        txtBrowseCount.setText(String.valueOf(browse.size()));
        if (browse.isEmpty()) {
            txtNoBrowse.setVisibility(View.VISIBLE);
            recyclerBrowse.setVisibility(View.GONE);
        } else {
            txtNoBrowse.setVisibility(View.GONE);
            recyclerBrowse.setVisibility(View.VISIBLE);
            recyclerBrowse.setAdapter(new EventAdapter(
                    browse, currentUid, this::onRegisterClicked));
        }
    }

    // ── Register / Unregister ─────────────────────────────────────────────────
    private void onRegisterClicked(SocietyEvent event, int position) {
        if (currentUid == null) {
            Toast.makeText(getContext(), "Please log in to register.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference regRef = FirebaseDatabase.getInstance()
                .getReference("Societies")
                .child(event.getSocietyId())
                .child("events")
                .child(event.getId())
                .child("registrations")
                .child(currentUid);

        boolean alreadyRegistered = event.isRegistered(currentUid);

        if (alreadyRegistered) {
            // Unregister
            regRef.removeValue()
                    .addOnSuccessListener(unused ->
                            Toast.makeText(getContext(),
                                    "Unregistered from " + event.getTitle(),
                                    Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(),
                                    "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            // Register — store uid: true
            regRef.setValue(true)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(getContext(),
                                    "Registered for " + event.getTitle() + "!",
                                    Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(),
                                    "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
        // The live listener on societiesRef will automatically re-render the lists
        // with updated registration state, so no manual notifyItemChanged needed
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private boolean matchesQuery(SocietyEvent e, String query) {
        if (e.getTitle()       != null && e.getTitle().toLowerCase().contains(query))       return true;
        if (e.getDescription() != null && e.getDescription().toLowerCase().contains(query)) return true;
        if (e.getSocietyName() != null && e.getSocietyName().toLowerCase().contains(query)) return true;
        if (e.getVenue()       != null && e.getVenue().toLowerCase().contains(query))       return true;
        return false;
    }

    private String currentSearchQuery() {
        if (etSearch == null || etSearch.getText() == null) return "";
        return etSearch.getText().toString().trim().toLowerCase();
    }
}