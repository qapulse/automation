package com.littleinc.MessageMe;

import java.util.HashMap;

public class MessageMeConfig {
    /**
     * Key to get the base URL used for all REST communication.  
     * 
     * This must start with http:// or https://
     */
    public static final String KEY_REST_URL = "restful_server_url";
    
    /**
     * Key to get the Web Socket URL.
     * 
     * The Web Socket used for all chat communication with the server.
     * We explicitly specify port 443 to ensure carriers don't mess with 
     * our traffic, which causes lots of problems.
     */
    public static final String KEY_WS_URL = "ws_url";
    
    /**
     * Key to get the bucket to use for Amazon S3.
     */
    public static final String KEY_S3_BUCKET = "amazon_s3_bucket";
    
    /**
     * Key to get the direct access URL to use for accessing media files 
     * directly from Amazon S3.  Must include the trailing path separator.
     */
    public static final String KEY_S3_DIRECT_ACCESS_URL = "amazon_s3_access_url";
    
    private static HashMap<String, HashMap<String, String>> sConfigMap;

    static {
        sConfigMap = new HashMap<String, HashMap<String, String>>();
        
        HashMap<String, String> production = new HashMap<String, String>();
        production.put(KEY_REST_URL, "https://api.msgme.im/v1");
        production.put(KEY_WS_URL, "ws://chat.msgme.im:443/v1/websocket?encoding=binary");
        production.put(KEY_S3_BUCKET, "msgme");
        production.put(KEY_S3_DIRECT_ACCESS_URL, "http://msgme.s3.amazonaws.com/");
        sConfigMap.put("production", production);
        
        HashMap<String, String> staging = new HashMap<String, String>();
        staging.put(KEY_REST_URL, "http://staging.msgme.im/v1");
        staging.put(KEY_WS_URL, "ws://staging-chat.msgme.im:443/v1/websocket?encoding=binary");
        staging.put(KEY_S3_BUCKET, "msgme_dev");
        staging.put(KEY_S3_DIRECT_ACCESS_URL, "http://msgme_dev.s3.amazonaws.com/");
        sConfigMap.put("staging", staging);
        
        HashMap<String, String> dan = new HashMap<String, String>();
        dan.put(KEY_REST_URL, "http://dan.msgme.im/v1");
        dan.put(KEY_WS_URL, "ws://dan.msgme.im:9999/v1/websocket?encoding=binary");
        dan.put(KEY_S3_BUCKET, "msgme_dev");
        dan.put(KEY_S3_DIRECT_ACCESS_URL, "http://msgme_dev.s3.amazonaws.com/");
        sConfigMap.put("dan", dan);
        
        HashMap<String, String> logn = new HashMap<String, String>();
        logn.put(KEY_REST_URL, "http://logn.foo.msgme.im/v1");
        logn.put(KEY_WS_URL, "ws://logn-foo-chat.msgme.im:443/v1/websocket?encoding=binary");
        logn.put(KEY_S3_BUCKET, "msgme_dev");
        logn.put(KEY_S3_DIRECT_ACCESS_URL, "http://msgme_dev.s3.amazonaws.com/");
        sConfigMap.put("logn", logn);

        HashMap<String, String> mike = new HashMap<String, String>();
        mike.put(KEY_REST_URL, "http://dv09.foo.msgme.im/v1");
        mike.put(KEY_WS_URL, "ws://dv09.foo.msgme.im:7051/v1/websocket?encoding=binary");
        mike.put(KEY_S3_BUCKET, "msgme_dev");
        mike.put(KEY_S3_DIRECT_ACCESS_URL, "http://msgme_dev.s3.amazonaws.com/");
        sConfigMap.put("mike", mike);
        
        HashMap<String, String> vivek = new HashMap<String, String>();
        vivek.put(KEY_REST_URL, "http://dv06.foo.msgme.im/v1");
        vivek.put(KEY_WS_URL, "ws://dv06-foo-chat.msgme.im:443/v1/websocket?encoding=binary");
        vivek.put(KEY_S3_BUCKET, "msgme_dev");
        vivek.put(KEY_S3_DIRECT_ACCESS_URL, "http://msgme_dev.s3.amazonaws.com/");
        sConfigMap.put("vivek", vivek);
    }
    
    public static HashMap<String, HashMap<String, String>> getConfigMap() {
        return sConfigMap;
    }
}
