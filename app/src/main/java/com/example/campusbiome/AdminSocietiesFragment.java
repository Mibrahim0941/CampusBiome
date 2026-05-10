package com.example.campusbiome;

import android.os.Bundle;
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
                
                java.util.List<DataSnapshot> activeSocieties = new java.util.ArrayList<>();
                java.util.List<DataSnapshot> suspendedSocieties = new java.util.ArrayList<>();
                java.util.List<DataSnapshot> pendingSocieties = new java.util.ArrayList<>();

                for (DataSnapshot societySnapshot : snapshot.getChildren()) {
                    String status = societySnapshot.child("status").getValue(String.class);
                    String accStatus = societySnapshot.child("accountStatus").getValue(String.class);
                    
                    if ("suspended".equals(accStatus)) {
                        suspendedSocieties.add(societySnapshot);
                    } else if ("approved".equals(status)) {
                        activeSocieties.add(societySnapshot);
                    } else if ("pending".equals(status)) {
                        pendingSocieties.add(societySnapshot);
                    }
                }

                addSocietiesToLayout(activeSocieties, false);
                addSocietiesToLayout(suspendedSocieties, true);
                addPendingSocietiesToLayout(pendingSocieties);
            }

            private void addSocietiesToLayout(java.util.List<DataSnapshot> societies, boolean isSuspended) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                for (DataSnapshot societySnapshot : societies) {
                    String name = societySnapshot.child("name").getValue(String.class);
                    String managerId = societySnapshot.child("managerId").getValue(String.class);
                    String admin = societySnapshot.child("adminName").getValue(String.class);

                    View row = inflater.inflate(R.layout.item_admin_society_row, llSocietiesList, false);
                    TextView tvName = row.findViewById(R.id.tvName);
                    TextView tvAdmin = row.findViewById(R.id.tvAdmin);

                    tvName.setText(name != null ? name : "N/A");
                    tvAdmin.setText(admin != null ? admin : "N/A");

                    // If managerId exists, fetch real-time admin name if not already correct
                    if (managerId != null) {
                        mDatabase.child("Users").child(managerId).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                String realName = userSnapshot.getValue(String.class);
                                if (realName != null) {
                                    tvAdmin.setText(realName);
                                }
                            }
                            @Override public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }

                    if (isSuspended) {
                        row.setBackgroundColor(android.graphics.Color.parseColor("#FFEBEE")); // Light Red
                        tvName.setTextColor(android.graphics.Color.RED);
                    }

                    row.setOnClickListener(v -> showSocietyDetailsDialog(societySnapshot));
                    llSocietiesList.addView(row);
                }
            }

            private void addPendingSocietiesToLayout(java.util.List<DataSnapshot> pendingList) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                for (DataSnapshot societySnapshot : pendingList) {
                    String name = societySnapshot.child("name").getValue(String.class);
                    String admin = societySnapshot.child("adminName").getValue(String.class);
                    String proposedBy = societySnapshot.child("proposedBy").getValue(String.class);
                    String societyId = societySnapshot.getKey();

                    View card = inflater.inflate(R.layout.item_admin_society_approval, llPendingSocieties, false);
                    ((TextView) card.findViewById(R.id.tvSocietyName)).setText(name != null ? name : "N/A");
                    ((TextView) card.findViewById(R.id.tvProposedBy)).setText("Proposed by: " + (proposedBy != null ? proposedBy : "Student"));
                    
                    ((TextView) card.findViewById(R.id.tvDetailName)).setText("Name: " + name);
                    ((TextView) card.findViewById(R.id.tvDetailAdmin)).setText("Proposed Admin: " + admin);

                    card.findViewById(R.id.btnAccept).setOnClickListener(v -> {
                        String adminProposedName = societySnapshot.child("adminName").getValue(String.class);
                        String proposedByUid = societySnapshot.child("proposedByUid").getValue(String.class);
                        
                        if (adminProposedName == null) adminProposedName = "Society Admin";
                        
                        // Generate email from name: e.g., "Bisma" -> "bisma@societyadmin.com"
                        String finalEmail = adminProposedName.replaceAll("\\s+", "").toLowerCase() + "@societyadmin.com";
                        String defaultPassword = "123456";
                        String finalAdminName = adminProposedName;

                        // Use Secondary Firebase to create the new Society Admin account
                        com.google.firebase.FirebaseOptions options = com.google.firebase.FirebaseApp.getInstance().getOptions();
                        String appName = "SocietyAdmin_" + System.currentTimeMillis();
                        com.google.firebase.FirebaseApp tempApp;
                        try {
                            tempApp = com.google.firebase.FirebaseApp.initializeApp(getContext(), options, appName);
                        } catch (Exception e) {
                            tempApp = com.google.firebase.FirebaseApp.getInstance(appName);
                        }
                        final com.google.firebase.FirebaseApp secondaryApp = tempApp;
                        
                        com.google.firebase.auth.FirebaseAuth secondaryAuth = com.google.firebase.auth.FirebaseAuth.getInstance(secondaryApp);
                        secondaryAuth.createUserWithEmailAndPassword(finalEmail, defaultPassword)
                            .addOnSuccessListener(authResult -> {
                                String newAdminUid = authResult.getUser().getUid();
                                
                                // 1. Create User Record
                                java.util.Map<String, Object> userData = new java.util.HashMap<>();
                                userData.put("id", newAdminUid);
                                userData.put("name", finalAdminName);
                                userData.put("email", finalEmail);
                                userData.put("role", "society_manager");
                                userData.put("status", "approved");
                                userData.put("societyId", societyId);
                                mDatabase.child("Users").child(newAdminUid).setValue(userData);

                                // 2. Update Society Record
                                java.util.Map<String, Object> updates = new java.util.HashMap<>();
                                updates.put("status", "approved");
                                updates.put("managerId", newAdminUid);
                                updates.put("adminName", finalAdminName); // Keep adminName for quick display
                                
                                mDatabase.child("Societies").child(societyId).updateChildren(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        // 3. Send Notification to the proposer
                                        if (proposedByUid != null) {
                                            java.util.Map<String, Object> notifData = new java.util.HashMap<>();
                                            notifData.put("title", "Society Approved: " + name);
                                            notifData.put("message", "Congratulations! Your society proposal has been accepted. Your manager account is: " + finalEmail + " with password: " + defaultPassword + ". Log in as Society Manager to manage it.");
                                            notifData.put("timestamp", System.currentTimeMillis());
                                            notifData.put("type", "alert");
                                            notifData.put("isRead", false);
                                            
                                            mDatabase.child("Notifications").child(proposedByUid).push().setValue(notifData);
                                        }

                                        Toast.makeText(getContext(), name + " Approved and Admin Account Created", Toast.LENGTH_LONG).show();
                                        secondaryApp.delete();
                                        fetchSocieties();
                                    });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Auth Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                secondaryApp.delete();
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

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded() && com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) Toast.makeText(getContext(), "Error fetching societies", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSocietyDetailsDialog(DataSnapshot societySnapshot) {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_admin_details, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        LinearLayout llContainer = dialogView.findViewById(R.id.llDetailsContainer);
        com.google.android.material.button.MaterialButton btnAction = dialogView.findViewById(R.id.btnPrimaryAction);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        tvTitle.setText("Society Details");
        String accStatus = societySnapshot.child("accountStatus").getValue(String.class);
        boolean isSuspended = "suspended".equals(accStatus);

        if (isSuspended) {
            btnAction.setText("Unsuspend Society");
        } else {
            btnAction.setText("Suspend Society");
        }

        addDetailRow(llContainer, "Name", getString(societySnapshot, "name", "Name"));
        addDetailRow(llContainer, "Description", getString(societySnapshot, "description", "Description"));
        addDetailRow(llContainer, "Category", getString(societySnapshot, "category", "Category"));
        addDetailRow(llContainer, "Manager Email", getString(societySnapshot, "managerEmail", "email"));

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAction.setOnClickListener(v -> {
            String sid = societySnapshot.getKey();
            if (sid != null) {
                if (isSuspended) {
                    // Unsuspend
                    mDatabase.child("Societies").child(sid).child("accountStatus").removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Society Unsuspended Successfully", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                fetchSocieties();
                            })
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to unsuspend: " + e.getMessage(), Toast.LENGTH_LONG).show());
                } else {
                    // Soft Suspend
                    mDatabase.child("Societies").child(sid).child("accountStatus").setValue("suspended")
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Society Suspended Successfully", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                fetchSocieties();
                            })
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to suspend: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            } else {
                Toast.makeText(getContext(), "Error: Society ID is null", Toast.LENGTH_SHORT).show();
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
