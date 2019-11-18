package com.java401.tag;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraX;
import androidx.lifecycle.LiveData;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.javaawesome.tag.R;

public class ShowMeYourFace extends AppCompatActivity {
    private static final String TAG = "ahren:javatag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_me_your_face);

        try {
            CameraInfo cameraInfo = CameraX.getCameraInfo(currentCameraLensFacing);
            LiveData<Boolean> isFlashAvailable = cameraInfo.isFlashAvailable();
            flashToggle.setVisibility(isFlashAvailable.getValue() ? View.VISIBLE : View.INVISIBLE);
        } catch (CameraInfoUnavailableException e) {
            Log.w(TAG, "Cannot get flash available information", e);
            flashToggle.setVisibility(View.VISIBLE);
        }
    }
}
