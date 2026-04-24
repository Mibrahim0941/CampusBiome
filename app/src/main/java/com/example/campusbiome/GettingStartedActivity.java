package com.example.campusbiome;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class GettingStartedActivity extends AppCompatActivity {

    private Button btnGetStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure this matches your XML file name
        setContentView(R.layout.activity_getting_started);

        // Initialize the button
        btnGetStarted = findViewById(R.id.btnGetStarted);

        // Set Click Listener
        btnGetStarted.setOnClickListener(view -> {
            Intent intent = new Intent(GettingStartedActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }
}