package com.example.campusbiome;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class EventRegistrationsFragment extends Fragment {

    private static final String ARG_SOCIETY_ID = "societyId";
    private static final String ARG_EVENT_ID   = "eventId";
    private static final String ARG_EVENT_TITLE = "eventTitle";

    private RecyclerView recycler;
    private TextView     txtEmpty, txtCount;

    private String societyId, eventId, eventTitle;

    // ── Factory ───────────────────────────────────────────────────────────────
    public static EventRegistrationsFragment newInstance(String societyId,
                                                         String eventId,
                                                         String eventTitle) {
        EventRegistrationsFragment f = new EventRegistrationsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SOCIETY_ID,  societyId);
        args.putString(ARG_EVENT_ID,    eventId);
        args.putString(ARG_EVENT_TITLE, eventTitle);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Build layout programmatically (no extra XML needed)
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#F5F7FA"));

        if (getArguments() != null) {
            societyId  = getArguments().getString(ARG_SOCIETY_ID);
            eventId    = getArguments().getString(ARG_EVENT_ID);
            eventTitle = getArguments().getString(ARG_EVENT_TITLE);
        }

        // ── Toolbar ──────────────────────────────────────────────────────────
        MaterialToolbar toolbar = new MaterialToolbar(requireContext());
        LinearLayout.LayoutParams tbParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 160);
        toolbar.setLayoutParams(tbParams);
        toolbar.setTitle(eventTitle != null ? eventTitle : "Registrations");
        toolbar.setSubtitle("Registered Students");
        toolbar.setBackgroundColor(Color.parseColor("#529F93"));
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setSubtitleTextColor(Color.parseColor("#D0EDE9"));
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationIconTint(Color.WHITE);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
        root.addView(toolbar);

        // ── Count header ──────────────────────────────────────────────────────
        txtCount = new TextView(getContext());
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        countParams.setMargins(48, 32, 48, 8);
        txtCount.setLayoutParams(countParams);
        txtCount.setTextSize(14f);
        txtCount.setTextColor(Color.parseColor("#526966"));
        txtCount.setText("Loading registrations…");
        root.addView(txtCount);

        // ── Empty state ───────────────────────────────────────────────────────
        txtEmpty = new TextView(getContext());
        LinearLayout.LayoutParams emptyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        emptyParams.setMargins(48, 32, 48, 8);
        txtEmpty.setLayoutParams(emptyParams);
        txtEmpty.setTextSize(14f);
        txtEmpty.setTextColor(Color.parseColor("#9E9E9E"));
        txtEmpty.setText("No students have registered yet.");
        txtEmpty.setGravity(android.view.Gravity.CENTER);
        txtEmpty.setVisibility(View.GONE);
        root.addView(txtEmpty);

        // ── RecyclerView ──────────────────────────────────────────────────────
        recycler = new RecyclerView(requireContext());
        LinearLayout.LayoutParams rvParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        rvParams.setMargins(32, 0, 32, 32);
        recycler.setLayoutParams(rvParams);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        root.addView(recycler);

        loadRegistrations();

        return root;
    }

    private void loadRegistrations() {
        FirebaseDatabase.getInstance()
                .getReference("Societies")
                .child(societyId)
                .child("events")
                .child(eventId)
                .child("registrations")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> uids = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            uids.add(ds.getKey());
                        }

                        txtCount.setText(uids.size() + " student(s) registered");

                        if (uids.isEmpty()) {
                            txtEmpty.setVisibility(View.VISIBLE);
                            recycler.setVisibility(View.GONE);
                            return;
                        }

                        txtEmpty.setVisibility(View.GONE);
                        recycler.setVisibility(View.VISIBLE);
                        fetchUserDetails(uids);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(),
                                "Failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── Fetch name+email for each registered UID ──────────────────────────────
    private void fetchUserDetails(List<String> uids) {
        List<String[]> users = new ArrayList<>(); // each: [name, email, uid]
        final int[] remaining = {uids.size()};

        for (String uid : uids) {
            FirebaseDatabase.getInstance()
                    .getReference("Users").child(uid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String name  = snapshot.child("name").getValue(String.class);
                            String email = snapshot.child("email").getValue(String.class);
                            users.add(new String[]{
                                    name  != null ? name  : "Unknown",
                                    email != null ? email : "",
                                    uid
                            });
                            remaining[0]--;
                            if (remaining[0] == 0) {
                                // All fetched — set adapter
                                recycler.setAdapter(new RegistrationAdapter(users));
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            users.add(new String[]{"Unknown", "", uid});
                            remaining[0]--;
                            if (remaining[0] == 0) {
                                recycler.setAdapter(new RegistrationAdapter(users));
                            }
                        }
                    });
        }
    }

    // ── Inline adapter ────────────────────────────────────────────────────────
    private static class RegistrationAdapter
            extends RecyclerView.Adapter<RegistrationAdapter.VH> {

        private final List<String[]> users; // [name, email, uid]

        RegistrationAdapter(List<String[]> users) { this.users = users; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            CardView card = new CardView(parent.getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 20);
            card.setLayoutParams(params);
            card.setRadius(24f);
            card.setCardElevation(2f);
            card.setCardBackgroundColor(Color.WHITE);

            LinearLayout row = new LinearLayout(parent.getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(32, 24, 32, 24);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            card.addView(row);

            // Avatar circle
            TextView avatar = new TextView(parent.getContext());
            LinearLayout.LayoutParams avParams = new LinearLayout.LayoutParams(80, 80);
            avParams.setMarginEnd(24);
            avatar.setLayoutParams(avParams);
            avatar.setGravity(android.view.Gravity.CENTER);
            avatar.setTextSize(16f);
            avatar.setTextColor(Color.WHITE);
            avatar.setTypeface(null, android.graphics.Typeface.BOLD);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(Color.parseColor("#529F93"));
            avatar.setBackground(bg);
            row.addView(avatar);

            // Name + email
            LinearLayout info = new LinearLayout(parent.getContext());
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            row.addView(info);

            TextView tvName = new TextView(parent.getContext());
            tvName.setTextSize(14f);
            tvName.setTextColor(Color.parseColor("#1A1C1C"));
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            info.addView(tvName);

            TextView tvEmail = new TextView(parent.getContext());
            tvEmail.setTextSize(12f);
            tvEmail.setTextColor(Color.parseColor("#526966"));
            LinearLayout.LayoutParams emailParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            emailParams.topMargin = 4;
            tvEmail.setLayoutParams(emailParams);
            info.addView(tvEmail);

            return new VH(card, avatar, tvName, tvEmail);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            String[] u = users.get(pos);
            h.tvName.setText(u[0]);
            h.tvEmail.setText(u[1]);
            // Avatar initial
            String initial = u[0].isEmpty() ? "?" : String.valueOf(u[0].charAt(0)).toUpperCase();
            h.avatar.setText(initial);
        }

        @Override public int getItemCount() { return users.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView avatar, tvName, tvEmail;
            VH(CardView card, TextView av, TextView n, TextView e) {
                super(card);
                avatar = av; tvName = n; tvEmail = e;
            }
        }
    }
}