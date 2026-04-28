package com.example.campusbiome;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class GenericLoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvTitle, tvForgotPassword;
    private ImageView ivBack;

    private FirebaseAuth mAuth;

    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generic_login);

        mAuth = FirebaseAuth.getInstance();

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
                    btnLogin.setEnabled(true);
                    // Without Firestore, we assume anyone with a valid account
                    // can log into the role they selected.
                    goToDashboard();
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    onFailed(e.getMessage());
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
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("role", role);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void onFailed(String message) {
        Toast.makeText(this, "Authentication failed: " + message, Toast.LENGTH_LONG).show();
    }

    private String rolePrettyName(String r) {
        switch (r) {
            case "faculty":        return "Faculty";
            case "admin":          return "Admin";
            case "society_manager":return "Society Manager";
            default:               return "User";
        }
    }
}