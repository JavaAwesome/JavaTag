package com.javaawesome.tag;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import android.os.Environment;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.concurrent.Executor;


public class ShowMeYourFace extends AppCompatActivity {
    private static final String TAG = "ahren:javatag";
    private ImageCapture imageCapture;
    final CameraX.LensFacing camera = CameraX.LensFacing.FRONT;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_me_your_face);

        Log.i(TAG, "onCreate: Hello World");
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,
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
            FloatingActionButton picSnap = findViewById(R.id.picSnap);
//            FloatingActionButton switchCamera = findViewById(R.id.fab_switch_camera);
//            FloatingActionButton fab_flash = findViewById(R.id.fab_flash);

            bindCamera();



//***************************************   Shutter Button Action ****************************************


//                @Override
//                public void onClick(View view){
//                    File file = new File(Environment.getExternalStorageDirectory() + "/" + System.currentTimeMillis() + ".png");
//                  imageCapture.takePicture(file, new ImageCapture.OnImageSavedListener(){
//                        @Override
//                        public void onImageSaved(@NonNull File file) {
//                            String msg = "Pic captured at " + file.getAbsolutePath();
//                            Toast.makeText(getBaseContext(), msg,Toast.LENGTH_LONG).show();
//                        }
//
//                        @Override
//                        public void onError(@NonNull ImageCapture.ImageCaptureError imageCaptureError, @NonNull String message, @Nullable Throwable cause) {
//                        }
//                    });
//                }

            picSnap.setOnClickListener(event -> {
                Log.i(TAG, "onCreate: taking picture?");
                File file = new File(Environment.getExternalStorageDirectory() + "/" + System.currentTimeMillis() + ".png");
                String msg = "file will be saved at " + file.getAbsolutePath();
                Toast.makeText(getBaseContext(), msg,Toast.LENGTH_LONG).show();
//              Why what is it used for???????
                Executor executor = runnable -> {
                };

                imageCapture.takePicture(file, executor,
                        new ImageCapture.OnImageSavedListener() {
                            @Override
                            public void onImageSaved(@NonNull File file) {
                                Log.i(TAG, "onImageSaved: INside Image Saved");
                                String msg = "Pic captured at " + file.getAbsolutePath();
                                Toast.makeText(getBaseContext(), msg,Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onError(
                                    ImageCapture.ImageCaptureError imageCaptureError, String message, Throwable cause) {
//                                       TODO: insert your code here.
                            }
                        });
            });


//*************************Setting up the image capture config*************************




//*****************************     Turn Off / On Flash***********************************************
//      Adapted from Kotlin code at https://gabrieltanner.org/blog/android-camerax
//            fab_flash.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    FlashMode flashMode = imageCapture.getFlashMode();
//                    if (flashMode == FlashMode.ON) {
//                        imageCapture.setFlashMode(FlashMode.OFF);
//                    } else {
//                        imageCapture.setFlashMode(FlashMode.ON);
//                    }
//                }
//            });

// ******************* Changes the lens direction if the button is clicked ****************************

//            switchCamera.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    if (CameraX.LensFacing.FRONT == camera) {
//                        camera = CameraX.LensFacing.BACK;
//                    } else {
//                       camera[0] = CameraX.LensFacing.FRONT;
//                    }
//                    bindCamera();
//                }
//            });
        }
    }

    private void bindCamera() {
        CameraX.unbindAll();
        final TextureView textureView = findViewById(R.id.view_finder);
        Size screen = new Size(textureView.getWidth(), textureView.getHeight()); //size of the screen



        PreviewConfig config = new PreviewConfig.Builder()
                .setLensFacing(camera)
                .setTargetResolution(screen)
                .build();
        Preview preview = new Preview(config);

//      Set the display view for the camera preview
        preview.setOnPreviewOutputUpdateListener(new Preview.OnPreviewOutputUpdateListener() {
            @Override
            public void onUpdated(Preview.PreviewOutput previewOutput) {
                // Your code here. For example, use
                textureView.setSurfaceTexture(previewOutput.getSurfaceTexture());
            }

        });

        ImageCaptureConfig config2 =
                new ImageCaptureConfig.Builder()
                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                        .setLensFacing(camera)
                        .build();

        imageCapture = new ImageCapture(config2);

//      Causes camera u=instance to only exist on this activity is started and destroyed on start and finish
        CameraX.bindToLifecycle(this, imageCapture, preview);
    }

}
//// **************** Checks to see if flash is present on the current camera and *********************
//            try {
//                CameraInfo cameraInfo = CameraX.getCameraInfo(camera);
//                LiveData<Boolean> isFlashAvailable = cameraInfo.isFlashAvailable();
//                fab_flash.setVisibility(isFlashAvailable.getValue() ? View.VISIBLE : View.INVISIBLE);
//            } catch (CameraInfoUnavailableException e) {
//                Log.w(TAG, "Cannot get flash available information", e);
//                fab_flash.setVisibility(View.VISIBLE);
//            }