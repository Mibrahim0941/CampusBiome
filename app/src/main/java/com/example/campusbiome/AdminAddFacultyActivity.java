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

        // Initialize Secondary Firebase for Auth creation with a UNIQUE name per request
        com.google.firebase.FirebaseOptions options = com.google.firebase.FirebaseApp.getInstance().getOptions();
        String appName = "Secondary_" + System.currentTimeMillis();
        com.google.firebase.FirebaseApp tempApp;
        try {
            tempApp = com.google.firebase.FirebaseApp.initializeApp(this, options, appName);
        } catch (Exception e) {
            tempApp = com.google.firebase.FirebaseApp.getInstance(appName);
        }
        final com.google.firebase.FirebaseApp secondaryApp = tempApp;

        com.google.firebase.auth.FirebaseAuth secondaryAuth = com.google.firebase.auth.FirebaseAuth.getInstance(secondaryApp);

        secondaryAuth.createUserWithEmailAndPassword(email, "123456")
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    
                    // 1. Write to Users node
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("id", uid);
                    userData.put("name", name);
                    userData.put("email", email);
                    userData.put("role", "faculty");
                    userData.put("status", "approved");

                    mDatabase.child("Users").child(uid).setValue(userData);

                    // 2. Write to Faculty node
                    Map<String, Object> facultyData = new HashMap<>();
                    facultyData.put("id", uid);
                    facultyData.put("name", name);
                    facultyData.put("email", email);
                    facultyData.put("post", post);
                    facultyData.put("department", department);
                    facultyData.put("role", "faculty");
                    facultyData.put("status", "approved");

                    mDatabase.child("Faculty").child(uid).setValue(facultyData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(AdminAddFacultyActivity.this, "Faculty account created with password: 123456", Toast.LENGTH_LONG).show();
                                secondaryApp.delete();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(AdminAddFacultyActivity.this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                secondaryApp.delete();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AdminAddFacultyActivity.this, "Auth Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    secondaryApp.delete();
                });
    }
}
