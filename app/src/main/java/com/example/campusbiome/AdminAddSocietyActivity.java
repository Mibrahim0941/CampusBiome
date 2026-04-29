package com.example.campusbiome;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class AdminAddSocietyActivity extends AppCompatActivity {

    private EditText etSocietyName, etCategory, etAdminName, etAdminEmail;
    private MaterialCardView btnSave;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_society);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        etSocietyName = findViewById(R.id.etSocietyName);
        etCategory = findViewById(R.id.etCategory);
        etAdminName = findViewById(R.id.etAdminName);
        etAdminEmail = findViewById(R.id.etAdminEmail);
        btnSave = findViewById(R.id.btnSave);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> saveSociety());
    }

    private void saveSociety() {
        String societyName = etSocietyName.getText().toString().trim();
        String category = etCategory.getText().toString().trim();
        String adminName = etAdminName.getText().toString().trim();
        String adminEmail = etAdminEmail.getText().toString().trim();

        if (TextUtils.isEmpty(societyName) || TextUtils.isEmpty(adminName) || TextUtils.isEmpty(adminEmail)) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String societyId = mDatabase.child("Societies").push().getKey();
        String userId = mDatabase.child("Users").push().getKey();
        if (societyId == null || userId == null) return;

        Map<String, Object> society = new HashMap<>();
        society.put("name", societyName);
        society.put("category", category);
        society.put("adminName", adminName);
        society.put("adminEmail", adminEmail);
        society.put("status", "approved");
        society.put("proposedBy", "Admin");

        Map<String, Object> user = new HashMap<>();
        user.put("name", adminName);
        user.put("email", adminEmail);
        user.put("role", "society_admin");
        user.put("society", societyName);
        user.put("status", "approved");

        Map<String, Object> updates = new HashMap<>();
        updates.put("/Societies/" + societyId, society);
        updates.put("/Users/" + userId, user);

        mDatabase.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AdminAddSocietyActivity.this, "Society and Admin registered successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(AdminAddSocietyActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
