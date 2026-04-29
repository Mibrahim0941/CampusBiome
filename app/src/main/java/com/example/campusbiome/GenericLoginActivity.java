package com.example.campusbiome;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
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

public class GenericLoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvTitle, tvForgotPassword;
    private ImageView ivBack;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private String role; // The role this login screen is for

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generic_login);

        mAuth    = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        role = getIntent().getStringExtra("role");
        if (role == null) role = "faculty";

        tvTitle          = findViewById(R.id.tvTitle);
        etEmail          = findViewById(R.id.etEmail);
        etPassword       = findViewById(R.id.etPassword);
        btnLogin         = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        ivBack           = findViewById(R.id.ivBack);

        tvTitle.setText(rolePrettyName(role) + " Login");
        ivBack.setOnClickListener(v -> finish());
        tvForgotPassword.setOnClickListener(v -> sendPasswordReset());
        btnLogin.setOnClickListener(v -> handleLogin());
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String pass  = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user != null) checkUserRole(user.getUid());
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    onFailed(e.getMessage());
                });
    }

    private void checkUserRole(String uid) {
        mDatabase.child("Users").child(uid).child("role")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        btnLogin.setEnabled(true);
                        String actualRole = snapshot.getValue(String.class);

                        if (actualRole != null && actualRole.equals(role)) {
                            goToDashboard();
                        } else {
                            mAuth.signOut();
                            Toast.makeText(GenericLoginActivity.this,
                                    "Access Denied: You are not registered as "
                                            + rolePrettyName(role),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        btnLogin.setEnabled(true);
                        onFailed(error.getMessage());
                    }
                });
    }

    private void sendPasswordReset() {
        String email = etEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Enter your email above first", Toast.LENGTH_SHORT).show();
            return;
        }
        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(v ->
                        Toast.makeText(this, "Reset email sent", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void goToDashboard() {
        // TODO: As you build Faculty/Admin/SocietyManager dashboards,
        //       add cases here:
        //         case "faculty":  intent = new Intent(this, FacultyDashboardActivity.class); break;
        //         case "admin":    intent = new Intent(this, AdminDashboardActivity.class);   break;
        //       For now everyone lands on the generic Dashboard.
        Intent intent = new Intent(this, Dashboard.class);
        intent.putExtra("role", role);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void onFailed(String message) {
        Toast.makeText(this, "Authentication failed: " + message, Toast.LENGTH_LONG).show();
    }

    private String rolePrettyName(String r) {
        switch (r) {
            case "faculty":         return "Faculty";
            case "admin":           return "Admin";
            case "society_manager": return "Society Manager";
            default:                return "User";
        }
    }
}