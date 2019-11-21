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


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class ShowMeYourFace extends AppCompatActivity {
    private static final String TAG = "ahren:javatag";
    private static boolean upload = false;
    private ImageCapture imageCapture;
    final CameraX.LensFacing camera = CameraX.LensFacing.FRONT;
    String profPicPath = null;
    String s3path = null;
    AWSAppSyncClient mAWSAppSyncClient;
    File profilePic = null;

    public static boolean isUpload() {
        return upload;
    }

    public static void setUpload(boolean upload) {
        ShowMeYourFace.upload = upload;
    }

    public void goToPicturePreview(String  profilePicPath){
        Intent goToPicturePreview = new Intent(this, picturePreview.class);
        this.startActivity(goToPicturePreview.putExtra("picpath",profilePicPath));
    }

    protected void onResume() {
        super.onResume();
        if(upload && profPicPath != null && profilePic != null){
            uploadDataToS3(s3path, profilePic);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_me_your_face);

        mAWSAppSyncClient = AWSAppSyncClient.builder()
                .context(getApplicationContext())
                .awsConfiguration(new AWSConfiguration(getApplicationContext()))
                .build();

//*************  Check If app has camera permissions ************************
        Log.i(TAG, "onCreate: Hello World");
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
                Log.i(TAG, "onCreate: permission not granted");
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

//************************************ Setup Buttons **********************************************
            FloatingActionButton picSnap = findViewById(R.id.picSnap);
//          FloatingActionButton switchCamera = findViewById(R.id.fab_switch_camera);
//          FloatingActionButton fab_flash = findViewById(R.id.fab_flash);

// **********************************   Setup Camera   **********************************************
            bindCamera();

//***************************************   Shutter Button Action ****************************************

            picSnap.setOnClickListener(event -> {
                s3path = AWSMobileClient.getInstance().getUsername()+ "profilePic.png";
                profilePic = new File(Environment.getExternalStorageDirectory() + "/" + s3path);
//
//          New Thread
                Executor executor = Executors.newSingleThreadExecutor();

                imageCapture.takePicture(profilePic, executor, new ImageCapture.OnImageSavedListener() {
                    @Override
                    public void onError(
                            @NonNull ImageCapture.ImageCaptureError imageCaptureError, @NonNull String message, Throwable cause) {
//                                       TODO: insert your code here.
                    }

                    @Override
                    public void onImageSaved(@NonNull File file) {
                        profPicPath = file.getAbsolutePath();
                        Log.v(TAG, "onImageSaved: Saved");
                        String msg = "file saved at " + file.getAbsolutePath();
                        Log.i(TAG, "onImageSaved: "+msg);
                        goToPicturePreview(file.getAbsolutePath());

                    }
                });

            });


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
//            ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
//                    .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();
//            final ImageCapture imgCap = new ImageCapture(imageCaptureConfig);








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


//    ******************************* Method that sets up camera and preview settings ***************************************
    private void bindCamera(){
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
            public void onUpdated(@NonNull Preview.PreviewOutput previewOutput) {
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

//************************************    Upload to S3          **********************************************
    protected void uploadDataToS3( String picName, File profilePic){
        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(getApplicationContext())
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(new AmazonS3Client(AWSMobileClient.getInstance()))
                        .build();
        final TransferObserver uploadObserver =
                transferUtility.upload("public/" + picName  , profilePic);

        // Attach a listener to the observer to get state update and progress notifications
        uploadObserver.setTransferListener(new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    upload=false;
                    Log.i(TAG, "onStateChanged: Uploaded Profile Pic");
                    Toast.makeText(getBaseContext(), "Picture Save Complete",Toast.LENGTH_LONG).show();
                    String bucketPath ="https://" + uploadObserver.getBucket() + ".s3-us-west-2.amazonaws.com/" +uploadObserver.getKey();
                    Log.i(TAG, "onStateChanged: " + bucketPath + "*************************************************************************");
                }
            }
            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                int percentDone = (int) percentDonef;
                Log.d(TAG, "ID:" + id + " bytesCurrent: " + bytesCurrent
                        + " bytesTotal: " + bytesTotal + " " + percentDone + "%");
            }

            @Override
            public void onError(int id, Exception ex) {
                // Handle errors
            }
        });
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
