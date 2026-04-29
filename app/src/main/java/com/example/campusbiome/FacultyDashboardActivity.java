package com.example.campusbiome;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

public class FacultyDashboardActivity extends AppCompatActivity {

    private TextView tvTitle;
    private ImageView ivLogout;

    private LinearLayout navAppointments, navOfficeHours, navAnnouncements, navProfile;
    private MaterialCardView navAppointmentsCard, navOfficeHoursCard, navAnnouncementsCard, navProfileCard;
    private ImageView navAppointmentsIcon, navOfficeHoursIcon, navAnnouncementsIcon, navProfileIcon;
    private TextView navAppointmentsText, navOfficeHoursText, navAnnouncementsText, navProfileText;

    private final String COLOR_ACTIVE_BG = "#E6F2ED";
    private final String COLOR_INACTIVE_BG = "#00000000"; // transparent
    private final String COLOR_ACTIVE_TINT = "#006B5E";
    private final String COLOR_INACTIVE_TINT = "#8F9B99";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_dashboard);

        tvTitle = findViewById(R.id.tvTitle);
        ivLogout = findViewById(R.id.ivLogout);

        initNavViews();

        ivLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, RoleSelectionActivity.class));
            finish();
        });

        // Set up click listeners
        navAppointments.setOnClickListener(v -> selectTab("appointments"));
        navOfficeHours.setOnClickListener(v -> selectTab("office_hours"));
        navAnnouncements.setOnClickListener(v -> selectTab("announcements"));
        navProfile.setOnClickListener(v -> selectTab("profile"));

        // Set default selection
        selectTab("appointments");
    }

    private void initNavViews() {
        navAppointments = findViewById(R.id.navAppointments);
        navOfficeHours = findViewById(R.id.navOfficeHours);
        navAnnouncements = findViewById(R.id.navAnnouncements);
        navProfile = findViewById(R.id.navProfile);

        navAppointmentsCard = findViewById(R.id.navAppointmentsCard);
        navOfficeHoursCard = findViewById(R.id.navOfficeHoursCard);
        navAnnouncementsCard = findViewById(R.id.navAnnouncementsCard);
        navProfileCard = findViewById(R.id.navProfileCard);

        navAppointmentsIcon = findViewById(R.id.navAppointmentsIcon);
        navOfficeHoursIcon = findViewById(R.id.navOfficeHoursIcon);
        navAnnouncementsIcon = findViewById(R.id.navAnnouncementsIcon);
        navProfileIcon = findViewById(R.id.navProfileIcon);

        navAppointmentsText = findViewById(R.id.navAppointmentsText);
        navOfficeHoursText = findViewById(R.id.navOfficeHoursText);
        navAnnouncementsText = findViewById(R.id.navAnnouncementsText);
        navProfileText = findViewById(R.id.navProfileText);
    }

    private void selectTab(String tab) {
        resetTabs();
        Fragment selectedFragment = null;

        switch (tab) {
            case "appointments":
                navAppointmentsCard.setCardBackgroundColor(Color.parseColor(COLOR_ACTIVE_BG));
                navAppointmentsIcon.setColorFilter(Color.parseColor(COLOR_ACTIVE_TINT));
                navAppointmentsText.setTextColor(Color.parseColor(COLOR_ACTIVE_TINT));
                selectedFragment = new FacultyAppointmentsFragment();
                tvTitle.setText("Welcome Back!");
                break;
            case "office_hours":
                navOfficeHoursCard.setCardBackgroundColor(Color.parseColor(COLOR_ACTIVE_BG));
                navOfficeHoursIcon.setColorFilter(Color.parseColor(COLOR_ACTIVE_TINT));
                navOfficeHoursText.setTextColor(Color.parseColor(COLOR_ACTIVE_TINT));
                selectedFragment = new FacultyOfficeHoursFragment();
                tvTitle.setText("Office Hours");
                break;
            case "announcements":
                navAnnouncementsCard.setCardBackgroundColor(Color.parseColor(COLOR_ACTIVE_BG));
                navAnnouncementsIcon.setColorFilter(Color.parseColor(COLOR_ACTIVE_TINT));
                navAnnouncementsText.setTextColor(Color.parseColor(COLOR_ACTIVE_TINT));
                selectedFragment = new FacultyAnnouncementsFragment();
                tvTitle.setText("Announcements");
                break;
            case "profile":
                navProfileCard.setCardBackgroundColor(Color.parseColor(COLOR_ACTIVE_BG));
                navProfileIcon.setColorFilter(Color.parseColor(COLOR_ACTIVE_TINT));
                navProfileText.setTextColor(Color.parseColor(COLOR_ACTIVE_TINT));
                selectedFragment = new FacultyProfileFragment();
                tvTitle.setText("Profile");
                break;
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
        }
    }

    private void resetTabs() {
        navAppointmentsCard.setCardBackgroundColor(Color.parseColor(COLOR_INACTIVE_BG));
        navOfficeHoursCard.setCardBackgroundColor(Color.parseColor(COLOR_INACTIVE_BG));
        navAnnouncementsCard.setCardBackgroundColor(Color.parseColor(COLOR_INACTIVE_BG));
        navProfileCard.setCardBackgroundColor(Color.parseColor(COLOR_INACTIVE_BG));

        navAppointmentsIcon.setColorFilter(Color.parseColor(COLOR_INACTIVE_TINT));
        navOfficeHoursIcon.setColorFilter(Color.parseColor(COLOR_INACTIVE_TINT));
        navAnnouncementsIcon.setColorFilter(Color.parseColor(COLOR_INACTIVE_TINT));
        navProfileIcon.setColorFilter(Color.parseColor(COLOR_INACTIVE_TINT));

        navAppointmentsText.setTextColor(Color.parseColor(COLOR_INACTIVE_TINT));
        navOfficeHoursText.setTextColor(Color.parseColor(COLOR_INACTIVE_TINT));
        navAnnouncementsText.setTextColor(Color.parseColor(COLOR_INACTIVE_TINT));
        navProfileText.setTextColor(Color.parseColor(COLOR_INACTIVE_TINT));
    }
}
