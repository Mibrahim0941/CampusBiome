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

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import android.content.res.ColorStateList;
import android.graphics.Color;
import com.google.firebase.database.ValueEventListener;

public class AdminDashboardActivity extends AppCompatActivity {

    private ImageView btnLogout, btnMenu;
    private TextView tvWelcomeUser;
    private LinearLayout navHome, navProfessors, navSocieties;
    private MaterialCardView navHomeCard, navProfessorsCard, navSocietiesCard;
    private ImageView navHomeIcon, navProfessorsIcon, navSocietiesIcon;
    private TextView navHomeText, navProfessorsText, navSocietiesText;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            goToRoleSelection();
            return;
        }

        btnLogout = findViewById(R.id.btnLogout);
        tvWelcomeUser = findViewById(R.id.tvWelcomeUser);
        ImageView btnNotifications = findViewById(R.id.btnNotifications);

        navHome = findViewById(R.id.navHome);
        navProfessors = findViewById(R.id.navProfessors);
        navSocieties = findViewById(R.id.navSocieties);

        navHomeCard = findViewById(R.id.navHomeCard);
        navProfessorsCard = findViewById(R.id.navProfessorsCard);
        navSocietiesCard = findViewById(R.id.navSocietiesCard);

        navHomeIcon = findViewById(R.id.navHomeIcon);
        navProfessorsIcon = findViewById(R.id.navProfessorsIcon);
        navSocietiesIcon = findViewById(R.id.navSocietiesIcon);

        navHomeText = findViewById(R.id.navHomeText);
        navProfessorsText = findViewById(R.id.navProfessorsText);
        navSocietiesText = findViewById(R.id.navSocietiesText);

        navHome.setOnClickListener(v -> showDashboard());
        navProfessors.setOnClickListener(v -> switchToProfessors());
        navSocieties.setOnClickListener(v -> switchToSocieties());

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            goToRoleSelection();
        });

        btnNotifications.setOnClickListener(v -> Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show());

        fetchAdminName(currentUser.getUid());

        if (savedInstanceState == null) {
            showDashboard();
        }
    }

    private void showDashboard() {
        updateBottomNav(0);
        openFragment(new AdminDashboardFragment());
    }

    public void switchToProfessors() {
        updateBottomNav(1);
        openFragment(new AdminProfessorsFragment());
    }

    public void switchToSocieties() {
        updateBottomNav(2);
        openFragment(new AdminSocietiesFragment());
    }

    private void updateBottomNav(int position) {
        // Reset all
        navHomeCard.setCardBackgroundColor(Color.TRANSPARENT);
        navProfessorsCard.setCardBackgroundColor(Color.TRANSPARENT);
        navSocietiesCard.setCardBackgroundColor(Color.TRANSPARENT);

        // Highlight selected
        int activeColor = Color.parseColor("#153838");
        int activeBg = Color.parseColor("#E8F0EE");
        int inactiveColor = Color.parseColor("#717878");

        navHomeIcon.setImageTintList(ColorStateList.valueOf(inactiveColor));
        navProfessorsIcon.setImageTintList(ColorStateList.valueOf(inactiveColor));
        navSocietiesIcon.setImageTintList(ColorStateList.valueOf(inactiveColor));

        navHomeText.setTextColor(inactiveColor);
        navProfessorsText.setTextColor(inactiveColor);
        navSocietiesText.setTextColor(inactiveColor);

        if (position == 0) {
            navHomeCard.setCardBackgroundColor(activeBg);
            navHomeIcon.setImageTintList(ColorStateList.valueOf(activeColor));
            navHomeText.setTextColor(activeColor);
        } else if (position == 1) {
            navProfessorsCard.setCardBackgroundColor(activeBg);
            navProfessorsIcon.setImageTintList(ColorStateList.valueOf(activeColor));
            navProfessorsText.setTextColor(activeColor);
        } else if (position == 2) {
            navSocietiesCard.setCardBackgroundColor(activeBg);
            navSocietiesIcon.setImageTintList(ColorStateList.valueOf(activeColor));
            navSocietiesText.setTextColor(activeColor);
        }
    }

    private void openFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void fetchAdminName(String uid) {
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
                    public void onCancelled(@NonNull DatabaseError error) {}
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
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (current != null && !(current instanceof AdminDashboardFragment)) {
            showDashboard();
        } else {
            super.onBackPressed();
        }
    }
}
