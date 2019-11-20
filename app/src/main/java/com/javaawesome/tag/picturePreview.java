package com.javaawesome.tag;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;


public class picturePreview extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_preview);
        String TAG = "ahren:picturePreview";
        ImageView profilePicPreview;
        Log.i(TAG, "onCreate: hello");
    }
}
