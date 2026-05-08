package com.example.campusbiome;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class CampusBiomeApp extends Application {

    private FirebaseAuth mAuth;
    private DatabaseReference mAppointmentsRef;
    private ChildEventListener appointmentListener;
    private HashMap<String, String> knownAppointmentStatuses = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        
        NotificationHelper.createChannels(this);

        mAuth = FirebaseAuth.getInstance();
        mAuth.addAuthStateListener(firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                listenToAppointments(user.getUid());
            } else {
                stopListeningToAppointments();
            }
        });
    }

    private void listenToAppointments(String uid) {
        if (mAppointmentsRef != null && appointmentListener != null) return;

        mAppointmentsRef = FirebaseDatabase.getInstance().getReference().child("FacultyAppointment");
        
        appointmentListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                processAppointmentSnapshot(snapshot, uid, false);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                processAppointmentSnapshot(snapshot, uid, true);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        mAppointmentsRef.orderByChild("student/uid").equalTo(uid).addChildEventListener(appointmentListener);
    }

    private void stopListeningToAppointments() {
        if (mAppointmentsRef != null && appointmentListener != null) {
            mAppointmentsRef.removeEventListener(appointmentListener);
            appointmentListener = null;
            mAppointmentsRef = null;
        }
        knownAppointmentStatuses.clear();
    }

    private void processAppointmentSnapshot(DataSnapshot snapshot, String currentUid, boolean isChange) {
        String id = snapshot.getKey();
        String status = snapshot.child("status").getValue(String.class);
        String day = snapshot.child("day").getValue(String.class);
        String startTime = snapshot.child("startTime").getValue(String.class);

        if (id == null || status == null) return;

        String prevStatus = knownAppointmentStatuses.get(id);
        knownAppointmentStatuses.put(id, status);

        // If status changed to approved, send notification and schedule reminder
        if (isChange && "approved".equals(status) && !"approved".equals(prevStatus)) {
            String title = "Appointment Approved!";
            String message = "Your meeting for " + day + " at " + startTime + " has been approved.";
            
            NotificationHelper.sendNotification(
                    this,
                    NotificationHelper.CHANNEL_APPOINTMENTS,
                    id.hashCode(),
                    title,
                    message
            );

            // The DB notification is now created at the source in FacultyAppointmentsFragment
            // This just handles the local Android push notification pop-up and alarm.

            if (day != null && startTime != null) {
                scheduleReminder(this, id, "Upcoming Appointment", "You have an appointment in 30 minutes!", day, startTime);
            }
        }
        
        // Also schedule if it's already approved when loaded (and wasn't scheduled in the past)
        if (!isChange && "approved".equals(status)) {
            if (day != null && startTime != null) {
                scheduleReminder(this, id, "Upcoming Appointment", "You have an appointment in 30 minutes!", day, startTime);
            }
        }
    }

    public static void scheduleReminder(Context context, String uniqueId, String title, String msg, String day, String timeStr) {
        long targetTimeMs = getNextOccurrenceTime(day, timeStr);
        if (targetTimeMs == -1) return;

        long reminderTimeMs = targetTimeMs - (30 * 60 * 1000); // 30 mins before
        
        if (reminderTimeMs > System.currentTimeMillis()) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, ReminderReceiver.class);
            intent.putExtra(ReminderReceiver.EXTRA_TITLE, title);
            intent.putExtra(ReminderReceiver.EXTRA_MESSAGE, msg);
            intent.putExtra(ReminderReceiver.EXTRA_NOTIF_ID, uniqueId.hashCode() + 100);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, uniqueId.hashCode(), intent, flags);

            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTimeMs, pendingIntent);
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTimeMs, pendingIntent);
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTimeMs, pendingIntent);
                }
            }
        }
    }

    public static long getNextOccurrenceTime(String dayString, String timeString) {
        // Check if dayString is a specific date in yyyy-MM-dd format
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date specificDate = dateFormat.parse(dayString);
            if (specificDate != null) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.US);
                Date parsedTime = timeFormat.parse(timeString);
                if (parsedTime != null) {
                    Calendar dateCal = Calendar.getInstance();
                    dateCal.setTime(specificDate);
                    
                    Calendar timeCal = Calendar.getInstance();
                    timeCal.setTime(parsedTime);
                    
                    dateCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
                    dateCal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
                    dateCal.set(Calendar.SECOND, 0);
                    dateCal.set(Calendar.MILLISECOND, 0);
                    
                    return dateCal.getTimeInMillis();
                }
            }
        } catch (ParseException e) {
            // Not a yyyy-MM-dd date, fall back to day of week mapping
        }

        // Map day string to Calendar day constant
        HashMap<String, Integer> dayMap = new HashMap<>();
        dayMap.put("mon", Calendar.MONDAY);
        dayMap.put("monday", Calendar.MONDAY);
        dayMap.put("tue", Calendar.TUESDAY);
        dayMap.put("tuesday", Calendar.TUESDAY);
        dayMap.put("wed", Calendar.WEDNESDAY);
        dayMap.put("wednesday", Calendar.WEDNESDAY);
        dayMap.put("thu", Calendar.THURSDAY);
        dayMap.put("thursday", Calendar.THURSDAY);
        dayMap.put("fri", Calendar.FRIDAY);
        dayMap.put("friday", Calendar.FRIDAY);
        dayMap.put("sat", Calendar.SATURDAY);
        dayMap.put("saturday", Calendar.SATURDAY);
        dayMap.put("sun", Calendar.SUNDAY);
        dayMap.put("sunday", Calendar.SUNDAY);

        String dayKey = dayString.trim().toLowerCase();
        Integer targetDayOfWeek = dayMap.get(dayKey);
        if (targetDayOfWeek == null) return -1;

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
        Date parsedTime;
        try {
            parsedTime = sdf.parse(timeString);
        } catch (ParseException e) {
            return -1;
        }

        if (parsedTime == null) return -1;

        Calendar timeCal = Calendar.getInstance();
        timeCal.setTime(parsedTime);
        int targetHour = timeCal.get(Calendar.HOUR_OF_DAY);
        int targetMinute = timeCal.get(Calendar.MINUTE);

        Calendar nextCal = Calendar.getInstance();
        nextCal.set(Calendar.HOUR_OF_DAY, targetHour);
        nextCal.set(Calendar.MINUTE, targetMinute);
        nextCal.set(Calendar.SECOND, 0);
        nextCal.set(Calendar.MILLISECOND, 0);

        int currentDayOfWeek = nextCal.get(Calendar.DAY_OF_WEEK);
        int daysToAdd = (targetDayOfWeek - currentDayOfWeek + 7) % 7;
        
        if (daysToAdd == 0 && nextCal.getTimeInMillis() <= System.currentTimeMillis()) {
            // It's today, but the time has passed, so next week
            daysToAdd = 7;
        }

        nextCal.add(Calendar.DAY_OF_YEAR, daysToAdd);
        return nextCal.getTimeInMillis();
    }
}
