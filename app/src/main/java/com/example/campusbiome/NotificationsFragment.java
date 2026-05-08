package com.example.campusbiome;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private ImageView btnBack;
    private LinearLayout llAllNotificationsContainer;
    private ProgressBar progressBar;
    
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        btnBack = view.findViewById(R.id.btnBack);
        llAllNotificationsContainer = view.findViewById(R.id.llAllNotificationsContainer);
        progressBar = view.findViewById(R.id.progressBar);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                ((StudentDashboardActivity) getActivity()).findViewById(R.id.navHome).performClick();
            }
        });

        fetchNotifications();

        return view;
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
                            "announcement", ts, true, true, null));
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
                                    "announcement", ts, true, true, null));
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
                                    "announcement", ts, true, true, null));
                            }
                        }
                    }
                }
                onQueryComplete.run();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { onQueryComplete.run(); }
        });
    }

    private void fetchNotifications() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        mDatabase.child("Notifications").child(uid).orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot userNotifsSnapshot) {
                        fetchAllRelevantAnnouncements(uid, announcementsItems -> {
                            if (!isAdded()) return;
                            progressBar.setVisibility(View.GONE);
                            llAllNotificationsContainer.removeAllViews();

                            java.util.List<NotificationItem> allItems = new java.util.ArrayList<>();

                            // Add user notifications
                            if (userNotifsSnapshot.exists()) {
                                for (DataSnapshot ds : userNotifsSnapshot.getChildren()) {
                                    String title = ds.child("title").getValue(String.class);
                                    String message = ds.child("message").getValue(String.class);
                                    String type = ds.child("type").getValue(String.class);
                                    Long timestamp = ds.child("timestamp").getValue(Long.class);
                                    Boolean isRead = ds.child("isRead").getValue(Boolean.class);
                                    if (timestamp != null) {
                                        allItems.add(new NotificationItem(title, message, type, timestamp, isRead != null && isRead, false, ds.getRef()));
                                    }
                                }
                            }

                            // Add relevant announcements
                            allItems.addAll(announcementsItems);

                            if (allItems.isEmpty()) {
                                TextView empty = new TextView(getContext());
                                empty.setText("No notifications available.");
                                empty.setTextColor(android.graphics.Color.parseColor("#8F9B99"));
                                llAllNotificationsContainer.addView(empty);
                                return;
                            }

                            // Sort descending
                            java.util.Collections.sort(allItems, (o1, o2) -> Long.compare(o2.timestamp, o1.timestamp));

                            for (NotificationItem item : allItems) {
                                View card = LayoutInflater.from(getContext())
                                        .inflate(R.layout.item_notification, llAllNotificationsContainer, false);

                                ImageView icon = card.findViewById(R.id.ivNotifIcon);
                                TextView tvTitle = card.findViewById(R.id.tvNotifTitle);
                                TextView tvMessage = card.findViewById(R.id.tvNotifMessage);
                                TextView tvTime = card.findViewById(R.id.tvNotifTime);

                                tvTitle.setText(item.title != null ? item.title : "Notification");
                                tvMessage.setText(item.message != null ? item.message : "");
                                
                                CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                                        item.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
                                tvTime.setText(relativeTime);

                                if ("alert".equals(item.type)) {
                                    icon.setImageResource(android.R.drawable.ic_dialog_alert);
                                    icon.setColorFilter(android.graphics.Color.parseColor("#D32F2F"));
                                    tvTitle.setTextColor(android.graphics.Color.parseColor("#D32F2F"));
                                } else if (item.isGlobal) {
                                    icon.setImageResource(android.R.drawable.ic_menu_agenda);
                                    icon.setColorFilter(android.graphics.Color.parseColor("#1A1C1C"));
                                }
                                
                                if (!item.isRead) {
                                    tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                                    tvMessage.setTextColor(android.graphics.Color.parseColor("#1A1C1C"));
                                    
                                    // Mark as read on click
                                    if (item.ref != null) {
                                        card.setOnClickListener(v -> {
                                            item.ref.child("isRead").setValue(true);
                                            tvTitle.setTypeface(null, android.graphics.Typeface.NORMAL);
                                        });
                                    }
                                }

                                llAllNotificationsContainer.addView(card);
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (isAdded()) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Failed to load notifications", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private static class NotificationItem {
        String title, message, type;
        long timestamp;
        boolean isRead, isGlobal;
        DatabaseReference ref;

        NotificationItem(String title, String message, String type, long timestamp, boolean isRead, boolean isGlobal, DatabaseReference ref) {
            this.title = title;
            this.message = message;
            this.type = type;
            this.timestamp = timestamp;
            this.isRead = isRead;
            this.isGlobal = isGlobal;
            this.ref = ref;
        }
    }
}
