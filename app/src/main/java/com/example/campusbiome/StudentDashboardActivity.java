package com.example.campusbiome;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

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

    // The home-screen content that lives directly in the XML (not a fragment yet)
    private View homeContent;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        mAuth     = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            goToRoleSelection();
            return;
        }

        btnLogout     = findViewById(R.id.btnLogout);
        tvWelcomeUser = findViewById(R.id.tvWelcomeUser);
        btnMenu       = findViewById(R.id.btnMenu);
        navHome       = findViewById(R.id.navHome);
        navMap        = findViewById(R.id.navMap);
        navTimetable  = findViewById(R.id.navTimetable);
        navProfessors = findViewById(R.id.navProfessors);
        navCommunity  = findViewById(R.id.navCommunity);

        // The static home content (NestedScrollView inside fragment_container in XML).
        // Fragments load on top of / replace it. On Home tap we restore it.
        homeContent = findViewById(R.id.homeContent);

        btnMenu.setOnClickListener(v ->
                Toast.makeText(this, "Menu - coming soon", Toast.LENGTH_SHORT).show());

        navHome.setOnClickListener(v -> showHome());

        navMap.setOnClickListener(v -> {
            // TODO: replace with openFragment(new CampusMapFragment());
            Toast.makeText(this, "Campus Map - coming soon", Toast.LENGTH_SHORT).show();
        });

        navTimetable.setOnClickListener(v -> {
            // TODO: replace with openFragment(new TimetableFragment());
            Toast.makeText(this, "Timetable - coming soon", Toast.LENGTH_SHORT).show();
        });

        navProfessors.setOnClickListener(v -> {
            // TODO: replace with openFragment(new ProfessorsFragment());
            Toast.makeText(this, "Professors - coming soon", Toast.LENGTH_SHORT).show();
        });

        navCommunity.setOnClickListener(v -> openFragment(new CommunityHubFragment()));

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            goToRoleSelection();
        });

        fetchUserName(currentUser.getUid());

        if (savedInstanceState == null) {
            showHome();
        }
    }

    /**
     * Shows the static home XML content and removes any loaded fragment.
     *
     * UPGRADE PATH: When you create HomeFragment, replace this whole method with:
     *     openFragment(new HomeFragment());
     * and delete the homeContent NestedScrollView from the XML.
     */
    private void showHome() {
        Fragment current = getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);
        if (current != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .remove(current)
                    .commitNow();
        }
        if (homeContent != null) homeContent.setVisibility(View.VISIBLE);
    }

    /**
     * Load any fragment into fragment_container.
     *
     * HOW TO ADD A NEW SCREEN (e.g. Campus Map):
     *   1. New > Fragment > Fragment (Blank)  ->  CampusMapFragment
     *   2. Build its layout in res/layout/fragment_campus_map.xml
     *   3. Change the navMap listener above to: openFragment(new CampusMapFragment());
     *   Done. Nothing else needs to change.
     */
    private void openFragment(Fragment fragment) {
        if (homeContent != null) homeContent.setVisibility(View.GONE);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void fetchUserName(String uid) {
        mDatabase.child("Users").child(uid).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String name = snapshot.getValue(String.class);
                            tvWelcomeUser.setText("Welcome, " + name + "!");
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(StudentDashboardActivity.this,
                                "Could not load name", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void goToRoleSelection() {
        Intent intent = new Intent(this, RoleSelectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        Fragment current = getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);
        if (current != null) {
            showHome();
        } else {
            super.onBackPressed();
        }
    }
}