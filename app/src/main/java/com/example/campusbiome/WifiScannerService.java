package com.example.campusbiome;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

public class WifiScannerService extends Service {

    private static final String TAG = "WifiScannerService";
    private static final String CHANNEL_ID = "WifiScannerChannel";
    private static final int NOTIFICATION_ID = 101;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    private String currentSsid = null;
    private DatabaseReference currentlyConnectedRouterRef = null;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("CampusBiome")
                .setContentText("Tracking Campus Wi-Fi Density...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        startTrackingWifi();

        return START_STICKY; // Keeps service running even if app is closed
    }

    private void startTrackingWifi() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return;

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities);
                handleWifiStateChange();
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                handleWifiStateChange();
            }
        };

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback);
            // Initial check just in case already connected
            handleWifiStateChange();
        } catch (Exception e) {
            Log.e(TAG, "Failed to register network callback", e);
        }
    }

    private void handleWifiStateChange() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager != null ? wifiManager.getConnectionInfo() : null;

        String ssid = null;
        if (wifiInfo != null && wifiInfo.getSupplicantState() == android.net.wifi.SupplicantState.COMPLETED) {
            ssid = wifiInfo.getSSID();
            if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid = ssid.substring(1, ssid.length() - 1);
            }
            if ("<unknown ssid>".equals(ssid)) {
                ssid = null; // Permission issues or not fully connected
            }
        }

        if (ssid == null && currentSsid == null) {
            return; // Still disconnected
        }

        if (ssid != null && ssid.equals(currentSsid)) {
            return; // Already connected to this SSID
        }

        // We disconnected from the old SSID, or switched. Decrement old one.
        if (currentSsid != null && currentlyConnectedRouterRef != null) {
            updateDeviceCount(currentlyConnectedRouterRef, -1);
            currentlyConnectedRouterRef = null;
        }

        currentSsid = ssid;
        Log.d(TAG, "Connected to new SSID: " + currentSsid);

        if (currentSsid != null) {
            findAndIncrementSsid(currentSsid);
        }
    }

    private void findAndIncrementSsid(final String targetSsid) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference("campus_layout/wifi_routers");
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DatabaseReference foundRef = searchForSsid(snapshot, targetSsid);
                if (foundRef != null) {
                    currentlyConnectedRouterRef = foundRef;
                    updateDeviceCount(foundRef, 1);
                } else {
                    Log.d(TAG, "SSID " + targetSsid + " not found in Campus database.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        });
    }

    private DatabaseReference searchForSsid(DataSnapshot snapshot, String targetSsid) {
        if (snapshot.hasChild("ssid")) {
            String dbSsid = snapshot.child("ssid").getValue(String.class);
            if (targetSsid.equals(dbSsid)) {
                return snapshot.getRef();
            }
        } else {
            for (DataSnapshot child : snapshot.getChildren()) {
                DatabaseReference ref = searchForSsid(child, targetSsid);
                if (ref != null) return ref;
            }
        }
        return null;
    }

    private void updateDeviceCount(DatabaseReference routerRef, final int delta) {
        routerRef.child("connected_devices").runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Integer currentVal = currentData.getValue(Integer.class);
                if (currentVal == null) {
                    if (delta > 0) currentData.setValue(delta);
                    else currentData.setValue(0);
                } else {
                    int newVal = currentVal + delta;
                    if (newVal < 0) newVal = 0;
                    currentData.setValue(newVal);
                }
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Log.e(TAG, "Transaction failed: " + error.getMessage());
                } else {
                    Log.d(TAG, "Transaction completed, delta: " + delta);
                }
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Wi-Fi Scanner Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Runs in background to track campus density");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                Log.e(TAG, "Failed to unregister network callback", e);
            }
        }
        if (currentlyConnectedRouterRef != null) {
            updateDeviceCount(currentlyConnectedRouterRef, -1);
            currentlyConnectedRouterRef = null;
            currentSsid = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
