package com.example.safetyapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.telecom.TelecomManager;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.HashSet;
import java.util.Set;

public class ShakeDetectionService extends Service implements SensorEventListener {
    private static final String TAG = "ShakeDetectionService";
    public static final String ACTION_TRIGGER_SOS = "TRIGGER_SOS_NOW";

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private static final float SHAKE_THRESHOLD = 20f;
    private static final long SHAKE_TIME_WINDOW = 800;
    private long lastShakeTime = 0;

    private FusedLocationProviderClient fusedLocationClient;
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ShakeDetectionService started");
        startForegroundServiceNotification();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        } else {
            Log.e(TAG, "Accelerometer not available");
            stopSelf();
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        sharedPreferences = getSharedPreferences("EmergencyContacts", Context.MODE_PRIVATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_TRIGGER_SOS.equals(intent.getAction())) {
            sendPanicSMSWithLocation();
        }
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) sensorManager.unregisterListener(this);
        Log.d(TAG, "Service stopped.");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            float acceleration = (float) Math.sqrt(x * x + y * y + z * z);
            long currentTime = System.currentTimeMillis();

            if (acceleration > SHAKE_THRESHOLD &&
                    (currentTime - lastShakeTime > SHAKE_TIME_WINDOW)) {
                lastShakeTime = currentTime;
                Log.d(TAG, "Shake detected! Sending alert...");
                sendPanicSMSWithLocation();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void sendPanicSMSWithLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                        != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permissions not granted.");
            return;
        }

        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
                != ConnectionResult.SUCCESS) {
            Log.e(TAG, "Google Play Services not available.");
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this::sendEmergencySMS)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Location failed: " + e.getMessage());
                    sendEmergencySMS(null);
                });
    }

    private void sendEmergencySMS(Location location) {
        Set<String> contacts = sharedPreferences.getStringSet("contacts", new HashSet<>());
        if (contacts.isEmpty()) {
            Log.e(TAG, "No emergency contacts.");
            return;
        }

        String locationUrl;
        if (location != null) {
            String encodedQuery = Uri.encode(
                    location.getLatitude() + " " + location.getLongitude());
            locationUrl = "https://www.google.com/maps/search/?api=1&query=" + encodedQuery;
        } else {
            locationUrl = "Location not available";
        }

        String message = "Emergency! Please help! " + locationUrl;
        SmsManager smsManager = SmsManager.getDefault();

        boolean isFirst = true;
        String firstPhoneNumber = "";

        for (String contact : contacts) {
            String[] parts = contact.split(":");
            if (parts.length == 2) {
                String phoneNumber = parts[1].trim();
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                Log.d(TAG, "SMS sent to: " + phoneNumber);

                if (isFirst) {
                    firstPhoneNumber = phoneNumber;
                    isFirst = false;
                }
            }
        }

        if (!firstPhoneNumber.isEmpty()) {
            placeCallDirectly(firstPhoneNumber);
        }

        showSMSNotification("Emergency Alert", "Help message sent to emergency contacts.");
    }

    /**
     * ✅ FINAL SOLUTION for Android 14+ / targetSdk 35
     *
     * Background Activity Launch (BAL) is completely blocked on Android 14+.
     * No startActivity() from background service works — period.
     *
     * The ONLY way to place a call from background without launching an Activity:
     * Use TelecomManager.placeCall() — this is a SYSTEM-LEVEL call API that
     * bypasses BAL restrictions entirely because it goes directly to the
     * telephony stack, not through the Activity stack.
     *
     * This works even when:
     * - App is in background
     * - Screen is locked
     * - User is in another app
     */
    private void placeCallDirectly(String phoneNumber) {
        Log.d(TAG, "placeCallDirectly() called for: " + phoneNumber);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "CALL_PHONE permission not granted.");
            return;
        }

        try {
            TelecomManager telecomManager =
                    (TelecomManager) getSystemService(Context.TELECOM_SERVICE);

            if (telecomManager == null) {
                Log.e(TAG, "TelecomManager is null.");
                return;
            }

            Uri callUri = Uri.fromParts("tel", phoneNumber, null);

            Bundle extras = new Bundle();
            // ✅ This flag tells TelecomManager this is a real outgoing call
            extras.putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, false);

            // ✅ placeCall() goes directly to telephony — no Activity needed, no BAL block
            telecomManager.placeCall(callUri, extras);
            Log.d(TAG, "✅ TelecomManager.placeCall() fired for: " + phoneNumber);

        } catch (Exception e) {
            Log.e(TAG, "❌ TelecomManager.placeCall() failed: " + e.getMessage());
        }
    }

    @SuppressLint("ForegroundServiceType")
    private void startForegroundServiceNotification() {
        String channelId = "ShakeServiceChannel";
        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(new NotificationChannel(
                    channelId,
                    "Shake Detection Service",
                    NotificationManager.IMPORTANCE_LOW));
        }

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Shake Detection Active")
                .setContentText("Listening for emergency shake gesture")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(1, notification);
        }
    }

    private void showSMSNotification(String title, String message) {
        String channelId = "EmergencyAlertChannel";
        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Emergency Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .build();

        nm.notify(2, notification);
    }
}