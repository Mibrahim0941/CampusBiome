package com.example.campusbiome;

import android.app.AlertDialog;
import android.os.Bundle;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SocietyAnnouncementsFragment extends Fragment {

    private RecyclerView rv;
    private FloatingActionButton btnAdd;
    private List<SocietyAnnouncements> list = new ArrayList<>();
    private SocietyAnnouncementAdapter adapter;
    private DatabaseReference dbRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_society_announcements, container, false);

        rv = view.findViewById(R.id.rvAnnouncements);
        btnAdd = view.findViewById(R.id.btnAddAnnouncement);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SocietyAnnouncementAdapter(list);
        rv.setAdapter(adapter);

        dbRef = FirebaseDatabase.getInstance().getReference("Announcements");

        loadAnnouncements();

        btnAdd.setOnClickListener(v -> showAddDialog());

        return view;
    }

    private void loadAnnouncements() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    SocietyAnnouncements a = snap.getValue(SocietyAnnouncements.class);
                    if (a != null) list.add(a);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void showAddDialog() {

        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_announcement, null);

        EditText etTitle = view.findViewById(R.id.etTitle);
        EditText etMessage = view.findViewById(R.id.etMessage);
        Button btnPost = view.findViewById(R.id.btnPost);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(view)
                .create();

        btnPost.setOnClickListener(v -> {

            String title = etTitle.getText().toString().trim();
            String msg = etMessage.getText().toString().trim();

            if (title.isEmpty() || msg.isEmpty()) {
                Toast.makeText(getContext(), "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            String id = dbRef.push().getKey();

            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .format(new Date());

            SocietyAnnouncements a = new SocietyAnnouncements(title, msg, time);

            dbRef.child(id).setValue(a)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(getContext(), "Posted!", Toast.LENGTH_SHORT).show());

            dialog.dismiss();
        });

        dialog.show();
    }
}
