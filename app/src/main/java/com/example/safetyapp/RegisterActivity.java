package com.example.safetyapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText name, mobile, email, password;
    private Button registerButton;
    private TextView loginRedirect;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(RegisterActivity.this, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }

        setContentView(R.layout.activity_register);

        name = findViewById(R.id.name);
        mobile = findViewById(R.id.mobile);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        registerButton = findViewById(R.id.registerButton);
        loginRedirect = findViewById(R.id.loginRedirect);

        registerButton.setOnClickListener(v -> {
            String fullName = name.getText().toString().trim();
            String mobileNumber = mobile.getText().toString().trim();
            String userEmail = email.getText().toString().trim();
            String userPassword = password.getText().toString().trim();

            if (fullName.isEmpty() || mobileNumber.isEmpty() || userEmail.isEmpty() || userPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (userPassword.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Registering user...", Toast.LENGTH_SHORT).show();

            mAuth.createUserWithEmailAndPassword(userEmail, userPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Firebase registration succeeded", Toast.LENGTH_SHORT).show();
                            FirebaseUser user = task.getResult().getUser();

                            if (user != null) {
                                String uid = user.getUid();
//                                Toast.makeText(this, "User UID: " + uid, Toast.LENGTH_SHORT).show();

                                Map<String, Object> userData = new HashMap<>();
                                userData.put("username", fullName);
                                userData.put("mobileNumber", mobileNumber);
                                userData.put("email", userEmail);

                                db.collection("users").document(uid).set(userData)
                                        .addOnSuccessListener(unused -> {
                                            Toast.makeText(RegisterActivity.this, "Registration successful! Redirecting...", Toast.LENGTH_SHORT).show();
                                            new Handler().postDelayed(() -> {
                                                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                                finish();
                                            }, 2000);
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(RegisterActivity.this, "Firestore error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            Log.e("FIRESTORE_ERROR", "Failed to save user data", e);
                                        });
                            } else {
                                Toast.makeText(this, "User object is null", Toast.LENGTH_LONG).show();
                            }

                        } else {
                            Toast.makeText(this, "Firebase registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            Log.e("FIREBASE_AUTH", "Registration failure", task.getException());
                        }
                    });
        });

        loginRedirect.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }
}
