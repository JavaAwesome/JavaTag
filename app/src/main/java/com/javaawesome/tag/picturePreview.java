package com.javaawesome.tag;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;


public class picturePreview extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_preview);
        String TAG = "ahren:picturePreview";
        ImageView profilePicPreview;
        Log.i(TAG, "onCreate: hello");
        ImageView pic = findViewById(R.id.profilePicPreview);
        String picPath = getIntent().getStringExtra("picpath");
        pic.setImageURI(Uri.parse(picPath));
        FloatingActionButton accept = findViewById(R.id.accept);
        FloatingActionButton deny = findViewById(R.id.deny);

        accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ShowMeYourFace.setUpload(true);
                finish();
            }
        });

        deny.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }
}
