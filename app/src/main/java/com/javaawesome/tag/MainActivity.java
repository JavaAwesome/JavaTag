package com.javaawesome.tag;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import com.amazonaws.amplify.generated.graphql.CreatePlayerMutation;
import com.amazonaws.amplify.generated.graphql.CreateSessionMutation;
import com.amazonaws.amplify.generated.graphql.ListSessionsQuery;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.SignInUIOptions;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import type.CreatePlayerInput;
import type.CreateSessionInput;
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MainActivity extends AppCompatActivity implements SessionAdapter.OnSessionInteractionListener {

    private final String TAG = "javatag";
    RecyclerView recyclerNearbySessions;
    SessionAdapter sessionAdapter;
    List<ListSessionsQuery.Item> sessions;
    AWSAppSyncClient awsAppSyncClient;
    FusedLocationProviderClient fusedLocationClient;
    String sessionId;
    LatLng currentUserLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION}, 10);

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

        // connect to AWS
        awsAppSyncClient = AWSAppSyncClient.builder()
                .context(getApplicationContext())
                .awsConfiguration(new AWSConfiguration(getApplicationContext()))
                .build();

        // initialize client for google location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        sessions = new LinkedList<>();

        // initialize recycler view to display nearby game sessions
        // TODO: have recycler view filter sessions by distance to user
        recyclerNearbySessions = findViewById(R.id.recycler_nearby_sessions);
        recyclerNearbySessions.setLayoutManager(new LinearLayoutManager(this));
        this.sessionAdapter = new SessionAdapter(this.sessions, this);
        recyclerNearbySessions.setAdapter(this.sessionAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getCurrentUserLocation();
        queryAllSessions();
    }

    //
    public void goToMap(View view) {
        // TODO: check if player already exist in the database
        EditText sessionName = findViewById(R.id.editText_session_name);
        CreateSessionInput input = CreateSessionInput.builder()
                .title(sessionName.getText().toString())
                .lat(currentUserLocation.latitude)
                .lon(currentUserLocation.longitude)
                .radius(500)
                .build();
        CreateSessionMutation createSessionMutation = CreateSessionMutation.builder().input(input).build();
        awsAppSyncClient.mutate(createSessionMutation).enqueue(new GraphQLCall.Callback<CreateSessionMutation.Data>() {
            @Override
            public void onResponse(@Nonnull Response<CreateSessionMutation.Data> response) {
                sessionId = response.data().createSession().id();

                CreatePlayerInput playerInput = CreatePlayerInput.builder()
                        .playerSessionId(response.data().createSession().id())
                        .isIt(false)
                        .lat(currentUserLocation.latitude)
                        .lon(currentUserLocation.longitude)
                        .username(AWSMobileClient.getInstance().getUsername())
                        .build();
                CreatePlayerMutation createPlayerMutation = CreatePlayerMutation.builder().input(playerInput).build();
                awsAppSyncClient.mutate(createPlayerMutation).enqueue((new GraphQLCall.Callback<CreatePlayerMutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<CreatePlayerMutation.Data> response) {
                        String userID = response.data().createPlayer().id();
                        Log.i(TAG, "player mutation happened! ... inside of a session mutation");
                        Intent goToMapIntent = new Intent(MainActivity.this, MapsActivity.class);
                        goToMapIntent.putExtra("sessionId", sessionId);
                        goToMapIntent.putExtra("userID", userID);
                        MainActivity.this.startActivity(goToMapIntent);
                    }
                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Log.i(TAG, "mutation of player failed, boohoo!");
                    }
                }));
            }
            @Override
            public void onFailure(@Nonnull ApolloException e) {
            }
        });

    }

    // Direct users to sign in page
    private void signInUser() {
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

    // onclick method for button to join existing game sessions
    @Override
    public void joinExistingGameSession(ListSessionsQuery.Item session) {
        Intent goToMapIntent = new Intent(this, MapsActivity.class);
        goToMapIntent.putExtra("sessionId", session.id());
        this.startActivity(goToMapIntent);
    }

    // get all sessions
    private void queryAllSessions() {
        awsAppSyncClient.query(ListSessionsQuery.builder().build())
                .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                .enqueue(getAllSessionsCallBack);
    }

    // Callback to update the list of sessions and recycler view that displays them
    private GraphQLCall.Callback<ListSessionsQuery.Data> getAllSessionsCallBack = new GraphQLCall.Callback<ListSessionsQuery.Data>() {
        @Override
        public void onResponse(@Nonnull final Response<ListSessionsQuery.Data> response) {
            Log.i(TAG, "got sessions data back from dynamodb");
            Handler h = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message inputMessage) {
                    sessions.clear();
                    sessions.addAll(response.data().listSessions().items());
                    Log.i(TAG, sessions.toString());
                    sessionAdapter.notifyDataSetChanged();
                }
            };
            h.obtainMessage().sendToTarget();
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {

        }
    };

    // get current user location
    private void getCurrentUserLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(final Location location) {
                Log.i(TAG, "this is location " + location.toString());
                if (location != null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            currentUserLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        }
                    }).run();

                }
            }
        });
    }

    // TODO: Build onDestroy that deletes user data from DB
}
