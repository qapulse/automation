package com.littleinc.MessageMe;

/**
 * Constants related to our interfaces with external APIs.
 */
public abstract class MessageMeExternalAPI {

    public static final String FACEBOOK_ACCESS_TOKEN = "214604328549924|y0A7qIA2-CZY7NOVbESfMZPTLGA";
    
    /** 
     * Our MessageMe GCM project ID from our Google APIs page 
     */
    public static final String SENDER_ID = "40681973717";

    public static final String YOUTUBE_GDATA_SERVER = "http://gdata.youtube.com";

    public static final String VIDEOS_FEED = YOUTUBE_GDATA_SERVER
            + "/feeds/mobile/videos?format=1,6";

    public static final String YOUTUBE_DEVELOPER_KEY = "AI39si71ePP9MvPCMVD1-GMEPtuYFU7osDAZl1pMa7VPIxudkHr916hz-hMM-zsRHcrlCqUho0HFxy1g_ZSZSaK6gwyCgLfcdw";

    /**
     * Image search Azure account key.
     * 
     *   Production: uxo4Ff5EfzmgL9/PooMXRTAJ4R8MDSVsbYQth2nUAEE=
     *   David's:    2XJrFb3QWde+1EtREEfwUH/XoVu4mu+4BQZG7tQQFys=
     */
    public static final String IMAGE_SEARCH_KEY = "uxo4Ff5EfzmgL9/PooMXRTAJ4R8MDSVsbYQth2nUAEE=";
    
    public static final String G_MAPS_URL = "http://maps.google.com/maps?q=%1$s,%2$s";
    
    public static final String AWS_TIME_OFFSET_ERROR = "RequestTimeTooSkewed";
}
