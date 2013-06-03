package com.littleinc.MessageMe.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import com.coredroid.util.LogIt;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class FacebookGraph {
    
    public static final String DISTANCE = "1000";
    
    public static final String GRAPH_API_URL = "https://graph.facebook.com/";
    
    public static final String GENERAL_PLACES_QUERY = "search?type=place&access_token=%s&center=%s&limit=%s&method=GET&format=json&distance=" + DISTANCE;
    
    public static final String SPECIFIC_PLACES_QUERY = "search?q=%s&type=place&access_token=%s&center=%s&method=GET&format=json&distance=" + DISTANCE;

    private String accessToken;

    public FacebookGraph(String accesToken) {
        this.accessToken = accesToken;
    }

    /**
     * Search for all nearby places given a specific location
     */
    public FacebookData<FacebookPlace> getPlaces(double latitude,
            double longitude, int limit) {
        
        String graphUrl = GRAPH_API_URL + GENERAL_PLACES_QUERY;

        String center = String.format("%s,%s", latitude, longitude);
        String urlPath = String.format(graphUrl, accessToken, center,
                limit);

        return getPlaces(urlPath);
    }
    
    /**
     * Search for all the matching places according the placeName for a 
     * specific location
     */
    public FacebookData<FacebookPlace> getPlaces(String placeName, double latitude,
            double longitude) {
        
        String graphUrl = GRAPH_API_URL + SPECIFIC_PLACES_QUERY;

        String name = String.format("%s", placeName);
        String center = String.format("%s,%s", latitude, longitude);
        String urlPath = String.format(graphUrl, name, accessToken, center
                );

        return getPlaces(urlPath);
        
    }

    public FacebookData<FacebookPlace> getPlaces(String urlPath) {
        try {
            URL url = new URL(urlPath);
            URLConnection conn = url.openConnection();
            InputStream response = conn.getInputStream();

            InputStreamReader ir = new InputStreamReader(response);
            Gson gson = new Gson();
            Type dataType = new TypeToken<FacebookData<FacebookPlace>>() {
            }.getType();
            FacebookData<FacebookPlace> data = gson.fromJson(ir, dataType);
            return data;
        } catch (MalformedURLException e) {
            LogIt.e(this, e, e.getMessage());
        } catch (IOException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return null;
    }
}