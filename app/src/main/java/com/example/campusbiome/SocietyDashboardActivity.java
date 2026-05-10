package com.example.campusbiome;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.google.firebase.auth.*;
import com.google.firebase.database.*;

import java.util.*;

public class SocietyDashboardActivity extends AppCompatActivity {

    private static final int PREVIEW_LIMIT  = 3;
    private static final int COLOR_ACTIVE   = 0xFF5C8374;
    private static final int COLOR_INACTIVE = 0xFF9AA8A4;

    // Top bar
    private ImageView btnLogout;
    private TextView  tvWelcomeUser, tvAvatarInitial;
    private TextView  tvSocietyName, tvSocietySubtitle;

    // Bottom-nav tab roots
    private LinearLayout navHome, navMembers, navEvents, navTasks, navAnnouncements;

    // Bottom-nav icons & labels (for highlight toggling)
    private ImageView iconHome, iconMembers, iconEvents, iconTasks, iconAnnouncements;
    private TextView  labelHome, labelMembers, labelEvents, labelTasks, labelAnnouncements;

    // The bottom nav bar itself (for shadow toggling)
    private LinearLayout bottomNav;

    // Stats
    private TextView tvStatMembers, tvStatEvents, tvStatTasks, tvStatAnnouncements;

    // Dashboard previews
    private RecyclerView rvEvents, rvRequests;
    private TextView     tvViewAllEvents, tvViewAllRequests;

    private final List<SocietyEvent>        allEvents       = new ArrayList<>();
    private final List<String>              allEventIds     = new ArrayList<>();
    private final List<SocietyEvent>        previewEvents   = new ArrayList<>();
    private final List<String>              previewEventIds = new ArrayList<>();
    private final List<RegistrationRequest> allRequests     = new ArrayList<>();
    private final List<RegistrationRequest> previewRequests = new ArrayList<>();

    private SocietyEventAdapter        eventAdapter;
    private RegistrationRequestAdapter requestAdapter;

    private View dashboardContent;

    private FirebaseAuth      auth;
    private DatabaseReference dbRef;
    private String            societyId;
    private String            activeTab = "home";

