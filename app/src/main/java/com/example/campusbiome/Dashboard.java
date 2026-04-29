package com.example.campusbiome;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
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

/**
 * Generic dashboard for non-student roles (Faculty, Admin, Society Manager).
 *
 * ERRORS THAT WERE FIXED:
 *   - R.layout.activity_dashboard  → does not exist. Reuses activity_student_dashboard
 *     which already has tvWelcomeUser and btnLogout.
 *   - R.id.tvUserEmail              → does not exist in any layout. Removed.
 *
 * TODO: When you build a proper Faculty/Admin UI, create activity_dashboard.xml
 *       with whatever views you need, then restore setContentView(R.layout.activity_dashboard).
 */
public class Dashboard extends AppCompatActivity {

    private TextView tvWelcomeUser;
    private ImageView btnLogout;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Reusing the student dashboard layout temporarily.
        // It has tvWelcomeUser and btnLogout which is all we need right now.
        setContentView(R.layout.activity_student_dashboard);

        mAuth     = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, RoleSelectionActivity.class));
            finish();
            return;
        }

        tvWelcomeUser = findViewById(R.id.tvWelcomeUser);
        btnLogout     = findViewById(R.id.btnLogout);

        // Get the role that was passed from GenericLoginActivity
        String role = getIntent().getStringExtra("role");
        if (role == null) role = "user";

        // Show name from Firebase, fall back to email
        fetchUserName(user.getUid(), role, user.getEmail());

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(Dashboard.this, RoleSelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void fetchUserName(String uid, String role, String fallbackEmail) {
        mDatabase.child("Users").child(uid).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.exists()
                                ? snapshot.getValue(String.class)
                                : fallbackEmail;
                        tvWelcomeUser.setText("Welcome, " + name + "!");
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(Dashboard.this,
                                "Could not load name", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}