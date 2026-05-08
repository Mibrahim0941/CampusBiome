package com.example.campusbiome;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfessorDetailsFragment extends Fragment {

    private String facultyId;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    private TextView tvName, tvPost, tvDept, tvAvailability, tvOffice, tvEmail, tvCourses, tvAppointmentsHeader;
    private LinearLayout llOfficeHoursContainer, llAppointmentsContainer;
    private ImageView btnBack;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_professor_details, container, false);

        if (getArguments() != null) {
            facultyId = getArguments().getString("facultyId");
        }

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        tvName = view.findViewById(R.id.tvName);
        tvPost = view.findViewById(R.id.tvPost);
        tvDept = view.findViewById(R.id.tvDept);
        tvAvailability = view.findViewById(R.id.tvAvailability);
        tvOffice = view.findViewById(R.id.tvOffice);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvCourses = view.findViewById(R.id.tvCourses);
        llOfficeHoursContainer = view.findViewById(R.id.llOfficeHoursContainer);
        tvAppointmentsHeader = view.findViewById(R.id.tvAppointmentsHeader);
        llAppointmentsContainer = view.findViewById(R.id.llAppointmentsContainer);
        TextView tvFacultyAnnouncementsHeader = view.findViewById(R.id.tvFacultyAnnouncementsHeader);
        LinearLayout llFacultyAnnouncementsContainer = view.findViewById(R.id.llFacultyAnnouncementsContainer);
        btnBack = view.findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        if (facultyId != null) {
            fetchProfessorDetails();
            fetchPreviousAppointments();
            fetchFacultyAnnouncements(tvFacultyAnnouncementsHeader, llFacultyAnnouncementsContainer);
        }

        return view;
    }

    private void fetchProfessorDetails() {
        mDatabase.child("Faculty").child(facultyId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || !snapshot.exists()) return;

                String name = getString(snapshot, "name");
                String post = getString(snapshot, "position", "post");
                String dept = getString(snapshot, "department");
                String email = getString(snapshot, "email");
                Boolean isAvailable = snapshot.child("Available").getValue(Boolean.class);

                // Check if we have office or courses, fallback to N/A if missing
                String office = snapshot.hasChild("office") ? snapshot.child("office").getValue(String.class) : "N/A";
                String courses = snapshot.hasChild("courses") ? snapshot.child("courses").getValue(String.class) : "N/A";

                tvName.setText(name);
                tvPost.setText(post);
                tvDept.setText("Dept. of " + dept);
                tvEmail.setText(email);
                tvOffice.setText(office);
                tvCourses.setText(courses);

                if (isAvailable != null && isAvailable) {
                    tvAvailability.setText("Available");
                    tvAvailability.setTextColor(Color.parseColor("#006B5E"));
                } else {
                    tvAvailability.setText("Busy");
                    tvAvailability.setTextColor(Color.parseColor("#D32F2F"));
                }

                llOfficeHoursContainer.removeAllViews();
                DataSnapshot officeHoursSnapshot = snapshot.child("officeHours");
                if (officeHoursSnapshot.exists()) {
                    for (DataSnapshot daySnap : officeHoursSnapshot.getChildren()) {
                        String day = daySnap.getKey();
                        String timeRange = daySnap.getValue(String.class);

                        LinearLayout row = new LinearLayout(getContext());
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setPadding(0, 0, 0, 16); // bottom padding

                        TextView tvDay = new TextView(getContext());
                        tvDay.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                        tvDay.setText(day);
                        tvDay.setTextColor(Color.parseColor("#1A1C1C"));
                        tvDay.setTypeface(null, Typeface.BOLD);

                        TextView tvTime = new TextView(getContext());
                        tvTime.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                        tvTime.setText(timeRange);
                        tvTime.setTextColor(Color.parseColor("#1A1C1C"));

                        row.addView(tvDay);
                        row.addView(tvTime);
                        llOfficeHoursContainer.addView(row);
                    }
                } else {
                    TextView tvNone = new TextView(getContext());
                    tvNone.setText("No office hours specified.");
                    tvNone.setTextColor(Color.parseColor("#526966"));
                    llOfficeHoursContainer.addView(tvNone);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to load details", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void fetchPreviousAppointments() {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        mDatabase.child("FacultyAppointment").orderByChild("facultyId").equalTo(facultyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;
                        llAppointmentsContainer.removeAllViews();
                        boolean hasAppointments = false;

                        for (DataSnapshot appSnap : snapshot.getChildren()) {
                            DataSnapshot studentSnap = appSnap.child("student");
                            if (studentSnap.exists() && uid.equals(studentSnap.child("uid").getValue(String.class))) {
                                hasAppointments = true;
                                String status = appSnap.child("status").getValue(String.class);
                                String day = appSnap.child("day").getValue(String.class);
                                String startTime = appSnap.child("startTime").getValue(String.class);
                                String duration = appSnap.child("duration").getValue(String.class);

                                View card = LayoutInflater.from(getContext()).inflate(R.layout.student_dashboard_event, llAppointmentsContainer, false);
                                
                                TextView tvDay = card.findViewById(R.id.tvEventDay);
                                TextView tvMonth = card.findViewById(R.id.tvEventMonth);
                                TextView tvTitle = card.findViewById(R.id.tvEventTitle);
                                TextView tvDesc = card.findViewById(R.id.tvEventDescription);
                                TextView tvTime = card.findViewById(R.id.tvEventTime);

                                tvDay.setText(""); // Or extract day number if stored, but we have string day
                                tvMonth.setText(day != null && day.length() >= 3 ? day.substring(0, 3).toUpperCase() : "");
                                
                                tvTitle.setText("Status: " + (status != null ? status.substring(0,1).toUpperCase() + status.substring(1) : "Pending"));
                                if ("approved".equalsIgnoreCase(status)) {
                                    tvTitle.setTextColor(Color.parseColor("#006B5E"));
                                } else if ("rejected".equalsIgnoreCase(status)) {
                                    tvTitle.setTextColor(Color.parseColor("#D32F2F"));
                                } else {
                                    tvTitle.setTextColor(Color.parseColor("#F57C00"));
                                }

                                tvDesc.setText("Duration: " + (duration != null ? duration : "N/A"));
                                
                                if (startTime != null) {
                                    tvTime.setText("🕐 " + startTime);
                                    tvTime.setVisibility(View.VISIBLE);
                                }
                                
                                llAppointmentsContainer.addView(card);
                            }
                        }

                        if (hasAppointments) {
                            tvAppointmentsHeader.setVisibility(View.VISIBLE);
                            llAppointmentsContainer.setVisibility(View.VISIBLE);
                        } else {
                            tvAppointmentsHeader.setVisibility(View.GONE);
                            llAppointmentsContainer.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void fetchFacultyAnnouncements(TextView header, LinearLayout container) {
        mDatabase.child("FacultyAnnouncement").orderByChild("createdby").equalTo(facultyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;
                        container.removeAllViews();
                        boolean hasAnnouncements = false;

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            hasAnnouncements = true;
                            String title = ds.child("title").getValue(String.class);
                            String message = ds.child("description").getValue(String.class);
                            String time = ds.child("createdAt").getValue(String.class);

                            View card = LayoutInflater.from(getContext()).inflate(R.layout.item_notification, container, false);
                            ImageView icon = card.findViewById(R.id.ivNotifIcon);
                            TextView tvTitle = card.findViewById(R.id.tvNotifTitle);
                            TextView tvMessage = card.findViewById(R.id.tvNotifMessage);
                            TextView tvTime = card.findViewById(R.id.tvNotifTime);

                            icon.setImageResource(android.R.drawable.ic_menu_agenda);
                            tvTitle.setText(title != null ? title : "Announcement");
                            tvMessage.setText(message != null ? message : "");
                            tvTime.setText(time != null ? time : "");

                            container.addView(card);
                        }

                        if (hasAnnouncements) {
                            header.setVisibility(View.VISIBLE);
                            container.setVisibility(View.VISIBLE);
                        } else {
                            header.setVisibility(View.GONE);
                            container.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private String getString(DataSnapshot snapshot, String... keys) {
        for (String key : keys) {
            Object val = snapshot.child(key).getValue();
            if (val != null) return String.valueOf(val);
        }
        return "N/A";
    }
}
