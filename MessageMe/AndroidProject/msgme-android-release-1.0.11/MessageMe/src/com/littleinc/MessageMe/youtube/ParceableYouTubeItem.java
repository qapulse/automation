package com.littleinc.MessageMe.youtube;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.api.client.util.DateTime;
import com.littleinc.MessageMe.youtube.YouTubeItem.Thumbnail;

public class ParceableYouTubeItem implements Parcelable {
    
    private YouTubeItem youtubeItem;
    
    public ParceableYouTubeItem(YouTubeItem item){
        this.youtubeItem = item;
    }

    public YouTubeItem getYoutubeItem() {
        return youtubeItem;
    }

    public void setYoutubeItem(YouTubeItem youtubeItem) {
        this.youtubeItem = youtubeItem;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(youtubeItem.getId());
        dest.writeString(youtubeItem.getTitle());
        dest.writeString(youtubeItem.getUploader());
        dest.writeString(youtubeItem.getThumbnail().getSqDefault());
        dest.writeString(youtubeItem.getThumbnail().getMqDefault());
        dest.writeString(youtubeItem.getUpdated().toString());
        dest.writeInt(youtubeItem.getViewCount());
        dest.writeInt(youtubeItem.getDuration());        
    }
    
    public static final Parcelable.Creator<ParceableYouTubeItem> CREATOR = new Parcelable.Creator<ParceableYouTubeItem>() {
        public ParceableYouTubeItem createFromParcel(Parcel in) {
            return new ParceableYouTubeItem(in);
        }

        public ParceableYouTubeItem[] newArray(int size) {
            return new ParceableYouTubeItem[size];
        }
    };
    
    private ParceableYouTubeItem(Parcel in) {
        Thumbnail thumbnail = new Thumbnail();
        youtubeItem = new YouTubeItem();
        youtubeItem.setId(in.readString());
        youtubeItem.setTitle(in.readString());
        youtubeItem.setUploader(in.readString());
        thumbnail.setSqDefault(in.readString());
        thumbnail.setMqDefault(in.readString());
        youtubeItem.setThumbnail(thumbnail);
        youtubeItem.setUpdated(new DateTime(in.readString()));
        youtubeItem.setViewCount(in.readInt());
        youtubeItem.setDuration(in.readInt());
    }
}
