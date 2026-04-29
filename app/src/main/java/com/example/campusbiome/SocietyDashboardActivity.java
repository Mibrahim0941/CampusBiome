package com.example.campusbiome;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SocietyDashboardActivity extends AppCompatActivity {

    private static final int PREVIEW_LIMIT = 3;

    // UI Components
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView btnMenu, btnLogout;
    private TextView tvWelcomeUser;
    private LinearLayout navHome, navMembers, navEvents, navTasks, navAnnouncements;
    private TextView tvStatMembers, tvStatEvents, tvStatTasks, tvStatAnnouncements;
    private RecyclerView rvEvents, rvRequests; // Renamed from rvMembers
    private TextView tvViewAllEvents, tvViewAllRequests; // Renamed from tvViewAllMembers

    // Data Lists
    private final List<SocietyEvent> allEvents = new ArrayList<>();
    private final List<SocietyEvent> previewEvents = new ArrayList<>();

    // NEW: Registration Request Lists
    private final List<RegistrationRequest> allRequests = new ArrayList<>();
    private final List<RegistrationRequest> previewRequests = new ArrayList<>();

    private SocietyEventAdapter eventAdapter;
    private RegistrationRequestAdapter requestAdapter; // NEW Adapter
    private View dashboardContent;
    private FirebaseAuth auth;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_society_manager_dashboard);

        auth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();

        bindViews();
        setupDrawer();
        setupBottomNav();
        setupViewAllButtons();
        loadManagerName();
        loadEvents();
        loadPendingRequests(); // Replaced loadMembers()
        loadTaskCount();
        loadAnnouncementCount();

        btnLogout.setOnClickListener(v -> logout());
    }

    private void bindViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        btnMenu = findViewById(R.id.btnMenu);
        tvWelcomeUser = findViewById(R.id.tvWelcomeUser);
        btnLogout = findViewById(R.id.btnLogout);

        navHome = findViewById(R.id.navHome);
        navMembers = findViewById(R.id.navMembers);
        navEvents = findViewById(R.id.navEvents);
        navTasks = findViewById(R.id.navTasks);
        navAnnouncements = findViewById(R.id.navAnnouncements);

        tvStatMembers = findViewById(R.id.tvStatMembers);
        tvStatEvents = findViewById(R.id.tvStatEvents);
        tvStatTasks = findViewById(R.id.tvStatTasks);
        tvStatAnnouncements = findViewById(R.id.tvStatAnnouncements);

        rvEvents = findViewById(R.id.rvEvents);
        rvRequests = findViewById(R.id.rvMembers); // Using the same ID from XML

        tvViewAllEvents = findViewById(R.id.tvViewAllEvents);
        tvViewAllRequests = findViewById(R.id.tvViewAllMembers); // Using the same ID from XML

        dashboardContent = findViewById(R.id.dashboardContent);
        // Events Adapter Setup
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setNestedScrollingEnabled(false);
        eventAdapter = new SocietyEventAdapter(previewEvents);
        rvEvents.setAdapter(eventAdapter);

        // NEW: Registration Requests Adapter Setup
        rvRequests.setLayoutManager(new LinearLayoutManager(this));
        rvRequests.setNestedScrollingEnabled(false);
        requestAdapter = new RegistrationRequestAdapter(previewRequests, this::handleRequestAction);        rvRequests.setAdapter(requestAdapter);
    }

    private void setupViewAllButtons() {
        tvViewAllEvents.setOnClickListener(v -> openFragment(new SocietyEventsFragment(), "events"));
        tvViewAllRequests.setOnClickListener(v -> openFragment(new SocietyMembersFragment(), "requests"));
    }

    private void loadPendingRequests() {
        dbRef.child("RegistrationRequests").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allRequests.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    RegistrationRequest req = snap.getValue(RegistrationRequest.class);
                    // Only show 'pending' on the dashboard
                    if (req != null && "pending".equals(req.getStatus())) {
                        req.setRequestId(snap.getKey()); // Critical for approval buttons
                        allRequests.add(req);
                    }
                }
                tvStatMembers.setText(String.valueOf(allRequests.size()));
                refreshPreviewRequests();
            }

            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void refreshPreviewRequests() {
        previewRequests.clear();
        int limit = Math.min(PREVIEW_LIMIT, allRequests.size());
        previewRequests.addAll(allRequests.subList(0, limit));
        requestAdapter.notifyDataSetChanged();

        if (allRequests.size() > PREVIEW_LIMIT) {
            tvViewAllRequests.setText("View all " + allRequests.size() + " requests →");
            tvViewAllRequests.setVisibility(View.VISIBLE);
        } else if (allRequests.size() > 0) {
            tvViewAllRequests.setText("View all requests →");
            tvViewAllRequests.setVisibility(View.VISIBLE);
        } else {
            tvViewAllRequests.setVisibility(View.GONE);
        }
    }

    // ── Drawer ───────────────────────────────────────────────────────────────
    private void setupDrawer() {
        btnMenu.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START))
                drawerLayout.closeDrawer(GravityCompat.START);
            else
                drawerLayout.openDrawer(GravityCompat.START);
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if      (id == R.id.nav_dashboard)     showDashboard();
            else if (id == R.id.nav_members)       openFragment(new SocietyMembersFragment(),       "members");
            else if (id == R.id.nav_events)        openFragment(new SocietyEventsFragment(),        "events");
            else if (id == R.id.nav_tasks)         openFragment(new SocietyTasksFragment(),         "tasks");
            else if (id == R.id.nav_announcements) openFragment(new SocietyAnnouncementsFragment(), "announcements");
            else if (id == R.id.nav_logout)        logout();
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    // ── Bottom nav ───────────────────────────────────────────────────────────
    private void setupBottomNav() {

        navHome.setOnClickListener(v -> showDashboard());
        navMembers.setOnClickListener(v ->
                openFragment(new SocietyMembersFragment(), "members"));
        navEvents.setOnClickListener(v ->
                openFragment(new SocietyEventsFragment(), "events"));
        navTasks.setOnClickListener(v ->
                openFragment(new SocietyTasksFragment(), "tasks"));
        navAnnouncements.setOnClickListener(v ->
                openFragment(new SocietyAnnouncementsFragment(), "announcements"));
    }

    /**
     * Replaces the content area with a fragment.
     * The fragment slides over the NestedScrollView + bottom nav area
     * by targeting R.id.fragmentContainer (added to layout below).
     */
    private void openFragment(Fragment fragment, String tag) {

        dashboardContent.setVisibility(View.GONE);
        findViewById(R.id.fragmentContainer).setVisibility(View.VISIBLE);

        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right,
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right)
                .replace(R.id.fragmentContainer, fragment, tag)
                .commit();
    }

    /** Pops all fragments to show the plain dashboard. */
    private void showDashboard() {
        dashboardContent.setVisibility(View.VISIBLE);
        findViewById(R.id.fragmentContainer).setVisibility(View.GONE);
        getSupportFragmentManager().popBackStack(null,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    // ── Firebase: Manager name ────────────────────────────────────────────────
    private void loadManagerName() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        dbRef.child("Users").child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        String name = snap.child("name").getValue(String.class);
                        if (name != null) tvWelcomeUser.setText("Welcome, " + name);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    // ── Firebase: Events ─────────────────────────────────────────────────────
    private void loadEvents() {
        dbRef.child("Events").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allEvents.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String day   = snap.child("day").getValue(String.class);
                    String month = snap.child("month").getValue(String.class);
                    String title = snap.child("title").getValue(String.class);
                    String desc  = snap.child("description").getValue(String.class);
                    allEvents.add(new SocietyEvent(day, month, title, desc));
                }
                // Update stat counter with full count
                tvStatEvents.setText(String.valueOf(allEvents.size()));
                // Rebuild the preview list (first PREVIEW_LIMIT items only)
                refreshPreviewEvents();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                Toast.makeText(SocietyDashboardActivity.this,
                        "Failed to load events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshPreviewEvents() {
        previewEvents.clear();
        int limit = Math.min(PREVIEW_LIMIT, allEvents.size());
        previewEvents.addAll(allEvents.subList(0, limit));
        eventAdapter.notifyDataSetChanged();

        // Show "View All (N)" if there are more than the preview limit
        if (allEvents.size() > PREVIEW_LIMIT) {
            tvViewAllEvents.setText("View all " + allEvents.size() + " events →");
            tvViewAllEvents.setVisibility(android.view.View.VISIBLE);
        } else {
            tvViewAllEvents.setVisibility(android.view.View.GONE);
        }
    }

    // ── Firebase: Task count ──────────────────────────────────────────────────
    private void loadTaskCount() {
        dbRef.child("Tasks").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                tvStatTasks.setText(String.valueOf(snap.getChildrenCount()));
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    // ── Firebase: Announcement count ─────────────────────────────────────────
    private void loadAnnouncementCount() {
        dbRef.child("Announcements").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                tvStatAnnouncements.setText(String.valueOf(snap.getChildrenCount()));
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void handleRequestAction(RegistrationRequest req, boolean accepted) {

        String reqId = req.getRequestId();
        String uid = req.getApplicantUid();

        if (reqId == null || uid == null) return;

        if (accepted) {

            SocietyMember member = new SocietyMember(
                    uid,
                    req.getApplicantName(),
                    new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            .format(new java.util.Date()),
                    "member",
                    "active"
            );

            dbRef.child("SocietyMembers").child(uid).setValue(member);
            dbRef.child("RegistrationRequests").child(reqId).child("status").setValue("approved");

            Toast.makeText(this, "Request Approved", Toast.LENGTH_SHORT).show();

        } else {

            dbRef.child("RegistrationRequests").child(reqId).child("status").setValue("rejected");

            Toast.makeText(this, "Request Rejected", Toast.LENGTH_SHORT).show();
        }
    }

    // ── Auth ─────────────────────────────────────────────────────────────────
    private void logout() {
        auth.signOut();
        startActivity(new Intent(this, RoleSelectionActivity.class));
        finishAffinity();
    }

    @Override
    public void onBackPressed() {
        // If a fragment is showing, pop it back to dashboard
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}