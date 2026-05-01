package com.example.campusbiome;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusbiome.societies.adapters.SocietyAdapter;
import com.example.campusbiome.societies.models.Society;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SocietiesFragment extends Fragment {

    private static final String TAG = "SocietiesFragment";

    // ── Views ────────────────────────────────────────────────────────────────
    private RecyclerView      recyclerMy, recyclerBrowse;
    private TextView          txtNoMy, txtMyCount, txtNoBrowse, txtBrowseCount;
    private Button            btnPropose;
    private TextInputEditText etSearch;

    // ── Data ─────────────────────────────────────────────────────────────────
    private final List<Society> allSocieties   = new ArrayList<>();
    private final List<String>  allSocietyIds  = new ArrayList<>();
    // societyIds where user has a PENDING registration request
    private final List<String>  pendingIds     = new ArrayList<>();

    private String currentUid = null;

    private DatabaseReference  societiesRef;
    private ValueEventListener societiesListener;

    // ── Lifecycle ────────────────────────────────────────────────────────────
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_societies, container, false);

        recyclerMy    = view.findViewById(R.id.recyclerMySocieties);
        recyclerBrowse = view.findViewById(R.id.recyclerBrowseSocieties);
        txtNoMy       = view.findViewById(R.id.txtNoMySocieties);
        txtMyCount    = view.findViewById(R.id.txtMySocietiesCount);
        txtNoBrowse   = view.findViewById(R.id.txtNoBrowse);
        txtBrowseCount = view.findViewById(R.id.txtBrowseCount);
        btnPropose    = view.findViewById(R.id.btnProposeSociety);
        etSearch      = view.findViewById(R.id.etSearchSociety);

        recyclerMy.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerMy.setNestedScrollingEnabled(false);
        recyclerBrowse.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerBrowse.setNestedScrollingEnabled(false);

        // Get current user UID
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) currentUid = user.getUid();

        // Propose button
        btnPropose.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new RegisterSocietyFragment())
                        .addToBackStack(null)
                        .commit());

        // Search
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                public void afterTextChanged(Editable s) {}
                public void onTextChanged(CharSequence s, int a, int b, int c) {
                    renderLists(s.toString().trim().toLowerCase());
                }
            });
        }

        loadData();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (societiesRef != null && societiesListener != null)
            societiesRef.removeEventListener(societiesListener);
    }

    // ── Firebase ─────────────────────────────────────────────────────────────
    private void loadData() {
        societiesRef = FirebaseDatabase.getInstance().getReference("Societies");

        societiesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allSocieties.clear();
                allSocietyIds.clear();
                pendingIds.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Society s = ds.getValue(Society.class);
                    if (s == null) continue;

                    // Only show approved societies to students
                    if (!"approved".equals(s.getStatus())) continue;

                    s.setId(ds.getKey());
                    allSocieties.add(s);
                    allSocietyIds.add(ds.getKey());

                    // Check if user has a pending request in this society
                    if (currentUid != null) {
                        DataSnapshot requests = ds.child("registrationRequests");
                        for (DataSnapshot req : requests.getChildren()) {
                            String reqUid   = req.child("applicantUid").getValue(String.class);
                            String reqStatus = req.child("status").getValue(String.class);
                            if (currentUid.equals(reqUid) && "pending".equals(reqStatus)) {
                                pendingIds.add(ds.getKey());
                            }
                        }
                    }
                }

                renderLists(currentSearchQuery());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to load societies.", Toast.LENGTH_SHORT).show();
            }
        };

        societiesRef.addValueEventListener(societiesListener);
    }

    // ── Rendering ────────────────────────────────────────────────────────────
    private void renderLists(String query) {
        List<Society> mySocieties    = new ArrayList<>();
        List<String>  myIds          = new ArrayList<>();
        List<Society> browseSocieties = new ArrayList<>();
        List<String>  browseIds      = new ArrayList<>();

        for (int i = 0; i < allSocieties.size(); i++) {
            Society s  = allSocieties.get(i);
            String  id = allSocietyIds.get(i);

            if (!query.isEmpty() && !matchesQuery(s, query)) continue;

            if (currentUid != null && s.isMember(currentUid)) {
                mySocieties.add(s);
                myIds.add(id);
            } else {
                browseSocieties.add(s);
                browseIds.add(id);
            }
        }

        // My Societies
        txtMyCount.setText(String.valueOf(mySocieties.size()));
        if (mySocieties.isEmpty()) {
            txtNoMy.setVisibility(View.VISIBLE);
            recyclerMy.setVisibility(View.GONE);
        } else {
            txtNoMy.setVisibility(View.GONE);
            recyclerMy.setVisibility(View.VISIBLE);
            recyclerMy.setAdapter(new SocietyAdapter(
                    mySocieties, myIds, currentUid, pendingIds,
                    this::openSocietyDetails));
        }

        // Browse Societies
        txtBrowseCount.setText(String.valueOf(browseSocieties.size()));
        if (browseSocieties.isEmpty()) {
            txtNoBrowse.setVisibility(View.VISIBLE);
            recyclerBrowse.setVisibility(View.GONE);
        } else {
            txtNoBrowse.setVisibility(View.GONE);
            recyclerBrowse.setVisibility(View.VISIBLE);
            recyclerBrowse.setAdapter(new SocietyAdapter(
                    browseSocieties, browseIds, currentUid, pendingIds,
                    this::openSocietyDetails));
        }
    }

    // ── Navigation ───────────────────────────────────────────────────────────
    private void openSocietyDetails(Society society, String societyId) {
        SocietyDetailsFragment detailFrag =
                SocietyDetailsFragment.newInstance(societyId, society);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, detailFrag)
                .addToBackStack(null)
                .commit();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private boolean matchesQuery(Society s, String query) {
        if (s.getName()        != null && s.getName().toLowerCase().contains(query))        return true;
        if (s.getDescription() != null && s.getDescription().toLowerCase().contains(query)) return true;
        if (s.getCategory()    != null && s.getCategory().toLowerCase().contains(query))    return true;
        return false;
    }

    private String currentSearchQuery() {
        if (etSearch == null || etSearch.getText() == null) return "";
        return etSearch.getText().toString().trim().toLowerCase();
    }
}