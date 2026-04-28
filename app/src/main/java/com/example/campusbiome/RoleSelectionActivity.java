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

        // If user is already signed in, skip straight to their dashboard.
        // You can enhance this by reading their role from Firestore.
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // TODO: redirect to appropriate dashboard based on stored role
            // For now, go to a generic MainActivity
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_role_selection);

        MaterialCardView cardStudent       = findViewById(R.id.cardStudent);
        MaterialCardView cardFaculty       = findViewById(R.id.cardFaculty);
        MaterialCardView cardAdmin         = findViewById(R.id.cardAdmin);
        MaterialCardView cardSocietyMgr    = findViewById(R.id.cardSocietyManager);

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