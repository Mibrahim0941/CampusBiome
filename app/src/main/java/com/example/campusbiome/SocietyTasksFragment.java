package com.example.campusbiome;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen 1 – lists this society's events.
 * Each event card shows a "View Tasks" button that navigates to SocietyTaskListFragment.
 */
public class SocietyTasksFragment extends Fragment {

    private RecyclerView rvEvents;
    private TextView     tvEmpty;

    private final List<SocietyEvent> eventList = new ArrayList<>();
    private final List<String>       eventIds  = new ArrayList<>();

    private DatabaseReference dbRef;
    private String societyId;

    public static SocietyTasksFragment newInstance(String societyId) {
        SocietyTasksFragment frag = new SocietyTasksFragment();
        Bundle args = new Bundle();
        args.putString("societyId", societyId);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_society_tasks, container, false);
        rvEvents = view.findViewById(R.id.rvSelectEvent);
        tvEmpty  = view.findViewById(R.id.tvNoEvents);

        if (getArguments() != null) {
            societyId = getArguments().getString("societyId");
        }

        if (societyId == null) {
            Toast.makeText(getContext(), "Society not found", Toast.LENGTH_SHORT).show();
            return view;
        }

        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));

        dbRef = FirebaseDatabase.getInstance()
                .getReference("Societies")
                .child(societyId)
                .child("events");

        loadEvents();
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
                        eventList.add(0, event);
                        eventIds.add(0, snap.getKey());
                    }
                }

                if (eventList.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvEvents.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvEvents.setVisibility(View.VISIBLE);

                    // "View Tasks" label instead of "View Registrations"
                    SocietyEventAdapter adapter = new SocietyEventAdapter(
                            eventList,
                            eventIds,
                            (event, eventId) -> openTaskList(event, eventId),
                            "View Tasks");

                    rvEvents.setAdapter(adapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openTaskList(SocietyEvent event, String eventId) {
        SocietyTaskListFragment frag =
                SocietyTaskListFragment.newInstance(societyId, eventId, event.getTitle());
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, frag)
                .addToBackStack(null)
                .commit();
    }
}