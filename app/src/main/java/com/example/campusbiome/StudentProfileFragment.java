package com.example.campusbiome;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class StudentProfileFragment extends Fragment {

    private ImageView btnBack, btnEdit;
    private TextView tvProfileName, tvProfileEmail, tvProfilePhone;
    private TextView tvProfileDegree, tvProfileProgram, tvProfileSemester;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_profile, container, false);

        btnBack = view.findViewById(R.id.btnBack);
        btnEdit = view.findViewById(R.id.btnEdit);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvProfilePhone = view.findViewById(R.id.tvProfilePhone);
        tvProfileDegree = view.findViewById(R.id.tvProfileDegree);
        tvProfileProgram = view.findViewById(R.id.tvProfileProgram);
        tvProfileSemester = view.findViewById(R.id.tvProfileSemester);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                ((StudentDashboardActivity) getActivity()).findViewById(R.id.navHome).performClick();
            }
        });

        btnEdit.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new EditStudentProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });

        fetchProfileData();

        return view;
    }

    private void fetchProfileData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        tvProfileEmail.setText(user.getEmail());

        mDatabase.child("Users").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                
                String name = snapshot.child("name").getValue(String.class);
                String phone = snapshot.child("phone").getValue(String.class);
                String degree = snapshot.child("Degree").getValue(String.class);
                String program = snapshot.child("Program").getValue(String.class);
                Integer semester = snapshot.child("Semester").getValue(Integer.class);
                String section = snapshot.child("Section").getValue(String.class);

                tvProfileName.setText(name != null ? name : "Student");
                tvProfilePhone.setText(phone != null && !phone.isEmpty() ? phone : "Not provided");
                tvProfileDegree.setText(degree != null ? degree : "-");
                tvProfileProgram.setText(program != null ? program : "-");
                
                if (semester != null && section != null) {
                    tvProfileSemester.setText("Semester " + semester + ", Section " + section);
                } else {
                    tvProfileSemester.setText("-");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
