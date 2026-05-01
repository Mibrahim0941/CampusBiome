package com.example.campusbiome;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SocietyHomeFragment extends Fragment {

    public SocietyHomeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 1. Initialize "View All" Buttons
        Button btnViewRequests = view.findViewById(R.id.btnViewAllRequests);
        Button btnViewEvents = view.findViewById(R.id.btnViewAllEvents);

        // 2. Initialize Dummy Action Icons (Example for first card)
        // Note: In a real app, these would be inside a RecyclerView
        ImageView ivApprove1 = view.findViewById(R.id.name1).getRootView().findViewWithTag("approve_tag");
        // Tip: For dummy data, you can just set generic listeners or find by ID if you added them

        // Setup View All Requests
        if (btnViewRequests != null) {
            btnViewRequests.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Opening all registration requests...", Toast.LENGTH_SHORT).show();
            });
        }

        // Setup View All Events
        if (btnViewEvents != null) {
            btnViewEvents.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Opening events calendar...", Toast.LENGTH_SHORT).show();
            });
        }

        // Example logic for the Check/Close icons (if you add IDs to them in XML)
        setupDummyActions(view);

        return view;
    }

    private void setupDummyActions(View view) {
        // This is a placeholder. Once you add IDs like id/ivApprove1 to your XML,
        // you can handle the clicks like this:
        /*
        view.findViewById(R.id.ivApprove1).setOnClickListener(v ->
            Toast.makeText(getContext(), "Request Approved", Toast.LENGTH_SHORT).show());
        */
    }
}