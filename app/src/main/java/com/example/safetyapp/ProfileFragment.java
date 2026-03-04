package com.example.safetyapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import static android.app.Activity.RESULT_OK;

public class ProfileFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView ivProfilePicture;
    private EditText etUsername, etMobileNumber, etEmail;
    private Button btnUpdate, btnViewProfile, btnChangePhoto, btnLogout;
    private Uri selectedImageUri;
    private String username, mobileNumber, email;
    private String uid;
    private SharedPreferences sharedPreferences;

    private FirebaseFirestore firestore;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_profile, container, false);

        ivProfilePicture = rootView.findViewById(R.id.ivProfilePicture);
        etUsername = rootView.findViewById(R.id.etUsername);
        etMobileNumber = rootView.findViewById(R.id.etMobileNumber);
        etEmail = rootView.findViewById(R.id.etEmail);
        btnUpdate = rootView.findViewById(R.id.btnUpdate);
        btnViewProfile = rootView.findViewById(R.id.btnViewProfile);
        btnChangePhoto = rootView.findViewById(R.id.btnChangePhoto);
        btnLogout = rootView.findViewById(R.id.button3); // Logout button

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(getActivity(), RegisterActivity.class));
            getActivity().finish();
            return rootView;
        }

        uid = currentUser.getUid();
        firestore = FirebaseFirestore.getInstance();
        sharedPreferences = getActivity().getSharedPreferences("UserProfilePrefs_" + uid, getActivity().MODE_PRIVATE);

//        loadUserProfile();

        btnChangePhoto.setOnClickListener(v -> openImageChooser());

        btnUpdate.setOnClickListener(v -> {
            username = etUsername.getText().toString().trim();
            mobileNumber = etMobileNumber.getText().toString().trim();
            email = etEmail.getText().toString().trim();

            if (username.isEmpty() || mobileNumber.isEmpty() || email.isEmpty()) {
                Toast.makeText(getActivity(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            saveUserProfile(); // Save to SharedPreferences and Firestore
            Toast.makeText(getActivity(), "Profile updated", Toast.LENGTH_SHORT).show();
        });

        btnViewProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ProfileViewActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            sharedPreferences.edit().clear().apply();
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), RegisterActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return rootView;
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            ivProfilePicture.setImageURI(selectedImageUri);
        }
    }

    private void saveUserProfile() {
        // Save to SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username", username);
        editor.putString("mobileNumber", mobileNumber);
        editor.putString("email", email);
        if (selectedImageUri != null) {
            editor.putString("profilePicUrl", selectedImageUri.toString());
        }
        editor.apply();

        // Save to Firestore
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("username", username);
        userMap.put("mobileNumber", mobileNumber);
        userMap.put("email", email);
        if (selectedImageUri != null) {
            userMap.put("profilePicUrl", selectedImageUri.toString());
        }

        firestore.collection("users").document(uid)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    // Successfully updated
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getActivity(), "Failed to update Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

//    private void loadUserProfile() {
//        username = sharedPreferences.getString("username", "");
//        mobileNumber = sharedPreferences.getString("mobileNumber", "");
//        email = sharedPreferences.getString("email", "");
//        String profilePicUrl = sharedPreferences.getString("profilePicUrl", "");
//
//        etUsername.setText(username);
//        etMobileNumber.setText(mobileNumber);
//        etEmail.setText(email);
//        if (!profilePicUrl.isEmpty()) {
//            selectedImageUri = Uri.parse(profilePicUrl);
//            ivProfilePicture.setImageURI(selectedImageUri);
//        }
//    }

}
