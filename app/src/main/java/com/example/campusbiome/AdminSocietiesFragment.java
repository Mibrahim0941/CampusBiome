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

public class AdminSocietiesFragment extends Fragment {

    private LinearLayout llSocietiesList, llPendingSocieties;
    private DatabaseReference mDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_societies, container, false);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        llSocietiesList = view.findViewById(R.id.llSocietiesList);
        llPendingSocieties = view.findViewById(R.id.llPendingSocieties);

        view.findViewById(R.id.btnAddSociety).setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(getActivity(), AdminAddSocietyActivity.class);
            startActivity(intent);
        });

        fetchSocieties();
        return view;
    }

    private void fetchSocieties() {
        mDatabase.child("Societies").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                llSocietiesList.removeAllViews();
                llPendingSocieties.removeAllViews();
                LayoutInflater inflater = LayoutInflater.from(getContext());

                for (DataSnapshot societySnapshot : snapshot.getChildren()) {
                    String name = societySnapshot.child("name").getValue(String.class);
                    String admin = societySnapshot.child("adminName").getValue(String.class);
                    String status = societySnapshot.child("status").getValue(String.class);
                    String proposedBy = societySnapshot.child("proposedBy").getValue(String.class);

                    if ("approved".equals(status)) {
                        View row = inflater.inflate(R.layout.item_admin_society_row, llSocietiesList, false);
                        ((TextView) row.findViewById(R.id.tvName)).setText(name != null ? name : "N/A");
                        ((TextView) row.findViewById(R.id.tvAdmin)).setText(admin != null ? admin : "N/A");
                        llSocietiesList.addView(row);
                    } else if ("pending".equals(status)) {
                        View card = inflater.inflate(R.layout.item_admin_society_approval, llPendingSocieties, false);
                        String societyId = societySnapshot.getKey();
                        ((TextView) card.findViewById(R.id.tvSocietyName)).setText(name != null ? name : "N/A");
                        ((TextView) card.findViewById(R.id.tvProposedBy)).setText("Proposed by: " + (proposedBy != null ? proposedBy : "Student"));
                        
                        ((TextView) card.findViewById(R.id.tvDetailName)).setText("Name: " + name);
                        ((TextView) card.findViewById(R.id.tvDetailAdmin)).setText("Proposed Admin: " + admin);

                        card.findViewById(R.id.btnAccept).setOnClickListener(v -> {
                            mDatabase.child("Societies").child(societyId).child("status").setValue("approved")
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), name + " Approved", Toast.LENGTH_SHORT).show();
                                    fetchSocieties();
                                });
                        });

                        card.findViewById(R.id.btnReject).setOnClickListener(v -> {
                            mDatabase.child("Societies").child(societyId).child("status").setValue("rejected")
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), name + " Rejected", Toast.LENGTH_SHORT).show();
                                    fetchSocieties();
                                });
                        });
                        
                        llPendingSocieties.addView(card);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) Toast.makeText(getContext(), "Error fetching societies", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
