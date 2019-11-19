package com.javaawesome.tag;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
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
import android.provider.MediaStore;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.util.concurrent.Executor;


public class ShowMeYourFace extends AppCompatActivity {
    private static final String TAG = "ahren:javatag";
    private ImageCapture imageCapture;

    private void goToPicPreview(View view){
        Intent goToPicturePreview = new Intent(this, picturePreview.class);
        this.startActivity(goToPicturePreview);
    }



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

            PreviewConfig config = new PreviewConfig.Builder()
//                    Allow the camera to rotate???
                    .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                    .build();

            Preview preview = new Preview(config);
//      Set the display view for the camera preview
            final TextureView textureView = findViewById(R.id.view_finder);

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

//      Causes camera u=instance to only exist on this activity is started and destroyed on start and finish
            CameraX.bindToLifecycle(this, imageCapture, preview);

            Button picSnap = findViewById(R.id.picSnap);
            picSnap.setOnClickListener(new View.OnClickListener() {
                   @Override
                   public void onClick(View event) {
//                       File profilePic = new File("./");

                       Executor executor = new Executor() {
                           @Override
                           public void execute(Runnable runnable) {

                           }
                       };

                       imageCapture.takePicture(executor,
                               new ImageCapture.OnImageCapturedListener() {

                                    public void onCaptureSuccess(ImageProxy image, int rotationDegrees){
                                        Log.i(TAG, "onCaptureSuccess: registered a camera click!");
                                        image.getImage();
                                       }


                                   @Override
                                   public void onError(
                                           ImageCapture.ImageCaptureError imageCaptureError, String message, Throwable cause) {
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
