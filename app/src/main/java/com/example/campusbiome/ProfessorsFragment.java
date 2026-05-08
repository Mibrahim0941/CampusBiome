package com.example.campusbiome;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ProfessorsFragment extends Fragment {

    private RecyclerView rvProfessors;
    private ProfessorAdapter adapter;
    private EditText etSearch;
    private DatabaseReference mDatabase;
    private List<DataSnapshot> allFacultyList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_professors, container, false);

        rvProfessors = view.findViewById(R.id.rvProfessors);
        rvProfessors.setLayoutManager(new LinearLayoutManager(getContext()));
        etSearch = view.findViewById(R.id.etSearch);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        adapter = new ProfessorAdapter(new ArrayList<>(), new ProfessorAdapter.OnProfessorClickListener() {
            @Override
            public void onViewDetails(DataSnapshot faculty) {
                ProfessorDetailsFragment fragment = new ProfessorDetailsFragment();
                Bundle args = new Bundle();
                args.putString("facultyId", faculty.getKey());
                fragment.setArguments(args);

                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void onBookSlot(DataSnapshot faculty) {
                BookSlotFragment fragment = new BookSlotFragment();
                Bundle args = new Bundle();
                args.putString("facultyId", faculty.getKey());
                fragment.setArguments(args);

                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
        rvProfessors.setAdapter(adapter);

        fetchFaculty();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void fetchFaculty() {
        mDatabase.child("Faculty").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                allFacultyList.clear();

                for (DataSnapshot facultySnapshot : snapshot.getChildren()) {
                    String status = facultySnapshot.child("accountStatus").getValue(String.class);
                    String role = facultySnapshot.child("role").getValue(String.class);
                    String accStatus = facultySnapshot.child("status").getValue(String.class);
                    
                    // Display approved faculty that are not suspended
                    if (!"suspended".equals(status) && "faculty".equals(role) && "approved".equals(accStatus)) {
                        allFacultyList.add(facultySnapshot);
                    } else if (!"suspended".equals(status) && facultySnapshot.hasChild("officeHours")) {
                        // Some faculty might just exist without specific role strings if imported differently,
                        // so checking officeHours or Available fields works too.
                        allFacultyList.add(facultySnapshot);
                    }
                }
                adapter.updateData(allFacultyList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Error fetching faculty", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void filter(String text) {
        if (text.isEmpty()) {
            adapter.updateData(allFacultyList);
            return;
        }
        List<DataSnapshot> filteredList = new ArrayList<>();
        for (DataSnapshot item : allFacultyList) {
            String name = item.child("name").getValue(String.class);
            String dept = item.child("department").getValue(String.class);
            if ((name != null && name.toLowerCase().contains(text.toLowerCase())) ||
                (dept != null && dept.toLowerCase().contains(text.toLowerCase()))) {
                filteredList.add(item);
            }
        }
        adapter.updateData(filteredList);
    }
}
