package com.example.campusbiome;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminProfileFragment extends Fragment {

    private TextView tvName, tvRole;
    private View itemEmail, itemAdminId;
    private MaterialButton btnEditProfile;
    
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_profile, container, false);

        tvName = view.findViewById(R.id.tvName);
        tvRole = view.findViewById(R.id.tvRole);
        itemEmail = view.findViewById(R.id.itemEmail);
        itemAdminId = view.findViewById(R.id.itemAdminId);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);

        setupItem(itemEmail, "Email Address", R.drawable.ic_email);
        setupItem(itemAdminId, "Admin ID", R.drawable.ic_profile);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            loadProfileData(user.getUid());
        }

        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        return view;
    }

    private void setupItem(View itemView, String label, int iconRes) {
        ((TextView) itemView.findViewById(R.id.tvLabel)).setText(label);
        ((ImageView) itemView.findViewById(R.id.ivIcon)).setImageResource(iconRes);
    }

    private void updateItemValue(View itemView, String value) {
        ((TextView) itemView.findViewById(R.id.tvValue)).setText(value != null ? value : "Not set");
    }

    private void loadProfileData(String uid) {
        mDatabase.child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && isAdded()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String role = snapshot.child("role").getValue(String.class);

                    if (name != null) tvName.setText(name);
                    if (role != null) tvRole.setText(role);
                    
                    updateItemValue(itemEmail, email);
                    updateItemValue(itemAdminId, uid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) Toast.makeText(getContext(), "Error loading profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditProfileDialog() {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_admin_profile, null);
        
        com.google.android.material.textfield.TextInputEditText etName = dialogView.findViewById(R.id.etName);
        com.google.android.material.textfield.TextInputEditText etRole = dialogView.findViewById(R.id.etRole);

        // Pre-fill existing values
        etName.setText(tvName.getText().toString());
        etRole.setText(tvRole.getText().toString());

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Edit Admin Profile");
        builder.setView(dialogView);

        builder.setPositiveButton("Save Changes", (dialog, which) -> {
            String newName = etName.getText().toString().trim();
            String newRole = etRole.getText().toString().trim();
            
            if (newName.isEmpty()) {
                Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("name", newName);
            updates.put("role", newRole);

            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                mDatabase.child(user.getUid()).updateChildren(updates).addOnSuccessListener(aVoid -> {
                    if (isAdded()) Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e -> {
                    if (isAdded()) Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                });
            }
        });

        builder.setNegativeButton("Cancel", null);
        
        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
        }
        dialog.show();
    }
}
