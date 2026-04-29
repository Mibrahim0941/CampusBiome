package com.example.campusbiome;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminStudentsActivity extends AppCompatActivity {

    private ImageView btnBack, btnLogout;
    private LinearLayout llStudentList, llSocietyRolesList, llSocietyAdminList;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_students);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        btnBack = findViewById(R.id.btnBack);
        btnLogout = findViewById(R.id.btnLogout);
        llStudentList = findViewById(R.id.llStudentList);
        llSocietyRolesList = findViewById(R.id.llSocietyRolesList);
        llSocietyAdminList = findViewById(R.id.llSocietyAdminList);

        btnBack.setOnClickListener(v -> finish());
        btnLogout.setOnClickListener(v -> {
            // Logout logic
            finish();
        });

        findViewById(R.id.btnAddStudent).setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(AdminStudentsActivity.this, AdminAddStudentActivity.class);
            startActivity(intent);
        });

        fetchStudents();
    }

    private void fetchStudents() {
        mDatabase.child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                llStudentList.removeAllViews();
                llSocietyRolesList.removeAllViews();
                llSocietyAdminList.removeAllViews();
                
                LayoutInflater inflater = LayoutInflater.from(AdminStudentsActivity.this);

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String role = userSnapshot.child("role").getValue(String.class);
                    String name = userSnapshot.child("name").getValue(String.class);
                    
                    if ("student".equals(role)) {
                        String semester = userSnapshot.child("semester").getValue(String.class);
                        String rollNo = userSnapshot.child("rollNo").getValue(String.class);
                        String section = userSnapshot.child("section").getValue(String.class);
                        String society = userSnapshot.child("society").getValue(String.class);
                        String societyRole = userSnapshot.child("societyRole").getValue(String.class);

                        // Row for first table
                        View row1 = inflater.inflate(R.layout.item_admin_student_row, llStudentList, false);
                        ((TextView) row1.findViewById(R.id.tvName)).setText(name != null ? name : "N/A");
                        ((TextView) row1.findViewById(R.id.tvSemester)).setText(semester != null ? semester : "N/A");
                        ((TextView) row1.findViewById(R.id.tvRollNo)).setText(rollNo != null ? rollNo : "N/A");
                        ((TextView) row1.findViewById(R.id.tvSection)).setText(section != null ? section : "N/A");
                        llStudentList.addView(row1);

                        // Row for second table (if they have a society role)
                        if (society != null && !society.isEmpty()) {
                            View row2 = inflater.inflate(R.layout.item_admin_student_row, llSocietyRolesList, false);
                            ((TextView) row2.findViewById(R.id.tvName)).setText(name != null ? name : "N/A");
                            ((TextView) row2.findViewById(R.id.tvSemester)).setText(rollNo != null ? rollNo : "N/A"); 
                            ((TextView) row2.findViewById(R.id.tvRollNo)).setText(society != null ? society : "N/A"); 
                            ((TextView) row2.findViewById(R.id.tvSection)).setText(societyRole != null ? societyRole : "N/A"); 
                            llSocietyRolesList.addView(row2);
                        }
                    } else if ("society_admin".equals(role)) {
                        String society = userSnapshot.child("society").getValue(String.class);
                        View row = inflater.inflate(R.layout.item_admin_faculty_row, llSocietyAdminList, false);
                        ((TextView) row.findViewById(R.id.tvName)).setText(name != null ? name : "N/A");
                        ((TextView) row.findViewById(R.id.tvPost)).setText(society != null ? society : "Society Admin");
                        llSocietyAdminList.addView(row);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminStudentsActivity.this, "Error fetching students", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
