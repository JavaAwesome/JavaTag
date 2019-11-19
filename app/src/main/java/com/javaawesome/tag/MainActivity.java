package com.javaawesome.tag;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.SignInUIOptions;
import com.amazonaws.mobile.client.UserStateDetails;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "javaawesome";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize aws mobile client and check if you are logged in or not
        AWSMobileClient.getInstance().initialize(getApplicationContext(), new Callback<UserStateDetails>() {
            @Override
            public void onResult(UserStateDetails result) {
                // if the user is signed out, show them the sign in page
                if (result.getUserState().toString().equals("SIGNED_OUT")) {
                    signInUser();
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, e.getMessage());
            }
        });
    }

    public void goToMap(View view) {
        Intent goToMapIntent = new Intent(this, MapsActivity.class);
        this.startActivity(goToMapIntent);
    }

    // Direct users to sign in page
    public void signInUser() {
        AWSMobileClient.getInstance().showSignIn(MainActivity.this,
                // customize the built in sign in page
                SignInUIOptions.builder().backgroundColor(16763080).build(),
                new Callback<UserStateDetails>() {
                    @Override
                    public void onResult(UserStateDetails result) {
                        Log.i(TAG, "successfully show signed in page");
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                });
    }

    // sign out user and show them sign in page
    public void signoutCurrentUser(View view) {
        AWSMobileClient.getInstance().signOut();
        signInUser();
    }
}
