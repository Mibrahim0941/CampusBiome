package com.example.campusbiome;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimetableFragment extends Fragment {

    private RecyclerView rvDays, rvClasses;
    private TextView tvSemesterLabel, tvSectionLabel;
    private ProgressBar progressBar;

    private View btnDayView, btnWeekView;
    private TextView tvDayView, tvWeekView;
    private View llDayViewHeader;
    private TextView tvTodayDay, tvTodayDate;

    private TimetableDayAdapter dayAdapter;
    private TimetableClassAdapter weekViewAdapter;
    private TimetableClassDayAdapter dayViewAdapter;

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    private Map<String, List<TimetableSlot>> slotsByDay = new HashMap<>();
    private String currentSelectedDayShort = "Mon";
    private boolean isDayView = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_timetable, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvDays = view.findViewById(R.id.rvDays);
        rvClasses = view.findViewById(R.id.rvClasses);
        tvSemesterLabel = view.findViewById(R.id.tvSemesterLabel);
        tvSectionLabel = view.findViewById(R.id.tvSectionLabel);
        progressBar = view.findViewById(R.id.progressBar);

        btnDayView = view.findViewById(R.id.btnDayView);
        btnWeekView = view.findViewById(R.id.btnWeekView);
        tvDayView = view.findViewById(R.id.tvDayView);
        tvWeekView = view.findViewById(R.id.tvWeekView);
        llDayViewHeader = view.findViewById(R.id.llDayViewHeader);
        tvTodayDay = view.findViewById(R.id.tvTodayDay);
        tvTodayDate = view.findViewById(R.id.tvTodayDate);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Setup Day RecyclerView
        dayAdapter = new TimetableDayAdapter((position, dayShortName) -> {
            currentSelectedDayShort = dayShortName;
            updateClassesDisplay();
        });
        rvDays.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvDays.setAdapter(dayAdapter);

        // Setup Class Adapters
        weekViewAdapter = new TimetableClassAdapter(new ArrayList<>());
        dayViewAdapter = new TimetableClassDayAdapter(new ArrayList<>());
        
        rvClasses.setLayoutManager(new LinearLayoutManager(getContext()));
        rvClasses.setAdapter(weekViewAdapter);

        setupToggleLogic();

        fetchUserAndTimetable();
    }

    private void setupToggleLogic() {
        btnDayView.setOnClickListener(v -> {
            isDayView = true;
            ((com.google.android.material.card.MaterialCardView) btnDayView).setCardBackgroundColor(android.graphics.Color.parseColor("#E6F2ED"));
            tvDayView.setTextColor(android.graphics.Color.parseColor("#006B5E"));
            
            ((com.google.android.material.card.MaterialCardView) btnWeekView).setCardBackgroundColor(android.graphics.Color.TRANSPARENT);
            tvWeekView.setTextColor(android.graphics.Color.parseColor("#8F9B99"));

            llDayViewHeader.setVisibility(View.VISIBLE);
            rvDays.setVisibility(View.GONE);
            
            // Set current date
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE,", Locale.US);
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
            Date now = new Date();
            tvTodayDay.setText(dayFormat.format(now));
            tvTodayDate.setText(dateFormat.format(now));
            
            // Find current day short name
            SimpleDateFormat shortDayFormat = new SimpleDateFormat("E", Locale.US);
            currentSelectedDayShort = shortDayFormat.format(now);
            
            rvClasses.setAdapter(dayViewAdapter);
            updateClassesDisplay();
        });

        btnWeekView.setOnClickListener(v -> {
            isDayView = false;
            ((com.google.android.material.card.MaterialCardView) btnWeekView).setCardBackgroundColor(android.graphics.Color.parseColor("#E6F2ED"));
            tvWeekView.setTextColor(android.graphics.Color.parseColor("#006B5E"));
            
            ((com.google.android.material.card.MaterialCardView) btnDayView).setCardBackgroundColor(android.graphics.Color.TRANSPARENT);
            tvDayView.setTextColor(android.graphics.Color.parseColor("#8F9B99"));

            llDayViewHeader.setVisibility(View.GONE);
            rvDays.setVisibility(View.VISIBLE);
            
            // Revert back to the day selected in the day pill adapter
            // (Simplification: just keep whatever currentSelectedDayShort was last clicked)
            
            rvClasses.setAdapter(weekViewAdapter);
            updateClassesDisplay();
        });
    }

    private void fetchUserAndTimetable() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        mDatabase.child("Users").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    progressBar.setVisibility(View.GONE);
                    return;
                }
                String degree = snapshot.child("Degree").getValue(String.class);
                String program = snapshot.child("Program").getValue(String.class);
                Integer semester = snapshot.child("Semester").getValue(Integer.class);
                String section = snapshot.child("Section").getValue(String.class);

                if (degree != null && program != null && semester != null && section != null) {
                    tvSemesterLabel.setText("Semester " + semester);
                    tvSectionLabel.setText(section);
                    fetchTimetable(degree, program, semester, section);
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "User details incomplete", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void fetchTimetable(String degree, String program, int semester, String section) {
        String path = "Timetable/" + degree + "/" + program + "/Semester_" + semester + "/" + section + "/courses";
        mDatabase.child(path).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                slotsByDay.clear();
                
                // Initialize lists for all days
                slotsByDay.put("Mon", new ArrayList<>());
                slotsByDay.put("Tue", new ArrayList<>());
                slotsByDay.put("Wed", new ArrayList<>());
                slotsByDay.put("Thu", new ArrayList<>());
                slotsByDay.put("Fri", new ArrayList<>());

                for (DataSnapshot courseSnap : snapshot.getChildren()) {
                    String title = courseSnap.child("title").getValue(String.class);
                    String code = courseSnap.child("code").getValue(String.class);
                    String instructor = courseSnap.child("instructor_short").getValue(String.class);
                    String durationStr = courseSnap.child("duration_minutes").getValue(String.class);
                    int duration = 60; // default
                    if (durationStr != null) {
                        try {
                            duration = Integer.parseInt(durationStr);
                        } catch (Exception ignored) {}
                    }

                    DataSnapshot scheduleSnap = courseSnap.child("schedule");
                    for (DataSnapshot slotSnap : scheduleSnap.getChildren()) {
                        String day = slotSnap.child("day").getValue(String.class);
                        String time = slotSnap.child("slot").getValue(String.class);
                        String venue = slotSnap.child("venue").getValue(String.class);

                        if (day != null && time != null && venue != null) {
                            TimetableSlot slot = new TimetableSlot(code, title, instructor, day, time, venue, duration);
                            if (slotsByDay.containsKey(day)) {
                                slotsByDay.get(day).add(slot);
                            }
                        }
                    }
                }

                // Sort all days
                for (List<TimetableSlot> list : slotsByDay.values()) {
                    Collections.sort(list);
                }

                // Initially show classes
                updateClassesDisplay();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Log.e("Timetable", "Error fetching timetable", error.toException());
            }
        });
    }

    private void updateClassesDisplay() {
        List<TimetableSlot> slots = slotsByDay.get(currentSelectedDayShort);
        if (slots == null) slots = new ArrayList<>();
        
        if (isDayView) {
            dayViewAdapter.updateData(slots);
        } else {
            weekViewAdapter.updateData(slots);
        }
    }
}
