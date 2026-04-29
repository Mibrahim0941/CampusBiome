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

public class AdminAddStudentActivity extends AppCompatActivity {

    private EditText etName, etEmail, etSemester, etSection, etRollNo;
    private MaterialCardView btnSave;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_student);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etSemester = findViewById(R.id.etSemester);
        etSection = findViewById(R.id.etSection);
        etRollNo = findViewById(R.id.etRollNo);
        btnSave = findViewById(R.id.btnSave);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> saveStudent());
    }

    private void saveStudent() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String semester = etSemester.getText().toString().trim();
        String section = etSection.getText().toString().trim();
        String rollNo = etRollNo.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(rollNo)) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mDatabase.child("Users").push().getKey();
        if (userId == null) return;

        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("semester", semester);
        user.put("section", section);
        user.put("rollNo", rollNo);
        user.put("role", "student");
        user.put("status", "approved");

        mDatabase.child("Users").child(userId).setValue(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AdminAddStudentActivity.this, "Student record added successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(AdminAddStudentActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
