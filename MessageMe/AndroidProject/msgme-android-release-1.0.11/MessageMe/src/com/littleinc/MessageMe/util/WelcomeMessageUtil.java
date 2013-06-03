package com.littleinc.MessageMe.util;

import android.content.Context;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.bo.ContactType;
import com.littleinc.MessageMe.bo.DoodleMessage;
import com.littleinc.MessageMe.bo.LocationMessage;
import com.littleinc.MessageMe.bo.SongMessage;
import com.littleinc.MessageMe.bo.TextMessage;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.protocol.Objects.PBImage;
import com.littleinc.MessageMe.protocol.Objects.PBImage.ImageType;
import com.littleinc.MessageMe.protocol.Objects.PBImageBundle;

/**
 * Utilities for displaying the messages in the Welcome room.
 */
public class WelcomeMessageUtil {

    private static User user;

    /**
     * Creates Welcome user information
     */
    private static void createWelcomeUser() {
        user = new User();
        user.setContactId(MessageMeConstants.WELCOME_ROOM_ID);
        user.setContactType(ContactType.USER.getValue());
        user.setEmail(MessageMeApplication.getInstance().getString(
                R.string.welcome_user_email));
        user.setFirstName(MessageMeApplication.getInstance().getString(
                R.string.welcome_user_name));
        user.setLastName("");
        user.setNameInitial("");
        user.setShown(false);

        user.save();
        LogIt.d(MessageMeApplication.getInstance(), "Welcome User created");

    }

