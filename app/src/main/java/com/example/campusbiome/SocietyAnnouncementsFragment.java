package com.example.campusbiome;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class SocietyAnnouncementsFragment extends Fragment {

    private RecyclerView rv;
    private FloatingActionButton btnAdd;

    private List<SocietyAnnouncements> list = new ArrayList<>();
    private SocietyAnnouncementAdapter adapter;

    private DatabaseReference dbRef;

    // 🔥 IMPORTANT: Society ID
    private String societyId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_society_announcements, container, false);

        rv = view.findViewById(R.id.rvAnnouncements);
        btnAdd = view.findViewById(R.id.btnAddAnnouncement);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SocietyAnnouncementAdapter(list);
        rv.setAdapter(adapter);

        // 🔥 Get societyId from bundle
        Bundle args = getArguments();
        if (args != null) {
            societyId = args.getString("societyId");
        }

        // ❗ Safety check
        if (societyId == null || societyId.isEmpty()) {
            Toast.makeText(getContext(), "Society not found", Toast.LENGTH_SHORT).show();
            return view;
        }

        // 🔥 Correct Firebase Path
        dbRef = FirebaseDatabase.getInstance()
                .getReference("Societies")
                .child(societyId)
                .child("announcements");

        loadAnnouncements();

        btnAdd.setOnClickListener(v -> showAddDialog());

        return view;
    }

    // 🔥 LOAD ANNOUNCEMENTS (REALTIME)
    private void loadAnnouncements() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                list.clear();

                for (DataSnapshot snap : snapshot.getChildren()) {
                    SocietyAnnouncements a = snap.getValue(SocietyAnnouncements.class);

                    if (a != null) {
                        list.add(0, a); // newest first
                    }
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(),
                        "Error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🔥 SHOW ADD ANNOUNCEMENT DIALOG
    private void showAddDialog() {

        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_add_announcement, null);

        EditText etTitle = dialogView.findViewById(R.id.etTitle);
        EditText etMessage = dialogView.findViewById(R.id.etMessage);
        Button btnPost = dialogView.findViewById(R.id.btnPost);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        btnPost.setOnClickListener(v -> {

            String title = etTitle.getText().toString().trim();
            String msg = etMessage.getText().toString().trim();

            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(msg)) {
                Toast.makeText(getContext(), "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            addAnnouncement(title, msg);
            dialog.dismiss();
        });

        dialog.show();
    }

    // 🔥 ADD ANNOUNCEMENT TO FIREBASE
    private void addAnnouncement(String title, String msg) {

        String id = dbRef.push().getKey();

        if (id == null) {
            Toast.makeText(getContext(), "Failed to generate ID", Toast.LENGTH_SHORT).show();
            return;
        }

        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(new Date());

        SocietyAnnouncements announcement =
                new SocietyAnnouncements(title, msg, time);

        dbRef.child(id).setValue(announcement)
                .addOnSuccessListener(unused ->
                        Toast.makeText(getContext(), "Posted!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }
}