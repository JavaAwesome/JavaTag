package com.javaawesome.tag;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class UserProfile extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
    }

    ///////////// Turn on Camera ///////////////////
    public void goToCameraClass(View view){
        Intent goToCamera = new Intent(this, ShowMeYourFace.class);
        this.startActivity(goToCamera);
    }

}
