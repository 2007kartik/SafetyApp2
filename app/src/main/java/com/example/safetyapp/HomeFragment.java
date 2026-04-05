package com.example.safetyapp;

import android.text.TextUtils;
import android.util.Log;
import android.location.Location;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HomeFragment extends Fragment {

    private HorizontalScrollView horizontalScrollView;
    private Handler handler = new Handler();
    private Runnable runnable;
    private int scrollSpeed = 90;
    private int totalScrollWidth;
    private int currentScrollX = 0;

    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST = 3;

    private SharedPreferences sharedPreferences;

    // 🎤 Voice recognition
    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;
    private boolean voiceEnabled = false;


    public HomeFragment() {}

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {



        View view = inflater.inflate(R.layout.fragment_home, container, false);

        sharedPreferences = requireActivity()
                .getSharedPreferences("EmergencyContacts", Context.MODE_PRIVATE);

        fusedLocationClient = LocationServices
                .getFusedLocationProviderClient(requireActivity());

        Button voiceToggle = view.findViewById(R.id.voiceToggle);

        voiceToggle.setOnClickListener(v -> {
            voiceEnabled = !voiceEnabled;

            if (voiceEnabled) {
                startVoiceRecognition();
                voiceToggle.setText("Voice SOS ON");
                Toast.makeText(getActivity(),"Voice activated",Toast.LENGTH_SHORT).show();
            } else {
                stopVoiceRecognition();
                voiceToggle.setText("Enable Voice SOS");
                Toast.makeText(getActivity(),"Voice stopped",Toast.LENGTH_SHORT).show();
            }
        });

        Button panicButton = view.findViewById(R.id.button);
        panicButton.setOnClickListener(v -> sendPanicSMSWithLocation());

        Button shareSafeButton = view.findViewById(R.id.button2);
        shareSafeButton.setOnClickListener(v -> sendSafeSMS());

        view.findViewById(R.id.EM1).setOnClickListener(v -> openPhoneNumber("100"));
        view.findViewById(R.id.EM2).setOnClickListener(v -> openPhoneNumber("108"));
        view.findViewById(R.id.EM3).setOnClickListener(v -> openPhoneNumber("101"));
        view.findViewById(R.id.EM4).setOnClickListener(v -> openPhoneNumber("1091"));

        view.findViewById(R.id.imageButton).setOnClickListener(v -> searchNearbyPlaces("police station"));
        view.findViewById(R.id.imageButton2).setOnClickListener(v -> searchNearbyPlaces("hospital"));
        view.findViewById(R.id.imageButton3).setOnClickListener(v -> searchNearbyPlaces("pharmacy"));
        view.findViewById(R.id.imageButton4).setOnClickListener(v -> searchNearbyPlaces("bus station"));

        view.findViewById(R.id.law1).setOnClickListener(v -> openLink("https://mind4survival.com/situational-awareness-staying-alert-and-staying-safe/"));
        view.findViewById(R.id.law2).setOnClickListener(v -> openLink("https://issuesiface.com/magazine/top-10-safety-tips-for-women"));
        view.findViewById(R.id.law3).setOnClickListener(v -> openLink("https://iso26262guide.com/articles/blog-post-safety-is-highest-priority"));
        view.findViewById(R.id.law4).setOnClickListener(v -> openLink("https://blog.acumenacademy.org/environmental-and-climate-friendly-actions?utm_source=bing&utm_medium=&utm_campaign=&utm_term=living%20sustainably&hsa_kw=living%20sustainably&hsa_cam=566105817&hsa_ver=3&hsa_acc=3091410687&hsa_ad=&hsa_grp=1173179916541089&hsa_src=o&hsa_mt=b&hsa_tgt=kwd-73323938016698:loc-90&hsa_net=adwords&msclkid=cef0ddebc5b01e49589cba387e01c5ac&utm_content=Sustainable%20Living"));


        horizontalScrollView = view.findViewById(R.id.HorizontalScrollView);

        horizontalScrollView.setOnScrollChangeListener(
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> currentScrollX = scrollX);

        horizontalScrollView.post(() ->
                totalScrollWidth = horizontalScrollView.getChildAt(0).getWidth());

        runnable = () -> {
            currentScrollX += 10;
            if (currentScrollX >= totalScrollWidth) currentScrollX = 0;
            horizontalScrollView.scrollTo(currentScrollX, 0);
            handler.postDelayed(runnable, scrollSpeed);
        };

        // 🚨 Start shake service
        Intent serviceIntent = new Intent(requireActivity(), ShakeDetectionService.class);
        requireActivity().startForegroundService(serviceIntent);

        // 🎤 Start voice system (foreground safe)
//        requestMicPermission();
//        startVoiceRecognition();

        return view;



    }

    //stopVoiceRecognition() method
    private void stopVoiceRecognition() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }


    // ================= VOICE COMMAND =================

    private void requestMicPermission() {
        if (ContextCompat.checkSelfPermission(requireActivity(),
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.RECORD_AUDIO}, 200);
        }
    }

    private void startVoiceRecognition() {

        if (speechRecognizer != null) return;


        if (!SpeechRecognizer.isRecognitionAvailable(requireActivity())) {
            Toast.makeText(getActivity(),"Speech not supported",Toast.LENGTH_LONG).show();
            return;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireActivity());

        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override public void onReadyForSpeech(Bundle params) {
                Toast.makeText(getActivity(),"Listening...",Toast.LENGTH_SHORT).show();
            }

            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {

                restartListening();
            }

            @Override
            public void onError(int error) {
                restartListening();
            }

            @Override
            public void onResults(Bundle results) {
                processSpeech(results);
                //restartListening();
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                processSpeech(partialResults);
            }

            @Override public void onEvent(int eventType, Bundle params) {}
        });

        speechRecognizer.startListening(speechIntent);
    }

    private void restartListening() {
        if (speechRecognizer != null) {
            try {
                speechRecognizer.cancel();
                speechRecognizer.startListening(speechIntent);
            } catch (Exception ignored) {}
        }
    }


    private void processSpeech(Bundle results) {

        ArrayList<String> words =
                results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        if (words == null) return;

        for (String text : words) {

            text = text.toLowerCase();
            Toast.makeText(getActivity(),"Heard: " + text,Toast.LENGTH_SHORT).show();

            if (text.contains("help")) {
                Toast.makeText(getActivity(),"EMERGENCY TRIGGERED!",Toast.LENGTH_LONG).show();
                sendPanicSMSWithLocation();
                break;
            }
        }
    }

    // ================= EXISTING CODE (UNCHANGED) =================

    @Override
    public void onResume() {
        super.onResume();
        handler.postDelayed(runnable, scrollSpeed);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(runnable);
        stopVoiceRecognition();

    }

    public void openLink(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    public void openPhoneNumber(String phoneNumber) {
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber)));
    }

    private void searchNearbyPlaces(String placeType) {
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(placeType));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }

    private void sendPanicSMSWithLocation() {
        if (ContextCompat.checkSelfPermission(requireActivity(),
                Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.SEND_SMS,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        sendSMSToAllContacts("Emergency! Please help! ");
    }

    private void sendSafeSMS() {
        sendSMSToAllContacts("I am safe. Don't worry!");
    }

    @SuppressLint("MissingPermission")
    private void sendSMSToAllContacts(String message) {

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {

            if (location == null) return;

            String mapsUrl = "https://www.google.com/maps/search/?api=1&query=" +
                    location.getLatitude() + "," + location.getLongitude();

            String finalMessage = message + "\n" + mapsUrl;

            Set<String> contacts =
                    sharedPreferences.getStringSet("contacts", new HashSet<>());

            boolean first = true;

            for (String contact : contacts) {

                String[] info = contact.split(":");
                if (info.length == 2) {

                    String phone = info[1];

                    SmsManager.getDefault()
                            .sendTextMessage(phone,null,finalMessage,null,null);

                    if (first) {
                        makeCallToNumber(phone);
                        first = false;
                    }
                }
            }
        });
    }

    private void makeCallToNumber(String phoneNumber) {

        if (ContextCompat.checkSelfPermission(requireActivity(),
                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.CALL_PHONE},1001);
            return;
        }

        startActivity(new Intent(Intent.ACTION_CALL,
                Uri.parse("tel:" + phoneNumber)));
    }
}
