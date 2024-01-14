package com.util.messagesender;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
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
import com.util.main.MainActivity;
import com.util.util.Common;
import com.util.util.Constants;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageSenderActivity extends AppCompatActivity {
    private Context baseContext;
    private String name;
    private String number;
    private TextView sendMessageButton;
    private TextView currentNumberOfMessage;
    private TextView noOfMessagesTextView;
    private TextView messageGapTextView;
    private Set<String> randomDigitsSet;
    private Set<String> randomStringSet;
    private TextView stopMessagingButton;
    private Timer timer;
    private SmsManager smsManager;
    private String templateMessage;
    private TextView enterMessageTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myInit();


        noOfMessagesTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!noOfMessagesTextView.getText().toString().isEmpty() && Integer.parseInt(noOfMessagesTextView.getText().toString()) > Integer.parseInt(getString(R.string.ms_max_no_of_message)))
                    noOfMessagesTextView.setText(R.string.ms_max_no_of_message);
            }
        });

        messageGapTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!messageGapTextView.getText().toString().isEmpty() && Integer.parseInt(messageGapTextView.getText().toString()) > Integer.parseInt(getString(R.string.ms_max_message_gap)))
                    messageGapTextView.setText(R.string.ms_max_message_gap);
            }
        });

        // select contact click listener
        Button selectContactButton = findViewById(R.id.ms_select_contact);
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
                                sendMessageButton.setText(getString(R.string.ms_send_message_text, name));
                                sendMessageButton.setClickable(true);
                                sendMessageButton.setBackgroundColor(MainActivity.COLOR10_DARK);
                                Snackbar.make(findViewById(R.id.ms_select_contact), "Contact selected: " + name, Snackbar.LENGTH_LONG).setAction("Action", null).show();
                            } else {
                                sendMessageButton.setClickable(false);
                                sendMessageButton.setBackgroundColor(MainActivity.COLOR10_LIGHT);
                                Snackbar.make(findViewById(R.id.ms_select_contact), "No contact is selected", Snackbar.LENGTH_LONG).setAction("Action", null).show();
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

        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            final ActivityResultLauncher<String> requestMessagingPermissionLauncher =
                    registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                        System.out.println("Is message permission granted: [" + isGranted + "]");
                        if (isGranted) {
                            performRepeatedMessages();
                        }
                    });
            @Override
            public void onClick(View v) {
                templateMessage = enterMessageTextView.getText().toString().trim();
                if (ContextCompat.checkSelfPermission(baseContext, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("Already have the permission " + Manifest.permission.SEND_SMS);

                    if (!templateMessage.isEmpty() && !noOfMessagesTextView.getText().toString().isEmpty() && !messageGapTextView.getText().toString().isEmpty() && (number != null && !number.isEmpty())) {
                        // Check for random digit string
                        if (templateMessage.contains(getString(R.string.ms_random_digit))) {
                            Pattern pattern = Pattern.compile(getString(R.string.ms_random_digit) + "(\\d+)#");
                            Matcher matcher = pattern.matcher(templateMessage);
                            randomDigitsSet = null;
                            while (matcher.find()) {
                                if (randomDigitsSet == null)
                                    randomDigitsSet = new HashSet<>();
                                String digitStr = matcher.group(1);
                                if (digitStr == null || digitStr.isEmpty() || !Pattern.matches("^\\d+$", digitStr)){
                                    Snackbar.make(v, "Please provide valid replaceable strings", Snackbar.LENGTH_LONG).show();
                                    return;
                                }
                                randomDigitsSet.add(digitStr);
                            }
                        }
                        // Check for random string string
                        if (templateMessage.contains(getString(R.string.ms_random_string))) {
                            Pattern pattern = Pattern.compile(getString(R.string.ms_random_string) + "(\\d+)#");
                            Matcher matcher = pattern.matcher(templateMessage);
                            randomStringSet = null;
                            while (matcher.find()) {
                                if (randomStringSet == null)
                                    randomStringSet = new HashSet<>();
                                String digitStr = matcher.group(1);
                                if (digitStr == null || digitStr.isEmpty() || !Pattern.matches("^\\d+$", digitStr)){
                                    Snackbar.make(v, "Please provide valid replaceable strings", Snackbar.LENGTH_LONG).show();
                                    return;
                                }
                                randomStringSet.add(digitStr);
                            }
                        }
                        performRepeatedMessages();
//                        enterMessageTextView.setEnabled(false);
                        Snackbar.make(v, "Sending " + noOfMessagesTextView.getText() + " message(s) to " + name + ", after every " + messageGapTextView.getText() + " sec", Snackbar.LENGTH_LONG).show();
                    } else
                        Snackbar.make(v, "Please enter required fields", Snackbar.LENGTH_LONG).show();
                } else {
                    System.out.println("Asking for permission " + Manifest.permission.SEND_SMS);
                    requestMessagingPermissionLauncher.launch(Manifest.permission.SEND_SMS);
                }
            }
        });

        stopMessagingButton.setOnClickListener(v -> {
            if(timer != null){
                timer.cancel();
//                    enterMessageTextView.setEnabled(true);
                System.out.println("Cancel messaging");
                currentNumberOfMessage.setVisibility(View.INVISIBLE);
                stopMessagingButton.setClickable(false);
                stopMessagingButton.setBackgroundColor(MainActivity.COLOR30_LIGHT);
                Snackbar.make(v, "Messaging stopped to " + name, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void performRepeatedMessages() {
        stopMessagingButton.setBackgroundColor(MainActivity.COLOR30_DARK);
        stopMessagingButton.setClickable(true);
        if (timer != null)
            timer.cancel();
        currentNumberOfMessage.setText(getString(R.string.ms_current_no_of_message, 0));
        currentNumberOfMessage.setVisibility(View.VISIBLE);
        timer = new Timer();
        timer.schedule(new MessageTask(), Constants.AUTOMATIC_MESSAGING_INITIAL_DELAY, Integer.parseInt(messageGapTextView.getText().toString()) * 1000L);
        Snackbar.make(findViewById(R.id.ms_button_message), "Messaging to " + name + " " + noOfMessagesTextView.getText().toString() + " times after every " + messageGapTextView.getText().toString() + " sec", Snackbar.LENGTH_LONG).setAction("Action", null).show();
    }

    private void myInit() {
        setContentView(R.layout.activity_message_sender);
        baseContext = getBaseContext();

        // Header
        TextView headerTxt = findViewById(R.id.app_header);
        headerTxt.setText(R.string.message_sender);

        // Main menu button
        Button mainMenuBtn = findViewById(R.id.main_menu);
        mainMenuBtn.setOnClickListener(v -> startActivity(new Intent(baseContext, MainActivity.class)));

        sendMessageButton = findViewById(R.id.ms_button_message);
        sendMessageButton.setText(getString(R.string.ms_send_message_text, ""));
        currentNumberOfMessage = findViewById(R.id.ms_current_message_number);
        currentNumberOfMessage.setText(getString(R.string.ms_current_no_of_message, 0));
        currentNumberOfMessage.setVisibility(View.INVISIBLE);
        noOfMessagesTextView = findViewById(R.id.ms_no_of_message);
        messageGapTextView = findViewById(R.id.ms_message_gap);
        stopMessagingButton = findViewById(R.id.ms_stop_message);
        enterMessageTextView = findViewById(R.id.ms_enter_message);
    }

    class MessageTask extends TimerTask {
        int currentReverseNoOfMessage = Integer.parseInt(noOfMessagesTextView.getText().toString());
        public void run() {
            if (currentReverseNoOfMessage > 0) {
                currentNumberOfMessage.setText(getString(R.string.ms_current_no_of_message, (Integer.parseInt(noOfMessagesTextView.getText().toString()) - currentReverseNoOfMessage + 1)));
                String messageToSend = new String(templateMessage);
                if(randomDigitsSet != null && !randomDigitsSet.isEmpty()){
                    for(String digitStr : randomDigitsSet)
                        messageToSend = Common.replacePlaceholderWithNumberStr(messageToSend, (getString(R.string.ms_random_digit) + digitStr + "#"), Integer.parseInt(digitStr));
                }
                if(randomStringSet != null && !randomStringSet.isEmpty()){
                    for(String digitStr : randomStringSet)
                        messageToSend = Common.replacePlaceholderWithCharStr(messageToSend, (getString(R.string.ms_random_string) + digitStr + "#"), Integer.parseInt(digitStr));
                }
                if(smsManager == null)
                    smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(number, null, messageToSend, null, null);
                currentReverseNoOfMessage--;
            } else {
                System.out.println("Time's up!");
                stopMessagingButton.setClickable(false);
//                enterMessageTextView.setEnabled(true);
                stopMessagingButton.setBackgroundColor(MainActivity.COLOR30_LIGHT);
                currentNumberOfMessage.setVisibility(View.INVISIBLE);
                Snackbar.make(findViewById(R.id.ms_stop_message), "Messaging stopped to " + name, Snackbar.LENGTH_LONG).show();
                timer.cancel();
            }
        }
    }
}