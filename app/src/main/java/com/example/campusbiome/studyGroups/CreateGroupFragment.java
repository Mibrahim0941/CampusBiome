package com.example.campusbiome.studyGroups;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.campusbiome.R;
import com.example.campusbiome.studyGroups.models.StudyGroup;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

// ── IMPORTANT: use TextInputEditText, NOT plain EditText ──
// The XML uses TextInputLayout + TextInputEditText.
// Using plain EditText here causes findViewById to return null → NPE.
import com.google.android.material.textfield.TextInputEditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CreateGroupFragment extends Fragment {

    private TextInputEditText etGroupName, etCourse, etDescription, etMaxMembers, etTags;
    private MaterialButton    btnSubmitGroup;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_create_group, container, false);

        // Toolbar back navigation
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // Wire fields — must be TextInputEditText to match XML
        etGroupName    = view.findViewById(R.id.etGroupName);
        etCourse       = view.findViewById(R.id.etCourse);
        etDescription  = view.findViewById(R.id.etDescription);
        etMaxMembers   = view.findViewById(R.id.etMaxMembers);
        etTags         = view.findViewById(R.id.etTags);
        btnSubmitGroup = view.findViewById(R.id.btnSubmitGroup);

        btnSubmitGroup.setOnClickListener(v -> attemptCreateGroup());

        return view;
    }

    private void attemptCreateGroup() {

        // ── Read & validate ──────────────────────────────────────────────────
        String name        = text(etGroupName);
        String course      = text(etCourse);
        String description = text(etDescription);
        String maxMembersStr = text(etMaxMembers);
        String tagsRaw     = text(etTags);

        if (name.isEmpty()) {
            etGroupName.setError("Group name is required");
            etGroupName.requestFocus();
            return;
        }
        if (course.isEmpty()) {
            etCourse.setError("Course name is required");
            etCourse.requestFocus();
            return;
        }
        if (maxMembersStr.isEmpty()) {
            etMaxMembers.setError("Max members is required");
            etMaxMembers.requestFocus();
            return;
        }

        int maxMembers;
        try {
            maxMembers = Integer.parseInt(maxMembersStr);
        } catch (NumberFormatException e) {
            etMaxMembers.setError("Enter a valid number");
            return;
        }

        if (maxMembers < 2 || maxMembers > 50) {
            etMaxMembers.setError("Must be between 2 and 50");
            return;
        }

        // ── Parse tags ───────────────────────────────────────────────────────
        List<String> tags = new ArrayList<>();
        if (!tagsRaw.isEmpty()) {
            for (String t : tagsRaw.split(",")) {
                String trimmed = t.trim();
                if (!trimmed.isEmpty()) tags.add(trimmed);
            }
        }
        if (tags.isEmpty()) tags.add("General");

        // ── Get current user ─────────────────────────────────────────────────
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String createdBy  = (user != null) ? user.getUid() : "unknown";

        // ── Write to Firebase ────────────────────────────────────────────────
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("StudyGroups");
        String groupId = ref.push().getKey();

        if (groupId == null) {
            Toast.makeText(getContext(), "Failed to generate group ID", Toast.LENGTH_SHORT).show();
            return;
        }

        StudyGroup newGroup = new StudyGroup(
                name, course,
                description.isEmpty() ? "No description provided." : description,
                1,          // currentMembers starts at 1 (the creator)
                maxMembers,
                true,
                tags,
                createdBy
        );

        btnSubmitGroup.setEnabled(false);
        btnSubmitGroup.setText("Creating…");

        ref.child(groupId).setValue(newGroup)
                .addOnSuccessListener(unused -> {
                    // Also record this group as joined for the creator
                    if (user != null) {
                        FirebaseDatabase.getInstance().getReference("Users")
                                .child(user.getUid())
                                .child("joinedGroups")
                                .child(groupId)
                                .setValue(true);
                    }
                    Toast.makeText(getContext(), "Group created!", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    btnSubmitGroup.setEnabled(true);
                    btnSubmitGroup.setText("Create Group");
                    Toast.makeText(getContext(),
                            "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /** Null-safe text extractor for TextInputEditText */
    private String text(TextInputEditText field) {
        if (field == null || field.getText() == null) return "";
        return field.getText().toString().trim();
    }
}