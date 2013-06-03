package com.littleinc.MessageMe.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.coredroid.util.LogIt;
import com.google.gson.Gson;

public class ITunesService {

    public static final String SEARCH_BY_TERM = "http://itunes.apple.com/search?entity=song&term=%s&limit=%s";

    /**
     * Returns the "howMany" top songs in iTunes from "start" position (paging)
     * 
     * @return
     */
    public List<ItunesMedia> getTopSongs(int howMany) {
        List<ItunesMedia> songs = new ArrayList<ItunesMedia>();

        return songs;
    }

    /**
     * Returns the top 10 songs
     * 
     * @return
     */
    public List<ItunesMedia> getTop10Songs() {
        return getTopSongs(10);
    }

    /**
     * Returns tne top 4 songs. Used on "Discovery View"
     * 
     * @return
     */
    public List<ItunesMedia> getTop4Songs() {
        return getTopSongs(4);
    }

    public List<ItunesMedia> searchMusic(String term, int limit)
            throws ItunesServiceException {

        String urlFormat = SEARCH_BY_TERM;

        try {
            String url = String.format(urlFormat,
                    URLEncoder.encode(term, "utf-8"), limit);
            String content = getData(url);
            Gson gson = new Gson();
            ITunesSearchResult result = gson.fromJson(content,
                    ITunesSearchResult.class);
            return result.getResultList();
        } catch (ClientProtocolException e) {
            LogIt.e(this, e, e.getMessage());
            throw new ItunesServiceException(e);
        } catch (IOException e) {
            LogIt.e(this, e, e.getMessage());
            throw new ItunesServiceException(e);
        }
    }

    private String getData(String url) throws ClientProtocolException,
            IOException {
        HttpClient client = new DefaultHttpClient();
        String result = null;
        HttpGet request = new HttpGet(url);

        HttpResponse response = client.execute(request);

        int responseCode = response.getStatusLine().getStatusCode();
        if (responseCode == 200) {
            HttpEntity entity = response.getEntity();
            InputStream in = entity.getContent();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            result = builder.toString();
        }
        return result;
    }
}
