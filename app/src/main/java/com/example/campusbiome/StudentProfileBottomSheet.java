package com.example.campusbiome;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StudentProfileBottomSheet extends BottomSheetDialogFragment {

    private ImageView ivBsProfilePic;
    private TextView tvBsName, tvBsEmail;
    private MaterialCardView btnChangePhoto;
    private MaterialButton btnBsEditProfile;
    private ProgressBar pbImageUpload;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    
    // TODO: Paste your deployed Google Apps Script Web App URL here
    private static final String APPS_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbxaK54pzkqL-cosZzix7SoVrjwaaJVJqSHrUsP3QdwGZr_lhKpTAX7Gftt2IpEHsnol/exec";

    private ActivityResultLauncher<String> photoPickerLauncher;
    private ExecutorService executorService;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        executorService = Executors.newSingleThreadExecutor();
        
        photoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadImageToGoogleDrive(uri);
                }
            }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_student_profile_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivBsProfilePic = view.findViewById(R.id.ivBsProfilePic);
        tvBsName = view.findViewById(R.id.tvBsName);
        tvBsEmail = view.findViewById(R.id.tvBsEmail);
        btnChangePhoto = view.findViewById(R.id.btnChangePhoto);
        btnBsEditProfile = view.findViewById(R.id.btnBsEditProfile);
        pbImageUpload = view.findViewById(R.id.pbImageUpload);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            tvBsEmail.setText(user.getEmail());
            fetchUserData(user.getUid());
        }

        btnChangePhoto.setOnClickListener(v -> {
            photoPickerLauncher.launch("image/*");
        });

        btnBsEditProfile.setOnClickListener(v -> {
            dismiss();
            if (getActivity() instanceof StudentDashboardActivity) {
                ((StudentDashboardActivity) getActivity()).openFragment(new EditStudentProfileFragment());
            }
        });
    }

    private void fetchUserData(String uid) {
        mDatabase.child("Users").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    if (name != null) tvBsName.setText(name);

                    String profilePic = snapshot.child("profile_pic").getValue(String.class);
                    if (profilePic != null && !profilePic.isEmpty() && getContext() != null) {
                        Glide.with(getContext())
                                .load(profilePic)
                                .placeholder(android.R.drawable.ic_menu_myplaces)
                                .error(android.R.drawable.ic_menu_myplaces)
                                .into(ivBsProfilePic);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Could not load profile details", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void uploadImageToGoogleDrive(Uri imageUri) {
        if (APPS_SCRIPT_URL.equals("YOUR_WEB_APP_URL_HERE")) {
            Toast.makeText(getContext(), "Error: Apps Script URL not configured in code.", Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || getContext() == null) return;

        pbImageUpload.setVisibility(View.VISIBLE);
        btnChangePhoto.setEnabled(false);

        executorService.execute(() -> {
            try {
                // 1. Read and compress the image
                InputStream imageStream = getContext().getContentResolver().openInputStream(imageUri);
                Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                
                // Scale down to prevent massive payload sizes
                int maxDim = 800;
                float scale = Math.min((float) maxDim / selectedImage.getWidth(), (float) maxDim / selectedImage.getHeight());
                if (scale < 1) {
                    selectedImage = Bitmap.createScaledBitmap(selectedImage, 
                        Math.round(selectedImage.getWidth() * scale), 
                        Math.round(selectedImage.getHeight() * scale), true);
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                selectedImage.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] imageBytes = baos.toByteArray();
                String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                // 2. Prepare JSON payload
                JSONObject jsonPayload = new JSONObject();
                jsonPayload.put("base64", base64Image);
                jsonPayload.put("filename", "profile_" + user.getUid() + ".jpg");
                jsonPayload.put("mimeType", "image/jpeg");

                // 3. Make HTTP POST Request
                URL url = new URL(APPS_SCRIPT_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(jsonPayload.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream in = conn.getInputStream();
                    java.util.Scanner scanner = new java.util.Scanner(in).useDelimiter("\\A");
                    String responseStr = scanner.hasNext() ? scanner.next() : "";
                    
                    JSONObject responseJson = new JSONObject(responseStr);
                    if (responseJson.has("url")) {
                        String downloadUrl = responseJson.getString("url");
                        
                        // Ensure the URL is formatted for direct image rendering
                        if (downloadUrl.contains("uc?id=") && !downloadUrl.contains("export=download")) {
                            downloadUrl = downloadUrl.replace("uc?id=", "uc?export=download&id=");
                        }
                        
                        final String finalUrl = downloadUrl;
                        
                        // 4. Update Firebase Realtime Database
                        mDatabase.child("Users").child(user.getUid()).child("profile_pic").setValue(finalUrl)
                            .addOnSuccessListener(aVoid -> runOnUiThread(() -> {
                                pbImageUpload.setVisibility(View.GONE);
                                btnChangePhoto.setEnabled(true);
                                Toast.makeText(getContext(), "Profile picture updated successfully!", Toast.LENGTH_SHORT).show();
                            }))
                            .addOnFailureListener(e -> handleUploadFailure(e));
                    } else {
                        throw new Exception("No URL returned from Google Drive script.");
                    }
                } else {
                    throw new Exception("HTTP Error: " + responseCode);
                }

            } catch (Exception e) {
                Log.e("Upload", "Error uploading to Drive", e);
                handleUploadFailure(e);
            }
        });
    }

    private void runOnUiThread(Runnable action) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(action);
        }
    }
    
    private void handleUploadFailure(Exception e) {
        runOnUiThread(() -> {
            if (getContext() != null) {
                pbImageUpload.setVisibility(View.GONE);
                btnChangePhoto.setEnabled(true);
                Toast.makeText(getContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
