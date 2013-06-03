package com.littleinc.MessageMe.youtube;

import com.coredroid.util.LogIt;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.Key;

public class YouTubeItem {

    @Key
    private int viewCount;

    @Key
    private String uploader;

    @Key
    private String title;

    @Key
    private DateTime updated;

    @Key
    private String id;

    @Key
    private int duration;

    @Key
    private Thumbnail thumbnail;

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public String getUploader() {
        return uploader;
    }

    public void setUploader(String uploader) {
        this.uploader = uploader;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public DateTime getUpdated() {
        return updated;
    }

    public void setUpdated(DateTime updated) {
        this.updated = updated;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public Thumbnail getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Thumbnail thumbnail) {
        this.thumbnail = thumbnail;
    }

    /**
     * The developer docs for the media thumbnail are here:
     *  https://developers.google.com/youtube/2.0/reference#youtube_data_api_tag_media:thumbnail
     */
    public static class Thumbnail {

        /**
         * The small thumbnail.
         */
        @Key
        private String sqDefault;

        /**
         * The medium size thumbnail - this matches what the iOS client uses.
         */
        @Key
        private String mqDefault;
        
        public String getSqDefault() {
            LogIt.d(this, "sqDefault", sqDefault);
            return sqDefault;
        }

        public void setSqDefault(String sqDefault) {
            this.sqDefault = sqDefault;
        }

        /**
         * XXX This method has a hacky implementation until we change our
         * Youtube queries to use JSON instead of JSONC.
         */
        public String getMqDefault() {
            // The small thumbnail ends "default.jpg", but we want to use the
            // medium sized one to match iOS.  Translate manually for now.
            //   http://i.ytimg.com/vi/mIA0W69U2_Y/default.jpg (small)
            //   http://i.ytimg.com/vi/mIA0W69U2_Y/mqdefault.jpg (medium)
            return sqDefault.replaceFirst("default.jpg", "mqdefault.jpg");
        }

        public void setMqDefault(String mqDefault) {
            this.mqDefault = mqDefault;
        }
    }
}
