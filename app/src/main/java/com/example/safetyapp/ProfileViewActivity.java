package com.example.safetyapp;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

public class ProfileViewActivity extends AppCompatActivity {

    private TextView tvUsername, tvMobileNumber, tvEmail;
    private ImageView ivProfilePicture;

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_view);

        // Initialize Views
        tvUsername = findViewById(R.id.tvUsername);
        tvMobileNumber = findViewById(R.id.tvMobileNumber);
        tvEmail = findViewById(R.id.tvEmail);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);

        // Initialize Firebase Firestore and User
        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadUserProfileFromFirestore();
    }

    private void loadUserProfileFromFirestore() {
        DocumentReference docRef = firestore.collection("users").document(currentUser.getUid());
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String username = documentSnapshot.getString("username");
                String mobileNumber = documentSnapshot.getString("mobileNumber");
                String email = documentSnapshot.getString("email");
                String profilePicUrl = documentSnapshot.getString("profilePicUrl");

                if (username != null) {
                    tvUsername.setText("Username: " + username);
                } else {
                    tvUsername.setText("Username not set");
                }

                if (mobileNumber != null) {
                    tvMobileNumber.setText("Mobile Number: " + mobileNumber);
                } else {
                    tvMobileNumber.setText("Mobile number not set");
                }

                if (email != null) {
                    tvEmail.setText("Email: " + email);
                } else {
                    tvEmail.setText("Email not set");
                }

                if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                    Picasso.get().load(profilePicUrl).into(ivProfilePicture);
                } else {
                    ivProfilePicture.setImageResource(R.drawable.ic_baseline_account_circle_24); // default image
                }

            } else {
                Toast.makeText(ProfileViewActivity.this, "Profile data not found.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(ProfileViewActivity.this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
}
