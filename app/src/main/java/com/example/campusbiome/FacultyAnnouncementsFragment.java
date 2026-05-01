package com.example.campusbiome;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FacultyAnnouncementsFragment extends Fragment {

    private RecyclerView rvAnnouncements;
    private MaterialButton btnNewAnnouncement;
    private FacultyAnnouncementAdapter adapter;
    private List<FacultyAnnouncement> announcementList;
    private DatabaseReference dbRef;
    private String currentUid;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_faculty_announcements, container, false);

        rvAnnouncements = view.findViewById(R.id.rvAnnouncements);
        btnNewAnnouncement = view.findViewById(R.id.btnNewAnnouncement);
        
        rvAnnouncements.setLayoutManager(new LinearLayoutManager(getContext()));
        announcementList = new ArrayList<>();
        adapter = new FacultyAnnouncementAdapter(announcementList);
        rvAnnouncements.setAdapter(adapter);

        btnNewAnnouncement.setOnClickListener(v -> showMakeAnnouncementDialog());

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            // Note: Schema stores all announcements in "FacultyAnnouncement". 
            // We may need to filter by faculty ID if we only want this faculty's announcements,
            // but the provided schema doesn't have a facultyId field in announcements.
            // We'll just fetch all for now, as per standard implementation.
            dbRef = FirebaseDatabase.getInstance().getReference("FacultyAnnouncement");
            fetchAnnouncements();
        }

        return view;
    }

    private void fetchAnnouncements() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                announcementList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    FacultyAnnouncement ann = ds.getValue(FacultyAnnouncement.class);
                    if (ann != null) {
                        ann.setId(ds.getKey());
                        announcementList.add(ann);
                    }
                }
                // Sort by date descending (assuming string dates format yyyy-MM-dd HH:mm allows basic string sorting)
                Collections.sort(announcementList, (a1, a2) -> {
                    if (a1.getCreatedAt() == null || a2.getCreatedAt() == null) return 0;
                    return a2.getCreatedAt().compareTo(a1.getCreatedAt());
                });
                
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FacultyAnnouncements", "Failed to load", error.toException());
            }
        });
    }

    private void showMakeAnnouncementDialog() {
        if (getContext() == null) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_new_announcement, null);
        builder.setView(view);
        
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        EditText etTitle = view.findViewById(R.id.etTitle);
        EditText etDesc = view.findViewById(R.id.etDesc);
        View btnCancel = view.findViewById(R.id.btnCancel);
        View btnPost = view.findViewById(R.id.btnPost);
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnPost.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();
            
            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(desc)) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
            
            String key = dbRef.push().getKey();
            FacultyAnnouncement newAnn = new FacultyAnnouncement(title, desc, currentTime);
            if (key != null) {
                dbRef.child(key).setValue(newAnn)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Announcement posted", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to post", Toast.LENGTH_SHORT).show());
            }
        });

        dialog.show();
    }
}
