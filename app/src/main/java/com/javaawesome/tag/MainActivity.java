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
    // final is a good idea for a variable like this that you don't ever want to accidentally change
    protected static final String photoBucketPath = "https://javatag091c7e33ab0441e4bdf34cbdf68d2bd1-local.s3-us-west-2.amazonaws.com/";
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

        getApplicationContext().startService(new Intent(getApplicationContext(), TransferService.class));

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
        // I think this was completed with the call to filter--so this is an out of date comment.
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
            checkIfPlayerAlreadyExistInDatabase();
        } else {
            buildAlertMessageNoGps();
        }
    }

    // Create new game session and go to map page
    // Called when the new session button is tapped, so it should probably have a clearer name!
    public void createAndGoToNewSession(View view) {
        // TODO: check if player already exist in the database
        // didn't we check if the player existed in onResume? Why do we need to call it again now?
        // Or is this just a leftover todo?
        EditText sessionName = findViewById(R.id.editText_session_name);
        Log.i(TAG, "createAndGoToNewSession: "+sessionName.getText());
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
                    joinExistingGameSession(sessionId);
                }

                @Override
                public void onFailure(@Nonnull ApolloException e) {
                    Log.e(TAG, "error in creating new game session" + e.getMessage());
                }
            });
        }else{
            Toast.makeText(getBaseContext(), "Please enter a session title.",Toast.LENGTH_LONG).show();
        }
    }

    ///////////// Go to user page ///////////////////
    public void goToUserPage(View view){
        Intent goToUserPage = new Intent(this, UserProfile.class);
        this.startActivity(goToUserPage.putExtra("playerId",playerId));
    }
    // dead code 💀

    // Direct users to sign in page
    private void signInUser() {
        AWSMobileClient.getInstance().showSignIn(MainActivity.this,
                // customize the built in sign in page
                SignInUIOptions.builder().backgroundColor(16763080).logo(R.mipmap.ic_launcher_round).build(),
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
    // If you make this a bit more generic, it'll work for new sessions and existing ones!
    @Override
    public void joinExistingGameSession(String sessionId) {
        Intent goToMapIntent = new Intent(this, MapsActivity.class);
        goToMapIntent.putExtra("sessionId", sessionId);
        goToMapIntent.putExtra("userID", playerId);
        this.startActivity(goToMapIntent);
    }

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
                            queryAllSessions();
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


    private void checkIfPlayerAlreadyExistInDatabase() {
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
                .photo(MainActivity.photoBucketPath + "avatar.png")
                .build();
        CreatePlayerMutation createPlayerMutation = CreatePlayerMutation.builder().input(input).build();
        awsAppSyncClient.mutate(createPlayerMutation).enqueue(new GraphQLCall.Callback<CreatePlayerMutation.Data>() {
            @Override
            public void onResponse(@Nonnull Response<CreatePlayerMutation.Data> response) {

                playerId = response.data().createPlayer().id();
                Log.i(TAG, "created a player"+ playerId);

            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                Log.e(TAG, "error in creating new player");
            }
        });
    }

    // Checks if Gps Location is turned on or not on the user's phone
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

    // filter out list of sessions to a smaller list that consist of sessions nearby the player's location
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
}