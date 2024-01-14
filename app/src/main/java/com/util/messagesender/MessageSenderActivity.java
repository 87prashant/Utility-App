package com.util.messagesender;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.util.R;
import com.util.main.MainActivity;

public class MessageSenderActivity extends AppCompatActivity {
    private Context baseContext;
    private String name;
    private String number;
    private TextView sendMessageButton;
    private TextView currentNumberOfMessage;
    private TextView noOfMessagesTextView;
    private TextView messageGapTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myInit();

        Button selectContactButton = findViewById(R.id.ms_select_contact);

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
    }
}