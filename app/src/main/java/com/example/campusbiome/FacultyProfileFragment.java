package com.example.campusbiome;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FacultyProfileFragment extends Fragment {

    private TextView tvName, tvPost, tvEmail, tvDepartment;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_faculty_profile, container, false);

        tvName = view.findViewById(R.id.tvName);
        tvPost = view.findViewById(R.id.tvPost);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvDepartment = view.findViewById(R.id.tvDepartment);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        loadProfileData();

        return view;
    }

    private void loadProfileData() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        mDatabase.child("Users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && isAdded()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String post = snapshot.child("post").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String department = snapshot.child("department").getValue(String.class);

                    if (name != null) tvName.setText(name);
                    if (post != null) tvPost.setText(post);
                    if (email != null) tvEmail.setText(email);
                    if (department != null) tvDepartment.setText(department);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
