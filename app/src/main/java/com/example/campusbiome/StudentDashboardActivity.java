package com.example.campusbiome;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class StudentDashboardActivity extends AppCompatActivity {

    private ImageView btnLogout, btnMenu;
    private TextView tvWelcomeUser;
    private LinearLayout navHome, navMap, navTimetable, navProfessors, navCommunity;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        btnLogout = findViewById(R.id.btnLogout);
        tvWelcomeUser = findViewById(R.id.tvWelcomeUser);
        btnMenu = findViewById(R.id.btnMenu);
        navHome = findViewById(R.id.navHome);
        navMap = findViewById(R.id.navMap);
        navTimetable = findViewById(R.id.navTimetable);
        navProfessors = findViewById(R.id.navProfessors);
        navCommunity = findViewById(R.id.navCommunity);

        btnMenu.setOnClickListener(v -> {
            Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show();
        });

        navHome.setOnClickListener(v -> {
            Toast.makeText(this, "Already on Home", Toast.LENGTH_SHORT).show();
        });

        navMap.setOnClickListener(v -> {
            Toast.makeText(this, "Campus Map clicked", Toast.LENGTH_SHORT).show();
            // startActivity(new Intent(StudentDashboardActivity.this, CampusMapActivity.class));
        });

        navTimetable.setOnClickListener(v -> {
            Toast.makeText(this, "Timetable clicked", Toast.LENGTH_SHORT).show();
        });

        navProfessors.setOnClickListener(v -> {
            Toast.makeText(this, "Professors clicked", Toast.LENGTH_SHORT).show();
        });

        navCommunity.setOnClickListener(v -> {
            Toast.makeText(this, "Community clicked", Toast.LENGTH_SHORT).show();
        });

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            fetchUserName(currentUser.getUid());
        }

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(StudentDashboardActivity.this, RoleSelectionActivity.class));
            finish();
        });
    }

    private void fetchUserName(String uid) {
        mDatabase.child("Users").child(uid).child("name").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.getValue(String.class);
                    tvWelcomeUser.setText("Welcome " + name + "!");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StudentDashboardActivity.this, "Failed to load name", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
