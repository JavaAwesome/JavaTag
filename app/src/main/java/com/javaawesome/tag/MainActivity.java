package com.javaawesome.tag;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
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
import com.amazonaws.mobileconnectors.s3.transferutility.TransferService;
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
    protected static String photoBucketPath = "https://javatag091c7e33ab0441e4bdf34cbdf68d2bd1-local.s3-us-west-2.amazonaws.com/";
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
    private final int distanceForNearbySessions = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Transfer service registers a listener with Android OS for network connectivity changes
        //If the network goes offline, the active transfers are paused
        //Active transfers resume when the nextwork comes back online
        //Works in the foreground and the background
        getApplicationContext().startService(new Intent(getApplicationContext(), TransferService.class));

        //If the app does not have course or fine location permission request them
        if (this.checkSelfPermission(ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                this.checkSelfPermission(ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION}, 10);
        }

        //Initialize AWS mobile client and check if the player is logged in
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

        //Connect to AWS
        awsAppSyncClient = AWSAppSyncClient.builder()
                .context(getApplicationContext())
                .awsConfiguration(new AWSConfiguration(getApplicationContext()))
                .build();

        //Initialize client for google location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //Initialize a new Linked List of game sessions
        sessions = new LinkedList<>();

        //Initialize a recycler view to display game sessions within the defined distance of the player's location
        recyclerNearbySessions = findViewById(R.id.recycler_nearby_sessions);
        recyclerNearbySessions.setLayoutManager(new LinearLayoutManager(this));
        this.sessionAdapter = new SessionAdapter(this.sessions, this);
        recyclerNearbySessions.setAdapter(this.sessionAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //If checkGpsStatus returns true to verify that the player's phone has GPS is turned on
        if (checkGpsStatus()) {
            //Check if the player is already in the database
            checkIfPlayerAlreadyExistInDatabase();
        } else {
            //Ask the player if they want to turn on their GPS to play
            buildAlertMessageNoGps();
        }
    }

    //Create a new game session and transfer the player to the games' map page
    public void goToMap(View view) {
        // TODO: check if player already exist in the database
        EditText sessionName = findViewById(R.id.editText_session_name);
        if(sessionName.getText().length()>0) {
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
                    Log.e(TAG, "Error in creating new game session" + e.getMessage());
                }
            });

        //Send prompt to player that a session name is required before creating the new game
        }else{
            Toast.makeText(getBaseContext(), "Please enter a session title.",Toast.LENGTH_LONG).show();
        }
    }

    //Go to user page when the zombie icon is clicked
    public void goToUserPage(View view){
        Intent goToUserPage = new Intent(this, UserProfile.class);
        this.startActivity(goToUserPage.putExtra("playerId",playerId));
    }

    //Show the signin page if the player is not signed in when they open the app
    private void signInUser() {
        AWSMobileClient.getInstance().showSignIn(MainActivity.this,
                //Customize the built-in signin page with the defined theme color and icon
                SignInUIOptions.builder().backgroundColor(16763080).logo(R.mipmap.ic_launcher_round).build(),
                new Callback<UserStateDetails>() {
                    @Override
                    public void onResult(UserStateDetails result) {
                        Log.i(TAG, "Successfully show signed in page");
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                });
    }

    //Sign out the player and show them the signin page
    public void signoutCurrentUser(View view) {
        AWSMobileClient.getInstance().signOut();
        signInUser();
    }

    //onclick method for player to join an existing game session
    @Override
    public void joinExistingGameSession(ListSessionsQuery.Item session) {
        Intent goToMapIntent = new Intent(this, MapsActivity.class);
        goToMapIntent.putExtra("sessionId", session.id());
        goToMapIntent.putExtra("userID", playerId);
        this.startActivity(goToMapIntent);
    }

    //Get all sessions from the database
    private void queryAllSessions() {
        awsAppSyncClient.query(ListSessionsQuery.builder().build())
                .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                .enqueue(getAllSessionsCallBack);
    }

    //Callback to update the list of sessions and recycler view that displays them
    private GraphQLCall.Callback<ListSessionsQuery.Data> getAllSessionsCallBack = new GraphQLCall.Callback<ListSessionsQuery.Data>() {
        @Override
        public void onResponse(@Nonnull final Response<ListSessionsQuery.Data> response) {
            Handler h = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message inputMessage) {
                    sessions.clear();
                    List<ListSessionsQuery.Item> filteredSessions = filterSessionsBasedOnDistance(response.data().listSessions().items());
                    sessions.addAll(filteredSessions);
                    sessionAdapter.notifyDataSetChanged();
                }
            };
            h.obtainMessage().sendToTarget();
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {

        }
    };

    //Get current player location
    private void getCurrentUserLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(final Location location) {
                Log.i(TAG, "" + location);
                if (location != null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            currentUserLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            queryAllSessions();
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

    //Check if the player's username is already in the database
    private void checkIfPlayerAlreadyExistInDatabase() {
        awsAppSyncClient.query(ListPlayersQuery.builder().build())
                .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                .enqueue(new GraphQLCall.Callback<ListPlayersQuery.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<ListPlayersQuery.Data> response) {
                        String playerName = AWSMobileClient.getInstance().getUsername();
                        List<ListPlayersQuery.Item> players = response.data().listPlayers().items();
                        for(ListPlayersQuery.Item player : players){
                            if(player.username().equals(playerName)){
                                playerId = player.id();
                                getCurrentUserLocation();
                                return;
                            }
                        }
                        getCurrentUserLocation();
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Log.e(TAG, "Error in checking if a player already exists in database");
                    }
                });
    }

    //Make a new player using their location, username, and start them as not it (human)
    private void createPlayer() {
        CreatePlayerInput input = CreatePlayerInput.builder()
                .lat(currentUserLocation.latitude)
                .lon(currentUserLocation.longitude)
                .username(AWSMobileClient.getInstance().getUsername())
                .isIt(false)
                .photo(MainActivity.photoBucketPath + "avatar.png")
                .build();
        CreatePlayerMutation createPlayerMutation = CreatePlayerMutation.builder().input(input).build();
        awsAppSyncClient.mutate(createPlayerMutation).enqueue(new GraphQLCall.Callback<CreatePlayerMutation.Data>() {
            @Override
            public void onResponse(@Nonnull Response<CreatePlayerMutation.Data> response) {
                playerId = response.data().createPlayer().id();

            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                Log.e(TAG, "Error in creating new player");
            }
        });
    }

    //Checks if GPS Location is turned on or not on the user's phone
    private boolean checkGpsStatus() {
        locationManager = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    //Prompt the player that their GPS is not enabled and to turn it on to play
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

    //Filter the sessions fed to the recyclerView so the player only sees games within a defined distance from their location
    private List<ListSessionsQuery.Item> filterSessionsBasedOnDistance(List<ListSessionsQuery.Item> allSessions) {
        List<ListSessionsQuery.Item> filteredSessions = new LinkedList<>();
        for (ListSessionsQuery.Item session : allSessions) {
            double distanceBetweenSessionAndPlayer = Utility.distanceBetweenLatLongPoints(currentUserLocation.latitude,
                    currentUserLocation.longitude,
                    session.lat(),
                    session.lon());
            if (distanceBetweenSessionAndPlayer < distanceForNearbySessions) {
                filteredSessions.add(session);
            }
        }
        return filteredSessions;
    }

    //TODO: Build onDestroy that deletes user data from DB
    //TODO: Need to remove the player marker when they close the app so they don't keep showing up in the game session after they think they've left
    //TODO: Add subscription to recyclerView
}