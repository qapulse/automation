package com.littleinc.MessageMe.util;

public class MapBoxStaticMapUtil {
    public static final String MAP_URL = "http://api.tiles.mapbox.com/v3/littleinc.map-l1tjx849/pin-l+E01B6A(%s,%s)/%s,%s,16/%sx%s.png";

    public static String getMapUrl(double latitude, double longitude,
            int height, int width) {
        return String.format(MAP_URL, longitude, latitude, longitude, latitude,
                width, height);
    }

}
