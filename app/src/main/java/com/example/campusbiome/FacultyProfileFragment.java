package com.example.campusbiome;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class FacultyProfileFragment extends Fragment {

    private TextView tvName, tvPost;
    private View itemEmail, itemDept, itemQual, itemExp;
    private Chip chipFyp, chipVisiting;
    private MaterialButton btnEditProfile;
    
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private String facultyId = "fac1"; // As per request

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_faculty_profile, container, false);

        tvName = view.findViewById(R.id.tvName);
        tvPost = view.findViewById(R.id.tvPost);
        
        itemEmail = view.findViewById(R.id.itemEmail);
        itemDept = view.findViewById(R.id.itemDept);
        itemQual = view.findViewById(R.id.itemQual);
        itemExp = view.findViewById(R.id.itemExp);
        
        chipFyp = view.findViewById(R.id.chipFyp);
        chipVisiting = view.findViewById(R.id.chipVisiting);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);

        setupItem(itemEmail, "Email Address", R.drawable.ic_email);
        setupItem(itemDept, "Department", R.drawable.ic_department);
        setupItem(itemQual, "Qualification", R.drawable.ic_school);
        setupItem(itemExp, "Experience", R.drawable.ic_work);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Faculty").child(facultyId);

        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        loadProfileData();

        return view;
    }

    private void setupItem(View itemView, String label, int iconRes) {
        ((TextView) itemView.findViewById(R.id.tvLabel)).setText(label);
        ((android.widget.ImageView) itemView.findViewById(R.id.ivIcon)).setImageResource(iconRes);
    }

    private void updateItemValue(View itemView, String value) {
        ((TextView) itemView.findViewById(R.id.tvValue)).setText(value != null ? value : "Not set");
    }

    private void loadProfileData() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && isAdded()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String post = snapshot.child("position").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String dept = snapshot.child("department").getValue(String.class);
                    String qual = snapshot.child("qualification").getValue(String.class);
                    String exp = snapshot.child("experience").getValue(String.class);
                    Boolean isFyp = snapshot.child("fypCoordinator").getValue(Boolean.class);
                    Boolean isVisiting = snapshot.child("visiting").getValue(Boolean.class);

                    if (name != null) tvName.setText(name);
                    if (post != null) tvPost.setText(post);
                    
                    updateItemValue(itemEmail, email);
                    updateItemValue(itemDept, dept);
                    updateItemValue(itemQual, qual);
                    updateItemValue(itemExp, exp);

                    chipFyp.setVisibility(Boolean.TRUE.equals(isFyp) ? View.VISIBLE : View.GONE);
                    chipVisiting.setVisibility(Boolean.TRUE.equals(isVisiting) ? View.VISIBLE : View.GONE);
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

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);
        
        android.widget.EditText etName = dialogView.findViewById(R.id.etName);
        android.widget.EditText etPost = dialogView.findViewById(R.id.etPost);
        android.widget.EditText etDept = dialogView.findViewById(R.id.etDept);
        android.widget.EditText etQual = dialogView.findViewById(R.id.etQual);
        android.widget.EditText etExp = dialogView.findViewById(R.id.etExp);

        // Pre-fill existing values
        etName.setText(tvName.getText().toString());
        etPost.setText(tvPost.getText().toString());
        etDept.setText(getVal(itemDept).equals("Not set") ? "" : getVal(itemDept));
        etQual.setText(getVal(itemQual).equals("Not set") ? "" : getVal(itemQual));
        etExp.setText(getVal(itemExp).equals("Not set") ? "" : getVal(itemExp));

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Edit Profile");
        builder.setView(dialogView);

        builder.setPositiveButton("Save Changes", (dialog, which) -> {
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", etName.getText().toString().trim());
            updates.put("position", etPost.getText().toString().trim());
            updates.put("department", etDept.getText().toString().trim());
            updates.put("qualification", etQual.getText().toString().trim());
            updates.put("experience", etExp.getText().toString().trim());

            mDatabase.updateChildren(updates).addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
            });
        });

        builder.setNegativeButton("Cancel", null);
        
        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
        }
        dialog.show();
    }

    private String getVal(View itemView) {
        return ((TextView) itemView.findViewById(R.id.tvValue)).getText().toString();
    }
}
