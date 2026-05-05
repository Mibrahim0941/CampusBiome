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

import java.util.HashMap;
import java.util.Map;

public class AdminProfessorsFragment extends Fragment {

    private LinearLayout llFacultyList, llPendingApprovals;
    private DatabaseReference mDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_professors, container, false);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        llFacultyList = view.findViewById(R.id.llFacultyList);
        llPendingApprovals = view.findViewById(R.id.llPendingApprovals);

        view.findViewById(R.id.btnAddFaculty).setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(getActivity(), AdminAddFacultyActivity.class);
            startActivity(intent);
        });

        fetchFaculty();
        migrateFaculty();
        return view;
    }

    private void migrateFaculty() {
        mDatabase.child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String role = userSnapshot.child("role").getValue(String.class);
                    String status = userSnapshot.child("status").getValue(String.class);
                    if ("faculty".equals(role) && "approved".equals(status)) {
                        String userId = userSnapshot.getKey();
                        mDatabase.child("Faculty").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot facultySnapshot) {
                                if (!facultySnapshot.exists()) {
                                    // Copy to Faculty node
                                    Map<String, Object> facultyData = (Map<String, Object>) userSnapshot.getValue();
                                    if (facultyData != null) {
                                        facultyData.put("id", userId);
                                        mDatabase.child("Faculty").child(userId).setValue(facultyData);
                                    }
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchFaculty() {
        // 1. Fetch Approved Faculty from "Faculty" node
        mDatabase.child("Faculty").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                llFacultyList.removeAllViews();
                LayoutInflater inflater = LayoutInflater.from(getContext());

                for (DataSnapshot facultySnapshot : snapshot.getChildren()) {
                    String name = facultySnapshot.child("name").getValue(String.class);
                    String post = facultySnapshot.child("post").getValue(String.class);

                    View row = inflater.inflate(R.layout.item_admin_faculty_row, llFacultyList, false);
                    ((TextView) row.findViewById(R.id.tvName)).setText(name != null ? name : "N/A");
                    ((TextView) row.findViewById(R.id.tvPost)).setText(post != null ? post : "Lecturer");
                    llFacultyList.addView(row);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 2. Fetch Pending Faculty from "Users" node
        mDatabase.child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                llPendingApprovals.removeAllViews();
                LayoutInflater inflater = LayoutInflater.from(getContext());

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String role = userSnapshot.child("role").getValue(String.class);
                    String status = userSnapshot.child("status").getValue(String.class);
                    String name = userSnapshot.child("name").getValue(String.class);
                    String post = userSnapshot.child("post").getValue(String.class);

                    if ("faculty".equals(role) && "pending".equals(status)) {
                        // Approval card
                        View card = inflater.inflate(R.layout.item_admin_faculty_approval, llPendingApprovals, false);
                        String userId = userSnapshot.getKey();
                        ((TextView) card.findViewById(R.id.tvName)).setText(name != null ? name : "N/A");
                        ((TextView) card.findViewById(R.id.tvRole)).setText("As: " + (post != null ? post : "Lecturer"));
                        
                        View expanded = card.findViewById(R.id.llExpanded);
                        card.findViewById(R.id.cbProcess).setOnClickListener(v -> {
                            expanded.setVisibility(expanded.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                        });

                        card.findViewById(R.id.btnAccept).setOnClickListener(v -> {
                            // Update status in Users
                            mDatabase.child("Users").child(userId).child("status").setValue("approved");
                            
                            // Move/Copy to Faculty node
                            Map<String, Object> facultyData = (Map<String, Object>) userSnapshot.getValue();
                            if (facultyData != null) {
                                facultyData.put("id", userId);
                                facultyData.put("status", "approved");
                                mDatabase.child("Faculty").child(userId).setValue(facultyData)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(getContext(), name + " Approved & Added to Faculty", Toast.LENGTH_SHORT).show();
                                        fetchFaculty();
                                    });
                            }
                        });

                        card.findViewById(R.id.btnReject).setOnClickListener(v -> {
                            mDatabase.child("Users").child(userId).child("status").setValue("rejected")
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), name + " Rejected", Toast.LENGTH_SHORT).show();
                                    fetchFaculty();
                                });
                        });
                        
                        llPendingApprovals.addView(card);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) Toast.makeText(getContext(), "Error fetching pending approvals", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
