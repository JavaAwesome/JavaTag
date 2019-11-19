package com.javaawesome.tag;

import com.amazonaws.amplify.generated.graphql.ListSessionsQuery;
import com.google.android.gms.maps.model.LatLng;

import java.util.LinkedList;
import java.util.List;

public class Session {
    private String id;
    private String title;
    private List<Player> players;
    private LatLng location;
    private int radius;

    public Session(String title, LatLng location, int radius) {
        this.title = title;
        this.players = new LinkedList<>();
        this.location = location;
        this.radius = radius;
    }

    public Session(ListSessionsQuery.Item item) {
        this.id = item.id();
        this.title = item.title();
        this.players = new LinkedList<>();
    }

    public void addPlayer(Player player) {
        this.players.add(player);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public int getTotalPlayers() { return players.size(); }

    public LatLng getLocation() {
        return location;
    }

    public int getRadius() {
        return radius;
    }
}