    // Persistent listeners — removed in onDestroy
    private DatabaseReference  eventsRef, membersRef, requestsRef;
    private ValueEventListener eventsListener, membersListener, requestsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_society_manager_dashboard);

        auth      = FirebaseAuth.getInstance();
        dbRef     = FirebaseDatabase.getInstance().getReference();
        societyId = getIntent().getStringExtra("societyId");

        bindViews();
        setupBottomNav();
        setupViewAllButtons();
        loadManagerData();

        btnLogout.setOnClickListener(v -> logout());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    showDashboard();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (eventsRef   != null && eventsListener   != null) eventsRef.removeEventListener(eventsListener);
        if (membersRef  != null && membersListener  != null) membersRef.removeEventListener(membersListener);
        if (requestsRef != null && requestsListener != null) requestsRef.removeEventListener(requestsListener);
    }

    // ── Bind views ────────────────────────────────────────────────────────────
    private void bindViews() {
        tvWelcomeUser     = findViewById(R.id.tvWelcomeUser);
        tvAvatarInitial   = findViewById(R.id.tvAvatarInitial);
        tvSocietyName     = findViewById(R.id.tvSocietyName);
        tvSocietySubtitle = findViewById(R.id.tvSocietySubtitle);
        btnLogout         = findViewById(R.id.btnLogout);
        bottomNav         = findViewById(R.id.bottomNav);

        navHome          = findViewById(R.id.navHome);
        navMembers       = findViewById(R.id.navMembers);
        navEvents        = findViewById(R.id.navEvents);
        navTasks         = findViewById(R.id.navTasks);
        navAnnouncements = findViewById(R.id.navAnnouncements);

        iconHome          = navHome.findViewById(R.id.iconHome);
        iconMembers       = navMembers.findViewById(R.id.iconMembers);
        iconEvents        = navEvents.findViewById(R.id.iconEvents);
        iconTasks         = navTasks.findViewById(R.id.iconTasks);
        iconAnnouncements = navAnnouncements.findViewById(R.id.iconAnnouncements);

        labelHome          = navHome.findViewById(R.id.labelHome);
        labelMembers       = navMembers.findViewById(R.id.labelMembers);
        labelEvents        = navEvents.findViewById(R.id.labelEvents);
        labelTasks         = navTasks.findViewById(R.id.labelTasks);
        labelAnnouncements = navAnnouncements.findViewById(R.id.labelAnnouncements);

        tvStatMembers       = findViewById(R.id.tvStatMembers);
        tvStatEvents        = findViewById(R.id.tvStatEvents);
        tvStatTasks         = findViewById(R.id.tvStatTasks);
        tvStatAnnouncements = findViewById(R.id.tvStatAnnouncements);

        rvEvents          = findViewById(R.id.rvEvents);
        rvRequests        = findViewById(R.id.rvMembers);
        tvViewAllEvents   = findViewById(R.id.tvViewAllEvents);
        tvViewAllRequests = findViewById(R.id.tvViewAllMembers);
        dashboardContent  = findViewById(R.id.dashboardContent);

        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        eventAdapter = new SocietyEventAdapter(previewEvents, previewEventIds, null);
        rvEvents.setAdapter(eventAdapter);

        rvRequests.setLayoutManager(new LinearLayoutManager(this));
        requestAdapter = new RegistrationRequestAdapter(previewRequests, this::handleRequestAction);
        rvRequests.setAdapter(requestAdapter);

        setActiveTab("home");
    }

    // ── Tab highlight + shadow ────────────────────────────────────────────────
    private void setActiveTab(String tab) {
        activeTab = tab;

        // Reset all tabs
        setTabState(iconHome,          labelHome,          navHome,          false);
        setTabState(iconMembers,       labelMembers,       navMembers,       false);
        setTabState(iconEvents,        labelEvents,        navEvents,        false);
        setTabState(iconTasks,         labelTasks,         navTasks,         false);
        setTabState(iconAnnouncements, labelAnnouncements, navAnnouncements, false);

        // Highlight active tab
        switch (tab) {
            case "home":          setTabState(iconHome,          labelHome,          navHome,          true); break;
            case "members":       setTabState(iconMembers,       labelMembers,       navMembers,       true); break;
            case "events":        setTabState(iconEvents,        labelEvents,        navEvents,        true); break;
            case "tasks":         setTabState(iconTasks,         labelTasks,         navTasks,         true); break;
            case "announcements": setTabState(iconAnnouncements, labelAnnouncements, navAnnouncements, true); break;
        }
    }

    private void setTabState(ImageView icon, TextView label, LinearLayout tab, boolean active) {
        int color = active ? COLOR_ACTIVE : COLOR_INACTIVE;

        if (icon != null) icon.setColorFilter(color);

        if (label != null) {
            label.setTextColor(color);
            label.setTypeface(null, active
                    ? android.graphics.Typeface.BOLD
                    : android.graphics.Typeface.NORMAL);
        }

        // Active tab gets a green top-border indicator strip
        if (tab != null) {
            if (active) {
                // Draw a 3dp green line at the top of the active tab via foreground
                tab.setBackgroundResource(0); // clear any previous
                tab.setPadding(0, 4, 0, 0);   // shift content down slightly
                // Top border via a child view approach: use a simple color on the parent
                tab.setBackground(buildTabBackground(true));
            } else {
                tab.setPadding(0, 0, 0, 0);
                tab.setBackground(buildTabBackground(false));
            }
        }
    }

    /** Returns a StateListDrawable-like solid background. Active tab has a green top border. */
    private android.graphics.drawable.Drawable buildTabBackground(boolean active) {
        if (!active) {
            // Transparent with ripple
            android.util.TypedValue value = new android.util.TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, value, true);
            return getDrawable(value.resourceId);
        }

        // Active: LayerDrawable — green top bar (3dp) + transparent rest
        android.graphics.drawable.ShapeDrawable topBar = new android.graphics.drawable.ShapeDrawable();
        topBar.getPaint().setColor(COLOR_ACTIVE);

        // Use a simple GradientDrawable for the top border
        android.graphics.drawable.GradientDrawable border = new android.graphics.drawable.GradientDrawable();
        border.setColor(android.graphics.Color.TRANSPARENT);
        border.setStroke(0, android.graphics.Color.TRANSPARENT);

        // Inset drawable: top strip only
        android.graphics.drawable.GradientDrawable strip = new android.graphics.drawable.GradientDrawable();
        strip.setColor(COLOR_ACTIVE);
        strip.setSize(0, dpToPx(3));

        android.graphics.drawable.LayerDrawable layer = new android.graphics.drawable.LayerDrawable(
                new android.graphics.drawable.Drawable[]{strip});
        layer.setLayerInset(0, 0, 0, 0, dpToPx(65)); // top strip, push rest down
        return layer;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    // ── Navigation ────────────────────────────────────────────────────────────
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
        setActiveTab(tag);
    }

    private void showDashboard() {
        dashboardContent.setVisibility(View.VISIBLE);
        findViewById(R.id.fragmentContainer).setVisibility(View.GONE);
        getSupportFragmentManager().popBackStack(null,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        setActiveTab("home");
    }

    private void setupBottomNav() {
        navHome.setOnClickListener(v          -> showDashboard());
        navMembers.setOnClickListener(v       -> openFragment(new SocietyMembersFragment(),      "members"));
        navEvents.setOnClickListener(v        -> openFragment(new SocietyEventsFragment(),       "events"));
        navTasks.setOnClickListener(v         -> openFragment(new SocietyTasksFragment(),        "tasks"));
        navAnnouncements.setOnClickListener(v -> openFragment(new SocietyAnnouncementsFragment(),"announcements"));
    }

    private void setupViewAllButtons() {
        tvViewAllEvents.setOnClickListener(v   -> openFragment(new SocietyEventsFragment(),  "events"));
        tvViewAllRequests.setOnClickListener(v -> openFragment(new SocietyMembersFragment(), "members"));
    }

    // ── Data loading ──────────────────────────────────────────────────────────
    private void loadManagerData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        dbRef.child("Users").child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        String name = snap.child("name").getValue(String.class);
                        if (name != null) {
                            if (tvWelcomeUser   != null) tvWelcomeUser.setText(name + "!");
                            // Avatar initial — first letter of name
                            if (tvAvatarInitial != null)
                                tvAvatarInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
                        }

                        if (societyId == null)
                            societyId = snap.child("societyId").getValue(String.class);

                        if (societyId != null) {
                            loadSocietyInfo();
                            loadEvents();
                            loadPendingRequests();
                            loadTaskCount();
                            loadAnnouncementCount();
                            loadMemberCount();
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void loadSocietyInfo() {
        dbRef.child("Societies").child(societyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        String name = snap.child("name").getValue(String.class);
                        if (name != null) {
                            if (tvSocietyName     != null) tvSocietyName.setText(name);
                            if (tvSocietySubtitle != null) tvSocietySubtitle.setText(name + " · Manager Dashboard");
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void loadMemberCount() {
        membersRef = dbRef.child("Societies").child(societyId).child("members");
        membersListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                if (isFinishing() || isDestroyed()) return;
                if (tvStatMembers != null) tvStatMembers.setText(String.valueOf(snap.getChildrenCount()));
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        };
        membersRef.addValueEventListener(membersListener);
    }

    private void loadEvents() {
        eventsRef = dbRef.child("Societies").child(societyId).child("events");
        eventsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;
                allEvents.clear(); allEventIds.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    SocietyEvent e = snap.getValue(SocietyEvent.class);
                    if (e == null) continue;
                    allEvents.add(e);
                    allEventIds.add(snap.getKey());
                }
                long approved = 0;
                for (SocietyEvent e : allEvents)
                    if ("approved".equalsIgnoreCase(e.getStatus())) approved++;
                if (tvStatEvents != null) tvStatEvents.setText(String.valueOf(approved));
                refreshPreviewEvents();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        eventsRef.addValueEventListener(eventsListener);
    }

    private void refreshPreviewEvents() {
        previewEvents.clear(); previewEventIds.clear();
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);      today.set(Calendar.MILLISECOND, 0);
        Calendar monthLater = (Calendar) today.clone();
        monthLater.add(Calendar.DAY_OF_YEAR, 30);

        for (int i = 0; i < allEvents.size(); i++) {
            SocietyEvent e = allEvents.get(i);
            if (!"approved".equalsIgnoreCase(e.getStatus())) continue;
            if (isWithinRange(e, today, monthLater)) {
                previewEvents.add(e); previewEventIds.add(allEventIds.get(i));
                if (previewEvents.size() >= PREVIEW_LIMIT) break;
            }
        }
        if (previewEvents.isEmpty()) {
            for (int i = 0; i < allEvents.size(); i++) {
                SocietyEvent e = allEvents.get(i);
                if (!"approved".equalsIgnoreCase(e.getStatus())) continue;
                previewEvents.add(e); previewEventIds.add(allEventIds.get(i));
                if (previewEvents.size() >= PREVIEW_LIMIT) break;
            }
        }
        if (eventAdapter != null) eventAdapter.notifyDataSetChanged();
    }

    private boolean isWithinRange(SocietyEvent e, Calendar from, Calendar to) {
        if (e.getDay() == null || e.getMonth() == null) return false;
        try {
            int day  = Integer.parseInt(e.getDay().trim());
            int year = e.getYear() != null ? Integer.parseInt(e.getYear().trim()) : 2026;
            int mon  = monthToInt(e.getMonth());
            if (mon == -1) return false;
            Calendar cal = Calendar.getInstance();
            cal.set(year, mon, day, 0, 0, 0); cal.set(Calendar.MILLISECOND, 0);
            return !cal.before(from) && !cal.after(to);
        } catch (Exception ex) { return false; }
    }

    private int monthToInt(String m) {
        if (m == null || m.length() < 3) return -1;
        switch (m.trim().toUpperCase().substring(0, 3)) {
            case "JAN": return Calendar.JANUARY;   case "FEB": return Calendar.FEBRUARY;
            case "MAR": return Calendar.MARCH;     case "APR": return Calendar.APRIL;
            case "MAY": return Calendar.MAY;        case "JUN": return Calendar.JUNE;
            case "JUL": return Calendar.JULY;      case "AUG": return Calendar.AUGUST;
            case "SEP": return Calendar.SEPTEMBER; case "OCT": return Calendar.OCTOBER;
            case "NOV": return Calendar.NOVEMBER;  case "DEC": return Calendar.DECEMBER;
            default: return -1;
        }
    }

    private void loadPendingRequests() {
        requestsRef = dbRef.child("Societies").child(societyId).child("registrationRequests");
        requestsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;
                allRequests.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    RegistrationRequest req = snap.getValue(RegistrationRequest.class);
                    if (req != null && "pending".equalsIgnoreCase(req.getStatus())) {
                        req.setRequestId(snap.getKey());
                        allRequests.add(req);
                    }
                }
                refreshPreviewRequests();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        requestsRef.addValueEventListener(requestsListener);
    }

    private void refreshPreviewRequests() {
        previewRequests.clear();
        previewRequests.addAll(allRequests.subList(0, Math.min(PREVIEW_LIMIT, allRequests.size())));
        if (requestAdapter != null) requestAdapter.notifyDataSetChanged();
    }

    private void loadTaskCount() {
        dbRef.child("Societies").child(societyId).child("events")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        if (isFinishing() || isDestroyed()) return;
                        long total = 0;
                        for (DataSnapshot es : snap.getChildren()) total += es.child("tasks").getChildrenCount();
                        if (tvStatTasks != null) tvStatTasks.setText(String.valueOf(total));
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void loadAnnouncementCount() {
        dbRef.child("Societies").child(societyId).child("announcements")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        if (isFinishing() || isDestroyed()) return;
                        if (tvStatAnnouncements != null)
                            tvStatAnnouncements.setText(String.valueOf(snap.getChildrenCount()));
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void handleRequestAction(RegistrationRequest req, boolean accepted) {
        String reqId = req.getRequestId(), uid = req.getApplicantUid();
        if (reqId == null || uid == null) return;
        if (accepted) {
            SocietyMember member = new SocietyMember(uid, req.getApplicantName(),
                    new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()),
                    "member", "active");
            dbRef.child("Societies").child(societyId).child("members").child(uid).setValue(member);
            dbRef.child("Societies").child(societyId).child("registrationRequests")
                    .child(reqId).child("status").setValue("approved");
        } else {
            dbRef.child("Societies").child(societyId).child("registrationRequests")
                    .child(reqId).child("status").setValue("rejected");
        }
    }

    private void logout() {
        if (eventsRef   != null && eventsListener   != null) eventsRef.removeEventListener(eventsListener);
        if (membersRef  != null && membersListener  != null) membersRef.removeEventListener(membersListener);
        if (requestsRef != null && requestsListener != null) requestsRef.removeEventListener(requestsListener);
        auth.signOut();
        Intent intent = new Intent(this, GenericLoginActivity.class);
        intent.putExtra("role", "society_manager");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
