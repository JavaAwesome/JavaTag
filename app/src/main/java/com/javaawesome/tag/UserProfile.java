package com.javaawesome.tag;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.amazonaws.amplify.generated.graphql.GetPlayerQuery;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.squareup.picasso.Picasso;

import javax.annotation.Nonnull;

public class UserProfile extends AppCompatActivity {
    AWSAppSyncClient awsAppSyncClient;
    String userPhoto;
    String TAG = "ahren:UserProfile";
    ImageView profPic;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // establish connection to AWS
        awsAppSyncClient = AWSAppSyncClient.builder()
                .context(getApplicationContext())
                .awsConfiguration(new AWSConfiguration(getApplicationContext()))
                .build();

        queryForPlayerObject(getIntent().getStringExtra("playerId"));


        profPic = findViewById(R.id.profilePicture);
        Log.i(TAG, "onCreate: "+userPhoto);




        TextView username = findViewById(R.id.UserName);
        username.setText(AWSMobileClient.getInstance().getUsername());
    }
    ///////////// Turn on Camera ///////////////////
    public void goToCameraClass(View view){
        Intent goToCamera = new Intent(this, ShowMeYourFace.class);
        this.startActivity(goToCamera.putExtra("playerId",getIntent().getStringExtra("playerId")));
    }
    private void queryForPlayerObject(String playerId) {
        GetPlayerQuery query = GetPlayerQuery.builder().id(playerId).build();
        awsAppSyncClient.query(query)
                .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                .enqueue(new GraphQLCall.Callback<GetPlayerQuery.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<GetPlayerQuery.Data> response) {
                        Log.i(TAG, "made it to making a query for player object");
                        userPhoto = response.data().getPlayer().Photo();

                        Handler h = new Handler(Looper.getMainLooper()){
                            @Override
                            public void handleMessage (Message inputMessage) {
                                Log.i(TAG, "handleMessage: getting photo***********************************************************************");
                                Picasso.get().load(userPhoto).into(profPic);
                                Log.i(TAG, "handleMessage: "+userPhoto);
                            }
                        };
                        h.obtainMessage().sendToTarget();
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {

                    }
                });
    }
}

