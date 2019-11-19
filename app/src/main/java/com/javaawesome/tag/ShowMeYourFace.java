package com.javaawesome.tag;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.util.concurrent.Executor;


public class ShowMeYourFace extends AppCompatActivity {
    private static final String TAG = "ahren:javatag";
    private ImageCapture imageCapture;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_me_your_face);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        1);
            }
        }else {

            PreviewConfig config = new PreviewConfig.Builder().build();
            Preview preview = new Preview(config);

            TextureView textureView = findViewById(R.id.view_finder);

            preview.setOnPreviewOutputUpdateListener(new Preview.OnPreviewOutputUpdateListener() {
                @Override
                public void onUpdated(Preview.PreviewOutput previewOutput) {
                    // Your code here. For example, use
                    textureView.setSurfaceTexture(previewOutput.getSurfaceTexture());
                    // and post to a GL renderer.
                }

            });


            CameraX.bindToLifecycle(this, preview);



            ImageCaptureConfig config2 =
                    new ImageCaptureConfig.Builder()
                            .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                            .build();

           imageCapture = new ImageCapture(config2);

            CameraX.bindToLifecycle(this, imageCapture, preview);

            Button picSnap = findViewById(R.id.picSnap);
            picSnap.setOnClickListener(new View.OnClickListener() {
                   @Override
                   public void onClick(View event) {
                       File profilePhoto = new File("userPicture.jpg");
                       Executor executor = new Executor() {
                           @Override
                           public void execute(Runnable runnable) {

                           }
                       };

                       imageCapture.takePicture(file, executor,
                               new ImageCapture.OnImageSavedListener() {
                                   @Override
                                   public void onImageSaved(File file) {
                                       // insert your code here.
                                   }

                                   @Override
                                   public void onError(
                                           ImageCapture.ImageCaptureError imageCaptureError,
                                           String message,
                                           Throwable cause) {
                                       // insert your code here.

                                   }
                               });
                   }
               });





//
//        try {
//            CameraInfo cameraInfo = CameraX.getCameraInfo(currentCameraLensFacing);
//            LiveData<Boolean> isFlashAvailable = cameraInfo.isFlashAvailable();
//            flashToggle.setVisibility(isFlashAvailable.getValue() ? View.VISIBLE : View.INVISIBLE);
//        } catch (CameraInfoUnavailableException e) {
//            Log.w(TAG, "Cannot get flash available information", e);
//            flashToggle.setVisibility(View.VISIBLE);
//
//        }}
        }
    }


}
