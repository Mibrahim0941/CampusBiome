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
import android.graphics.Color;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminStudentsActivity extends AppCompatActivity {

    private ImageView btnBack;
    private LinearLayout llStudentList;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_students);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        btnBack = findViewById(R.id.btnBack);
        llStudentList = findViewById(R.id.llStudentList);

        btnBack.setOnClickListener(v -> finish());

        fetchStudents();
    }

    private void fetchStudents() {
        mDatabase.child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                llStudentList.removeAllViews();
                java.util.List<DataSnapshot> activeStudents = new java.util.ArrayList<>();
                java.util.List<DataSnapshot> disabledStudents = new java.util.ArrayList<>();

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String role = userSnapshot.child("role").getValue(String.class);
                    if (!"student".equals(role)) continue;

                    String status = userSnapshot.child("accountStatus").getValue(String.class);
                    if ("disabled".equals(status)) {
                        disabledStudents.add(userSnapshot);
                    } else {
                        activeStudents.add(userSnapshot);
                    }
                }

                addStudentsToLayout(activeStudents, false);
                addStudentsToLayout(disabledStudents, true);
            }

            private void addStudentsToLayout(java.util.List<DataSnapshot> students, boolean isDisabled) {
                LayoutInflater inflater = LayoutInflater.from(AdminStudentsActivity.this);
                for (DataSnapshot userSnapshot : students) {
                    String name = getString(userSnapshot, "name", "Name", "fullname");
                    String semester = getString(userSnapshot, "semester", "Semester", "sem", "Sem");
                    String rollNo = getString(userSnapshot, "rollNo", "RollNo", "rollno");
                    String section = getString(userSnapshot, "section", "Section", "sec", "Sec");

                    View row = inflater.inflate(R.layout.item_admin_student_row, llStudentList, false);
                    TextView tvName = row.findViewById(R.id.tvName);
                    TextView tvSem = row.findViewById(R.id.tvSemester);
                    TextView tvRoll = row.findViewById(R.id.tvRollNo);
                    TextView tvSec = row.findViewById(R.id.tvSection);

                    tvName.setText(name);
                    tvSem.setText(semester);
                    tvRoll.setText(rollNo);
                    tvSec.setText(section);

                    if (isDisabled) {
                        row.setBackgroundColor(android.graphics.Color.parseColor("#FFEBEE")); // Light Red
                        tvName.setTextColor(android.graphics.Color.RED);
                    }

                    row.setOnClickListener(v -> showStudentDetailsDialog(userSnapshot));
                    llStudentList.addView(row);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
                    Toast.makeText(AdminStudentsActivity.this, "Error fetching students", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showStudentDetailsDialog(DataSnapshot userSnapshot) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_admin_details, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        LinearLayout llContainer = dialogView.findViewById(R.id.llDetailsContainer);
        com.google.android.material.button.MaterialButton btnAction = dialogView.findViewById(R.id.btnPrimaryAction);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        tvTitle.setText("Student Profile");

        String status = userSnapshot.child("accountStatus").getValue(String.class);
        boolean isDisabled = "disabled".equals(status);

        if (isDisabled) {
            btnAction.setText("Enable Account");
        } else {
            btnAction.setText("Disable Account");
        }

        addDetailRow(llContainer, "Name", getString(userSnapshot, "name", "Name", "fullname"));
        addDetailRow(llContainer, "Email", getString(userSnapshot, "email", "Email"));
        addDetailRow(llContainer, "Roll No", getString(userSnapshot, "rollNo", "RollNo", "roll", "Roll_No"));
        addDetailRow(llContainer, "Semester", getString(userSnapshot, "semester", "Semester", "sem"));
        addDetailRow(llContainer, "Section", getString(userSnapshot, "section", "Section", "sec"));

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAction.setOnClickListener(v -> {
            String uid = userSnapshot.getKey();
            if (uid != null) {
                if (isDisabled) {
                    // Re-enable account
                    mDatabase.child("Users").child(uid).child("accountStatus").removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(AdminStudentsActivity.this, "Account Enabled Successfully", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                fetchStudents();
                            })
                            .addOnFailureListener(e -> Toast.makeText(AdminStudentsActivity.this, "Failed to enable account: " + e.getMessage(), Toast.LENGTH_LONG).show());
                } else {
                    // Soft Disable: Just set accountStatus = "disabled"
                    mDatabase.child("Users").child(uid).child("accountStatus").setValue("disabled")
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(AdminStudentsActivity.this, "Account Disabled Successfully", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                fetchStudents();
                            })
                            .addOnFailureListener(e -> Toast.makeText(AdminStudentsActivity.this, "Failed to disable account: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            } else {
                Toast.makeText(AdminStudentsActivity.this, "Error: UID is null", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private String getString(DataSnapshot snapshot, String... keys) {
        for (String key : keys) {
            Object val = snapshot.child(key).getValue();
            if (val != null) return String.valueOf(val);
        }
        return "null";
    }

    private void addDetailRow(LinearLayout container, String label, String value) {
        TextView tv = new TextView(this);
        tv.setText(label + ": " + (value != null ? value : "null"));
        tv.setTextSize(16);
        tv.setTextColor(Color.parseColor("#191C1D"));
        tv.setPadding(0, 0, 0, 20);
        container.addView(tv);
    }
}
