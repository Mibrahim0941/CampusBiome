package com.example.campusbiome;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegisterSocietyFragment extends Fragment {

    private TextInputEditText etName, etCategory, etDescription, etAdminName, etAdminEmail;
    private MaterialButton    btnSubmit;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_register_society, container, false);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        etName       = view.findViewById(R.id.etSocietyName);
        etCategory   = view.findViewById(R.id.etCategory);
        etDescription = view.findViewById(R.id.etDescription);
        etAdminName  = view.findViewById(R.id.etAdminName);
        etAdminEmail = view.findViewById(R.id.etAdminEmail);
        btnSubmit    = view.findViewById(R.id.btnSubmitProposal);

        // Pre-fill name and email from logged in user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            etAdminEmail.setText(user.getEmail());
        }

        btnSubmit.setOnClickListener(v -> attemptSubmit());

        return view;
    }

    private void attemptSubmit() {
        String name        = text(etName);
        String category    = text(etCategory);
        String description = text(etDescription);
        String adminName   = text(etAdminName);
        String adminEmail  = text(etAdminEmail);

        // Validation
        if (name.isEmpty()) {
            etName.setError("Society name is required");
            etName.requestFocus();
            return;
        }
        if (description.isEmpty()) {
            etDescription.setError("Description is required");
            etDescription.requestFocus();
            return;
        }
        if (adminName.isEmpty()) {
            etAdminName.setError("Your name is required");
            etAdminName.requestFocus();
            return;
        }
        if (adminEmail.isEmpty()) {
            etAdminEmail.setError("Your email is required");
            etAdminEmail.requestFocus();
            return;
        }

        submitProposal(name, category, description, adminName, adminEmail);
    }

    private void submitProposal(String name, String category, String description,
                                String adminName, String adminEmail) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String proposedByUid = user != null ? user.getUid() : "unknown";

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Societies");
        String societyId = ref.push().getKey();
        if (societyId == null) {
            Toast.makeText(getContext(), "Failed to generate ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        // ── Society proposal node ────────────────────────────────────────────
        // Status = "pending" → admin sees it in AdminSocietiesFragment
        // and can approve/reject. Your partner's AdminSocietiesFragment
        // already handles this via the "pending" status check.
        Map<String, Object> proposal = new HashMap<>();
        proposal.put("name",         name);
        proposal.put("category",     category.isEmpty() ? "General" : category);
        proposal.put("description",  description);
        proposal.put("adminName",    adminName);
        proposal.put("adminEmail",   adminEmail);
        proposal.put("proposedBy",   adminName);
        proposal.put("proposedByUid", proposedByUid);
        proposal.put("status",       "pending");  // admin will approve/reject

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting…");

        ref.child(societyId).setValue(proposal)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(),
                            "Proposal submitted! Await admin approval.",
                            Toast.LENGTH_LONG).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Submit Proposal");
                    Toast.makeText(getContext(),
                            "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String text(TextInputEditText field) {
        if (field == null || field.getText() == null) return "";
        return field.getText().toString().trim();
    }
}