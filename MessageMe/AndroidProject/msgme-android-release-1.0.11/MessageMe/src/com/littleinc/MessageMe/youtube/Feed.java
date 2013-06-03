package com.littleinc.MessageMe.youtube;

import java.util.List;

import com.google.api.client.util.Key;

public class Feed <T extends YouTubeItem>{
    @Key
    public List<T> items;

    @Key
    public int totalItems;
}
