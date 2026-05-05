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

public class AdminAddFacultyActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPost, etDepartment;
    private MaterialCardView btnSave;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_faculty);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPost = findViewById(R.id.etPost);
        etDepartment = findViewById(R.id.etDepartment);
        btnSave = findViewById(R.id.btnSave);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> saveFaculty());
    }

    private void saveFaculty() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String post = etPost.getText().toString().trim();
        String department = etDepartment.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(post)) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String facultyId = mDatabase.child("Faculty").push().getKey();
        if (facultyId == null) return;

        Map<String, Object> faculty = new HashMap<>();
        faculty.put("id", facultyId);
        faculty.put("name", name);
        faculty.put("email", email);
        faculty.put("post", post);
        faculty.put("department", department);
        faculty.put("role", "faculty");
        faculty.put("status", "approved");

        mDatabase.child("Faculty").child(facultyId).setValue(faculty)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AdminAddFacultyActivity.this, "Faculty member added successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(AdminAddFacultyActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
