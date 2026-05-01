package com.example.campusbiome;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.*;
import com.google.firebase.database.*;

import java.util.*;

public class SocietyDashboardActivity extends AppCompatActivity {

    private static final int PREVIEW_LIMIT = 3;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView btnMenu, btnLogout;
    private TextView tvWelcomeUser;
    private LinearLayout navHome, navMembers, navEvents, navTasks, navAnnouncements;

    private TextView tvStatMembers, tvStatEvents, tvStatTasks, tvStatAnnouncements;
    private RecyclerView rvEvents, rvRequests;
    private TextView tvViewAllEvents, tvViewAllRequests;

    // ALL events loaded from Firebase
    private final List<SocietyEvent> allEvents     = new ArrayList<>();
    private final List<String>       allEventIds   = new ArrayList<>();

    // Only events within next month shown in dashboard preview
    private final List<SocietyEvent> previewEvents    = new ArrayList<>();
    private final List<String>       previewEventIds  = new ArrayList<>();

    private final List<RegistrationRequest> allRequests     = new ArrayList<>();
    private final List<RegistrationRequest> previewRequests = new ArrayList<>();

    private SocietyEventAdapter eventAdapter;
    private RegistrationRequestAdapter requestAdapter;

    private View dashboardContent;

    private FirebaseAuth auth;
    private DatabaseReference dbRef;
    private String societyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_society_manager_dashboard);

        auth  = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();

        societyId = getIntent().getStringExtra("societyId");

        bindViews();
        setupBottomNav();
        setupViewAllButtons();
        loadManagerData();

        btnLogout.setOnClickListener(v -> logout());

        // ── Back: if fragment showing → go to dashboard; otherwise exit ──
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    showDashboard();
                } else if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void bindViews() {
        drawerLayout      = findViewById(R.id.drawer_layout);
        navigationView    = findViewById(R.id.nav_view);
        btnMenu           = findViewById(R.id.btnMenu);
        tvWelcomeUser     = findViewById(R.id.tvWelcomeUser);
        btnLogout         = findViewById(R.id.btnLogout);

        navHome           = findViewById(R.id.navHome);
        navMembers        = findViewById(R.id.navMembers);
        navEvents         = findViewById(R.id.navEvents);
        navTasks          = findViewById(R.id.navTasks);
        navAnnouncements  = findViewById(R.id.navAnnouncements);

        tvStatMembers     = findViewById(R.id.tvStatMembers);
        tvStatEvents      = findViewById(R.id.tvStatEvents);
        tvStatTasks       = findViewById(R.id.tvStatTasks);
        tvStatAnnouncements = findViewById(R.id.tvStatAnnouncements);

        rvEvents          = findViewById(R.id.rvEvents);
        rvRequests        = findViewById(R.id.rvMembers);
        tvViewAllEvents   = findViewById(R.id.tvViewAllEvents);
        tvViewAllRequests = findViewById(R.id.tvViewAllMembers);
        dashboardContent  = findViewById(R.id.dashboardContent);

        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        // Dashboard preview: pass null listener so "View Registrations" button
        // is hidden by the adapter (handled by null check in SocietyEventAdapter)
        eventAdapter = new SocietyEventAdapter(previewEvents, previewEventIds, null);
        rvEvents.setAdapter(eventAdapter);

        rvRequests.setLayoutManager(new LinearLayoutManager(this));
        requestAdapter = new RegistrationRequestAdapter(previewRequests, this::handleRequestAction);
        rvRequests.setAdapter(requestAdapter);
    }

    private void openFragment(Fragment fragment, String tag) {
        if (societyId != null) {
            Bundle bundle = new Bundle();
            bundle.putString("societyId", societyId);
            fragment.setArguments(bundle);
        }

        dashboardContent.setVisibility(View.GONE);
        findViewById(R.id.fragmentContainer).setVisibility(View.VISIBLE);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment, tag)
                .addToBackStack(tag)
                .commit();
    }

    private void showDashboard() {
        dashboardContent.setVisibility(View.VISIBLE);
        findViewById(R.id.fragmentContainer).setVisibility(View.GONE);
        getSupportFragmentManager().popBackStack(null,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    private void setupBottomNav() {
        navHome.setOnClickListener(v -> showDashboard());
        navMembers.setOnClickListener(v -> openFragment(new SocietyMembersFragment(), "members"));
        navEvents.setOnClickListener(v -> openFragment(new SocietyEventsFragment(), "events"));
        navTasks.setOnClickListener(v -> openFragment(new SocietyTasksFragment(), "tasks"));
        navAnnouncements.setOnClickListener(v -> openFragment(new SocietyAnnouncementsFragment(), "announcements"));
    }

    private void setupViewAllButtons() {
        // "See All" on events preview → open SocietyEventsFragment (full list + View Registrations)
        tvViewAllEvents.setOnClickListener(v -> openFragment(new SocietyEventsFragment(), "events"));
        tvViewAllRequests.setOnClickListener(v -> openFragment(new SocietyMembersFragment(), "requests"));
    }

    private void loadManagerData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        dbRef.child("Users").child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        String name = snap.child("name").getValue(String.class);
                        if (name != null) tvWelcomeUser.setText("Welcome, " + name);

                        if (societyId == null)
                            societyId = snap.child("societyId").getValue(String.class);

                        if (societyId != null) {
                            loadEvents();
                            loadPendingRequests();
                            loadTaskCount();
                            loadAnnouncementCount();
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void loadEvents() {
        dbRef.child("Societies").child(societyId).child("events")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        allEvents.clear();
                        allEventIds.clear();

                        for (DataSnapshot snap : snapshot.getChildren()) {
                            SocietyEvent e = snap.getValue(SocietyEvent.class);
                            if (e != null) {

                                allEvents.add(e);
                                allEventIds.add(snap.getKey());
                            }
                        }

                        // stat shows ALL events
                        tvStatEvents.setText(String.valueOf(allEvents.size()));
                        // preview shows only next-month events
                        refreshPreviewEvents();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    /**
     * Filters allEvents to those happening within the next 30 days and
     * takes up to PREVIEW_LIMIT of them for the dashboard RecyclerView.
     */
    private void refreshPreviewEvents() {
        previewEvents.clear();
        previewEventIds.clear();

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar monthLater = (Calendar) today.clone();
        monthLater.add(Calendar.DAY_OF_YEAR, 30);

        for (int i = 0; i < allEvents.size(); i++) {
            SocietyEvent e = allEvents.get(i);
            if (isWithinRange(e, today, monthLater)) {
                previewEvents.add(e);
                previewEventIds.add(allEventIds.get(i));
                if (previewEvents.size() >= PREVIEW_LIMIT) break;
            }
        }

        // If nothing upcoming in next month, just show the most recent PREVIEW_LIMIT
        if (previewEvents.isEmpty()) {
            int limit = Math.min(PREVIEW_LIMIT, allEvents.size());
            previewEvents.addAll(allEvents.subList(0, limit));
            previewEventIds.addAll(allEventIds.subList(0, limit));
        }

        eventAdapter.notifyDataSetChanged();
    }

    /**
     * Returns true if the event's date falls between today and the given end calendar.
     */
    private boolean isWithinRange(SocietyEvent e, Calendar from, Calendar to) {
        if (e.getDay() == null || e.getMonth() == null) return false;
        try {
            int day  = Integer.parseInt(e.getDay().trim());
            int year = (e.getYear() != null) ? Integer.parseInt(e.getYear().trim()) : 2026;
            int mon  = monthToInt(e.getMonth());
            if (mon == -1) return false;

            Calendar eventCal = Calendar.getInstance();
            eventCal.set(year, mon, day, 0, 0, 0);
            eventCal.set(Calendar.MILLISECOND, 0);

            return !eventCal.before(from) && !eventCal.after(to);
        } catch (Exception ex) {
            return false;
        }
    }

    private int monthToInt(String month) {
        if (month == null || month.length() < 3) return -1;
        switch (month.trim().toUpperCase().substring(0, 3)) {
            case "JAN": return Calendar.JANUARY;
            case "FEB": return Calendar.FEBRUARY;
            case "MAR": return Calendar.MARCH;
            case "APR": return Calendar.APRIL;
            case "MAY": return Calendar.MAY;
            case "JUN": return Calendar.JUNE;
            case "JUL": return Calendar.JULY;
            case "AUG": return Calendar.AUGUST;
            case "SEP": return Calendar.SEPTEMBER;
            case "OCT": return Calendar.OCTOBER;
            case "NOV": return Calendar.NOVEMBER;
            case "DEC": return Calendar.DECEMBER;
            default:    return -1;
        }
    }

    private void loadPendingRequests() {
        dbRef.child("Societies").child(societyId).child("registrationRequests")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        allRequests.clear();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            RegistrationRequest req = snap.getValue(RegistrationRequest.class);
                            if (req != null && "pending".equalsIgnoreCase(req.getStatus())) {
                                req.setRequestId(snap.getKey());
                                allRequests.add(req);
                            }
                        }
                        tvStatMembers.setText(String.valueOf(allRequests.size()));
                        refreshPreviewRequests();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void refreshPreviewRequests() {
        previewRequests.clear();
        previewRequests.addAll(
                allRequests.subList(0, Math.min(PREVIEW_LIMIT, allRequests.size())));
        requestAdapter.notifyDataSetChanged();
    }

    private void loadTaskCount() {
        dbRef.child("Societies").child(societyId).child("tasks")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        tvStatTasks.setText(String.valueOf(snap.getChildrenCount()));
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void loadAnnouncementCount() {
        dbRef.child("Societies").child(societyId).child("announcements")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        tvStatAnnouncements.setText(String.valueOf(snap.getChildrenCount()));
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void handleRequestAction(RegistrationRequest req, boolean accepted) {
        String reqId = req.getRequestId();
        String uid   = req.getApplicantUid();
        if (reqId == null || uid == null) return;

        if (accepted) {
            SocietyMember member = new SocietyMember(
                    uid,
                    req.getApplicantName(),
                    new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(new Date()),
                    "member",
                    "active"
            );
            dbRef.child("Societies").child(societyId).child("members").child(uid).setValue(member);
            dbRef.child("Societies").child(societyId).child("registrationRequests")
                    .child(reqId).child("status").setValue("approved");
        } else {
            dbRef.child("Societies").child(societyId).child("registrationRequests")
                    .child(reqId).child("status").setValue("rejected");
        }
    }

    private void logout() {
        auth.signOut();
        startActivity(new Intent(this, RoleSelectionActivity.class));
        finishAffinity();
    }
}