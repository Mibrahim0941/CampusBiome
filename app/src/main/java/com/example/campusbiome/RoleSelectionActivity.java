package com.example.campusbiome;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;

public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        android.util.Log.d("RoleSelection", "onCreate started");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            android.util.Log.d("RoleSelection", "User logged in, checking role: " + currentUser.getUid());
            checkRoleAndRedirect(currentUser.getUid());
            return;
        }

        android.util.Log.d("RoleSelection", "No user logged in, showing role selection UI");

        MaterialCardView cardStudent    = findViewById(R.id.cardStudent);
        MaterialCardView cardFaculty    = findViewById(R.id.cardFaculty);
        MaterialCardView cardAdmin      = findViewById(R.id.cardAdmin);
        MaterialCardView cardSocietyMgr = findViewById(R.id.cardSocietyManager);

        cardStudent.setOnClickListener(v -> navigate("student"));
        cardFaculty.setOnClickListener(v -> navigate("faculty"));
        cardAdmin.setOnClickListener(v -> navigate("admin"));
        cardSocietyMgr.setOnClickListener(v -> navigate("society_manager"));
    }

    private void checkRoleAndRedirect(String uid) {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        db.child("Users").child(uid).child("role").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String role = snapshot.getValue(String.class);
                android.util.Log.d("RoleSelection", "Role fetched: " + role);
                Intent intent;
                if ("admin".equals(role)) {
                    intent = new Intent(RoleSelectionActivity.this, AdminDashboardActivity.class);
                } else if ("student".equals(role)) {
                    intent = new Intent(RoleSelectionActivity.this, StudentDashboardActivity.class);
                } else if ("faculty".equals(role)) {
                    intent = new Intent(RoleSelectionActivity.this, FacultyDashboardActivity.class);
                } else {
                    intent = new Intent(RoleSelectionActivity.this, Dashboard.class);
                }
                intent.putExtra("role", role);
                startActivity(intent);
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("RoleSelection", "Firebase error: " + error.getMessage());
                // If it fails, default to student dashboard or stay here
                startActivity(new Intent(RoleSelectionActivity.this, StudentDashboardActivity.class));
                finish();
            }
        });
    }

    private void navigate(String role) {
        Intent intent;
        if ("student".equals(role)) {
            intent = new Intent(this, StudentLoginActivity.class);
        } else {
            intent = new Intent(this, GenericLoginActivity.class);
            intent.putExtra("role", role);
        }
        startActivity(intent);
    }
}