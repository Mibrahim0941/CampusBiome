package com.example.campusbiome;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminManageEventsFragment extends Fragment {

    private LinearLayout llEventsContainer;
    private DatabaseReference mDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_manage_events, container, false);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        llEventsContainer = view.findViewById(R.id.llEventsContainer);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        fetchPendingEvents();

        return view;
    }

    private void fetchPendingEvents() {
        mDatabase.child("Societies").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot societiesSnapshot) {
                if (!isAdded()) return;
                llEventsContainer.removeAllViews();

                for (DataSnapshot societySnap : societiesSnapshot.getChildren()) {
                    String societyId = societySnap.getKey();
                    String societyName = societySnap.child("name").getValue(String.class);
                    DataSnapshot eventsSnap = societySnap.child("events");

                    boolean hasPending = false;
                    for (DataSnapshot eventSnap : eventsSnap.getChildren()) {
                        String status = eventSnap.child("status").getValue(String.class);
                        if ("pending".equals(status)) {
                            hasPending = true;
                            break;
                        }
                    }

                    if (hasPending) {
                        addSocietySection(societyName, societyId, eventsSnap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addSocietySection(String societyName, String societyId, DataSnapshot eventsSnap) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        
        // Society Header
        View header = inflater.inflate(R.layout.item_admin_event_society_header, llEventsContainer, false);
        ((TextView) header.findViewById(R.id.tvSocietyName)).setText(societyName);
        llEventsContainer.addView(header);

        for (DataSnapshot eventSnap : eventsSnap.getChildren()) {
            String status = eventSnap.child("status").getValue(String.class);
            if (!"pending".equals(status)) continue;

            String eventId = eventSnap.getKey();
            String title = eventSnap.child("title").getValue(String.class);
            String desc = eventSnap.child("description").getValue(String.class);
            String venue = eventSnap.child("venue").getValue(String.class);
            String date = eventSnap.child("day").getValue(String.class) + " " + eventSnap.child("month").getValue(String.class);

            View eventCard = inflater.inflate(R.layout.item_admin_event_approval, llEventsContainer, false);
            ((TextView) eventCard.findViewById(R.id.tvEventTitle)).setText(title);
            ((TextView) eventCard.findViewById(R.id.tvEventDetails)).setText(date + " • " + venue);
            ((TextView) eventCard.findViewById(R.id.tvEventDesc)).setText(desc);

            eventCard.findViewById(R.id.btnAccept).setOnClickListener(v -> {
                mDatabase.child("Societies").child(societyId).child("events").child(eventId).child("status").setValue("approved")
                        .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Event Approved", Toast.LENGTH_SHORT).show());
            });

            eventCard.findViewById(R.id.btnReject).setOnClickListener(v -> {
                mDatabase.child("Societies").child(societyId).child("events").child(eventId).child("status").setValue("rejected")
                        .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Event Rejected", Toast.LENGTH_SHORT).show());
            });

            llEventsContainer.addView(eventCard);
        }
    }
}
