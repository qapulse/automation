package com.littleinc.MessageMe.bo;

import com.google.gson.annotations.SerializedName;

public class GCMBundleMessage {

    //  Sample GCM data sent by the server (This is the intent data)
    //
    //    TEXT MESSAGE
    //    Bundle[{
    //      first_name=david,
    //      title=david froyo: Fggghg,
    //      collapse_key=do_not_collapse,
    //      sender_id=893200631840509952,
    //      recipient_id=893200631840509952,
    //      last_name=froyo,
    //      loc_key=pmt,
    //      from=40681973717,
    //      loc_args={
    //        "body": "Fggghg"
    //      },
    //      recipient_is_user=true,
    //      recipient_name=david froyo,
    //      cid=893200631840509952
    //    }]
    //    
    //    SONG MESSAGE
    //    Bundle[{
    //      first_name=david,
    //      title=david froyo sent you a song: "One More Time" by OneMoreTime,
    //      collapse_key=do_not_collapse,
    //      sender_id=893200631840509952,
    //      recipient_id=893200631840509952,
    //      last_name=froyo,
    //      loc_key=pms,
    //      from=40681973717,
    //      loc_args={
    //          "artist_name": "Daft Punk",
    //          "track_name": "One More Time"
    //      },
    //      recipient_is_user=true,
    //      recipient_name=david froyo,
    //      cid=893200631840509952
    //      }]
    //    
    //    PHOTO MESSAGE
    //    Bundle[{
    //      first_name=david,
    //      title=david froyo sent you a picture,
    //      collapse_key=do_not_collapse,
    //      sender_id=893200631840509952,
    //      recipient_id=893200631840509952,
    //      last_name=froyo,
    //      loc_key=pmp,
    //      from=40681973717,
    //      recipient_is_user=true,
    //      recipient_name=davidfroyo,
    //      cid=893200631840509952
    //      }]
    //    
    //    USER LEFT GROUP
    //    Bundle[{
    //      first_name=david,
    //      title=david froyo left the group Date Divider,
    //      collapse_key=do_not_collapse,
    //      sender_id=893200631840509952,
    //      last_name=froyo,
    //      room_id=905506491589595136,
    //      loc_key=pgl,
    //      from=40681973717,
    //      loc_args={
    //          "name": "Date Divider"
    //      },
    //      cid=905506491589595136
    //      }]

    private String body;

    @SerializedName("artist_name")
    private String artistName;

    @SerializedName("track_name")
    private String trackName;

    @SerializedName("title")
    private String title;

    @SerializedName("name")
    private String groupName;

    @SerializedName("old_name")
    private String oldName;

    @SerializedName("new_name")
    private String newName;

    @SerializedName("match_count")
    private int matchCount = -1; 
    
    private String contact;

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getTrackName() {
        return trackName;
    }

    public void setTrackName(String trackName) {
        this.trackName = trackName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String name) {
        this.groupName = name;
    }

    public String getContactName() {
        return contact;
    }

    public void setContactName(String contactName) {
        this.contact = contactName;
    }

    public String getOldName() {
        return oldName;
    }

    public void setOldName(String oldName) {
        this.oldName = oldName;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }
    
    public int getMatchCount() {
        return matchCount;
    }

    public void setMatchCount(int matchCount) {
        this.matchCount = matchCount;
    }
}
