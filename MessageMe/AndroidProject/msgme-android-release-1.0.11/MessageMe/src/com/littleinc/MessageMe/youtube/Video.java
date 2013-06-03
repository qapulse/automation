package com.littleinc.MessageMe.youtube;

import java.util.ArrayList;
import java.util.List;

import com.google.api.client.util.Key;

public class Video extends YouTubeItem {

    @Key
    public String description;

    @Key
    public List<String> tags = new ArrayList<String>();

    @Key
    public Player player;

}
