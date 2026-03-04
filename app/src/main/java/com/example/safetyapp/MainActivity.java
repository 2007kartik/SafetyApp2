package com.example.safetyapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.safetyapp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;

    // 🔘 Volume SOS variables
    private long lastPressTime = 0;
    private int pressCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        replaceFragment(new HomeFragment());
        binding.bottomNavigationView.setBackground(null);

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {

            if (item.getItemId() == R.id.home) {
                replaceFragment(new HomeFragment());
            } else if (item.getItemId() == R.id.Gurdian) {
                replaceFragment(new GuardianFragment());
            } else if (item.getItemId() == R.id.Contact) {
                replaceFragment(new ContactFragment());
            } else if (item.getItemId() == R.id.Profile) {
                replaceFragment(new ProfileFragment());
            }

            return true;
        });

        requestMicPermission();
    }

    // 🎤 Mic permission
    private void requestMicPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    1001
            );
        }
    }

    // 🚨 VOLUME BUTTON SOS (works even when app minimized)

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {

            long now = System.currentTimeMillis();

            if (now - lastPressTime < 1500) {
                pressCount++;
            } else {
                pressCount = 1;
            }

            lastPressTime = now;

            if (pressCount == 3) {

                pressCount = 0;

                // Trigger emergency service
                Intent sosIntent = new Intent(this, ShakeDetectionService.class);
                sosIntent.setAction(ShakeDetectionService.ACTION_TRIGGER_SOS);
                startForegroundService(sosIntent);


                Toast.makeText(this,"SOS ACTIVATED!",Toast.LENGTH_LONG).show();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    // ================= EXISTING FRAGMENT LOGIC (UNCHANGED) =================

    private void replaceFragment(ProfileFragment profileFragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, profileFragment);
        fragmentTransaction.commit();
    }

    private void replaceFragment(ContactFragment contactFragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, contactFragment);
        fragmentTransaction.commit();
    }

    private void replaceFragment(GuardianFragment guardianFragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, guardianFragment);
        fragmentTransaction.commit();
    }

    private void replaceFragment(HomeFragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }
}