    /**
     * Creates the welcome messages thread
     */
    private static void createWelcomeMessages() {

        double nextSortedBy = DateUtil.getCurrentTimeMicros();
        
        Context context = MessageMeApplication.getInstance();
        
        TextMessage welcomeMessage = new TextMessage();
        welcomeMessage.setText(context.getString(
                R.string.welcome));
        welcomeMessage.setCommandId(0);
        welcomeMessage.setChannelId(user.getContactId());
        welcomeMessage.setSenderId(user.getContactId());
        welcomeMessage.setClientId(DateUtil.now().getTime());
        welcomeMessage.setCreatedAt(DateUtil.getCurrentTimestamp());
        welcomeMessage.setSortedBy(nextSortedBy);
        welcomeMessage.save(false);

        nextSortedBy += 1.0d;
        
        TextMessage weHopeMessage = new TextMessage();
        weHopeMessage.setText(context.getString(
                R.string.welcome_we_hope));
        weHopeMessage.setCommandId(0);
        weHopeMessage.setChannelId(user.getContactId());
        weHopeMessage.setSenderId(user.getContactId());
        weHopeMessage.setClientId(DateUtil.now().getTime());
        weHopeMessage.setCreatedAt(DateUtil.getCurrentTimestamp());
        weHopeMessage.setSortedBy(nextSortedBy);
        weHopeMessage.save(false);

        nextSortedBy += 1.0d;
        
        DoodleMessage doodleMessage = new DoodleMessage();
        
        // Create the PBImageBundle for the Doodle, otherwise the message 
        // thread won't know the size of the thumbnail until the image has
        // been downloaded.
        PBImage.Builder thumbBuilder = PBImage.newBuilder(); 
        thumbBuilder.setWidth(300);
        thumbBuilder.setHeight(300);
        thumbBuilder.setType(ImageType.THUMB_STANDARD);
        thumbBuilder.setKey(context.getString(
                R.string.welcome_doodle_thumb_url));
        
        PBImage.Builder detailBuilder = PBImage.newBuilder(); 
        detailBuilder.setWidth(640);
        detailBuilder.setHeight(640);
        detailBuilder.setType(ImageType.NORMAL_STANDARD);
        detailBuilder.setKey(context.getString(
                R.string.welcome_doodle_url));
        
        PBImageBundle.Builder imgBundleBuilder = PBImageBundle.newBuilder();
        imgBundleBuilder.setThumbStandardResolution(thumbBuilder.build());
        imgBundleBuilder.setNormalStandardResolution(detailBuilder.build());
        
        doodleMessage.setImageBundle(imgBundleBuilder.build());
        
        doodleMessage.setImageKey(context.getString(
                R.string.welcome_doodle_url));
        doodleMessage.setThumbKey(context.getString(
                R.string.welcome_doodle_thumb_url));
        doodleMessage.setCommandId(0);
        doodleMessage.setChannelId(user.getContactId());
        doodleMessage.setSenderId(user.getContactId());
        doodleMessage.setClientId(DateUtil.now().getTime());
        doodleMessage.setCreatedAt(DateUtil.getCurrentTimestamp());
        doodleMessage.setSortedBy(nextSortedBy);
        doodleMessage.save(false);

        nextSortedBy += 1.0d;
        
        SongMessage songMessage = new SongMessage();
        songMessage.setArtistName(context.getString(
                R.string.welcome_song_artist_name));
        songMessage.setArtworkUrl(context.getString(
                R.string.welcome_song_artwork_url));
        songMessage.setPreviewUrl(context.getString(
                R.string.welcome_song_preview_url));
        songMessage.setTrackName(context.getString(
                R.string.welcome_song_track_name));
        songMessage.setTrackUrl(context.getString(
                R.string.welcome_song_track_url));
        songMessage.setCommandId(0);
        songMessage.setChannelId(user.getContactId());
        songMessage.setSenderId(user.getContactId());
        songMessage.setClientId(DateUtil.now().getTime());
        songMessage.setCreatedAt(DateUtil.getCurrentTimestamp());
        songMessage.setSortedBy(nextSortedBy);
        songMessage.save(false);

        nextSortedBy += 1.0d;
        
        LocationMessage locationMessage = new LocationMessage();
        locationMessage.setAddress(context
                .getString(R.string.welcome_location_address));
        locationMessage.setLatitude(Float.parseFloat(MessageMeApplication
                .getInstance().getString(R.string.welcome_location_latitude)));
        locationMessage.setLongitude(Float.parseFloat(MessageMeApplication
                .getInstance().getString(R.string.welcome_location_longitude)));
        locationMessage.setLocationId(context
                .getString(R.string.welcome_location_id));
        locationMessage.setName(context.getString(
                R.string.welcome_location_name));
        locationMessage.setCommandId(0);
        locationMessage.setChannelId(user.getContactId());
        locationMessage.setSenderId(user.getContactId());
        locationMessage.setClientId(DateUtil.now().getTime());
        locationMessage.setCreatedAt(DateUtil.getCurrentTimestamp());
        locationMessage.setSortedBy(nextSortedBy);
        locationMessage.save(false);

        nextSortedBy += 1.0d;
        
        TextMessage andMoreMessage = new TextMessage();
        andMoreMessage.setText(context.getString(
                R.string.welcome_and_more));
        andMoreMessage.setCommandId(0);
        andMoreMessage.setChannelId(user.getContactId());
        andMoreMessage.setSenderId(user.getContactId());
        andMoreMessage.setClientId(DateUtil.now().getTime());
        andMoreMessage.setCreatedAt(DateUtil.getCurrentTimestamp());
        andMoreMessage.setSortedBy(nextSortedBy);
        andMoreMessage.save(false);

        nextSortedBy += 1.0d;
        
        TextMessage tellUsMessage = new TextMessage();
        tellUsMessage.setText(context.getString(
                R.string.welcome_tell_us));
        tellUsMessage.setCommandId(0);
        tellUsMessage.setChannelId(user.getContactId());
        tellUsMessage.setSenderId(user.getContactId());
        tellUsMessage.setClientId(DateUtil.now().getTime());
        tellUsMessage.setCreatedAt(DateUtil.getCurrentTimestamp());
        tellUsMessage.setSortedBy(nextSortedBy);
        tellUsMessage.save(false);

        LogIt.d(context,
                "Welcome messages thread created");

    }

    /**
     * Wrapper for the creation of the user
     * and the welcome messages thread
     */
    public static void createWelcomeInformation() {
        createWelcomeUser();
        createWelcomeMessages();
    }

}
