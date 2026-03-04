package com.example.safetyapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ContactFragment extends Fragment {

    private LinearLayout contactsList;
    private Button btnDeleteSelected;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact, container, false);

        // Initialize UI elements
        contactsList = view.findViewById(R.id.contactsList);
        btnDeleteSelected = view.findViewById(R.id.btnDeleteSelected);

        // SharedPreferences to retrieve emergency contacts
        sharedPreferences = requireActivity().getSharedPreferences("EmergencyContacts", Context.MODE_PRIVATE);

        // Show the saved contacts when the fragment is created
        showContacts();

        // Set up the delete selected contacts button
        btnDeleteSelected.setOnClickListener(v -> deleteSelectedContacts());

        return view;
    }

    // Show the saved emergency contacts with checkboxes
    private void showContacts() {
        // Retrieve the saved contacts from SharedPreferences
        Set<String> contacts = sharedPreferences.getStringSet("contacts", new HashSet<>());

        // Clear previous views before adding new ones
        contactsList.removeAllViews();

        if (contacts.isEmpty()) {
            TextView noContactsText = new TextView(getContext());
            noContactsText.setText("No contacts added yet.");
            noContactsText.setTextSize(30); // Increase text size
            contactsList.addView(noContactsText);
        } else {
            // Loop through the contacts and create a checkbox for each with larger text
            for (String contact : contacts) {
                CheckBox checkBox = new CheckBox(getContext());
                checkBox.setText(contact.replace(":", " :- "));
                checkBox.setTag(contact); // Store the contact as a tag on the checkbox
                checkBox.setTextSize(28); // Increase text size
                checkBox.setPadding(10, 10, 10, 10); // Add padding for better visibility
                contactsList.addView(checkBox);
            }
        }
    }


    // Delete the selected contacts
    private void deleteSelectedContacts() {
        Set<String> contacts = sharedPreferences.getStringSet("contacts", new HashSet<>());
        Iterator<String> iterator = contacts.iterator();

        // Loop through the contacts and remove the ones that are checked
        while (iterator.hasNext()) {
            String contact = iterator.next();
            for (int i = 0; i < contactsList.getChildCount(); i++) {
                CheckBox checkBox = (CheckBox) contactsList.getChildAt(i);
                if (checkBox.isChecked() && checkBox.getTag().equals(contact)) {
                    iterator.remove();
                    break;
                }
            }
        }

        // Save updated contacts list back to SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("contacts", contacts);
        editor.apply();

        // Refresh the displayed contacts
        showContacts();

        // Show success message
        Toast.makeText(getContext(), "Selected contacts deleted.", Toast.LENGTH_SHORT).show();
    }
}
