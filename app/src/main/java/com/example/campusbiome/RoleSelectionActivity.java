package com.example.campusbiome;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // BUG FIX: Was calling startActivity(RoleSelectionActivity) → infinite loop.
        // Now redirects to StudentDashboardActivity (most common case).
        // TODO: When you add role-based dashboards, read role from Firebase here
        //       and route to the correct dashboard (Faculty, Admin, etc.)
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(this, StudentDashboardActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_role_selection);

        MaterialCardView cardStudent    = findViewById(R.id.cardStudent);
        MaterialCardView cardFaculty    = findViewById(R.id.cardFaculty);
        MaterialCardView cardAdmin      = findViewById(R.id.cardAdmin);
        MaterialCardView cardSocietyMgr = findViewById(R.id.cardSocietyManager);

        cardStudent.setOnClickListener(v -> navigate("student"));
        cardFaculty.setOnClickListener(v -> navigate("faculty"));
        cardAdmin.setOnClickListener(v -> navigate("admin"));
        cardSocietyMgr.setOnClickListener(v -> navigate("society_manager"));
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