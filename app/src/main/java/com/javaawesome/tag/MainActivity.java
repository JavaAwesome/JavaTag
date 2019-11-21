package com.javaawesome.tag;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import com.amazonaws.amplify.generated.graphql.CreatePlayerMutation;
import com.amazonaws.amplify.generated.graphql.CreateSessionMutation;
import com.amazonaws.amplify.generated.graphql.ListPlayersQuery;
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
import com.google.android.gms.tasks.OnFailureListener;
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
    String playerId;
    LatLng currentUserLocation;
    LocationManager locationManager;
    AlertDialog alert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (this.checkSelfPermission(ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                this.checkSelfPermission(ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION}, 10);
        }

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
        Log.i(TAG, "onresume called");
        if (checkGpsStatus()) {
//            getCurrentUserLocation();
            checkIfPlayerAlreadyExistInLocalDatabase();
        } else {
            buildAlertMessageNoGps();
        }
        queryAllSessions();
    }

    // Create new game session and go to map page
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
                Intent goToMapIntent = new Intent(MainActivity.this, MapsActivity.class);
                goToMapIntent.putExtra("sessionId", sessionId);
                goToMapIntent.putExtra("userID", playerId);
                MainActivity.this.startActivity(goToMapIntent);
            }
            @Override
            public void onFailure(@Nonnull ApolloException e) {
                Log.e(TAG, "error in creating new game session" + e.getMessage());
            }
        });

    }

    //////// TEST BUTTON /////
    public void onTestyClick(View view) {
        startActivity(new Intent(MainActivity.this, NotificationActivity.class));
    }

    ///////////// Turn on Camera ///////////////////
    public void goToCameraClass(View view){
        Intent goToCamera = new Intent(this, ShowMeYourFace.class);
        this.startActivity(goToCamera);
    }

    /////////////

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
        goToMapIntent.putExtra("userID", playerId);
        this.startActivity(goToMapIntent);
    }

//    @Override
//    public void addPlayerToChosenGame(final ListSessionsQuery.Item session) {
////        Query
//        CreatePlayerInput playerInput = CreatePlayerInput.builder()
//                .playerSessionId(session.id())
//                .isIt(false)
//                .lat(currentUserLocation.latitude)
//                .lon(currentUserLocation.longitude)
//                .username(AWSMobileClient.getInstance().getUsername())
//                .build();
//        CreatePlayerMutation createPlayerMutation = CreatePlayerMutation.builder().input(playerInput).build();
//        awsAppSyncClient.mutate(createPlayerMutation).enqueue((new GraphQLCall.Callback<CreatePlayerMutation.Data>() {
//            @Override
//            public void onResponse(@Nonnull Response<CreatePlayerMutation.Data> response) {
//                String userID = response.data().createPlayer().id();
//                Log.i(TAG, "player mutation happened! ... inside of a session mutation");
//                Intent goToMapIntent = new Intent(MainActivity.this, MapsActivity.class);
//                goToMapIntent.putExtra("sessionId", session.id());
//                goToMapIntent.putExtra("userID", userID);
//                Log.i("veach", session.id() + "\n" +userID);
//            }
//            @Override
//            public void onFailure(@Nonnull ApolloException e) {
//                Log.i(TAG, "mutation of player failed, boohoo!");
//            }
//        }));
//    }

    // get all sessions
    private void queryAllSessions() {
        Log.i(TAG, "query all sessions");
        awsAppSyncClient.query(ListSessionsQuery.builder().build())
                .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                .enqueue(getAllSessionsCallBack);
    }

    // Callback to update the list of sessions and recycler view that displays them
    private GraphQLCall.Callback<ListSessionsQuery.Data> getAllSessionsCallBack = new GraphQLCall.Callback<ListSessionsQuery.Data>() {
        @Override
        public void onResponse(@Nonnull final Response<ListSessionsQuery.Data> response) {
            Handler h = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message inputMessage) {
                    sessions.clear();
                    sessions.addAll(response.data().listSessions().items());
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
        Log.i(TAG, "called getCurrentUserLocation");
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(final Location location) {
                Log.i(TAG, "" + location);
                if (location != null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            currentUserLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            Log.i(TAG, "playerId in getcurrentUserlocation " + playerId);
                            if (playerId == null) {
                                createPlayer();
                            }
                        }
                    }).run();
                }
            }
        }).addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, e.getMessage());
            }
        });
    }

    // TODO: Build onDestroy that deletes user data from DB


    private void checkIfPlayerAlreadyExistInLocalDatabase() {
        awsAppSyncClient.query(ListPlayersQuery.builder().build())
                .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                .enqueue(new GraphQLCall.Callback<ListPlayersQuery.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<ListPlayersQuery.Data> response) {
                        Log.i(TAG, "this is playerID " + playerId);
                        String playerName = AWSMobileClient.getInstance().getUsername();
                        List<ListPlayersQuery.Item> players = response.data().listPlayers().items();
                        for(ListPlayersQuery.Item player : players){
                            if(player.username().equals(playerName)){
                                Log.i(TAG, "Username match " + playerName + " " + player.id());
                                playerId = player.id();
                                getCurrentUserLocation();
                                return;
                            }
                        }
                        getCurrentUserLocation();
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Log.e(TAG, "error in checking if a player already exists in database");
                    }
                });
    }

    // Make a Player
    private void createPlayer() {
        CreatePlayerInput input = CreatePlayerInput.builder()
                .lat(currentUserLocation.latitude)
                .lon(currentUserLocation.longitude)
                .username(AWSMobileClient.getInstance().getUsername())
                .isIt(false)
                .build();
        CreatePlayerMutation createPlayerMutation = CreatePlayerMutation.builder().input(input).build();
        awsAppSyncClient.mutate(createPlayerMutation).enqueue(new GraphQLCall.Callback<CreatePlayerMutation.Data>() {
            @Override
            public void onResponse(@Nonnull Response<CreatePlayerMutation.Data> response) {
                Log.i(TAG, "created a player");
                playerId = response.data().createPlayer().id();
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                Log.e(TAG, "error in creating new player");
            }
        });
    }

    private boolean checkGpsStatus() {
        locationManager = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS is disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        alert = builder.create();
        alert.show();
    }
}