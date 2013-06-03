package com.littleinc.MessageMe.youtube;

import com.google.api.client.googleapis.GoogleUrl;
import com.google.api.client.util.Key;

public class YouTubeUrl extends GoogleUrl{
    
    // XXX This can be set to false once the code is working perfectly
    /** Whether to pretty print HTTP requests and responses. */
    private static final boolean PRETTY_PRINT = true;
    
    @Key
    public
    String author;
    
    @Key 
    public String q;

    @Key("max-results")
    public Integer maxResults = 10;

    public YouTubeUrl(String encodedUrl) {
      super(encodedUrl);
      
      // Set the fields so we get the same data back as the iOS client.  This 
      // is needed to get the mqDefault thumnail image URLs.
      //
      // XXX Unfortunately these params are not allowed while using JSONC
      //  "Partial retrieval disabled for JSONC"
      //setFields("entry[link/@rel='http://gdata.youtube.com/schemas/2007%%23mobile']");
      
      setAlt("jsonc");
      setPrettyPrint(PRETTY_PRINT);
    }

 
}
