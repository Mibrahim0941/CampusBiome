package com.example.campusbiome;

import android.os.Bundle;
import android.widget.ImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;

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
                java.util.List<DataSnapshot> activeFaculty = new java.util.ArrayList<>();
                java.util.List<DataSnapshot> suspendedFaculty = new java.util.ArrayList<>();

                for (DataSnapshot facultySnapshot : snapshot.getChildren()) {
                    String status = facultySnapshot.child("accountStatus").getValue(String.class);
                    if ("suspended".equals(status)) {
                        suspendedFaculty.add(facultySnapshot);
                    } else {
                        activeFaculty.add(facultySnapshot);
                    }
                }

                addFacultyToLayout(activeFaculty, false);
                addFacultyToLayout(suspendedFaculty, true);
            }

            private void addFacultyToLayout(java.util.List<DataSnapshot> facultyList, boolean isSuspended) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                for (DataSnapshot facultySnapshot : facultyList) {
                    String name = getString(facultySnapshot, "name");
                    String post = getString(facultySnapshot, "position", "post");
                    // Fix: Database field is capitalized "Available"
                    Boolean isAvailable = facultySnapshot.child("Available").getValue(Boolean.class);

                    View row = inflater.inflate(R.layout.item_admin_faculty_row, llFacultyList, false);
                    TextView tvName = row.findViewById(R.id.tvName);
                    TextView tvPost = row.findViewById(R.id.tvPost);
                    ImageView ivStatus = row.findViewById(R.id.ivStatus);

                    tvName.setText(name);
                    tvPost.setText(post);
                    
                    if (isSuspended) {
                        row.setBackgroundColor(android.graphics.Color.parseColor("#FFEBEE")); // Light Red
                        tvName.setTextColor(android.graphics.Color.RED);
                        ivStatus.setVisibility(View.GONE);
                    } else {
                        // Make sure we show red if explicitly false
                        if (isAvailable != null && !isAvailable) {
                            ivStatus.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#D32F2F"))); // Bright Red
                        } else {
                            ivStatus.setImageTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.primary)));
                        }
                    }

                    row.setOnClickListener(v -> showFacultyDetailsDialog(facultySnapshot));
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
                if (isAdded() && com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) Toast.makeText(getContext(), "Error fetching pending approvals", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showFacultyDetailsDialog(DataSnapshot facultySnapshot) {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_admin_details, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        LinearLayout llContainer = dialogView.findViewById(R.id.llDetailsContainer);
        com.google.android.material.button.MaterialButton btnAction = dialogView.findViewById(R.id.btnPrimaryAction);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        tvTitle.setText("Faculty Profile");
        String status = facultySnapshot.child("accountStatus").getValue(String.class);
        boolean isSuspended = "suspended".equals(status);

        if (isSuspended) {
            btnAction.setText("Unsuspend Faculty");
        } else {
            btnAction.setText("Suspend Faculty");
        }

        addDetailRow(llContainer, "Name", getString(facultySnapshot, "name"));
        addDetailRow(llContainer, "Email", getString(facultySnapshot, "email"));
        addDetailRow(llContainer, "Post", getString(facultySnapshot, "position", "post"));
        addDetailRow(llContainer, "Department", getString(facultySnapshot, "department"));

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAction.setOnClickListener(v -> {
            String uid = facultySnapshot.getKey();
            if (uid != null) {
                if (isSuspended) {
                    // Unsuspend: Remove accountStatus from both
                    mDatabase.child("Faculty").child(uid).child("accountStatus").removeValue();
                    mDatabase.child("Users").child(uid).child("accountStatus").removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Faculty Unsuspended Successfully", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                fetchFaculty();
                            })
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to unsuspend: " + e.getMessage(), Toast.LENGTH_LONG).show());
                } else {
                    // Soft Suspend
                    mDatabase.child("Faculty").child(uid).child("accountStatus").setValue("suspended");
                    mDatabase.child("Users").child(uid).child("accountStatus").setValue("suspended")
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Faculty Suspended Successfully", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                fetchFaculty();
                            })
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to suspend: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            } else {
                Toast.makeText(getContext(), "Error: UID is null", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private String getString(DataSnapshot snapshot, String... keys) {
        for (String key : keys) {
            Object val = snapshot.child(key).getValue();
            if (val != null) return String.valueOf(val);
        }
        return "null";
    }

    private void addDetailRow(LinearLayout container, String label, String value) {
        if (getContext() == null) return;
        TextView tv = new TextView(getContext());
        tv.setText(label + ": " + (value != null ? value : "null"));
        tv.setTextSize(16);
        tv.setTextColor(android.graphics.Color.parseColor("#191C1D"));
        tv.setPadding(0, 0, 0, 20);
        container.addView(tv);
    }
}
