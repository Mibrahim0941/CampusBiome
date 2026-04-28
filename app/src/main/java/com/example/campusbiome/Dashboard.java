package com.example.campusbiome;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Dashboard extends AppCompatActivity {

    private TextView tvWelcome, tvUserEmail;
    private Button btnLogout;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        // If no user is logged in, force go back to Login
        if (user == null) {
            startActivity(new Intent(this, StudentLoginActivity.class));
            finish();
            return;
        }

        tvWelcome = findViewById(R.id.tvWelcome);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        btnLogout = findViewById(R.id.btnLogout);

        // Display user info (using DisplayName set during signup or Email)
        String name = user.getDisplayName();
        if (name != null && !name.isEmpty()) {
            tvWelcome.setText("Welcome, " + name + "!");
        }
        tvUserEmail.setText(user.getEmail());

        btnLogout.setOnClickListener(v -> {
            // 1. Sign out from Firebase
            mAuth.signOut();

            // 2. Clear activity stack and go to Login
            Intent intent = new Intent(Dashboard.this, RoleSelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}