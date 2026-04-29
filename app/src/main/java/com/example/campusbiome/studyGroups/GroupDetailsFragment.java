package com.example.campusbiome.studyGroups;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusbiome.R;
import com.example.campusbiome.studyGroups.adapters.SessionAdapter;
import com.example.campusbiome.studyGroups.models.StudyGroup;
import com.example.campusbiome.studyGroups.models.StudySession;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class GroupDetailsFragment extends Fragment {

    // ── Argument keys ────────────────────────────────────────────────────────
    public static final String ARG_GROUP_ID = "groupId";
    public static final String ARG_GROUP    = "group";

    // ── Views ────────────────────────────────────────────────────────────────
    private MaterialToolbar toolbar;
    private TextView        txtName, txtCourse, txtDescription;
    private TextView        txtStatMembers, txtStatCapacity, txtStatSessions;
    private LinearLayout    detailTagsContainer;
    private LinearLayout    layoutMemberActions;
    private Button          btnScheduleSession, btnLeaveGroup, btnJoinGroup;
    private RecyclerView    recyclerSessions;
    private TextView        txtNoSessions;

    // ── Data ─────────────────────────────────────────────────────────────────
    private String     groupId;
    private StudyGroup group;
    private boolean    isMember = false;

    private DatabaseReference  sessionsRef;
    private ValueEventListener sessionsListener;

    // ── Factory method (clean way to pass args to fragments) ─────────────────
    public static GroupDetailsFragment newInstance(String groupId, StudyGroup group) {
        GroupDetailsFragment f = new GroupDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_GROUP_ID, groupId);
        args.putSerializable(ARG_GROUP, group);
        f.setArguments(args);
        return f;
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_group_details, container, false);

        // Read arguments
        if (getArguments() != null) {
            groupId = getArguments().getString(ARG_GROUP_ID);
            group   = (StudyGroup) getArguments().getSerializable(ARG_GROUP);
        }

        if (groupId == null || group == null) {
            Toast.makeText(getContext(), "Error loading group.", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return view;
        }

        // Wire views
        toolbar              = view.findViewById(R.id.toolbar);
        txtName              = view.findViewById(R.id.txtDetailGroupName);
        txtCourse            = view.findViewById(R.id.txtDetailCourse);
        txtDescription       = view.findViewById(R.id.txtDetailDescription);
        txtStatMembers       = view.findViewById(R.id.txtStatMembers);
        txtStatCapacity      = view.findViewById(R.id.txtStatCapacity);
        txtStatSessions      = view.findViewById(R.id.txtStatSessions);
        detailTagsContainer  = view.findViewById(R.id.detailTagsContainer);
        layoutMemberActions  = view.findViewById(R.id.layoutMemberActions);
        btnScheduleSession   = view.findViewById(R.id.btnScheduleSession);
        btnLeaveGroup        = view.findViewById(R.id.btnLeaveGroup);
        btnJoinGroup         = view.findViewById(R.id.btnJoinGroup);
        recyclerSessions     = view.findViewById(R.id.recyclerSessions);
        txtNoSessions        = view.findViewById(R.id.txtNoSessions);

        // Toolbar back
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
        toolbar.setTitle(group.getName());

        // Populate static group info
        populateGroupInfo();

        // Determine if current user is a member
        checkMembershipThenSetupButtons();

        // Load sessions
        recyclerSessions.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerSessions.setNestedScrollingEnabled(false);
        loadSessions();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (sessionsRef != null && sessionsListener != null)
            sessionsRef.removeEventListener(sessionsListener);
    }

    // ── Populate group header ────────────────────────────────────────────────
    private void populateGroupInfo() {
        txtName.setText(group.getName());
        txtCourse.setText(group.getCourse());
        txtDescription.setText(group.getDescription() != null
                ? group.getDescription() : "No description provided.");

        txtStatMembers.setText(String.valueOf(group.getCurrentMembers()));
        txtStatCapacity.setText(String.valueOf(group.getMaxMembers()));
        // Sessions count is set after loading sessions

        // Tags
        detailTagsContainer.removeAllViews();
        if (group.getTags() != null) {
            for (String tag : group.getTags()) {
                detailTagsContainer.addView(makeTagChip(tag));
            }
        }
    }

    // ── Check if user is member, then show correct buttons ───────────────────
    private void checkMembershipThenSetupButtons() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showJoinButton();
            return;
        }

        FirebaseDatabase.getInstance().getReference("StudyGroups")
                .child(groupId).child("members").child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        isMember = snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                        if (isMember) showMemberButtons();
                        else          showJoinButton();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showJoinButton();
                    }
                });
    }

    private void showMemberButtons() {
        layoutMemberActions.setVisibility(View.VISIBLE);
        btnJoinGroup.setVisibility(View.GONE);

        btnScheduleSession.setOnClickListener(v -> openScheduleSession());
        btnLeaveGroup.setOnClickListener(v -> confirmLeaveGroup());
    }

    private void showJoinButton() {
        layoutMemberActions.setVisibility(View.GONE);
        btnJoinGroup.setVisibility(View.VISIBLE);
        btnJoinGroup.setOnClickListener(v -> joinGroup());
    }

    // ── Join group ───────────────────────────────────────────────────────────
    private void joinGroup() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        if (group.getCurrentMembers() >= group.getMaxMembers()) {
            Toast.makeText(getContext(), "This group is full.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();

        db.child("Users").child(uid).child("joinedGroups").child(groupId).setValue(true);
        db.child("StudyGroups").child(groupId).child("members").child(uid).setValue(true);
        db.child("StudyGroups").child(groupId).child("currentMembers")
                .setValue(group.getCurrentMembers() + 1);

        Toast.makeText(getContext(), "Joined " + group.getName() + "!", Toast.LENGTH_SHORT).show();

        // Update local state
        group.setCurrentMembers(group.getCurrentMembers() + 1);
        txtStatMembers.setText(String.valueOf(group.getCurrentMembers()));
        isMember = true;
        showMemberButtons();
    }

    // ── Leave group ──────────────────────────────────────────────────────────
    private void confirmLeaveGroup() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Leave Group")
                .setMessage("Are you sure you want to leave \"" + group.getName() + "\"?")
                .setPositiveButton("Leave", (dialog, which) -> leaveGroup())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void leaveGroup() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();

        db.child("Users").child(uid).child("joinedGroups").child(groupId).removeValue();
        db.child("StudyGroups").child(groupId).child("members").child(uid).removeValue();

        int newCount = Math.max(0, group.getCurrentMembers() - 1);
        db.child("StudyGroups").child(groupId).child("currentMembers").setValue(newCount);

        Toast.makeText(getContext(), "Left " + group.getName(), Toast.LENGTH_SHORT).show();

        // Go back to list
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    // ── Open schedule session screen ─────────────────────────────────────────
    private void openScheduleSession() {
        ScheduleSessionFragment schedFrag = ScheduleSessionFragment.newInstance(groupId, group.getName());
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, schedFrag)
                .addToBackStack(null)
                .commit();
    }

    // ── Load sessions from Firebase ──────────────────────────────────────────
    private void loadSessions() {
        // Sessions are stored at: StudySessions/{groupId}/{sessionId}
        sessionsRef = FirebaseDatabase.getInstance()
                .getReference("StudySessions").child(groupId);

        sessionsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<StudySession> sessions = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    StudySession session = ds.getValue(StudySession.class);
                    if (session != null) sessions.add(session);
                }

                txtStatSessions.setText(String.valueOf(sessions.size()));

                if (sessions.isEmpty()) {
                    txtNoSessions.setVisibility(View.VISIBLE);
                    recyclerSessions.setVisibility(View.GONE);
                } else {
                    txtNoSessions.setVisibility(View.GONE);
                    recyclerSessions.setVisibility(View.VISIBLE);
                    recyclerSessions.setAdapter(new SessionAdapter(sessions));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                txtNoSessions.setVisibility(View.VISIBLE);
                recyclerSessions.setVisibility(View.GONE);
                txtStatSessions.setText("0");
            }
        };

        sessionsRef.addValueEventListener(sessionsListener);
    }

    // ── Tag chip helper ──────────────────────────────────────────────────────
    private TextView makeTagChip(String label) {
        TextView chip = new TextView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd(8);
        chip.setLayoutParams(params);
        chip.setText(label);
        chip.setTextSize(11f);
        chip.setTextColor(Color.parseColor("#6C63FF"));
        chip.setPadding(20, 6, 20, 6);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        bg.setCornerRadius(30f);
        bg.setColor(Color.parseColor("#1A6C63FF"));
        chip.setBackground(bg);
        return chip;
    }
}