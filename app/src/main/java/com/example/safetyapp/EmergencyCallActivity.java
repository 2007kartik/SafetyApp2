package com.example.safetyapp;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import androidx.core.app.ActivityCompat;

// ✅ IMPORTANT: extends Activity NOT AppCompatActivity
// AppCompatActivity crashes with transparent themes silently
public class EmergencyCallActivity extends Activity {

    private static final String TAG = "EmergencyCallActivity";
    public static final String EXTRA_PHONE_NUMBER = "phone_number";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Wake up screen and show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            if (km != null) {
                km.requestDismissKeyguard(this, null);
            }
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        }

        String phoneNumber = getIntent().getStringExtra(EXTRA_PHONE_NUMBER);
        Log.d(TAG, "EmergencyCallActivity started, number = " + phoneNumber);

        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            makeCall(phoneNumber);
        } else {
            Log.e(TAG, "No phone number received.");
            finish();
        }
    }

    private void makeCall(String phoneNumber) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "CALL_PHONE permission not granted — cannot place call.");
            finish();
            return;
        }

        try {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + phoneNumber));
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(callIntent);
            Log.d(TAG, "✅ Emergency call placed to: " + phoneNumber);
        } catch (Exception e) {
            Log.e(TAG, "❌ Call failed: " + e.getMessage());
        }

        finish();
    }
}