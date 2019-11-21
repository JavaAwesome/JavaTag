package com.javaawesome.tag;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.amazonaws.amplify.generated.graphql.CreatePlayerMutation;
import com.amazonaws.amplify.generated.graphql.GetPlayerQuery;
import com.amazonaws.amplify.generated.graphql.GetSessionQuery;
import com.amazonaws.amplify.generated.graphql.OnCreatePlayerSubscription;
import com.amazonaws.amplify.generated.graphql.OnUpdatePlayerSubscription;
import com.amazonaws.amplify.generated.graphql.UpdatePlayerMutation;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.appsync.AppSyncSubscriptionCall;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import type.CreatePlayerInput;
import type.UpdatePlayerInput;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMyLocationClickListener {

    private GoogleMap mMap;
    AWSAppSyncClient awsAppSyncClient;
    GetSessionQuery.GetSession currentSession;

    LatLng startingPoint;
    final static long REFRESHRATE = 3*1000;
    final static int SUBJECT = 0;
    Handler locationHandler;
    LocationCallback mLocationCallback;
    private FusedLocationProviderClient mFusedLocationClient;
    final private int tagDistance = 50;
    List<Player> itPlayers;
    int itColor = Color.GREEN;
    int notItColor = Color.BLUE;
    BitmapDescriptor zombiepin;
    BitmapDescriptor playerpin;
    List<Player> players;
    private final String TAG = "javatag";
    String playerID;
    Player player;
    String sessionId;
    private AppSyncSubscriptionCall<OnUpdatePlayerSubscription.Data> subscriptionWatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // initialize connection with google location services
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // establish connection to AWS
        awsAppSyncClient = AWSAppSyncClient.builder()
                .context(getApplicationContext())
                .awsConfiguration(new AWSConfiguration(getApplicationContext()))
                .build();

        zombiepin = BitmapDescriptorFactory.fromResource(R.drawable.zombiepin);
        playerpin = BitmapDescriptorFactory.fromResource(R.drawable.playerpin);

        // getting extras
        playerID = getIntent().getStringExtra("userID");
        sessionId = getIntent().getStringExtra("sessionId");
        Log.i(TAG, "Session ID for map is: " + sessionId + "the player Id is " + playerID);

        queryForSelectedSession(sessionId);

        // Pull user ID from MainActivity
        // If player comes from the recyclerView it will come through as null so we will create a new player
        // Else the player created the game and we will query the player object
//        if (playerID == null) {
//            createPlayer();
//        } else {
//            queryForPlayerObject();
//        }

        //Stuff doesn't start running until the map is ready in onMapReady(Map stuff)
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Log.i(TAG, "Location call back results " + locationResult.toString());
                if (locationResult == null) {
                    Log.i(TAG, "location result is null");
                    return;
                }

                sendUserLocationQuery(locationResult);
                updateMarkerAndCircleForAllPlayers(players);

            }
        };
    }

    // ===== Send user info to DynamoDB ====
    private void sendUserLocationQuery(LocationResult locationResult) {
        Log.i(TAG, "player being sent " + (player == null ? "null" : player.toString()));
        UpdatePlayerInput updatePlayerInput = UpdatePlayerInput.builder()
                .id(playerID)
                .playerSessionId(sessionId)
                .lat(locationResult.getLastLocation().getLatitude())
                .lon(locationResult.getLastLocation().getLongitude())
                .isIt(player.getIt())
                .build();

        UpdatePlayerMutation updatePlayerMutation = UpdatePlayerMutation.builder()
                .input(updatePlayerInput).build();

        awsAppSyncClient.mutate(updatePlayerMutation)
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

    // ===== Subscribe to Data real-time ======
    // https://aws-amplify.github.io/docs/android/api

    private void subscribe() {

            OnUpdatePlayerSubscription subscription = OnUpdatePlayerSubscription.builder().build();
            subscriptionWatcher = awsAppSyncClient.subscribe(subscription);
            subscriptionWatcher.execute(subCallback);
        }

        private AppSyncSubscriptionCall.Callback<OnUpdatePlayerSubscription.Data> subCallback = new AppSyncSubscriptionCall.Callback<OnUpdatePlayerSubscription.Data>() {
            @Override
            public void onResponse(@Nonnull final Response<OnUpdatePlayerSubscription.Data> response) {
                Log.i(TAG, "************* !!!! *******" + response.data().toString());

                Handler h = new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage (Message inputMessage) {
                        // Iterate over the players on the map
                        //TODO: if session matches; then if player in players update location, else make a new player and add their marker.
                        OnUpdatePlayerSubscription.OnUpdatePlayer updatePlayer = response.data().onUpdatePlayer();

                        Log.i(TAG, "updated user is " + updatePlayer.toString() + " session id is " + sessionId);
                        //checking if the updated player is in this session
                        if(updatePlayer.session().id().equals(sessionId)){
                            boolean contains = false;

                            //checking if the updated player is in the current player list
                            for(Player player : players){

                                // if we have a match update players lat/long
                                if(updatePlayer.id().equals(player.getId())){
                                    List<LatLng> bananasList = new LinkedList<>();
                                    bananasList.add(new LatLng(response.data().onUpdatePlayer().lat(),
                                            response.data().onUpdatePlayer().lon()));
                                    player.setLocations(bananasList); // sets location for the player
                                    contains = true;
                                }
                            }

                            //if the player is in the session, but not in the player list, then make a new player and add them to the players list and add a marker
                            if(contains ==  false){
                                Marker marker = mMap.addMarker(new MarkerOptions()
                                        .position(player.getLastLocation())
                                        .title(player.getUsername()));
                                Circle circle = mMap.addCircle(new CircleOptions()
                                        .center(player.getLastLocation())
                                        .radius(tagDistance)
                                        .fillColor(Color.TRANSPARENT)
                                        .strokeWidth(3));

                                marker.setIcon(playerpin);
                                circle.setStrokeColor(notItColor);

                                Player newPlayer = new Player();
                                newPlayer.setId(updatePlayer.id());
                                newPlayer.setIt(false);
                                newPlayer.setMarker(marker);
                                newPlayer.setCircle(circle);
                                newPlayer.setUsername(updatePlayer.username());
                                List<LatLng> potatoes = new LinkedList<>();
                                potatoes.add(new LatLng(updatePlayer.lat(), updatePlayer.lon()));
                                newPlayer.setLocations(potatoes);

                                //adding player to the list of players in the game
                                players.add(newPlayer);
                            }
                        }
//                        for(Player player : players) {
//                            if(response.data().onUpdatePlayer().id().equals(player.getId())) {
//                                // if true (we have a match) update players lat/long
//                                List<LatLng> bananasList = new LinkedList<>();
//                                bananasList.add(new LatLng(response.data().onUpdatePlayer().lat(),
//                                        response.data().onUpdatePlayer().lon()));
//                                player.setLocations(bananasList); // sets location for the player
//                                //Might have been causing the starting point to move
////                                player.getCircle().setCenter(player.getLastLocation());
////                                player.getMarker().setPosition(player.getLastLocation());
//                            }
//                        }
                    }
                };
                h.obtainMessage().sendToTarget();
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                Log.e(TAG, e.toString());
            }

        @Override
        public void onCompleted() {
            Log.i(TAG, "Subscription completed ");
        }
    };

    // =============

    @Override
    protected void onStop() {
        super.onStop();
        stopTrackingLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        subscribe();
//        startLocationUpdates();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */


    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.i(TAG, "map is ready");
        mMap = googleMap;

        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);

        startLocationUpdates();
    }

    public void stopTrackingLocation() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    // Starts pulling location updates from the DB. 3 second delay to let the aws callbacks load first.
    private void startLocationUpdates() {
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mFusedLocationClient.requestLocationUpdates(getLocationRequest(), mLocationCallback, Looper.getMainLooper());
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Current Location:\n" + location, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "My Location button clicked", Toast.LENGTH_SHORT).show();
        return false;
    }

    // Called in startLocationUpdates to pull location updates from the DB
    private LocationRequest getLocationRequest() {
        Log.i(TAG, "getting location request");
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        return locationRequest;
    }

    // Creates markers and circles for each player in the list for that session
    private void initializeMarkersAndCirclesForPlayers(List<Player> players) {
        Log.i(TAG, "made it to initialized markers");
        for(Player player: players) {
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(player.getLastLocation())
                    .title(player.getUsername()));
            Circle circle = mMap.addCircle(new CircleOptions()
                    .center(player.getLastLocation())
                    .radius(tagDistance)
                    .fillColor(Color.TRANSPARENT)
                    .strokeWidth(3));

            // change color of marker depending on if player is it or not
            if (player.isIt()) {
                marker.setIcon(zombiepin);
                circle.setStrokeColor(itColor);
            } else {
                marker.setIcon(playerpin);
                circle.setStrokeColor(notItColor);
            }

            player.setCircle(circle);
            player.setMarker(marker);

        }
        //      Add a marker in center of game camera and move the camera
        mMap.moveCamera(CameraUpdateFactory.zoomTo(16));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(startingPoint));
        Circle gameBounds = mMap.addCircle(new CircleOptions()
                .center(startingPoint)
                .radius(currentSession.radius())
                .strokeColor(Color.BLUE)
                .fillColor(Color.TRANSPARENT)
                .strokeWidth(5));
    }


    private void updateMarkerAndCircleForAllPlayers(List<Player> players) {
        Log.i(TAG, "updating markers");
        Log.i(TAG, "How many players? " + players.size());

        if(itPlayers == null){
            itPlayers = new LinkedList<>();
            for(Player player : players) {
                if (player.isIt()) {
                    itPlayers.add(player);
                }
            }

            if (itPlayers.isEmpty()) {
                itPlayers.add(players.get(0));
            }
        }

        List<Player> playersJustGotTagged = new LinkedList<>();
        for (Player player : players) {
            player.getMarker().setPosition(player.getLastLocation());
            player.getCircle().setCenter(player.getLastLocation());
            if (checkForTag(player)) {
                Log.i(TAG, "In the updateMarkerAndCircleForAllPlayers");
                playersJustGotTagged.add(player);
                player.getMarker().setIcon(zombiepin);
                player.getCircle().setStrokeColor(itColor);


//                mMap.addCircle(player.getCircle());
            }
        }
        itPlayers.addAll(playersJustGotTagged);
    }

    // Equation is from https://stackoverflow.com/questions/639695/how-to-convert-latitude-or-longitude-to-meters
    // convert to two location points to distance between them in meters
    private double distanceBetweenLatLongPoints(double lat1, double long1, double lat2, double long2) {
        // radius of the Earth in km
        double R = 6378.137;
        double dLat = (lat2 * Math.PI / 180) - (lat1 * Math.PI / 180);
        double dLong = (long2 * Math.PI / 180) - (long1 * Math.PI / 180);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
                        Math.sin(dLong / 2) * Math.sin(dLong / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = R * c;
        return d * 1000;
    }

    // check if the player is tagged by the it player
    // check if the distance between the it player and the other player is less than the specified tag distance
    private boolean isTagged(Player player, Player itPlayer) {
        double distanceBetweenPlayers = distanceBetweenLatLongPoints(itPlayer.getLastLocation().latitude,
                itPlayer.getLastLocation().longitude,
                player.getLastLocation().latitude,
                player.getLastLocation().longitude);

        Log.i(TAG, "distance between players is " + distanceBetweenPlayers + " meters");

//        if (distanceBetweenPlayers < tagDistance) {
        if (distanceBetweenPlayers < tagDistance && itPlayer != player) {
            player.setIt(true);
            return true;
        } else {
            return false;
        }
    }

    private boolean checkForTag(Player player) {
        Log.i(TAG, "Made it into checkForTag");
        if (player.isIt()) {
            return false;
        }
        for(Player itPlayer : itPlayers) {
            Log.i(TAG, itPlayer.toString());
            if (isTagged(player, itPlayer)) {
//                Toast.makeText(this, "" + player.getUsername() + " is now it!!!", Toast.LENGTH_SHORT);
                //TODO: If future views added to app, may need to change "this"?

                //TODO: this activity traps the user, so disabled for now.
//                startActivity(new Intent(MapsActivity.this, NotificationActivity.class));
                return true;

            }
        }
        return false;
    }

    // query for the session associated with the sessionId that was passed from MainActivity
    private void queryForSelectedSession(String sessionId) {
        GetSessionQuery getSessionQuery = GetSessionQuery.builder().id(sessionId).build();
        awsAppSyncClient.query(getSessionQuery)
                .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                .enqueue(getSessionCallBack);
    }

    // Make a Player
    private void createPlayer() {
        Log.i(TAG, "Making a player with " + sessionId + " " + startingPoint.toString());
        CreatePlayerInput input = CreatePlayerInput.builder()
                .playerSessionId(sessionId)
                .lat(startingPoint.latitude)
                .lon(startingPoint.longitude)
                .username(AWSMobileClient.getInstance().getUsername())
                .isIt(false)
                .build();
        CreatePlayerMutation createPlayerMutation = CreatePlayerMutation.builder().input(input).build();

        awsAppSyncClient.mutate(createPlayerMutation).enqueue(new GraphQLCall.Callback<CreatePlayerMutation.Data>() {
            @Override
            public void onResponse(@Nonnull Response<CreatePlayerMutation.Data> response) {
                Log.i(TAG, "made it to creating a new player");
                playerID = response.data().createPlayer().id();
                player = new Player();
                player.setId(playerID);
                player.setIt(false);
                player.setUsername(AWSMobileClient.getInstance().getUsername());
                List<LatLng> bananas = new LinkedList<>();
                bananas.add(startingPoint);
                player.setLocations(bananas);
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                Log.e(TAG, "couldn't make a new player");
            }
        });
    }

    // Query for Player
    private void queryForPlayerObject() {
        GetPlayerQuery query = GetPlayerQuery.builder().id(playerID).build();
        awsAppSyncClient.query(query)
                .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                .enqueue(new GraphQLCall.Callback<GetPlayerQuery.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<GetPlayerQuery.Data> response) {
                        Log.i(TAG, "made it to making a query for player object");
                        // make a player instance
                        player = new Player(response.data().getPlayer());

                        // Making stuff on the map for the player
                        Handler markerHandler = new Handler(Looper.getMainLooper()){
                            @Override
                            public void handleMessage (Message inputMessage) {
                                Marker marker = mMap.addMarker(new MarkerOptions()
                                        .position(player.getLastLocation())
                                        .title(player.getUsername()));
                                Circle circle = mMap.addCircle(new CircleOptions()
                                        .center(player.getLastLocation())
                                        .radius(tagDistance)
                                        .fillColor(Color.TRANSPARENT)
                                        .strokeWidth(3));
                                // change color of marker depending on if player is it or not
                                if (player.isIt()) {
                                    marker.setIcon(zombiepin);
                                    circle.setStrokeColor(itColor);
                                } else {
                                    marker.setIcon(playerpin);
                                    circle.setStrokeColor(notItColor);
                                }

                                if(players == null){
                                    players = new LinkedList<>();
                                }

                                player.setCircle(circle);
                                player.setMarker(marker);
                                //adding player to the list of players in the game
                                players.add(player);
                            }
                        };
                        markerHandler.obtainMessage().sendToTarget();
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Log.e(TAG, "failed to get query for player object");
                    }
                });
    }

    // Callback to get current game session
    private GraphQLCall.Callback<GetSessionQuery.Data> getSessionCallBack = new GraphQLCall.Callback<GetSessionQuery.Data>() {
        @Override
        public void onResponse(@Nonnull final Response<GetSessionQuery.Data> response) {
            currentSession = response.data().getSession();
            Log.i(TAG, "Current session is "+ currentSession.toString());
            startingPoint = new LatLng(currentSession.lat(), currentSession.lon());
            Log.i(TAG, "Starting point is " + startingPoint);

            //once the session ID and starting loc are in place, then make the first player.
            if (playerID == null) {
                createPlayer();
            } else {
                queryForPlayerObject();
            }

            Log.i(TAG, "Made it to the after the if/else within getSessionCallBack");
            //converting from GetSessionItems to players
            players = playerConverter(currentSession.players().items());

            Handler h = new Handler(Looper.getMainLooper()){
                @Override
                public void handleMessage(Message inputMessage){
                    Log.i(TAG, "Made it to handleMessage");
                    //lat and long for the session

                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    initializeMarkersAndCirclesForPlayers(players);

                }
            };
            h.obtainMessage().sendToTarget();
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e(TAG, "error from getSessionQuery: " + e.getMessage());
        }
    };

    private List<Player> playerConverter(List<GetSessionQuery.Item> incomingList){
        List<Player> outGoingList = new LinkedList<>();
        for(GetSessionQuery.Item item : incomingList){
            Player newPlayer = new Player(item);
            outGoingList.add(newPlayer);
        }
        return outGoingList;
    };

    // TODO: Build onDestroy that deletes user data from DB

}
