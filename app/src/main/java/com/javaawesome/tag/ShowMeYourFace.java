package com.javaawesome.tag;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
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
import android.widget.Toast;

import com.amazonaws.amplify.generated.graphql.UpdatePlayerMutation;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;

import type.UpdatePlayerInput;


public class ShowMeYourFace extends AppCompatActivity {
    // weird to have the tag reference a single person!
    private static final String TAG = "ahren:javatag";
    private static boolean upload = false;
    private ImageCapture imageCapture;
    final CameraX.LensFacing camera = CameraX.LensFacing.FRONT;

//    set to absolute path eventually
    String profPicPath = null;
//
    String s3path = null;
    AWSAppSyncClient mAWSAppSyncClient;
// created by picSnap
    File profilePic = null;

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
//            update photo in Dynamo
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
            // This check isn't being used correctly at all! If the user should see an explanation
            // of why the permission is necessary, you're instead silently logging and refusing to
            // let them either accept or deny the permission.
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
                Log.i(TAG, "onCreate: permission not granted");
                // You should cite where this comes from! https://developer.android.com/training/permissions/requesting
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1);
            }
        }else {

//************************************ Setup Buttons **********************************************
            FloatingActionButton picSnap = findViewById(R.id.picSnap);

// **********************************   Setup Camera   **********************************************
            bindCamera();

//***************************************   Shutter Button Action ****************************************

            picSnap.setOnClickListener(event -> {
//                put picture locally in the phone
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
                        // please, really, do something if there is an error.
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
            // PLEASE get rid of the zombie code!
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
                // All of these "your code here"-style comments make it clear that this code came
                // from an outside source, and that you didn't even read the code closely enough
                // to notice that these comments were weird.
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

//      Causes camera instance to only exist on this activity is started and destroyed on start and finish
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
                    String imgUrl = MainActivity.photoBucketPath + uploadObserver.getKey();
                    Log.i(TAG, "onStateChanged: " + imgUrl + "*************************************************************************");
                    updatePlayerInput(imgUrl);
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


    private void updatePlayerInput(String imgURL){
        Log.i(TAG, "created a player"+ getIntent().getStringExtra("playerId"));
        UpdatePlayerInput updatePlayerInput = UpdatePlayerInput.builder()
                .id(getIntent().getStringExtra("playerId"))
                .photo(imgURL)
                .build();

        UpdatePlayerMutation updatePlayerMutation = UpdatePlayerMutation.builder()
                .input(updatePlayerInput).build();
                mAWSAppSyncClient.mutate(updatePlayerMutation)
                .enqueue(new GraphQLCall.Callback<UpdatePlayerMutation.Data>() {
            @Override
            public void onResponse(@Nonnull Response<UpdatePlayerMutation.Data> response) {
                Log.i(TAG, "update success");

            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                Log.e(TAG, "update not successful");
            }
        });
    }

}
