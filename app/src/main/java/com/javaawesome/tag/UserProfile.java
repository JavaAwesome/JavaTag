package com.javaawesome.tag;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.squareup.picasso.Picasso;

public class UserProfile extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        ImageView profPic = findViewById(R.id.profilePicture);
        Picasso.get().load(AWSMobileClient.getInstance().photo()).into(profPic);


        TextView username = findViewById(R.id.username);
        username.setText(AWSMobileClient.getInstance().getUsername());
    }
    ///////////// Turn on Camera ///////////////////
    public void goToCameraClass(View view){
        Intent goToCamera = new Intent(this, ShowMeYourFace.class);
        this.startActivity(goToCamera);
    }

}

