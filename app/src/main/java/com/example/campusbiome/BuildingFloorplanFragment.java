package com.example.campusbiome;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
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

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class BuildingFloorplanFragment extends Fragment {

    private String buildingName;
    private CampusMapView floorplanMapView;
    private ProgressBar floorplanProgressBar;
    private LinearLayout llFloorPills;
    private TextView tvBuildingName;

    private Map<String, String> floorUrls = new HashMap<>();
    private List<String> floorKeys = new ArrayList<>();
    private String selectedFloorKey = null;

    private ValueEventListener wifiRoutersListener;
    private DatabaseReference currentWifiRoutersRef;

    public static BuildingFloorplanFragment newInstance(String buildingName) {
        BuildingFloorplanFragment fragment = new BuildingFloorplanFragment();
        Bundle args = new Bundle();
        args.putString("BUILDING_NAME", buildingName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_building_floorplan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            buildingName = getArguments().getString("BUILDING_NAME");
        }

        floorplanMapView = view.findViewById(R.id.floorplanMapView);
        floorplanProgressBar = view.findViewById(R.id.floorplanProgressBar);
        llFloorPills = view.findViewById(R.id.llFloorPills);
        tvBuildingName = view.findViewById(R.id.tvBuildingName);
        ImageView btnBack = view.findViewById(R.id.btnBack);

        if (buildingName != null) {
            tvBuildingName.setText(buildingName);
        }

        btnBack.setOnClickListener(v -> {
            if (getActivity() instanceof StudentDashboardActivity) {
                ((StudentDashboardActivity) getActivity()).openFragment(new CampusMapFragment());
            }
        });

        ImageView btnInfo = view.findViewById(R.id.btnInfo);
        if (btnInfo != null) {
            btnInfo.setOnClickListener(v -> showInfoDialog());
        }

        com.google.android.material.floatingactionbutton.FloatingActionButton btnResetMap = view.findViewById(R.id.btnResetMap);
        if (floorplanMapView != null) {
            floorplanMapView.setOnTransformChangeListener(isModified -> {
                if (btnResetMap != null) {
                    btnResetMap.setVisibility(isModified ? View.VISIBLE : View.GONE);
                }
            });
        }

        if (btnResetMap != null) {
            btnResetMap.setOnClickListener(v -> {
                if (floorplanMapView != null) {
                    floorplanMapView.resetTransform();
                }
            });
        }

        loadFloorsFromFirebase();
    }

    private void showInfoDialog() {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_map_info, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        com.google.android.material.button.MaterialButton btnGotIt = dialogView.findViewById(R.id.btnGotIt);
        btnGotIt.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void loadFloorsFromFirebase() {
        if (buildingName == null) return;
        floorplanProgressBar.setVisibility(View.VISIBLE);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("campus_layout")
                .child(buildingName)
                .child("floors");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    floorUrls.clear();
                    floorKeys.clear();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String floorKey = ds.getKey();
                        String url = ds.getValue(String.class);
                        if (floorKey != null && url != null) {
                            floorKeys.add(floorKey);
                            floorUrls.put(floorKey, url);
                        }
                    }
                    if (!floorKeys.isEmpty()) {
                        // Determine default floor (e.g. Ground or first available)
                        selectedFloorKey = floorKeys.contains("G") ? "G" : floorKeys.get(0);
                        populateFloorPills();
                        loadSvgForFloor(selectedFloorKey);
                        fetchWifiRouters(selectedFloorKey);
                    } else {
                        floorplanProgressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "No floorplans uploaded yet.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    floorplanProgressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "No floorplans available for this building.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                floorplanProgressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Error loading floors.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateFloorPills() {
        llFloorPills.removeAllViews();
        for (String floorKey : floorKeys) {
            MaterialCardView card = new MaterialCardView(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(8, 0, 8, 0);
            card.setLayoutParams(params);
            card.setRadius(dpToPx(16));
            card.setCardElevation(0);

            TextView tv = new TextView(requireContext());
            tv.setText(floorKey);
            tv.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
            tv.setTextSize(14f);
            tv.setIncludeFontPadding(false);

            if (floorKey.equals(selectedFloorKey)) {
                card.setCardBackgroundColor(Color.parseColor("#4B736B")); // Active dark green
                tv.setTextColor(Color.WHITE);
                tv.setTypeface(android.graphics.Typeface.defaultFromStyle(android.graphics.Typeface.BOLD));
            } else {
                card.setCardBackgroundColor(Color.parseColor("#B1C9C4")); // Inactive light green
                tv.setTextColor(Color.parseColor("#1A1C1C"));
                tv.setTypeface(android.graphics.Typeface.defaultFromStyle(android.graphics.Typeface.NORMAL));
            }

            card.addView(tv);
            card.setOnClickListener(v -> {
                if (!floorKey.equals(selectedFloorKey)) {
                    selectedFloorKey = floorKey;
                    populateFloorPills(); // Refresh UI
                    loadSvgForFloor(selectedFloorKey);
                    fetchWifiRouters(selectedFloorKey);
                }
            });
            llFloorPills.addView(card);
        }
    }

    private void loadSvgForFloor(String floorKey) {
        String urlString = floorUrls.get(floorKey);
        if (urlString == null) return;
        
        floorplanProgressBar.setVisibility(View.VISIBLE);
        floorplanMapView.setSvgString(""); // clear current

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    reader.close();

                    final String svgContent = stringBuilder.toString();

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            floorplanProgressBar.setVisibility(View.GONE);
                            floorplanMapView.setSvgString(svgContent);
                        });
                    }
                } else {
                    showErrorOnUIThread("HTTP " + connection.getResponseCode());
                }
            } catch (Exception e) {
                Log.e("BuildingFloorplan", "Error downloading SVG", e);
                showErrorOnUIThread("Download Error");
            }
        });
    }

    private void showErrorOnUIThread(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                floorplanProgressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Failed to load floorplan: " + message, Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void fetchWifiRouters(String floorKey) {
        if (currentWifiRoutersRef != null && wifiRoutersListener != null) {
            currentWifiRoutersRef.removeEventListener(wifiRoutersListener);
        }
        currentWifiRoutersRef = FirebaseDatabase.getInstance().getReference("campus_layout")
                .child("wifi_routers")
                .child(buildingName)
                .child(floorKey);
                
        wifiRoutersListener = currentWifiRoutersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<com.example.campusbiome.models.WifiRouter> routers = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    com.example.campusbiome.models.WifiRouter router = ds.getValue(com.example.campusbiome.models.WifiRouter.class);
                    if (router != null) routers.add(router);
                }
                if (floorplanMapView != null) {
                    floorplanMapView.setWifiRouters(routers);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null && com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
                    Log.e("BuildingFloorplan", "Failed to fetch routers: " + error.getMessage());
                }
            }
        });
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
