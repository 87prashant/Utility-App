package com.util.automaticredialer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.util.R;
import com.util.databinding.ActivityAutomaticRedialerBinding;
import com.util.main.MainActivity;
import com.util.util.Constants;

import java.util.Timer;
import java.util.TimerTask;

public class AutomaticRedialerActivity extends AppCompatActivity {

    Timer timer = null;
    Context baseContext = null;
    String name = null;
    String number = null;
    TextView noOfCallsTextView = null;
    Button stopCallingButton = null;
    TextView currentNoOfCalls = null;
    TextView callGapTextView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityAutomaticRedialerBinding binding = ActivityAutomaticRedialerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        baseContext = getBaseContext();

        // Header
        TextView headerTxt = findViewById(R.id.app_header);
        headerTxt.setText(R.string.automatic_redialer);

        // Main menu button
        Button mainMenuBtn = findViewById(R.id.main_menu);
        mainMenuBtn.setOnClickListener(v -> startActivity(new Intent(baseContext, MainActivity.class)));

        noOfCallsTextView = findViewById(R.id.no_of_calls);
        noOfCallsTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!noOfCallsTextView.getText().toString().isEmpty() && Integer.parseInt(noOfCallsTextView.getText().toString()) > Integer.parseInt(getString(R.string.ar_max_no_of_calls)))
                    noOfCallsTextView.setText(R.string.ar_max_no_of_calls);
            }
        });

        callGapTextView = findViewById(R.id.call_gap);
        callGapTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!callGapTextView.getText().toString().isEmpty() && Integer.parseInt(callGapTextView.getText().toString()) > Integer.parseInt(getString(R.string.ar_max_call_gap)))
                    callGapTextView.setText(R.string.ar_max_call_gap);
            }
        });

        currentNoOfCalls = findViewById(R.id.current_call_number);
        currentNoOfCalls.setVisibility(View.INVISIBLE);

        Button selectContactButton = findViewById(R.id.select_contact);
        Button callButton = findViewById(R.id.button_call);
        callButton.setText(getString(R.string.ar_default_call_text, ""));
        callButton.setClickable(false);
        stopCallingButton = findViewById(R.id.stop_call);
        stopCallingButton.setClickable(false);

        // select contact click listener
        selectContactButton.setOnClickListener(new View.OnClickListener() {
            final ActivityResultLauncher<Void> getContact = registerForActivityResult(new ActivityResultContracts.PickContact(), uri -> {
                // Handle the returned Uri
                if (uri != null) {
                    Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        int contactIdIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                        String contactId = cursor.getString(contactIdIndex);
                        Cursor phoneCursor = getContentResolver().query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                new String[]{contactId},
                                null
                        );

                        if (phoneCursor != null && phoneCursor.moveToFirst()) {
                            int numberIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                            int nameIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                            if (nameIndex != -1 && numberIndex != -1) {
                                name = phoneCursor.getString(nameIndex);
                                number = phoneCursor.getString(numberIndex);
                                callButton.setText(getString(R.string.ar_default_call_text, name));
                                callButton.setClickable(true);
                                callButton.setBackgroundColor(MainActivity.COLOR10_DARK);
                                Snackbar.make(findViewById(R.id.select_contact), "Contact selected: " + name, Snackbar.LENGTH_LONG).setAction("Action", null).show();
                            } else {
                                callButton.setClickable(false);
                                callButton.setBackgroundColor(MainActivity.COLOR10_LIGHT);
                                Snackbar.make(findViewById(R.id.select_contact), "No contact is selected", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                            }
                            phoneCursor.close();
                        }
                        cursor.close();
                    }
                }
            });

            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(baseContext, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("Already have the permission of " + Manifest.permission.READ_CONTACTS);
                    getContact.launch(null);

                } else {
                    System.out.println("Asking for permission " + Manifest.permission.READ_CONTACTS);
                    getContact.launch(null);
                }
            }
        });

        // call button click listener
        callButton.setOnClickListener(new View.OnClickListener() {
            final ActivityResultLauncher<String> requestCallingPermissionLauncher =
                    registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                        System.out.println("Is calling permission granted: [" + isGranted + "]");
                        if (isGranted) {
                            performRepeatedCalls();
                        }
                    });

            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(baseContext, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("Already have the permission " + Manifest.permission.CALL_PHONE);
                    performRepeatedCalls();
                } else {
                    System.out.println("Asking for permission " + Manifest.permission.CALL_PHONE);
                    requestCallingPermissionLauncher.launch(Manifest.permission.CALL_PHONE);
                }
            }
        });

        stopCallingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (timer != null) {
                    System.out.println("Cancel calling");
                    currentNoOfCalls.setVisibility(View.INVISIBLE);
                    stopCallingButton.setClickable(false);
                    stopCallingButton.setBackgroundColor(MainActivity.COLOR30_LIGHT);
                    timer.cancel();
                    Snackbar.make(v, "Calling stopped to " + name, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void performRepeatedCalls() {
        if (!noOfCallsTextView.getText().toString().isEmpty() && number != null && !number.isEmpty()) {
            stopCallingButton.setBackgroundColor(MainActivity.COLOR30_DARK);
            stopCallingButton.setClickable(true);
            if (timer != null)
                timer.cancel();
            if(callGapTextView.getText().toString().isEmpty())
                callGapTextView.setText(R.string.ar_default_call_gap);
            currentNoOfCalls.setText(getString(R.string.ar_current_no_of_call, 0));
            currentNoOfCalls.setVisibility(View.VISIBLE);
            timer = new Timer();
            timer.schedule(new AutomaticRedialerActivity.CallTask(), Constants.AUTOMATIC_REDIALER_INITIAL_DELAY, Integer.parseInt(callGapTextView.getText().toString()) * 1000L);
            Snackbar.make(findViewById(R.id.button_call), "Calling to " + name + " " + noOfCallsTextView.getText().toString() + " times after every " + callGapTextView.getText().toString() + " sec", Snackbar.LENGTH_LONG).setAction("Action", null).show();
        }
    }

    class CallTask extends TimerTask {
        int currentReverseNoOfCall = Integer.parseInt(noOfCallsTextView.getText().toString());
        public void run() {
            if (currentReverseNoOfCall > 0) {
                currentNoOfCalls.setText(getString(R.string.ar_current_no_of_call, (Integer.parseInt(noOfCallsTextView.getText().toString()) - currentReverseNoOfCall + 1)));
                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
                TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                System.out.println("is Telephony Enabled: " + (telephonyManager != null && telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY));
                startActivity(intent);
                currentReverseNoOfCall--;
            } else {
                System.out.println("Time's up!");
                stopCallingButton.setClickable(false);
                stopCallingButton.setBackgroundColor(MainActivity.COLOR30_LIGHT);
                currentNoOfCalls.setVisibility(View.INVISIBLE);
                Snackbar.make(findViewById(R.id.stop_call), "Calling stopped to " + name, Snackbar.LENGTH_LONG).show();
                timer.cancel();
            }
        }
    }
}