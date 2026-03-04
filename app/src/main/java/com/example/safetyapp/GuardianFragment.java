package com.example.safetyapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.HashSet;
import java.util.Set;

public class GuardianFragment extends Fragment {

    private EditText etName, etPhone;
    private Button btnSave;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_guardian, container, false);

        // Initialize UI elements
        etName = view.findViewById(R.id.etName);
        etPhone = view.findViewById(R.id.etPhone);
        btnSave = view.findViewById(R.id.btnSave);

        // SharedPreferences to store emergency contacts
        sharedPreferences = requireActivity().getSharedPreferences("EmergencyContacts", Context.MODE_PRIVATE);

        // Save the contact when the save button is clicked
        btnSave.setOnClickListener(v -> saveContact());

        return view;
    }

    // Save the emergency contact into SharedPreferences
    private void saveContact() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
            Toast.makeText(getContext(), "Please enter both name and phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Store the contact as "name:phone"
        Set<String> contacts = sharedPreferences.getStringSet("contacts", new HashSet<>());
        contacts.add(name + ":" + phone);  // Add contact in "name:phone" format

        // Save the updated set of contacts back into SharedPreferences
        sharedPreferences.edit().putStringSet("contacts", contacts).apply();
        Toast.makeText(getContext(), "Contact Saved!", Toast.LENGTH_SHORT).show();

        // Clear the input fields
        etName.setText("");
        etPhone.setText("");
    }
}
