package com.example.campusbiome;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdminCampusMapFragment extends Fragment {

    private CampusMapView campusMapView;
    private ProgressBar progressBar;
    private TextView tvMapTitle, tvUploadPrompt, tvCurrentView, tvViewDescription;
    private LinearLayout layoutUploadPrompt, layoutBuildingActions, layoutFloorSelector, containerFloors;
    private MaterialButton btnUploadSvg, btnUpdateBlockSvg, btnBackToOverview, btnAddFloor;
    private ImageView btnBack;
    private com.google.android.material.floatingactionbutton.FloatingActionButton btnResetMap;

    private DatabaseReference mDatabase;
    private ExecutorService executorService;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    private static final String APPS_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbxaK54pzkqL-cosZzix7SoVrjwaaJVJqSHrUsP3QdwGZr_lhKpTAX7Gftt2IpEHsnol/exec";

    private String currentViewMode = "MAIN"; // "MAIN" or "BUILDING"
    private String selectedBuildingId = null;
    private String selectedFloorNum = null; // null for single-floor or overview

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        executorService = Executors.newSingleThreadExecutor();
        mDatabase = FirebaseDatabase.getInstance().getReference("campus_layout");

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            uploadSvgToDrive(uri);
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_campus_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        campusMapView = view.findViewById(R.id.campusMapView);
        progressBar = view.findViewById(R.id.mapProgressBar);
        tvMapTitle = view.findViewById(R.id.tvMapTitle);
        tvUploadPrompt = view.findViewById(R.id.tvUploadPrompt);
        tvCurrentView = view.findViewById(R.id.tvCurrentView);
        tvViewDescription = view.findViewById(R.id.tvViewDescription);
        layoutUploadPrompt = view.findViewById(R.id.layoutUploadPrompt);
        layoutBuildingActions = view.findViewById(R.id.layoutBuildingActions);
        layoutFloorSelector = view.findViewById(R.id.layoutFloorSelector);
        containerFloors = view.findViewById(R.id.containerFloors);
        btnUploadSvg = view.findViewById(R.id.btnUploadSvg);
        btnUpdateBlockSvg = view.findViewById(R.id.btnUpdateBlockSvg);
        btnBackToOverview = view.findViewById(R.id.btnBackToOverview);
        btnAddFloor = view.findViewById(R.id.btnAddFloor);
        btnBack = view.findViewById(R.id.btnBack);
        btnResetMap = view.findViewById(R.id.btnResetMap);

        btnUploadSvg.setOnClickListener(v -> openFilePicker());
        btnUpdateBlockSvg.setOnClickListener(v -> openFilePicker());
        btnBackToOverview.setOnClickListener(v -> backToOverview());
        btnBack.setOnClickListener(v -> backToOverview());
        btnAddFloor.setOnClickListener(v -> showAddFloorDialog());

        if (campusMapView != null) {
            campusMapView.setOnBuildingClickListener(buildingName -> {
                if (currentViewMode.equals("MAIN")) {
                    enterBuildingView(buildingName);
                }
            });

            campusMapView.setOnTransformChangeListener(isModified -> {
                if (btnResetMap != null) {
                    btnResetMap.setVisibility(isModified ? View.VISIBLE : View.GONE);
                }
            });

            btnResetMap.setOnClickListener(v -> campusMapView.resetTransform());
        }

        loadMapData();
    }

    private boolean isMultiFloorBuilding(String buildingId) {
        if (buildingId == null) return false;
        String lower = buildingId.toLowerCase();
        return lower.contains("block") || lower.contains("library") || lower.contains("lib");
    }

    private void loadMapData() {
        showLoading(true);
        DatabaseReference ref;
        
        if (currentViewMode.equals("MAIN")) {
            ref = mDatabase.child("map_svg_url");
        } else {
            if (isMultiFloorBuilding(selectedBuildingId)) {
                if (selectedFloorNum == null) {
                    // We need to fetch floors first
                    fetchFloorsForBuilding(selectedBuildingId);
                    return;
                }
                ref = mDatabase.child(selectedBuildingId).child("floors").child(selectedFloorNum);
            } else {
                ref = mDatabase.child(selectedBuildingId + "_url");
            }
        }

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String url = snapshot.getValue(String.class);
                if (url != null && !url.isEmpty()) {
                    layoutUploadPrompt.setVisibility(View.GONE);
                    campusMapView.setVisibility(View.VISIBLE);
                    downloadSvg(url);
                } else {
                    showLoading(false);
                    campusMapView.setVisibility(View.INVISIBLE);
                    layoutUploadPrompt.setVisibility(View.VISIBLE);
                    String msg = currentViewMode.equals("MAIN") ? "No Campus Map Uploaded" : 
                                (selectedFloorNum != null ? "No Map for Floor " + selectedFloorNum : "No Map for " + selectedBuildingId);
                    tvUploadPrompt.setText(msg);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                if (getContext() != null && com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
                    Toast.makeText(getContext(), "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void fetchFloorsForBuilding(String buildingId) {
        mDatabase.child(buildingId).child("floors").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> floors = new ArrayList<>();
                for (DataSnapshot floorSnap : snapshot.getChildren()) {
                    floors.add(floorSnap.getKey());
                }
                
                // Sort floors naturally if possible
                Collections.sort(floors);

                runOnUiThread(() -> {
                    renderFloorCards(floors);
                    if (!floors.isEmpty()) {
                        selectedFloorNum = floors.get(0);
                        loadMapData();
                    } else {
                        showLoading(false);
                        campusMapView.setVisibility(View.INVISIBLE);
                        layoutUploadPrompt.setVisibility(View.VISIBLE);
                        tvUploadPrompt.setText("No Floors Added for " + buildingId);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
            }
        });
    }

    private void renderFloorCards(List<String> floors) {
        containerFloors.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        
        for (String floor : floors) {
            View cardView = inflater.inflate(R.layout.item_floor_card, containerFloors, false);
            TextView tvLabel = cardView.findViewById(R.id.tvFloorLabel);
            MaterialCardView cardFloor = cardView.findViewById(R.id.cardFloor);
            
            tvLabel.setText(floor);
            
            // Highlight selected
            if (floor.equals(selectedFloorNum)) {
                cardFloor.setStrokeWidth(4);
                cardFloor.setCardBackgroundColor(android.graphics.Color.parseColor("#F0F9F5"));
            } else {
                cardFloor.setStrokeWidth(0);
                cardFloor.setCardBackgroundColor(android.graphics.Color.WHITE);
            }
            
            cardFloor.setOnClickListener(v -> {
                selectedFloorNum = floor;
                renderFloorCards(floors);
                loadMapData();
            });
            
            containerFloors.addView(cardView);
        }
    }

    private void showAddFloorDialog() {
        if (getContext() == null) return;
        
        EditText etFloor = new EditText(getContext());
        etFloor.setHint("e.g. 1, 2, G");
        etFloor.setPadding(40, 40, 40, 40);

        new AlertDialog.Builder(getContext())
                .setTitle("Add New Floor")
                .setMessage("Enter the floor number or name for " + selectedBuildingId)
                .setView(etFloor)
                .setPositiveButton("Add", (dialog, which) -> {
                    String floor = etFloor.getText().toString().trim();
                    if (!floor.isEmpty()) {
                        addNewFloorToDb(floor);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addNewFloorToDb(String floorNum) {
        // Just set an empty string or placeholder to create the node
        mDatabase.child(selectedBuildingId).child("floors").child(floorNum).setValue("")
                .addOnSuccessListener(aVoid -> {
                    selectedFloorNum = floorNum;
                    fetchFloorsForBuilding(selectedBuildingId);
                });
    }

    private void downloadSvg(String urlString) {
        executorService.execute(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    reader.close();

                    final String svgContent = sb.toString();
                    runOnUiThread(() -> {
                        showLoading(false);
                        campusMapView.setSvgString(svgContent);
                    });
                } else {
                    throw new Exception("HTTP Error: " + conn.getResponseCode());
                }
            } catch (Exception e) {
                Log.e("AdminMap", "Download failed", e);
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(getContext(), "Failed to download SVG map", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        String[] mimeTypes = {"image/svg+xml", "image/png"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(Intent.createChooser(intent, "Select SVG or PNG Map"));
    }

    private void uploadSvgToDrive(Uri uri) {
        showLoading(true);
        executorService.execute(() -> {
            try {
                InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
                ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    byteBuffer.write(buffer, 0, len);
                }
                byte[] bytes = byteBuffer.toByteArray();
                
                String mimeType = getContext().getContentResolver().getType(uri);
                String base64Content;

                if ("image/png".equals(mimeType)) {
                    // Wrap the PNG in an SVG so it can be parsed natively by CampusMapView
                    android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                    int width = options.outWidth > 0 ? options.outWidth : 1000;
                    int height = options.outHeight > 0 ? options.outHeight : 1000;
                    
                    String base64Png = Base64.encodeToString(bytes, Base64.DEFAULT).replaceAll("\\s+", "");
                    String svgWrapper = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + width + " " + height + "\" width=\"" + width + "\" height=\"" + height + "\">\n" +
                                        "  <!-- map_background -->\n" +
                                        "  <image href=\"data:image/png;base64," + base64Png + "\" x=\"0\" y=\"0\" width=\"" + width + "\" height=\"" + height + "\" />\n" +
                                        "</svg>";
                    base64Content = Base64.encodeToString(svgWrapper.getBytes(), Base64.DEFAULT);
                } else {
                    base64Content = Base64.encodeToString(bytes, Base64.DEFAULT);
                }

                String filename;
                if (currentViewMode.equals("MAIN")) {
                    filename = "campus_map.svg";
                } else if (selectedFloorNum != null) {
                    filename = selectedBuildingId + "_floor_" + selectedFloorNum + ".svg";
                } else {
                    filename = selectedBuildingId + "_map.svg";
                }

                JSONObject json = new JSONObject();
                json.put("base64", base64Content);
                json.put("filename", filename);
                json.put("mimeType", "image/svg+xml");

                URL url = new URL(APPS_SCRIPT_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    Scanner scanner = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                    String response = scanner.hasNext() ? scanner.next() : "";
                    JSONObject respJson = new JSONObject(response);

                    if (respJson.has("url")) {
                        String driveUrl = respJson.getString("url");
                        if (driveUrl.contains("uc?id=") && !driveUrl.contains("export=download")) {
                            driveUrl = driveUrl.replace("uc?id=", "uc?export=download&id=");
                        }
                        
                        final String finalUrl = driveUrl;
                        DatabaseReference ref;
                        if (currentViewMode.equals("MAIN")) {
                            ref = mDatabase.child("map_svg_url");
                        } else if (selectedFloorNum != null) {
                            ref = mDatabase.child(selectedBuildingId).child("floors").child(selectedFloorNum);
                        } else {
                            ref = mDatabase.child(selectedBuildingId + "_url");
                        }

                        ref.setValue(finalUrl).addOnSuccessListener(aVoid -> {
                            runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Map uploaded and linked successfully!", Toast.LENGTH_SHORT).show();
                                loadMapData();
                            });
                        });
                    } else {
                        throw new Exception("Script returned no URL");
                    }
                } else {
                    throw new Exception("Upload HTTP Error: " + conn.getResponseCode());
                }

            } catch (Exception e) {
                Log.e("AdminMap", "Upload failed", e);
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void enterBuildingView(String buildingId) {
        currentViewMode = "BUILDING";
        selectedBuildingId = buildingId;
        selectedFloorNum = null;
        
        tvMapTitle.setText(buildingId.replace("_", " "));
        tvCurrentView.setText("Viewing: " + buildingId);
        tvViewDescription.setText(isMultiFloorBuilding(buildingId) ? "Select a floor to manage its layout." : "Managing the layout of " + buildingId);
        
        layoutBuildingActions.setVisibility(View.VISIBLE);
        btnBack.setVisibility(View.VISIBLE);
        
        if (isMultiFloorBuilding(buildingId)) {
            layoutFloorSelector.setVisibility(View.VISIBLE);
        } else {
            layoutFloorSelector.setVisibility(View.GONE);
        }
        
        loadMapData();
    }

    private void backToOverview() {
        currentViewMode = "MAIN";
        selectedBuildingId = null;
        selectedFloorNum = null;
        
        tvMapTitle.setText("Campus Map Admin");
        tvCurrentView.setText("Campus Overview");
        tvViewDescription.setText("Click on building blocks to manage their specific layouts.");
        
        layoutBuildingActions.setVisibility(View.GONE);
        layoutFloorSelector.setVisibility(View.GONE);
        btnBack.setVisibility(View.GONE);
        
        loadMapData();
    }

    private void showLoading(boolean loading) {
        runOnUiThread(() -> progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));
    }

    private void runOnUiThread(Runnable action) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(action);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) executorService.shutdown();
    }
}
