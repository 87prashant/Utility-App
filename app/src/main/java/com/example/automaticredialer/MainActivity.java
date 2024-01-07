package com.example.automaticredialer;

import android.Manifest;
import android.app.Activity;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.transition.Visibility;

import com.example.automaticredialer.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    Timer timer = null;
    Context context = null;
    Activity activity = null;
    String name = null;
    String number = null;
    TextView noOfCallsTextView = null;
    Button stopCallingButton = null;
//    int currentReverseCallNo = 0;
    TextView currentCallNo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        activity = getParent();

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        noOfCallsTextView = ((TextView) findViewById(R.id.no_of_calls));
        noOfCallsTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                System.out.println("Focus change listener of No of calls TextView");
                if (!noOfCallsTextView.getText().toString().isEmpty() && Integer.parseInt(noOfCallsTextView.getText().toString()) > 40)
                    noOfCallsTextView.setText("40");
            }
        });

        currentCallNo = findViewById(R.id.current_call_number);
        currentCallNo.setVisibility(View.INVISIBLE);

        Button selectContactButton = findViewById(R.id.select_contact);
        Button callButton = findViewById(R.id.button_call);
        callButton.setClickable(false);
        stopCallingButton = findViewById(R.id.stop_call);
        stopCallingButton.setClickable(false);

        // select contact click listener
        selectContactButton.setOnClickListener(new View.OnClickListener() {
            final ActivityResultLauncher<Void> getContact = registerForActivityResult(new ActivityResultContracts.PickContact(), uri -> {
                // Handle the returned Uri
                if (uri != null) {
                    Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                    if(cursor != null && cursor.moveToFirst()) {
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
                                callButton.setText("Call " + name);
                                callButton.setClickable(true);
                                callButton.setBackgroundColor(0xFF022d9c);
                                Snackbar.make(findViewById(R.id.select_contact), "Contact selected: " + name, Snackbar.LENGTH_LONG).setAction("Action", null).show();
                            } else {
                                callButton.setClickable(false);
                                callButton.setBackgroundColor(0xFF70a0ff);
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
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
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
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
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
                System.out.println("Cancel calling");
                currentCallNo.setVisibility(View.INVISIBLE);
                stopCallingButton.setClickable(false);
                stopCallingButton.setBackgroundColor(0xFFa8ffbf);
                if (timer != null)
                    timer.cancel();
                Snackbar.make(v, "Calling stopped to: " + name, Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });
    }

    private void performRepeatedCalls() {
        if (!noOfCallsTextView.getText().toString().isEmpty() && number != null && !number.isEmpty()) {
            if (timer != null)
                timer.cancel();
            currentCallNo.setVisibility(View.VISIBLE);
            stopCallingButton.setClickable(true);
            stopCallingButton.setBackgroundColor(0xFF059600);
            timer = new Timer();
            timer.schedule(new CallTask(), 3*1000, 50 * 1000);
            Snackbar.make(findViewById(R.id.button_call), "Calling to: " + name + ", " + noOfCallsTextView.getText().toString() + " times after every 50 sec", Snackbar.LENGTH_LONG).setAction("Action", null).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    class CallTask extends TimerTask {
        int currentReverseCallNo = Integer.parseInt(noOfCallsTextView.getText().toString());
        public void run() {
            if (currentReverseCallNo > 0) {
                currentCallNo.setText("Current Call: " + (Integer.parseInt(noOfCallsTextView.getText().toString()) - currentReverseCallNo + 1));
                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
                TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                System.out.println("is Telephony Enabled: " + (telephonyManager != null && telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY));
                startActivity(intent);
                System.out.println("Calling: " + currentReverseCallNo);
                currentReverseCallNo--;
            } else {
                System.out.println("Time's up!");
                stopCallingButton.setClickable(false);
                stopCallingButton.setBackgroundColor(0xFFa8ffbf);
                currentCallNo.setVisibility(View.INVISIBLE);
                timer.cancel();
            }
        }
    }

}