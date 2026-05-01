package com.example.campusbiome;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Screen 2 – shows tasks for ONE specific event.
 * Firebase path: Societies/{societyId}/events/{eventId}/tasks/{taskId}
 */
public class SocietyTaskListFragment extends Fragment {

    private String societyId;
    private String eventId;
    private String eventTitle;

    private RecyclerView         rvTasks;
    private FloatingActionButton btnAddTask;
    private TextView             tvEventTitle, tvEmpty;

    private final List<SocietyTask> taskList = new ArrayList<>();
    private final List<String>      taskIds  = new ArrayList<>();
    private SocietyTaskAdapter adapter;

    // ✅ Path: Societies/{societyId}/events/{eventId}/tasks
    private DatabaseReference tasksRef;

    // ── Factory ───────────────────────────────────────────────────────────────
    public static SocietyTaskListFragment newInstance(String societyId,
                                                      String eventId,
                                                      String eventTitle) {
        SocietyTaskListFragment frag = new SocietyTaskListFragment();
        Bundle args = new Bundle();
        args.putString("societyId",  societyId);
        args.putString("eventId",    eventId);
        args.putString("eventTitle", eventTitle);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_society_task_list, container, false);

        rvTasks      = view.findViewById(R.id.rvTasks);
        btnAddTask   = view.findViewById(R.id.btnAddTask);
        tvEventTitle = view.findViewById(R.id.tvTasksEventTitle);
        tvEmpty      = view.findViewById(R.id.tvNoTasks);

        if (getArguments() != null) {
            societyId  = getArguments().getString("societyId");
            eventId    = getArguments().getString("eventId");
            eventTitle = getArguments().getString("eventTitle");
        }

        if (societyId == null || eventId == null) {
            Toast.makeText(getContext(), "Missing event info", Toast.LENGTH_SHORT).show();
            return view;
        }

        if (tvEventTitle != null && eventTitle != null) {
            tvEventTitle.setText(eventTitle);
        }

        rvTasks.setLayoutManager(new LinearLayoutManager(getContext()));

        // ✅ Tasks are nested under each event, not at society level
        tasksRef = FirebaseDatabase.getInstance()
                .getReference("Societies")
                .child(societyId)
                .child("events")
                .child(eventId)
                .child("tasks");

        adapter = new SocietyTaskAdapter(taskList, taskIds, this::toggleTaskStatus);
        rvTasks.setAdapter(adapter);

        loadTasks();
        btnAddTask.setOnClickListener(v -> showAddTaskDialog());

        return view;
    }

    // ── Load tasks for THIS event only ────────────────────────────────────────
    private void loadTasks() {
        tasksRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                taskList.clear();
                taskIds.clear();

                for (DataSnapshot snap : snapshot.getChildren()) {
                    SocietyTask task = snap.getValue(SocietyTask.class);
                    if (task != null) {
                        task.setId(snap.getKey());
                        task.setSocietyId(societyId);
                        taskList.add(0, task);
                        taskIds.add(0, snap.getKey());
                    }
                }

                if (taskList.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvTasks.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvTasks.setVisibility(View.VISIBLE);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Toggle pending ↔ completed ────────────────────────────────────────────
    private void toggleTaskStatus(SocietyTask task, String taskId) {
        String newStatus = task.isCompleted() ? "pending" : "completed";
        tasksRef.child(taskId).child("status").setValue(newStatus)
                .addOnSuccessListener(unused ->
                        Toast.makeText(getContext(),
                                "Status updated to " + newStatus, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ── Add task dialog ───────────────────────────────────────────────────────
    private void showAddTaskDialog() {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_add_task, null);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        EditText etTaskName    = dialogView.findViewById(R.id.etTaskName);
        EditText etTime        = dialogView.findViewById(R.id.etTaskTime);
        EditText etDescription = dialogView.findViewById(R.id.etTaskDescription);
        Spinner  spDay         = dialogView.findViewById(R.id.spTaskDay);
        Spinner  spMonth       = dialogView.findViewById(R.id.spTaskMonth);
        Spinner  spYear        = dialogView.findViewById(R.id.spTaskYear);
        Spinner  spAmPm        = dialogView.findViewById(R.id.spTaskAmPm);
        Spinner  spPriority    = dialogView.findViewById(R.id.spTaskPriority);
        Button   btnCancel     = dialogView.findViewById(R.id.btnTaskCancel);
        Button   btnPublish    = dialogView.findViewById(R.id.btnTaskPublish);

        setupSpinners(spDay, spMonth, spYear, spAmPm, spPriority);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnPublish.setOnClickListener(v -> {
            String name     = etTaskName.getText().toString().trim();
            String desc     = etDescription.getText().toString().trim();
            String day      = spDay.getSelectedItem().toString();
            String month    = spMonth.getSelectedItem().toString();
            String year     = spYear.getSelectedItem().toString();
            String time     = etTime.getText().toString().trim();
            String ampm     = spAmPm.getSelectedItem().toString();
            String priority = spPriority.getSelectedItem().toString();

            if (TextUtils.isEmpty(name)) {
                Toast.makeText(getContext(), "Task name is required", Toast.LENGTH_SHORT).show();
                return;
            }

            addTask(name, desc, day, month, year, time, ampm, priority);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void setupSpinners(Spinner spDay, Spinner spMonth,
                               Spinner spYear, Spinner spAmPm, Spinner spPriority) {
        List<String> days = new ArrayList<>();
        for (int i = 1; i <= 31; i++) days.add(String.valueOf(i));

        List<String> months   = Arrays.asList("January","February","March","April",
                "May","June","July","August","September","October","November","December");
        List<String> years    = Arrays.asList("2025","2026","2027");
        List<String> ampm     = Arrays.asList("am","pm");
        List<String> priority = Arrays.asList("High","Medium","Low");

        spDay.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, days));
        spMonth.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, months));
        spYear.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, years));
        spAmPm.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, ampm));
        spPriority.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, priority));
    }

    // ── Write new task under Societies/{societyId}/events/{eventId}/tasks/ ────
    private void addTask(String name, String desc,
                         String day, String month, String year,
                         String time, String ampm, String priority) {

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "unknown";

        String taskKey = tasksRef.push().getKey();
        if (taskKey == null) return;

        String createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(new Date());

        SocietyTask task = new SocietyTask(
                name, desc,
                null, null,   // assignedTo / assignedToName — extend later
                uid,
                day, month, year, time, ampm,
                priority, "pending");
        task.setCreatedAt(createdAt);

        // ✅ Writes directly under the event's tasks node
        tasksRef.child(taskKey).setValue(task)
                .addOnSuccessListener(unused ->
                        Toast.makeText(getContext(), "Task Added!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
