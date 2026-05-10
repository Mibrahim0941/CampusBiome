package com.example.campusbiome;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class CampusMapFragment extends Fragment {

    private CampusMapView campusMapView;
    private android.widget.ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_campus_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        campusMapView = view.findViewById(R.id.campusMapView);
        progressBar = view.findViewById(R.id.mapProgressBar);
        com.google.android.material.floatingactionbutton.FloatingActionButton btnResetMap = view.findViewById(R.id.btnResetMap);

        if (campusMapView != null) {
            campusMapView.setOnBuildingClickListener(new CampusMapView.OnBuildingClickListener() {
                @Override
                public void onBuildingClick(String buildingName) {
                    if (getActivity() instanceof StudentDashboardActivity) {
                        BuildingFloorplanFragment floorplanFragment = BuildingFloorplanFragment.newInstance(buildingName);
                        ((StudentDashboardActivity) getActivity()).openFragment(floorplanFragment);
                    }
                }
            });

            campusMapView.setOnTransformChangeListener(new CampusMapView.OnTransformChangeListener() {
                @Override
                public void onTransformChanged(boolean isModified) {
                    if (btnResetMap != null) {
                        btnResetMap.setVisibility(isModified ? View.VISIBLE : View.GONE);
                    }
                }
            });
        }

        if (btnResetMap != null) {
            btnResetMap.setOnClickListener(v -> {
                if (campusMapView != null) {
                    campusMapView.resetTransform();
                }
            });
        }
        
        android.widget.ImageView btnInfo = view.findViewById(R.id.btnInfo);
        if (btnInfo != null) {
            btnInfo.setOnClickListener(v -> {
                showInfoDialog();
            });
        }

        loadMapDataFromFirebase();
    }
    
    private void loadMapDataFromFirebase() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        com.google.firebase.database.DatabaseReference ref = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("campus_layout/map_svg_url");
        ref.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                String url = snapshot.getValue(String.class);
                if (url != null && !url.isEmpty()) {
                    downloadSvgContent(url);
                } else {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    android.widget.Toast.makeText(getContext(), "No map URL found in database.", android.widget.Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                android.widget.Toast.makeText(getContext(), "Failed to load map data.", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadSvgContent(String urlString) {
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                java.net.URL url = new java.net.URL(urlString);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                if (connection.getResponseCode() == java.net.HttpURLConnection.HTTP_OK) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    reader.close();

                    final String svgContent = stringBuilder.toString();
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                            if (campusMapView != null) {
                                campusMapView.setSvgString(svgContent);
                            }
                        });
                    }
                } else {
                    showErrorOnUIThread("Failed to download map (HTTP " + connection.getResponseCode() + ")");
                }
            } catch (Exception e) {
                android.util.Log.e("CampusMapFragment", "Error downloading SVG", e);
                showErrorOnUIThread("Error downloading map data");
            }
        });
    }
    
    private void showErrorOnUIThread(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                android.widget.Toast.makeText(getContext(), "Failed to load map: " + message, android.widget.Toast.LENGTH_SHORT).show();
            });
        }
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
}
