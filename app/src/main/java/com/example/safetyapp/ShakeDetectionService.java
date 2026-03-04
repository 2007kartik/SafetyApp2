package com.example.safetyapp;

//import android.app.ServiceInfo;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
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
    private static final float SHAKE_THRESHOLD = 20f;  // Adjusted for better accuracy
    private static final long SHAKE_TIME_WINDOW = 800; // 1 second to avoid multiple triggers
    private long lastShakeTime = 0;

    private FusedLocationProviderClient fusedLocationClient;
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ShakeDetectionService started");

        startForegroundService();

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
            sendPanicSMSWithLocation();   // 🚨 direct trigger
        }

        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }

        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
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
            if (acceleration > SHAKE_THRESHOLD && (currentTime - lastShakeTime > SHAKE_TIME_WINDOW)) {
                lastShakeTime = currentTime;
                Log.d(TAG, "Shake detected! Sending alert...");
                sendPanicSMSWithLocation();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void sendPanicSMSWithLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Required permissions (SMS, Location, or Call) not granted.");
            return;
        }

        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS) {
            Log.e(TAG, "Google Play Services not available.");
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                sendEmergencySMS(location);
            } else {
                Log.e(TAG, "Unable to retrieve location.");
                sendEmergencySMS(null);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Location request failed", e);
            sendEmergencySMS(null);
        });
    }

    private void sendEmergencySMS(Location location) {
        Set<String> contacts = sharedPreferences.getStringSet("contacts", new HashSet<>());
        if (contacts.isEmpty()) {
            Log.e(TAG, "No emergency contacts found.");
            return;
        }

        String locationUrl = "";
        if (location != null) {
            String query = location.getLatitude() + " " + location.getLongitude();
            String encodedQuery = Uri.encode(query);  // encodes space to %20
            locationUrl = "https://www.google.com/maps/search/?api=1&query=" + encodedQuery;
        } else {
            locationUrl = "Location not available";
        }

        String message = "Emergency! Please help! " + locationUrl;

        SmsManager smsManager = SmsManager.getDefault();
        boolean isFirst = true;
        String phoneNumber = "";
        for (String contact : contacts) {
            String[] contactDetails = contact.split(":");
            if (contactDetails.length == 2) {
                 phoneNumber = contactDetails[1].trim();
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                Log.d(TAG, "SMS Sent to: " + phoneNumber);

                // Call only the first contact
                if (isFirst) {
                    makeCallToNumber(phoneNumber);
                    isFirst = false;
                }
            }
        }

        showNotification("Emergency Alert", "Help message sent to emergency contacts.");
        showCallNotification(phoneNumber);



    }

//    private void makeCallToNumber(String phoneNumber) {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
//            Log.e(TAG, "Call permission not granted.");
//            return;
//        }
//        Intent callIntent = new Intent(Intent.ACTION_CALL);
//        callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        callIntent.setData(Uri.parse("tel:" + phoneNumber));
//        startActivity(callIntent);
//    }
private void makeCallToNumber(String phoneNumber) {

    if (ActivityCompat.checkSelfPermission(this,
            Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
        return;
    }

    Intent callIntent = new Intent(Intent.ACTION_CALL);
    callIntent.setData(Uri.parse("tel:" + phoneNumber));
    callIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
    );

    startActivity(callIntent);
}

//line 184 code new updated:
private void showCallNotification(String number) {

    String channelId = "CallEmergency";

    NotificationManager nm =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        nm.createNotificationChannel(
                new NotificationChannel(channelId,"Emergency Call",
                        NotificationManager.IMPORTANCE_HIGH));
    }

    Intent callIntent = new Intent(Intent.ACTION_CALL);
    callIntent.setData(Uri.parse("tel:" + number));
    callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    PendingIntent callPending = PendingIntent.getActivity(
            this, 0, callIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

    Notification notification = new NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Emergency Detected!")
            .setContentText("Tap to call guardian immediately")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_launcher_foreground, "CALL NOW", callPending)
            .build();

    nm.notify(100, notification);
}



    @SuppressLint("ForegroundServiceType")
    private void startForegroundService() {
        String channelId = "ShakeServiceChannel";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Shake Detection Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Shake Detection Active")
                .setContentText("Listening for emergency shake gesture")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();

// 👇 This tells Android what type of foreground service it is
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 0x00000004 is the value of ServiceInfo.FOREGROUND_SERVICE_TYPE_SENSOR
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(1, notification);
        }

    }

    private void showNotification(String title, String message) {
        String channelId = "EmergencyAlertChannel";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Emergency Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(channel);
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

        notificationManager.notify(2, notification);
    }
}
