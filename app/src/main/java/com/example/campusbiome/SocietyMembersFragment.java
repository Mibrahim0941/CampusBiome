package com.example.campusbiome;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

public class SocietyMembersFragment extends Fragment {

    private RecyclerView rv;
    private TextView tabRequests, tabMembers;

    private List<RegistrationRequest> requestList = new ArrayList<>();
    private List<SocietyMember> memberList = new ArrayList<>();

    private RegistrationRequestAdapter requestAdapter;
    private SocietyMemberAdapter memberAdapter;

    private DatabaseReference dbRef;

    private String societyId; // 🔥 REQUIRED

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_society_members, container, false);

        rv = view.findViewById(R.id.rvMembers);
        tabRequests = view.findViewById(R.id.tabRequests);
        tabMembers = view.findViewById(R.id.tabMembers);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        requestAdapter = new RegistrationRequestAdapter(requestList, this::handleRequestAction);
        memberAdapter = new SocietyMemberAdapter(memberList);

        dbRef = FirebaseDatabase.getInstance().getReference();

        // 🔥 GET SOCIETY ID
        if (getArguments() != null) {
            societyId = getArguments().getString("societyId");
        }

        if (societyId == null) {
            Toast.makeText(getContext(), "Society not found", Toast.LENGTH_SHORT).show();
            return view;
        }

        // Default tab
        switchToRequests();

        tabRequests.setOnClickListener(v -> switchToRequests());
        tabMembers.setOnClickListener(v -> switchToMembers());

        return view;
    }

    private void switchToRequests() {
        rv.setAdapter(requestAdapter);
        tabRequests.setBackgroundColor(getResources().getColor(R.color.primary));
        tabMembers.setBackgroundColor(getResources().getColor(R.color.card_bg));
        loadRequests();
    }

    private void switchToMembers() {
        rv.setAdapter(memberAdapter);
        tabMembers.setBackgroundColor(getResources().getColor(R.color.primary));
        tabRequests.setBackgroundColor(getResources().getColor(R.color.card_bg));
        loadMembers();
    }

    // ✅ FIXED PATH
    private void loadRequests() {
        dbRef.child("Societies")
                .child(societyId)
                .child("registrationRequests")
                .addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        requestList.clear();

                        for (DataSnapshot snap : snapshot.getChildren()) {
                            RegistrationRequest req = snap.getValue(RegistrationRequest.class);

                            if (req != null && "pending".equalsIgnoreCase(req.getStatus())) {
                                req.setRequestId(snap.getKey());
                                requestList.add(req);
                            }
                        }

                        requestAdapter.notifyDataSetChanged();
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // ✅ FIXED PATH
    private void loadMembers() {
        dbRef.child("Societies")
                .child(societyId)
                .child("members")
                .addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        memberList.clear();

                        for (DataSnapshot snap : snapshot.getChildren()) {
                            SocietyMember member = snap.getValue(SocietyMember.class);

                            if (member != null) {
                                member.setUid(snap.getKey()); // 🔥 IMPORTANT
                                memberList.add(member);
                            }
                        }

                        memberAdapter.notifyDataSetChanged();
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // 🔥 FIXED APPROVE / REJECT
    private void handleRequestAction(RegistrationRequest req, boolean accepted) {

        String reqId = req.getRequestId();
        String uid = req.getApplicantUid();

        if (reqId == null || uid == null) return;

        if (accepted) {

            SocietyMember member = new SocietyMember(
                    uid,
                    req.getApplicantName(),
                    new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()),
                    "member",
                    "active"
            );

            // ✅ SAVE INSIDE SOCIETY
            dbRef.child("Societies")
                    .child(societyId)
                    .child("members")
                    .child(uid)
                    .setValue(member);

            dbRef.child("Societies")
                    .child(societyId)
                    .child("registrationRequests")
                    .child(reqId)
                    .child("status")
                    .setValue("approved");

        } else {

            dbRef.child("Societies")
                    .child(societyId)
                    .child("registrationRequests")
                    .child(reqId)
                    .child("status")
                    .setValue("rejected");
        }
    }
}
