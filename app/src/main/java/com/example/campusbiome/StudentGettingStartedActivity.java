package com.example.campusbiome;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class StudentGettingStartedActivity extends AppCompatActivity {

    private Spinner spinnerDegree, spinnerProgram, spinnerSemester, spinnerSection;
    private Button btnContinue;
    private ProgressBar progressBar;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    private ArrayAdapter<String> degreeAdapter, programAdapter, semesterAdapter, sectionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_getting_started);

        spinnerDegree = findViewById(R.id.spinnerDegree);
        spinnerProgram = findViewById(R.id.spinnerProgram);
        spinnerSemester = findViewById(R.id.spinnerSemester);
        spinnerSection = findViewById(R.id.spinnerSection);
        btnContinue = findViewById(R.id.btnContinue);
        progressBar = findViewById(R.id.progressBar);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        setupAdapters();
        setupListeners();
        fetchDegrees();

        btnContinue.setOnClickListener(v -> saveAndContinue());
    }

    private void setupAdapters() {
        degreeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        degreeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDegree.setAdapter(degreeAdapter);

        programAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        programAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProgram.setAdapter(programAdapter);

        semesterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        semesterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSemester.setAdapter(semesterAdapter);

        sectionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSection.setAdapter(sectionAdapter);
    }

    private void setupListeners() {
        spinnerDegree.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    fetchPrograms((String) parent.getItemAtPosition(position));
                } else {
                    resetSpinner(spinnerProgram, programAdapter, "Select Program");
                    resetSpinner(spinnerSemester, semesterAdapter, "Select Semester");
                    resetSpinner(spinnerSection, sectionAdapter, "Select Section");
                    btnContinue.setEnabled(false);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerProgram.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    String degree = (String) spinnerDegree.getSelectedItem();
                    fetchSemesters(degree, (String) parent.getItemAtPosition(position));
                } else {
                    resetSpinner(spinnerSemester, semesterAdapter, "Select Semester");
                    resetSpinner(spinnerSection, sectionAdapter, "Select Section");
                    btnContinue.setEnabled(false);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerSemester.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    String degree = (String) spinnerDegree.getSelectedItem();
                    String program = (String) spinnerProgram.getSelectedItem();
                    fetchSections(degree, program, (String) parent.getItemAtPosition(position));
                } else {
                    resetSpinner(spinnerSection, sectionAdapter, "Select Section");
                    btnContinue.setEnabled(false);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerSection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                btnContinue.setEnabled(position > 0);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void resetSpinner(Spinner spinner, ArrayAdapter<String> adapter, String placeholder) {
        adapter.clear();
        adapter.add(placeholder);
        spinner.setSelection(0);
        spinner.setEnabled(false);
    }

    private void fetchDegrees() {
        progressBar.setVisibility(View.VISIBLE);
        resetSpinner(spinnerDegree, degreeAdapter, "Select Degree");
        resetSpinner(spinnerProgram, programAdapter, "Select Program");
        resetSpinner(spinnerSemester, semesterAdapter, "Select Semester");
        resetSpinner(spinnerSection, sectionAdapter, "Select Section");
        
        mDatabase.child("Timetable").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                for (DataSnapshot degreeSnap : snapshot.getChildren()) {
                    if (degreeSnap.hasChildren()) {
                        degreeAdapter.add(degreeSnap.getKey());
                    }
                }
                spinnerDegree.setEnabled(degreeAdapter.getCount() > 1);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { progressBar.setVisibility(View.GONE); }
        });
    }

    private void fetchPrograms(String degree) {
        progressBar.setVisibility(View.VISIBLE);
        resetSpinner(spinnerProgram, programAdapter, "Select Program");
        resetSpinner(spinnerSemester, semesterAdapter, "Select Semester");
        resetSpinner(spinnerSection, sectionAdapter, "Select Section");
        
        mDatabase.child("Timetable").child(degree).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                for (DataSnapshot programSnap : snapshot.getChildren()) {
                    programAdapter.add(programSnap.getKey());
                }
                spinnerProgram.setEnabled(programAdapter.getCount() > 1);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { progressBar.setVisibility(View.GONE); }
        });
    }

    private void fetchSemesters(String degree, String program) {
        progressBar.setVisibility(View.VISIBLE);
        resetSpinner(spinnerSemester, semesterAdapter, "Select Semester");
        resetSpinner(spinnerSection, sectionAdapter, "Select Section");
        
        mDatabase.child("Timetable").child(degree).child(program).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                for (DataSnapshot semSnap : snapshot.getChildren()) {
                    semesterAdapter.add(semSnap.getKey()); // "Semester_6"
                }
                spinnerSemester.setEnabled(semesterAdapter.getCount() > 1);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { progressBar.setVisibility(View.GONE); }
        });
    }

    private void fetchSections(String degree, String program, String semester) {
        progressBar.setVisibility(View.VISIBLE);
        resetSpinner(spinnerSection, sectionAdapter, "Select Section");
        
        mDatabase.child("Timetable").child(degree).child(program).child(semester).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                for (DataSnapshot secSnap : snapshot.getChildren()) {
                    sectionAdapter.add(secSnap.getKey()); // "BCS-6G"
                }
                spinnerSection.setEnabled(sectionAdapter.getCount() > 1);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { progressBar.setVisibility(View.GONE); }
        });
    }

    private void saveAndContinue() {
        if (mAuth.getCurrentUser() == null) return;
        
        String degree = (String) spinnerDegree.getSelectedItem();
        String program = (String) spinnerProgram.getSelectedItem();
        String semesterStr = (String) spinnerSemester.getSelectedItem();
        String section = (String) spinnerSection.getSelectedItem();

        // Extract semester integer (e.g. "Semester_6" -> 6)
        int semester = 0;
        try {
            semester = Integer.parseInt(semesterStr.replace("Semester_", ""));
        } catch (Exception e) {
            e.printStackTrace();
        }

        btnContinue.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        DatabaseReference userRef = mDatabase.child("Users").child(mAuth.getCurrentUser().getUid());
        userRef.child("Degree").setValue(degree);
        userRef.child("Program").setValue(program);
        userRef.child("Semester").setValue(semester);
        userRef.child("Section").setValue(section).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                Intent intent = new Intent(this, StudentDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Failed to save profile.", Toast.LENGTH_SHORT).show();
                btnContinue.setEnabled(true);
            }
        });
    }
}
