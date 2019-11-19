package com.javaawesome.tag;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.amazonaws.amplify.generated.graphql.GetSessionQuery;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMyLocationClickListener {

    private GoogleMap mMap;
    AWSAppSyncClient awsAppSyncClient;
    GetSessionQuery.GetSession currentSession;

    LatLng startingPoint;
    Session gameSession;
    final static long REFRESHRATE = 3*1000;
    final static int SUBJECT = 0;
    Handler locationHandler;
    private int index = 0;
    LocationCallback mLocationCallback;
    private FusedLocationProviderClient mFusedLocationClient;
    final private int tagDistance = 50;
    Player itPlayer;
    int itColor = Color.RED;
    int notItColor = Color.GREEN;
    float itHue = BitmapDescriptorFactory.HUE_RED;
    float notItHue = BitmapDescriptorFactory.HUE_GREEN;
    List<Marker> playerMarkers;
    List<Circle> playerCircles;
    private final String TAG = "thequangnguyen";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // asks users for permissions
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

        // initialize connection with google location services
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // establish connection to AWS
        awsAppSyncClient = AWSAppSyncClient.builder()
                .context(getApplicationContext())
                .awsConfiguration(new AWSConfiguration(getApplicationContext()))
                .build();

        // get the session that user selected from mainactivity
        String sessionId = getIntent().getStringExtra("sessionId");
        queryForSelectedSession(sessionId);

        playerMarkers = new LinkedList<>();
        playerCircles = new LinkedList<>();

        startingPoint = new LatLng(47.653120, -122.351991);
        gameSession = new Session("testing", startingPoint, 200);
        Player me = new Player("Qyoung", gameSession);
        Player picolas = new Player("Picolas", gameSession);

        gameSession.addPlayer(me);
        gameSession.addPlayer(picolas);
        me.addLocations(new LatLng(47.653200, -122.352200));
        picolas.addLocations(new LatLng(47.652300, -122.353100));
        me.addLocations(new LatLng(47.653100, -122.352300));
        picolas.addLocations(new LatLng(47.652400, -122.353000));
        me.addLocations(new LatLng(47.653000, -122.352400));
        picolas.addLocations(new LatLng(47.652500, -122.352900));
        me.addLocations(new LatLng(47.652900, -122.352500));
        picolas.addLocations(new LatLng(47.652600, -122.352800));
        me.addLocations(new LatLng(47.652800, -122.352600));
        picolas.addLocations(new LatLng(47.652700, -122.352700));

        itPlayer = picolas;
        picolas.setIt(true);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                updateMarkerAndCircleForAllPlayers(gameSession.getPlayers());
            }
        };

//        mFusedLocationClient.requestLocationUpdates(getLocationRequest(), mLocationCallback, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTrackingLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        mMap = googleMap;

        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);

//      Add a marker in center of game camera and move the camera
//        mMap.addMarker(new MarkerOptions().position(startingPoint).title("Game Center").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(16));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(startingPoint));
        Circle gameBounds = mMap.addCircle(new CircleOptions()
                .center(startingPoint)
                .radius(gameSession.getRadius())
                .strokeColor(Color.YELLOW)
                .fillColor(Color.TRANSPARENT)
                .strokeWidth(5));

        initializeMarkersAndCirclesForPlayers(gameSession.getPlayers());
        startLocationUpdates();
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

    private LocationRequest getLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        return locationRequest;
    }

    public void stopTrackingLocation() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    private void startLocationUpdates() {
        mFusedLocationClient.requestLocationUpdates(getLocationRequest(), mLocationCallback, null);
        mMap.setMyLocationEnabled(true);
    }

    private void initializeMarkersAndCirclesForPlayers(List<Player> players) {
        for(Player player: players) {
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(player.getLocations().get(index))
                    .title(player.getUsername()));
            Circle circle = mMap.addCircle(new CircleOptions()
                    .center(player.getLocations().get(index))
                    .radius(tagDistance)
                    .fillColor(Color.TRANSPARENT)
                    .strokeWidth(3));

            // change color of marker depending on if player is it or not
            if (player.isIt()) {
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(itHue));
                circle.setStrokeColor(itColor);
            } else {
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(notItHue));
                circle.setStrokeColor(notItColor);
            }

            playerMarkers.add(marker);
            playerCircles.add(circle);

//            playerMarkers.add(mMap.addMarker(new MarkerOptions()
//                    .position(player.getLocations().get(index))
//                    .title(player.getUsername()).icon(BitmapDescriptorFactory.defaultMarker(notItHue))));
//            playerCircles.add(mMap.addCircle(new CircleOptions()
//                    .center(player.getLocations().get(index))
//                    .radius(tagDistance)
//                    .strokeColor(notItColor)
//                    .fillColor(Color.TRANSPARENT)
//                    .strokeWidth(3)));
        }
    }


    private void updateMarkerAndCircleForAllPlayers(List<Player> players) {
        index++;
        for (int i = 0; i < players.size(); i++) {
            playerMarkers.get(i).setPosition(players.get(i).getLocations().get(index));
            playerCircles.get(i).setCenter(players.get(i).getLocations().get(index));
            checkForTag();
            if (players.get(i).isIt()) {
                playerMarkers.get(i).setIcon(BitmapDescriptorFactory.defaultMarker(itHue));
                playerCircles.get(i).setStrokeColor(itColor);
            } else {
                playerMarkers.get(i).setIcon(BitmapDescriptorFactory.defaultMarker(notItHue));
                playerCircles.get(i).setStrokeColor(notItColor);
            }

        }
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
    private boolean isTagged(Player player) {
        double distanceBetweenPlayers = distanceBetweenLatLongPoints(itPlayer.getLocations().get(index).latitude,
                itPlayer.getLocations().get(index).longitude,
                player.getLocations().get(index).latitude,
                player.getLocations().get(index).longitude);

        Log.i(TAG, "distance between players is " + distanceBetweenPlayers + " meters");

        if (distanceBetweenPlayers < tagDistance) {
            player.setIt(true);
            itPlayer.setIt(false);
            itPlayer = player;
            return true;
        } else {
            return false;
        }
    }

    private void checkForTag() {
        for(Player player: gameSession.getPlayers()) {
            if (player == itPlayer) {
                continue;
            }
            if (isTagged(player)) {
                Toast.makeText(this, "" + player.getUsername() + " is now it!!!", Toast.LENGTH_SHORT);
                return;
            }
        }
    }

    // query for the session associated with the sessionId that was passed from MainActivity
    private void queryForSelectedSession(String sessionId) {
        GetSessionQuery getSessionQuery =GetSessionQuery.builder().id(sessionId).build();
        awsAppSyncClient.query(getSessionQuery)
                .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                .enqueue(getSessionCallBack);
    }

    // Callback to get current game session
    private GraphQLCall.Callback<GetSessionQuery.Data> getSessionCallBack = new GraphQLCall.Callback<GetSessionQuery.Data>() {
        @Override
        public void onResponse(@Nonnull Response<GetSessionQuery.Data> response) {
            currentSession = response.data().getSession();
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e(TAG, "error from getSessionQuery: " + e.getMessage());
        }
    };
}
