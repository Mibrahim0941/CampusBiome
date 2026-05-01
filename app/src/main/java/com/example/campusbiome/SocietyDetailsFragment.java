package com.example.campusbiome;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // Changed from MaterialButton to Button to match XML
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusbiome.societies.models.Society;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SocietyDetailsFragment extends Fragment {

    public static final String ARG_SOCIETY_ID = "societyId";
    public static final String ARG_SOCIETY    = "society";

    // ── Views (Mapped to your exact XML IDs) ─────────────────────────────────
    private ImageButton btnBack;
    private TextView    txtName, txtCategory, txtDescription;
    private TextView    txtStatMembers, txtStatEvents, txtStatPosts;
    private Button      btnJoin, btnLeave; // Changed from MaterialButton
    private CardView    cardPending;
    private LinearLayout layoutMemberActions;
    private RecyclerView recyclerAnnouncements, recyclerEvents;
    private TextView     txtNoAnnouncements, txtNoEvents;

    private String  societyId;
    private Society society;
    private String  currentUid;
    private boolean isMember  = false;
    private boolean isPending = false;

    private DatabaseReference  societyRef;
    private ValueEventListener societyListener;

    public static SocietyDetailsFragment newInstance(String societyId, Society society) {
        SocietyDetailsFragment f = new SocietyDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SOCIETY_ID, societyId);
        args.putSerializable(ARG_SOCIETY, society);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_society_details, container, false);

        if (getArguments() != null) {
            societyId = getArguments().getString(ARG_SOCIETY_ID);
            society   = (Society) getArguments().getSerializable(ARG_SOCIETY);
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) currentUid = user.getUid();

        // ── Wire views to XML IDs ─────────────────────────────────────────────
        txtName               = view.findViewById(R.id.txtDetailName);
        txtCategory           = view.findViewById(R.id.txtDetailCategory);
        txtDescription        = view.findViewById(R.id.txtDetailDescription);

        txtStatMembers        = view.findViewById(R.id.txtStatMembersCount);
        txtStatEvents         = view.findViewById(R.id.txtStatEventsCount);
        txtStatPosts          = view.findViewById(R.id.txtStatAnnouncementsCount);

        btnJoin               = view.findViewById(R.id.btnJoinSociety); // Fixed ID
        btnLeave              = view.findViewById(R.id.btnLeaveSociety); // Fixed ID
        cardPending           = view.findViewById(R.id.cardPendingNotice);
        layoutMemberActions   = view.findViewById(R.id.layoutMemberActions);

        recyclerAnnouncements = view.findViewById(R.id.recyclerAnnouncements);
        recyclerEvents        = view.findViewById(R.id.recyclerEvents);
        txtNoAnnouncements    = view.findViewById(R.id.txtNoAnnouncements);
        txtNoEvents           = view.findViewById(R.id.txtNoEvents);

        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        recyclerAnnouncements.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerEvents.setLayoutManager(new LinearLayoutManager(getContext()));

        populateHeader();
        listenToSocietyUpdates();

        return view;
    }

    private void populateHeader() {
        if (society == null) return;
        txtName.setText(society.getName());
        txtCategory.setText(society.getCategory());
        txtDescription.setText(society.getDescription());
    }

    private void listenToSocietyUpdates() {
        societyRef = FirebaseDatabase.getInstance().getReference("Societies").child(societyId);
        societyListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isMember = currentUid != null && snapshot.child("members").child(currentUid).exists();

                isPending = false;
                DataSnapshot requests = snapshot.child("registrationRequests");
                for (DataSnapshot req : requests.getChildren()) {
                    if (currentUid != null && currentUid.equals(req.child("applicantUid").getValue(String.class))
                            && "pending".equals(req.child("status").getValue(String.class))) {
                        isPending = true;
                        break;
                    }
                }

                updateButtons();

                // Stats
                txtStatMembers.setText(String.valueOf(snapshot.child("members").getChildrenCount()));
                txtStatEvents.setText(String.valueOf(snapshot.child("events").getChildrenCount()));
                txtStatPosts.setText(String.valueOf(snapshot.child("announcements").getChildrenCount()));

                loadAnnouncements(snapshot);
                loadEvents(snapshot);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        societyRef.addValueEventListener(societyListener);
    }

    private void updateButtons() {
        if (isMember) {
            btnJoin.setVisibility(View.GONE);
            cardPending.setVisibility(View.GONE);
            layoutMemberActions.setVisibility(View.VISIBLE);
            btnLeave.setOnClickListener(v -> confirmLeave());
        } else if (isPending) {
            btnJoin.setVisibility(View.GONE);
            cardPending.setVisibility(View.VISIBLE);
            layoutMemberActions.setVisibility(View.GONE);
        } else {
            btnJoin.setVisibility(View.VISIBLE);
            cardPending.setVisibility(View.GONE);
            layoutMemberActions.setVisibility(View.GONE);
            btnJoin.setOnClickListener(v -> applyToJoin());
        }
    }

    // ── Join ──────────────────────────────────────────────────────────────────
    private void applyToJoin() {
        if (currentUid == null) {
            Toast.makeText(getContext(), "Please log in to join.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseDatabase.getInstance().getReference("Users").child(currentUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name  = snapshot.child("name").getValue(String.class);
                        String email = snapshot.child("email").getValue(String.class);
                        submitJoinRequest(
                                name  != null ? name  : "Unknown",
                                email != null ? email : "");
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        submitJoinRequest("Unknown", "");
                    }
                });
    }

    private void submitJoinRequest(String name, String email) {
        DatabaseReference reqRef = FirebaseDatabase.getInstance()
                .getReference("Societies").child(societyId)
                .child("registrationRequests").push();

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        reqRef.child("applicantUid").setValue(currentUid);
        reqRef.child("applicantName").setValue(name);
        reqRef.child("applicantEmail").setValue(email);
        reqRef.child("appliedDate").setValue(today);
        reqRef.child("appliedFor").setValue("Member");
        reqRef.child("status").setValue("pending")
                .addOnSuccessListener(unused ->
                        Toast.makeText(getContext(),
                                "Request submitted! Pending admin approval.",
                                Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ── Leave ─────────────────────────────────────────────────────────────────
    private void confirmLeave() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Leave Society")
                .setMessage("Are you sure you want to leave \"" + society.getName() + "\"?")
                .setPositiveButton("Leave", (d, w) -> leaveGroup())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void leaveGroup() {
        if (currentUid == null) return;
        FirebaseDatabase.getInstance().getReference("Societies")
                .child(societyId).child("members").child(currentUid)
                .removeValue()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(),
                            "Left " + society.getName(), Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                });
    }

    // ── Load announcements ────────────────────────────────────────────────────
    private void loadAnnouncements(DataSnapshot snapshot) {
        DataSnapshot annSnap = snapshot.child("announcements");
        if (!annSnap.exists() || annSnap.getChildrenCount() == 0) {
            txtNoAnnouncements.setVisibility(View.VISIBLE);
            recyclerAnnouncements.setVisibility(View.GONE);
            return;
        }

        java.util.List<String[]> items = new java.util.ArrayList<>();
        for (DataSnapshot ds : annSnap.getChildren()) {
            String title   = ds.child("title").getValue(String.class);
            String message = ds.child("message").getValue(String.class);
            String time    = ds.child("createdAt").getValue(String.class);
            items.add(new String[]{
                    title   != null ? title   : "Announcement",
                    message != null ? message : "",
                    time    != null ? time    : ""
            });
        }

        txtNoAnnouncements.setVisibility(View.GONE);
        recyclerAnnouncements.setVisibility(View.VISIBLE);
        recyclerAnnouncements.setAdapter(new AnnouncementEventAdapter(items, true));
    }

    // ── Load events ───────────────────────────────────────────────────────────
    private void loadEvents(DataSnapshot snapshot) {
        DataSnapshot evSnap = snapshot.child("events");
        if (!evSnap.exists() || evSnap.getChildrenCount() == 0) {
            txtNoEvents.setVisibility(View.VISIBLE);
            recyclerEvents.setVisibility(View.GONE);
            return;
        }

        java.util.List<String[]> items = new java.util.ArrayList<>();
        for (DataSnapshot ds : evSnap.getChildren()) {
            String title  = ds.child("title").getValue(String.class);
            String desc   = ds.child("description").getValue(String.class);
            String day    = ds.child("day").getValue(String.class);
            String month  = ds.child("month").getValue(String.class);
            String venue  = ds.child("venue").getValue(String.class);
            String date   = (day != null && month != null) ? day + " " + month : "";
            String sub    = (venue != null ? "📍 " + venue + "  " : "") + date;
            items.add(new String[]{
                    title != null ? title : "Event",
                    desc  != null ? desc  : "",
                    sub
            });
        }

        txtNoEvents.setVisibility(View.GONE);
        recyclerEvents.setVisibility(View.VISIBLE);
        recyclerEvents.setAdapter(new AnnouncementEventAdapter(items, false));
    }

    // ── Inline adapter ────────────────────────────────────────────────────────
    private static class AnnouncementEventAdapter
            extends RecyclerView.Adapter<AnnouncementEventAdapter.VH> {

        private final java.util.List<String[]> items;
        private final boolean isAnnouncement;

        AnnouncementEventAdapter(java.util.List<String[]> items, boolean isAnnouncement) {
            this.items          = items;
            this.isAnnouncement = isAnnouncement;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            CardView card = new CardView(parent.getContext());
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, 24);
            card.setLayoutParams(cardParams);
            card.setRadius(24f);
            card.setCardElevation(4f);
            card.setCardBackgroundColor(Color.WHITE);

            LinearLayout ll = new LinearLayout(parent.getContext());
            ll.setOrientation(LinearLayout.VERTICAL);
            ll.setPadding(36, 28, 36, 28);
            card.addView(ll);

            TextView tvTitle = new TextView(parent.getContext());
            tvTitle.setTextSize(14f);
            tvTitle.setTextColor(Color.parseColor("#1A1A2E"));
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvBody = new TextView(parent.getContext());
            tvBody.setTextSize(13f);
            tvBody.setTextColor(Color.parseColor("#6C7A9C"));
            tvBody.setLineSpacing(0f, 1.3f);
            LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            bodyParams.topMargin = 8;
            tvBody.setLayoutParams(bodyParams);

            TextView tvSub = new TextView(parent.getContext());
            tvSub.setTextSize(11f);
            tvSub.setTextColor(Color.parseColor("#9E9E9E"));
            LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            subParams.topMargin = 6;
            tvSub.setLayoutParams(subParams);

            ll.addView(tvTitle);
            ll.addView(tvBody);
            ll.addView(tvSub);

            return new VH(card, tvTitle, tvBody, tvSub);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            String[] item = items.get(pos);
            h.tvTitle.setText(item[0]);
            h.tvBody.setText(item[1]);
            h.tvSub.setText(item[2]);
            h.card.setCardBackgroundColor(Color.WHITE);
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            CardView card;
            TextView tvTitle, tvBody, tvSub;

            VH(CardView card, TextView t, TextView b, TextView s) {
                super(card);
                this.card = card;
                tvTitle = t;
                tvBody  = b;
                tvSub   = s;
            }
        }
    }
}