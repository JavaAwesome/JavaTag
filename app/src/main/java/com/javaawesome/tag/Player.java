package com.javaawesome.tag;

import com.amazonaws.amplify.generated.graphql.GetSessionQuery;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.LinkedList;
import java.util.List;

public class Player {
    private String id;
    private String username;
    private Session gameSession;
    private boolean isIt;
    private List<LatLng> locations;
    private Marker marker;
    private Circle circle;

    public Player(String username, Session gameSession) {
        this.username = username;
        this.gameSession = gameSession;
        this.isIt = false;
        this.locations = new LinkedList<>();
    }

    public Player(GetSessionQuery.Item item){
        this.id = item.id();
        this.username = item.username();
        this.gameSession = null;
        this.isIt = item.isIt();
        locations = new LinkedList<>();
        locations.add(new LatLng(item.lat(), item.lon()));
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Session getGameSession() {
        return gameSession;
    }

    public boolean isIt() {
        return isIt;
    }

    public List<LatLng> getLocations() {
        return locations;
    }

    public LatLng getLastLocation() { return locations.get(locations.size() -1); }

    public void setIt(boolean it) {
        isIt = it;
    }

    public void addLocations(LatLng location) {
        this.locations.add(location);
    }

    public Marker getMarker() {
        return marker;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }

    public Circle getCircle() {
        return circle;
    }

    public void setCircle(Circle circle) {
        this.circle = circle;
    }

    @Override
    public String toString() {
        return "Player{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", gameSession=" + gameSession +
                ", isIt=" + isIt +
                ", locations=" + locations +
                ", marker=" + marker +
                ", circle=" + circle +
                '}';
    }
}

