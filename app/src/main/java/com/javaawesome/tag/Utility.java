package com.javaawesome.tag;

public class Utility {

    // Equation is from https://stackoverflow.com/questions/639695/how-to-convert-latitude-or-longitude-to-meters
    // convert to two location points to distance between them in meters
    protected static double distanceBetweenLatLongPoints(double lat1, double long1, double lat2, double long2) {
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
}
