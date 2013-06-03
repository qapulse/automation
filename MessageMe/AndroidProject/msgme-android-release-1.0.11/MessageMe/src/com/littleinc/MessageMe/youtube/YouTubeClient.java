package com.littleinc.MessageMe.youtube;

import java.io.IOException;

import com.google.api.client.googleapis.GoogleHeaders;
import com.google.api.client.googleapis.json.JsonCParser;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;

public class YouTubeClient {

    JsonFactory jsonFactory = new GsonFactory();

    private final HttpTransport transport = new NetHttpTransport();

    private final HttpRequestFactory requestFactory;
    
    public YouTubeClient() {
        final JsonCParser parser = new JsonCParser(jsonFactory);
        requestFactory = transport.createRequestFactory(new HttpRequestInitializer() {

          @Override
          public void initialize(HttpRequest request) {
            // headers
            GoogleHeaders headers = new GoogleHeaders();
            headers.setApplicationName("MessageMe");
            headers.setGDataVersion("2");
            request.setHeaders(headers);
            request.setParser(parser);
          }
        });
      }

    public VideoFeed executeGetVideoFeed(YouTubeUrl url) throws IOException {
        return executeGetFeed(url, VideoFeed.class);
      }

      private <F extends Feed<? extends YouTubeItem>> F executeGetFeed(YouTubeUrl url, Class<F> feedClass)
          throws IOException {
        HttpRequest request = requestFactory.buildGetRequest(url);
        return request.execute().parseAs(feedClass);
      }
}
