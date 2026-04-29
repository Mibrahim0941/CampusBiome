package com.example.campusbiome;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class StudentDashboardActivity extends AppCompatActivity {

    private ImageView btnLogout, btnMenu;
    private TextView tvWelcomeUser;
    private LinearLayout navHome, navMap, navTimetable, navProfessors, navCommunity;
    private LinearLayout llEventsContainer;

    // The home-screen content that lives directly in the XML (not a fragment yet)
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
            goToRoleSelection();
            return;
        }

        btnLogout     = findViewById(R.id.btnLogout);
        tvWelcomeUser = findViewById(R.id.tvWelcomeUser);
        btnMenu       = findViewById(R.id.btnMenu);
        navHome       = findViewById(R.id.navHome);
        navMap        = findViewById(R.id.navMap);
        navTimetable  = findViewById(R.id.navTimetable);
        navProfessors = findViewById(R.id.navProfessors);
        navCommunity  = findViewById(R.id.navCommunity);

        // The static home content (NestedScrollView inside fragment_container in XML).
        // Fragments load on top of / replace it. On Home tap we restore it.
        homeContent = findViewById(R.id.homeContent);
        llEventsContainer = findViewById(R.id.llEventsContainer);

        btnMenu.setOnClickListener(v ->
                Toast.makeText(this, "Menu - coming soon", Toast.LENGTH_SHORT).show());

        navHome.setOnClickListener(v -> {
            updateNavSelection(0);
            showHome();
        });

        navMap.setOnClickListener(v -> {
            updateNavSelection(1);
            openFragment(new CampusMapFragment());
        });

        navTimetable.setOnClickListener(v -> {
            updateNavSelection(2);
            openFragment(new TimetableFragment());
        });

        navProfessors.setOnClickListener(v -> {
            updateNavSelection(3);
            openFragment(new ProfessorsFragment());
        });

        navCommunity.setOnClickListener(v -> {
            updateNavSelection(4);
            openFragment(new CommunityHubFragment());
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            goToRoleSelection();
        });

        fetchUserName(currentUser.getUid());
        fetchEvents();

        if (savedInstanceState == null) {
            showHome();
        }
    }

    /**
     * Shows the static home XML content and removes any loaded fragment.
     *
     * UPGRADE PATH: When you create HomeFragment, replace this whole method with:
     *     openFragment(new HomeFragment());
     * and delete the homeContent NestedScrollView from the XML.
     */
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
    }

    /**
     * Load any fragment into fragment_container.
     *
     * HOW TO ADD A NEW SCREEN (e.g. Campus Map):
     *   1. New > Fragment > Fragment (Blank)  ->  CampusMapFragment
     *   2. Build its layout in res/layout/fragment_campus_map.xml
     *   3. Change the navMap listener above to: openFragment(new CampusMapFragment());
     *   Done. Nothing else needs to change.
     */
    private void openFragment(Fragment fragment) {
        if (homeContent != null) homeContent.setVisibility(View.GONE);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void fetchUserName(String uid) {
        mDatabase.child("Users").child(uid).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String name = snapshot.getValue(String.class);
                            tvWelcomeUser.setText("Welcome, " + name + "!");
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(StudentDashboardActivity.this,
                                "Could not load name", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateNavSelection(int selectedIndex) {
        // IDs: 0=Home, 1=Map, 2=Timetable, 3=Professors, 4=Community
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
                cards[i].setCardBackgroundColor(android.graphics.Color.parseColor("#E6F2ED"));
                icons[i].setColorFilter(android.graphics.Color.parseColor("#006B5E"));
                texts[i].setTextColor(android.graphics.Color.parseColor("#006B5E"));
            } else {
                cards[i].setCardBackgroundColor(android.graphics.Color.TRANSPARENT);
                icons[i].setColorFilter(android.graphics.Color.parseColor("#8F9B99"));
                texts[i].setTextColor(android.graphics.Color.parseColor("#8F9B99"));
            }
        }
    }

    private void goToRoleSelection() {
        Intent intent = new Intent(this, RoleSelectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        Fragment current = getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);
        if (current != null) {
            showHome();
        } else {
            super.onBackPressed();
        }
    }

    private void fetchEvents() {
        android.util.Log.d("EventsFetch", "Starting to fetch events...");
        mDatabase.child("Events").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (llEventsContainer != null) {
                    llEventsContainer.removeAllViews();
                }

                if (!snapshot.exists()) {
                    Toast.makeText(StudentDashboardActivity.this, "No events found in DB", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    java.util.List<EventItem> eventList = new java.util.ArrayList<>();
                    for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                        eventList.add(new EventItem(eventSnapshot));
                    }

                    java.util.Collections.sort(eventList);

                    int count = 0;
                    for (EventItem item : eventList) {
                        if (count >= 3) break;

                        View eventView = LayoutInflater.from(StudentDashboardActivity.this)
                                .inflate(R.layout.item_event, llEventsContainer, false);

                        TextView tvTitle = eventView.findViewById(R.id.tvEventTitle);
                        TextView tvDesc = eventView.findViewById(R.id.tvEventDescription);
                        TextView tvDay = eventView.findViewById(R.id.tvEventDay);
                        TextView tvMonth = eventView.findViewById(R.id.tvEventMonth);

                        tvTitle.setText(item.title);
                        tvDesc.setText(item.description);

                        if (item.dayStr != null && item.monthStr != null) {
                            tvDay.setText(item.dayStr);
                            tvMonth.setText(item.monthStr);
                        } else if (item.dateStr != null) {
                            String[] parts = item.dateStr.split(" ");
                            if (parts.length >= 2) {
                                tvDay.setText(parts[0]);
                                tvMonth.setText(parts[1]);
                            } else {
                                tvDay.setText(item.dateStr);
                                tvMonth.setText("");
                            }
                        } else {
                            tvDay.setText("--");
                            tvMonth.setText("");
                        }

                        llEventsContainer.addView(eventView);
                        count++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(StudentDashboardActivity.this, "Error parsing event: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StudentDashboardActivity.this, "DB Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private static class EventItem implements Comparable<EventItem> {
        String title, description, dayStr, monthStr, dateStr;
        int monthNum = 99, dayNum = 99;

        public EventItem(DataSnapshot snapshot) {
            title = String.valueOf(snapshot.child("title").getValue());
            description = String.valueOf(snapshot.child("description").getValue());
            Object dObj = snapshot.child("day").getValue();
            Object mObj = snapshot.child("month").getValue();
            Object dtObj = snapshot.child("date").getValue();

            dayStr = dObj != null ? String.valueOf(dObj) : null;
            monthStr = mObj != null ? String.valueOf(mObj) : null;
            dateStr = dtObj != null ? String.valueOf(dtObj) : null;

            if (title.equals("null")) title = "No Title";
            if (description.equals("null")) description = "No Description";

            parseDate();
        }

        private void parseDate() {
            String m = monthStr;
            String d = dayStr;
            if (m == null && d == null && dateStr != null) {
                String[] parts = dateStr.split(" ");
                if (parts.length >= 2) {
                    d = parts[0];
                    m = parts[1];
                }
            }
            if (d != null) {
                try { dayNum = Integer.parseInt(d.trim()); } catch (Exception ignored) {}
            }
            if (m != null) {
                String mUpper = m.trim().toUpperCase();
                if (mUpper.startsWith("JAN")) monthNum = 1;
                else if (mUpper.startsWith("FEB")) monthNum = 2;
                else if (mUpper.startsWith("MAR")) monthNum = 3;
                else if (mUpper.startsWith("APR")) monthNum = 4;
                else if (mUpper.startsWith("MAY")) monthNum = 5;
                else if (mUpper.startsWith("JUN")) monthNum = 6;
                else if (mUpper.startsWith("JUL")) monthNum = 7;
                else if (mUpper.startsWith("AUG")) monthNum = 8;
                else if (mUpper.startsWith("SEP")) monthNum = 9;
                else if (mUpper.startsWith("OCT")) monthNum = 10;
                else if (mUpper.startsWith("NOV")) monthNum = 11;
                else if (mUpper.startsWith("DEC")) monthNum = 12;
            }
        }

        @Override
        public int compareTo(EventItem o) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            int currentMonth = cal.get(java.util.Calendar.MONTH) + 1;
            int currentDay = cal.get(java.util.Calendar.DAY_OF_MONTH);

            int adjustedThisMonth = this.monthNum < currentMonth || (this.monthNum == currentMonth && this.dayNum < currentDay) ? this.monthNum + 12 : this.monthNum;
            int adjustedOtherMonth = o.monthNum < currentMonth || (o.monthNum == currentMonth && o.dayNum < currentDay) ? o.monthNum + 12 : o.monthNum;

            if (adjustedThisMonth != adjustedOtherMonth) {
                return Integer.compare(adjustedThisMonth, adjustedOtherMonth);
            }
            return Integer.compare(this.dayNum, o.dayNum);
        }
    }
}