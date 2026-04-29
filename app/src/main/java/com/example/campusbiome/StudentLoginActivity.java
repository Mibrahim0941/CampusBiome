package com.example.campusbiome;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class StudentLoginActivity extends AppCompatActivity {

    private TextView tabLogin, tabSignup, tvForgotPassword, tvNameLabel, tvConfirmLabel;
    private MaterialCardView cardName, cardConfirm;
    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnAction;
    private ImageView ivBack;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_login);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        tabLogin          = findViewById(R.id.tabLogin);
        tabSignup         = findViewById(R.id.tabSignup);
        tvForgotPassword  = findViewById(R.id.tvForgotPassword);
        tvNameLabel       = findViewById(R.id.tvNameLabel);
        tvConfirmLabel    = findViewById(R.id.tvConfirmLabel);
        cardName          = findViewById(R.id.cardName);
        cardConfirm       = findViewById(R.id.cardConfirm);
        etName            = findViewById(R.id.etName);
        etEmail           = findViewById(R.id.etEmail);
        etPassword        = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnAction         = findViewById(R.id.btnAction);
        ivBack            = findViewById(R.id.ivBack);

        ivBack.setOnClickListener(v -> finish());
        tabLogin.setOnClickListener(v -> switchMode(true));
        tabSignup.setOnClickListener(v -> switchMode(false));
        tvForgotPassword.setOnClickListener(v -> sendPasswordReset());

        btnAction.setOnClickListener(v -> {
            if (isLoginMode) handleLogin();
            else             handleSignup();
        });
    }

    private void switchMode(boolean loginMode) {
        isLoginMode = loginMode;
        tabLogin.setTextColor(getColor(loginMode ? android.R.color.white : R.color.teal_accent));
        tabSignup.setTextColor(getColor(loginMode ? R.color.teal_accent : android.R.color.white));
        tabLogin.setBackgroundResource(loginMode ? R.drawable.drawable_tab_selected : android.R.color.transparent);
        tabSignup.setBackgroundResource(loginMode ? android.R.color.transparent : R.drawable.drawable_tab_selected);

        int signupVisibility = loginMode ? View.GONE : View.VISIBLE;
        tvNameLabel.setVisibility(signupVisibility);
        cardName.setVisibility(signupVisibility);
        tvConfirmLabel.setVisibility(signupVisibility);
        cardConfirm.setVisibility(signupVisibility);
        tvForgotPassword.setVisibility(loginMode ? View.VISIBLE : View.GONE);

        btnAction.setText(loginMode ? "Login" : "Create Account");
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String pass  = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAction.setEnabled(false);
        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(result -> checkRoleAndNavigate(result.getUser()))
                .addOnFailureListener(e -> {
                    btnAction.setEnabled(true);
                    onAuthFailed(e.getMessage());
                });
    }

    private void checkRoleAndNavigate(FirebaseUser user) {
        if (user == null) return;
        mDatabase.child("Users").child(user.getUid()).child("role").get()
                .addOnSuccessListener(dataSnapshot -> {
                    if (dataSnapshot.exists()) {
                        String role = dataSnapshot.getValue(String.class);
                        if ("student".equals(role)) {
                            goToDashboard();
                        } else {
                            btnAction.setEnabled(true);
                            Toast.makeText(this, "Access denied: Not a student account", Toast.LENGTH_SHORT).show();
                            mAuth.signOut();
                        }
                    } else {
                        // Fallback if role is not set
                        goToDashboard();
                    }
                })
                .addOnFailureListener(e -> {
                    // Even if database fails, we might allow entry if the auth succeeded
                    goToDashboard();
                });
    }

    private void handleSignup() {
        String name    = etName.getText().toString().trim();
        String email   = etEmail.getText().toString().trim();
        String pass    = etPassword.getText().toString().trim();
        String confirm = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pass.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAction.setEnabled(false);
        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user != null) {
                        saveUserToDatabase(user.getUid(), name, email);
                    }
                })
                .addOnFailureListener(e -> {
                    btnAction.setEnabled(true);
                    onAuthFailed(e.getMessage());
                });
    }

    private void saveUserToDatabase(String uid, String name, String email) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("role", "student");

        mDatabase.child("Users").child(uid).setValue(userData)
                .addOnSuccessListener(aVoid -> {
                    // Also update Auth profile name
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build();
                        user.updateProfile(profileUpdates);
                    }
                    goToGettingStarted();
                })
                .addOnFailureListener(e -> {
                    btnAction.setEnabled(true);
                    Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendPasswordReset() {
        String email = etEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Enter email", Toast.LENGTH_SHORT).show();
            return;
        }
        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(v -> Toast.makeText(this, "Reset email sent", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void goToDashboard() {
        Intent intent = new Intent(this, StudentDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void goToGettingStarted() {
        Intent intent = new Intent(this, StudentGettingStartedActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void onAuthFailed(String message) {
        Toast.makeText(this, "Authentication failed: " + message, Toast.LENGTH_LONG).show();
    }
}