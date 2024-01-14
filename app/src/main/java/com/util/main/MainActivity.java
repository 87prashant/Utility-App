package com.util.main;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.util.R;
import com.util.automaticredialer.AutomaticRedialerActivity;
import com.util.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    public static int COLOR10_DARK;
    public static int COLOR10_LIGHT;
    public static int COLOR30_DARK;
    public static int COLOR30_LIGHT;
    public static int COLOR60_DARK;
    public static int COLOR60_LIGHT;
    Context baseContext = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainInit();

        // Header
        TextView headerTxt = findViewById(R.id.app_header);
        headerTxt.setText(R.string.app_name);

        // Automatic redialer activity
        Button automaticRedialerBtn = findViewById(R.id.automatic_redialer);
        automaticRedialerBtn.setOnClickListener(v -> startActivity(new Intent(baseContext, AutomaticRedialerActivity.class)));
    }

    private void mainInit() {
        setMainColors();
        ActivityMainBinding mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        baseContext = getBaseContext();
    }

    private void setMainColors() {
        Resources r = getResources();
        COLOR10_DARK = r.getColor(R.color.color10_dark, null);
        COLOR10_LIGHT = r.getColor(R.color.color10_light, null);
        COLOR30_DARK = r.getColor(R.color.color30_dark, null);
        COLOR30_LIGHT = r.getColor(R.color.color30_light, null);
        COLOR60_DARK = r.getColor(R.color.color60_dark, null);
        COLOR60_LIGHT = r.getColor(R.color.color60_light, null);
    }
}