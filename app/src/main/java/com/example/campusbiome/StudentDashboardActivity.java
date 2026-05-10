package com.example.campusbiome;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class StudentDashboardActivity extends AppCompatActivity {

    private ImageView btnLogout, btnNotifications, ivProfile;
    private MaterialCardView cardProfileImage;
    private TextView tvWelcomeUser;
    private LinearLayout navHome, navMap, navTimetable, navProfessors, navCommunity;
    private LinearLayout llEventsContainer, llHomeNotifications;
    private View homeContent;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        mAuth     = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            goToLogin();
            return;
        }

        btnLogout         = findViewById(R.id.btnLogout);
        btnNotifications  = findViewById(R.id.btnNotifications);
        tvWelcomeUser     = findViewById(R.id.tvWelcomeUser);
        ivProfile         = findViewById(R.id.ivProfile);
        cardProfileImage  = findViewById(R.id.cardProfileImage);
        navHome           = findViewById(R.id.navHome);
        navMap            = findViewById(R.id.navMap);
        navTimetable      = findViewById(R.id.navTimetable);
        navProfessors     = findViewById(R.id.navProfessors);
        navCommunity      = findViewById(R.id.navCommunity);
        homeContent       = findViewById(R.id.homeContent);
        llEventsContainer = findViewById(R.id.llEventsContainer);
        llHomeNotifications = findViewById(R.id.llHomeNotifications);

        navHome.setOnClickListener(v ->      { updateNavSelection(0); showHome(); });
        navMap.setOnClickListener(v ->       { updateNavSelection(1); openFragment(new CampusMapFragment()); });
        navTimetable.setOnClickListener(v -> { updateNavSelection(2); openFragment(new TimetableFragment()); });
        navProfessors.setOnClickListener(v ->{ updateNavSelection(3); openFragment(new ProfessorsFragment()); });
        navCommunity.setOnClickListener(v -> { updateNavSelection(4); openFragment(new CommunityHubFragment()); });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            goToLogin();
        });

        btnNotifications.setOnClickListener(v -> {
            openFragment(new NotificationsFragment());
        });

        cardProfileImage.setOnClickListener(v -> {
            StudentProfileBottomSheet bottomSheet = new StudentProfileBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), "StudentProfileBottomSheet");
        });

        // Back button: if a fragment is showing go home, otherwise exit
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Fragment current = getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_container);
                if (current != null) {
                    showHome();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        requestNotificationPermission();
        fetchUserData(currentUser.getUid());
        fetchEvents();
        fetchRecentNotifications(currentUser.getUid());

        if (savedInstanceState == null) {
            showHome();
        }
    }

    private void requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private interface AnnouncementsCallback {
        void onLoaded(java.util.List<NotificationItem> items);
    }

    private long parseTimestamp(String createdAt, java.text.SimpleDateFormat sdf) {
        if (createdAt != null) {
            try {
                java.util.Date d = sdf.parse(createdAt);
                if (d != null) return d.getTime();
            } catch (Exception ignored) {}
        }
        return 0;
    }

    private void fetchAllRelevantAnnouncements(String uid, AnnouncementsCallback callback) {
        java.util.List<NotificationItem> items = new java.util.ArrayList<>();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US);
        
        int[] pendingQueries = {3};
        Runnable onQueryComplete = () -> {
            pendingQueries[0]--;
            if (pendingQueries[0] == 0) {
                callback.onLoaded(items);
            }
        };

        // 1. Admin Announcements
        mDatabase.child("Admin_announcements").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                if (snap.exists()) {
                    for (DataSnapshot ds : snap.getChildren()) {
                        long ts = parseTimestamp(ds.child("createdAt").getValue(String.class), sdf);
                        items.add(new NotificationItem(
                            ds.child("title").getValue(String.class),
                            ds.child("message").getValue(String.class),
                            "announcement", ts, true));
                    }
                }
                onQueryComplete.run();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { onQueryComplete.run(); }
        });

        // 2. Society Announcements
        mDatabase.child("Societies").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                if (snap.exists()) {
                    for (DataSnapshot ds : snap.getChildren()) {
                        if (ds.child("members").child(uid).exists()) {
                            DataSnapshot annSnap = ds.child("announcements");
                            for (DataSnapshot aDs : annSnap.getChildren()) {
                                long ts = parseTimestamp(aDs.child("createdAt").getValue(String.class), sdf);
                                String sName = ds.child("name").getValue(String.class);
                                items.add(new NotificationItem(
                                    (sName != null ? sName + ": " : "") + aDs.child("title").getValue(String.class),
                                    aDs.child("message").getValue(String.class),
                                    "announcement", ts, true));
                            }
                        }
                    }
                }
                onQueryComplete.run();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { onQueryComplete.run(); }
        });

        // 3. Study Group Announcements
        mDatabase.child("StudyGroups").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                if (snap.exists()) {
                    for (DataSnapshot ds : snap.getChildren()) {
                        if (ds.child("members").child(uid).exists()) {
                            DataSnapshot annSnap = ds.child("announcements");
                            for (DataSnapshot aDs : annSnap.getChildren()) {
                                long ts = parseTimestamp(aDs.child("createdAt").getValue(String.class), sdf);
                                String gName = ds.child("groupName").getValue(String.class);
                                items.add(new NotificationItem(
                                    (gName != null ? gName + ": " : "") + aDs.child("title").getValue(String.class),
                                    aDs.child("message").getValue(String.class),
                                    "announcement", ts, true));
                            }
                        }
                    }
                }
                onQueryComplete.run();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { onQueryComplete.run(); }
        });
    }

    private void fetchRecentNotifications(String uid) {
        mDatabase.child("Notifications").child(uid).orderByChild("timestamp")
                .limitToLast(3)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot userNotifsSnapshot) {
                        fetchAllRelevantAnnouncements(uid, announcementsItems -> {
                            if (llHomeNotifications == null) return;
                            llHomeNotifications.removeAllViews();
                            
                            java.util.List<NotificationItem> allItems = new java.util.ArrayList<>();

                            // Add user notifications
                            if (userNotifsSnapshot.exists()) {
                                for (DataSnapshot ds : userNotifsSnapshot.getChildren()) {
                                    String title = ds.child("title").getValue(String.class);
                                    String message = ds.child("message").getValue(String.class);
                                    String type = ds.child("type").getValue(String.class);
                                    Long timestamp = ds.child("timestamp").getValue(Long.class);
                                    if (timestamp != null) {
                                        allItems.add(new NotificationItem(title, message, type, timestamp, false));
                                    }
                                }
                            }

                            // Add announcements
                            allItems.addAll(announcementsItems);

                            if (allItems.isEmpty()) {
                                TextView empty = new TextView(StudentDashboardActivity.this);
                                empty.setText("No recent notifications");
                                empty.setTextColor(android.graphics.Color.parseColor("#8F9B99"));
                                llHomeNotifications.addView(empty);
                                return;
                            }

                            // Sort descending
                            java.util.Collections.sort(allItems, (o1, o2) -> Long.compare(o2.timestamp, o1.timestamp));

                            int count = 0;
                            for (NotificationItem item : allItems) {
                                if (count >= 3) break;

                                View card = LayoutInflater.from(StudentDashboardActivity.this)
                                        .inflate(R.layout.item_notification, llHomeNotifications, false);

                                ImageView icon = card.findViewById(R.id.ivNotifIcon);
                                TextView tvTitle = card.findViewById(R.id.tvNotifTitle);
                                TextView tvMessage = card.findViewById(R.id.tvNotifMessage);
                                TextView tvTime = card.findViewById(R.id.tvNotifTime);

                                tvTitle.setText(item.title != null ? item.title : "Notification");
                                tvMessage.setText(item.message != null ? item.message : "");
                                
                                CharSequence relativeTime = android.text.format.DateUtils.getRelativeTimeSpanString(
                                        item.timestamp, System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS);
                                tvTime.setText(relativeTime);

                                if ("alert".equals(item.type)) {
                                    icon.setImageResource(android.R.drawable.ic_dialog_alert);
                                    icon.setColorFilter(android.graphics.Color.parseColor("#D32F2F"));
                                    tvTitle.setTextColor(android.graphics.Color.parseColor("#D32F2F"));
                                } else if (item.isGlobal) {
                                    icon.setImageResource(android.R.drawable.ic_menu_agenda);
                                    icon.setColorFilter(android.graphics.Color.parseColor("#1A1C1C"));
                                }

                                llHomeNotifications.addView(card);
                                count++;
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private static class NotificationItem {
        String title, message, type;
        long timestamp;
        boolean isGlobal;

        NotificationItem(String title, String message, String type, long timestamp, boolean isGlobal) {
            this.title = title;
            this.message = message;
            this.type = type;
            this.timestamp = timestamp;
            this.isGlobal = isGlobal;
        }
    }

    private void showHome() {
        Fragment current = getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);
        if (current != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .remove(current)
                    .commitNow();
        }
        if (homeContent != null) homeContent.setVisibility(View.VISIBLE);
        updateNavSelection(0);
    }

    public void openFragment(Fragment fragment) {
        if (homeContent != null) homeContent.setVisibility(View.GONE);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void fetchUserData(String uid) {
        mDatabase.child("Users").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    if (name != null) tvWelcomeUser.setText("Welcome, " + name + "!");

                    String profilePic = snapshot.child("profile_pic").getValue(String.class);
                    if (profilePic != null && !profilePic.isEmpty()) {
                        Glide.with(StudentDashboardActivity.this)
                                .load(profilePic)
                                .placeholder(android.R.drawable.ic_menu_myplaces)
                                .error(android.R.drawable.ic_menu_myplaces)
                                .into(ivProfile);
                    } else {
                        ivProfile.setImageResource(android.R.drawable.ic_menu_myplaces);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (mAuth.getCurrentUser() != null) {
                    Toast.makeText(StudentDashboardActivity.this, "Could not load user data", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateNavSelection(int selectedIndex) {
        com.google.android.material.card.MaterialCardView[] cards = {
                findViewById(R.id.navHomeCard),
                findViewById(R.id.navMapCard),
                findViewById(R.id.navTimetableCard),
                findViewById(R.id.navProfessorsCard),
                findViewById(R.id.navCommunityCard)
        };
        ImageView[] icons = {
                findViewById(R.id.navHomeIcon),
                findViewById(R.id.navMapIcon),
                findViewById(R.id.navTimetableIcon),
                findViewById(R.id.navProfessorsIcon),
                findViewById(R.id.navCommunityIcon)
        };
        TextView[] texts = {
                findViewById(R.id.navHomeText),
                findViewById(R.id.navMapText),
                findViewById(R.id.navTimetableText),
                findViewById(R.id.navProfessorsText),
                findViewById(R.id.navCommunityText)
        };

        for (int i = 0; i < cards.length; i++) {
            if (cards[i] == null || icons[i] == null || texts[i] == null) continue;
            if (i == selectedIndex) {
                cards[i].setCardBackgroundColor(android.graphics.Color.parseColor("#C8DED9")); // card_bg
                icons[i].setColorFilter(android.graphics.Color.parseColor("#006B5E"));
                texts[i].setTextColor(android.graphics.Color.parseColor("#006B5E"));
            } else {
                cards[i].setCardBackgroundColor(android.graphics.Color.TRANSPARENT);
                icons[i].setColorFilter(android.graphics.Color.parseColor("#8F9B99"));
                texts[i].setTextColor(android.graphics.Color.parseColor("#8F9B99"));
            }
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(this, StudentLoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ── Events ────────────────────────────────────────────────────────────────

    private void fetchEvents() {
        mDatabase.child("Events").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (llEventsContainer != null) llEventsContainer.removeAllViews();
                if (!snapshot.exists()) return;

                java.util.List<EventItem> upcoming = new java.util.ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    EventItem item = new EventItem(ds);
                    if (item.isWithinNextWeek()) upcoming.add(item);
                }

                java.util.Collections.sort(upcoming);

                int count = 0;
                for (EventItem item : upcoming) {
                    if (count >= 3) break;

                    View card = LayoutInflater.from(StudentDashboardActivity.this)
                            .inflate(R.layout.student_dashboard_event, llEventsContainer, false);

                    ((TextView) card.findViewById(R.id.tvEventDay)).setText(
                            item.dayStr != null ? item.dayStr : "--");
                    ((TextView) card.findViewById(R.id.tvEventMonth)).setText(
                            item.monthStr != null ? item.monthStr : "");
                    ((TextView) card.findViewById(R.id.tvEventTitle)).setText(item.title);
                    ((TextView) card.findViewById(R.id.tvEventDescription)).setText(item.description);

                    TextView tvSociety = card.findViewById(R.id.tvSocietyName);
                    if (item.societyName != null && !item.societyName.isEmpty()) {
                        tvSociety.setText(item.societyName);
                        tvSociety.setVisibility(View.VISIBLE);
                    }

                    TextView tvVenue = card.findViewById(R.id.tvEventVenue);
                    if (item.venue != null && !item.venue.isEmpty()) {
                        tvVenue.setText("📍 " + item.venue);
                        tvVenue.setVisibility(View.VISIBLE);
                    }

                    TextView tvTime = card.findViewById(R.id.tvEventTime);
                    if (item.timeStr != null && !item.timeStr.isEmpty()) {
                        tvTime.setText("🕐 " + item.timeStr);
                        tvTime.setVisibility(View.VISIBLE);
                    }

                    llEventsContainer.addView(card);
                    count++;
                }

                if (upcoming.isEmpty()) {
                    TextView empty = new TextView(StudentDashboardActivity.this);
                    empty.setText("No events this Month");
                    empty.setTextColor(android.graphics.Color.parseColor("#526966"));
                    empty.setPadding(0, 8, 0, 8);
                    llEventsContainer.addView(empty);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (mAuth.getCurrentUser() != null) {
                    Toast.makeText(StudentDashboardActivity.this,
                            "Could not load events", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private static class EventItem implements Comparable<EventItem> {

        String title, description, dayStr, monthStr;
        String venue, societyName, timeStr;
        int monthNum = 99, dayNum = 99, yearNum = 2026;

        EventItem(DataSnapshot ds) {
            Object t    = ds.child("title").getValue();
            Object d    = ds.child("description").getValue();
            Object day  = ds.child("day").getValue();
            Object mon  = ds.child("month").getValue();
            Object yr   = ds.child("year").getValue();
            Object ven  = ds.child("venue").getValue();
            Object soc  = ds.child("societyName").getValue();
            Object time = ds.child("time").getValue();
            Object ampm = ds.child("ampm").getValue();

            title       = t   != null ? String.valueOf(t)   : "No Title";
            description = d   != null ? String.valueOf(d)   : "";
            dayStr      = day != null ? String.valueOf(day) : null;
            monthStr    = mon != null ? String.valueOf(mon) : null;
            venue       = ven != null ? String.valueOf(ven) : null;
            societyName = soc != null ? String.valueOf(soc) : null;
            String tStr  = time != null ? String.valueOf(time) : null;
            String apStr = ampm != null ? String.valueOf(ampm) : null;
            timeStr      = tStr != null ? (apStr != null ? tStr + " " + apStr : tStr) : null;

            if (yr != null) {
                try { yearNum = Integer.parseInt(String.valueOf(yr).trim()); }
                catch (Exception ignored) {}
            }
            parseDate();
        }

        void parseDate() {
            if (dayStr != null) {
                try { dayNum = Integer.parseInt(dayStr.trim()); }
                catch (Exception ignored) {}
            }
            if (monthStr != null && monthStr.length() >= 3) {
                switch (monthStr.trim().toUpperCase().substring(0, 3)) {
                    case "JAN": monthNum = 1;  break;
                    case "FEB": monthNum = 2;  break;
                    case "MAR": monthNum = 3;  break;
                    case "APR": monthNum = 4;  break;
                    case "MAY": monthNum = 5;  break;
                    case "JUN": monthNum = 6;  break;
                    case "JUL": monthNum = 7;  break;
                    case "AUG": monthNum = 8;  break;
                    case "SEP": monthNum = 9;  break;
                    case "OCT": monthNum = 10; break;
                    case "NOV": monthNum = 11; break;
                    case "DEC": monthNum = 12; break;
                }
            }
        }

        boolean isWithinNextWeek() {
            if (monthNum == 99 || dayNum == 99) return false;
            try {
                java.util.Calendar eventCal = java.util.Calendar.getInstance();
                eventCal.set(yearNum, monthNum - 1, dayNum, 0, 0, 0);
                eventCal.set(java.util.Calendar.MILLISECOND, 0);

                java.util.Calendar today = java.util.Calendar.getInstance();
                today.set(java.util.Calendar.HOUR_OF_DAY, 0);
                today.set(java.util.Calendar.MINUTE, 0);
                today.set(java.util.Calendar.SECOND, 0);
                today.set(java.util.Calendar.MILLISECOND, 0);

                java.util.Calendar weekLater = (java.util.Calendar) today.clone();
                weekLater.add(java.util.Calendar.DAY_OF_YEAR, 30);

                return !eventCal.before(today) && !eventCal.after(weekLater);
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public int compareTo(EventItem o) {
            if (this.yearNum  != o.yearNum)  return Integer.compare(this.yearNum,  o.yearNum);
            if (this.monthNum != o.monthNum) return Integer.compare(this.monthNum, o.monthNum);
            return Integer.compare(this.dayNum, o.dayNum);
        }
    }
}