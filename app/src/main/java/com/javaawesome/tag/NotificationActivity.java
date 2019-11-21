package com.javaawesome.tag;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;

public class NotificationActivity extends AppCompatActivity {

    Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        // Variable for vibration notification
        vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(1000);
    }

    public void acknowledgePlayerIsIt (View view) {
        finish();
    }
}
