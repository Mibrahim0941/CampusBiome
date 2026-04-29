package com.example.campusbiome.studyGroups;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusbiome.R;
import com.example.campusbiome.studyGroups.adapters.StudyGroupAdapter;
import com.example.campusbiome.studyGroups.models.StudyGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class StudyGroupsFragment extends Fragment {

    private static final String TAG = "StudyGroupsFragment";

    // ── Views ────────────────────────────────────────────────────────────────
    private RecyclerView      recyclerJoined, recyclerBrowse;
    private TextView          txtNoJoined, txtJoinedCount;
    private TextView          txtNoBrowse,  txtBrowseCount;
    private Button            btnCreateGroup;
    private TextInputEditText etSearch;

    // ── Data ─────────────────────────────────────────────────────────────────
    // All groups from Firebase
    private final List<StudyGroup> allGroups   = new ArrayList<>();
    private final List<String>     allGroupIds = new ArrayList<>();

    // Ids of groups the current user has joined (loaded from Users/{uid}/joinedGroups)
    private final List<String>     joinedIds   = new ArrayList<>();

    // Firebase listener reference (so we can remove it in onDestroyView)
    private DatabaseReference  groupsRef;
    private ValueEventListener groupsListener;

    // ── Lifecycle ────────────────────────────────────────────────────────────
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_study_groups, container, false);

        // Wire views
        recyclerJoined = view.findViewById(R.id.recyclerJoinedGroups);
        recyclerBrowse = view.findViewById(R.id.recyclerBrowseGroups);
        txtNoJoined    = view.findViewById(R.id.txtNoJoined);
        txtJoinedCount = view.findViewById(R.id.txtJoinedCount);
        txtNoBrowse    = view.findViewById(R.id.txtNoBrowse);
        txtBrowseCount = view.findViewById(R.id.txtBrowseCount);
        btnCreateGroup = view.findViewById(R.id.btnCreateGroup);
        etSearch       = view.findViewById(R.id.etSearch);

        // Layout managers
        recyclerJoined.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerJoined.setNestedScrollingEnabled(false);
        recyclerBrowse.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerBrowse.setNestedScrollingEnabled(false);

        // Create group button
        btnCreateGroup.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new CreateGroupFragment())
                        .addToBackStack(null)
                        .commit());

        // Search
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                public void afterTextChanged(Editable s) {}
                public void onTextChanged(CharSequence s, int a, int b, int c) {
                    renderLists(s.toString().trim().toLowerCase());
                }
            });
        }

        // Load data
        loadData();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (groupsRef != null && groupsListener != null)
            groupsRef.removeEventListener(groupsListener);
    }

    // ── Firebase loading ─────────────────────────────────────────────────────

    /**
     * Step 1: Load all StudyGroups from Firebase.
     * Step 2: Once loaded, fetch which ones the current user has joined.
     * Step 3: Render both lists.
     */
    private void loadData() {
        groupsRef = FirebaseDatabase.getInstance().getReference("StudyGroups");

        groupsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allGroups.clear();
                allGroupIds.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    StudyGroup group = ds.getValue(StudyGroup.class);
                    if (group != null) {
                        group.setId(ds.getKey()); // store the Firebase key inside the object
                        allGroups.add(group);
                        allGroupIds.add(ds.getKey());
                        Log.d(TAG, "Loaded group: " + group.getName());
                    } else {
                        // This usually means a field in Firebase doesn't match the model.
                        // Check that StudyGroup has a no-arg constructor and matching field names.
                        Log.w(TAG, "null group at key=" + ds.getKey());
                    }
                }

                // Now load which groups this user has joined
                fetchJoinedThenRender();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase read cancelled: " + error.getMessage());
                // ── Most common cause: security rules ──────────────────────────────────
                // In Firebase Console → Realtime Database → Rules, set:
                //   { "rules": { ".read": "auth != null", ".write": "auth != null" } }
                // ──────────────────────────────────────────────────────────────────────
                Toast.makeText(getContext(),
                        "Failed to load groups: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        };

        // addValueEventListener keeps listening for real-time updates
        groupsRef.addValueEventListener(groupsListener);
    }

    private void fetchJoinedThenRender() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            // Not logged in — show all groups in Browse, nothing in Joined
            joinedIds.clear();
            renderLists("");
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(user.getUid())
                .child("joinedGroups")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        joinedIds.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            joinedIds.add(ds.getKey());
                        }
                        Log.d(TAG, "User has joined " + joinedIds.size() + " groups");
                        renderLists(currentSearchQuery());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.w(TAG, "Could not load joinedGroups: " + error.getMessage());
                        renderLists(currentSearchQuery());
                    }
                });
    }

    // ── Rendering ────────────────────────────────────────────────────────────

    private void renderLists(String query) {
        List<StudyGroup> joinedGroups    = new ArrayList<>();
        List<String>     joinedGroupIds  = new ArrayList<>();
        List<StudyGroup> browseGroups    = new ArrayList<>();
        List<String>     browseGroupIds  = new ArrayList<>();

        for (int i = 0; i < allGroups.size(); i++) {
            StudyGroup g  = allGroups.get(i);
            String     id = allGroupIds.get(i);

            // Apply search filter
            if (!query.isEmpty() && !matchesQuery(g, query)) continue;

            if (joinedIds.contains(id)) {
                joinedGroups.add(g);
                joinedGroupIds.add(id);
            } else {
                browseGroups.add(g);
                browseGroupIds.add(id);
            }
        }

        // ── Joined section ──
        txtJoinedCount.setText(String.valueOf(joinedGroups.size()));
        if (joinedGroups.isEmpty()) {
            txtNoJoined.setVisibility(View.VISIBLE);
            recyclerJoined.setVisibility(View.GONE);
        } else {
            txtNoJoined.setVisibility(View.GONE);
            recyclerJoined.setVisibility(View.VISIBLE);
            recyclerJoined.setAdapter(new StudyGroupAdapter(
                    joinedGroups, joinedGroupIds, joinedIds,
                    this::onGroupActionClicked));
        }

        // ── Browse section ──
        txtBrowseCount.setText(String.valueOf(browseGroups.size()));
        if (browseGroups.isEmpty()) {
            txtNoBrowse.setVisibility(View.VISIBLE);
            recyclerBrowse.setVisibility(View.GONE);
        } else {
            txtNoBrowse.setVisibility(View.GONE);
            recyclerBrowse.setVisibility(View.VISIBLE);
            recyclerBrowse.setAdapter(new StudyGroupAdapter(
                    browseGroups, browseGroupIds, joinedIds,
                    this::onGroupActionClicked));
        }
    }

    // ── Action handler ───────────────────────────────────────────────────────

    /**
     * Called when user taps "Join" or "View Group" on any card.
     * If already joined → open details (TODO).
     * If not joined → join the group in Firebase.
     */
    private void onGroupActionClicked(StudyGroup group, String groupId) {
        if (joinedIds.contains(groupId)) {
            // Already a member → open Group Details screen (to be implemented)
            // Uncomment and use once GroupDetailsFragment is ready:
            // Bundle args = new Bundle();
            // args.putString("groupId", groupId);
            // args.putSerializable("group", group);
            // GroupDetailsFragment detailFrag = new GroupDetailsFragment();
            // detailFrag.setArguments(args);
            // requireActivity().getSupportFragmentManager()
            //         .beginTransaction()
            //         .replace(R.id.fragment_container, detailFrag)
            //         .addToBackStack(null)
            //         .commit();
            Toast.makeText(getContext(),
                    "Opening " + group.getName() + "…", Toast.LENGTH_SHORT).show();
            return;
        }

        // Not yet joined → join
        joinGroup(group, groupId);
    }

    private void joinGroup(StudyGroup group, String groupId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Please log in to join groups.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference db = FirebaseDatabase.getInstance().getReference();

        // 1. Record membership under Users/{uid}/joinedGroups/{groupId}
        db.child("Users").child(user.getUid())
                .child("joinedGroups").child(groupId)
                .setValue(true);

        // 2. Increment currentMembers in StudyGroups/{groupId}
        db.child("StudyGroups").child(groupId).child("currentMembers")
                .setValue(group.getCurrentMembers() + 1);

        Toast.makeText(getContext(),
                "Joined " + group.getName() + "!", Toast.LENGTH_SHORT).show();

        // joinedIds will update automatically because loadData() is a live listener
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean matchesQuery(StudyGroup g, String query) {
        if (g.getName()        != null && g.getName().toLowerCase().contains(query))        return true;
        if (g.getCourse()      != null && g.getCourse().toLowerCase().contains(query))      return true;
        if (g.getDescription() != null && g.getDescription().toLowerCase().contains(query)) return true;
        if (g.getTags()        != null)
            for (String t : g.getTags())
                if (t.toLowerCase().contains(query)) return true;
        return false;
    }

    /** Returns the current text in the search box (empty string if box is null/empty). */
    private String currentSearchQuery() {
        if (etSearch == null || etSearch.getText() == null) return "";
        return etSearch.getText().toString().trim().toLowerCase();
    }
}