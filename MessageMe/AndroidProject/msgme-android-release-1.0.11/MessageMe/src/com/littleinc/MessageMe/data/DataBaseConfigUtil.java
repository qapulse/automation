package com.littleinc.MessageMe.data;

import com.j256.ormlite.android.apptools.OrmLiteConfigUtil;
import com.littleinc.MessageMe.bo.ABContactInfo;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.ConversationReader;
import com.littleinc.MessageMe.bo.DoodleMessage;
import com.littleinc.MessageMe.bo.DoodlePicMessage;
import com.littleinc.MessageMe.bo.LocationMessage;
import com.littleinc.MessageMe.bo.MatchedABRecord;
import com.littleinc.MessageMe.bo.Message;
import com.littleinc.MessageMe.bo.MessageMeCursor;
import com.littleinc.MessageMe.bo.NoticeMessage;
import com.littleinc.MessageMe.bo.PhotoMessage;
import com.littleinc.MessageMe.bo.Room;
import com.littleinc.MessageMe.bo.RoomMember;
import com.littleinc.MessageMe.bo.SongMessage;
import com.littleinc.MessageMe.bo.TextMessage;
import com.littleinc.MessageMe.bo.UnsupportedMessage;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.bo.VideoMessage;
import com.littleinc.MessageMe.bo.VoiceMessage;
import com.littleinc.MessageMe.bo.YoutubeMessage;

public class DataBaseConfigUtil extends OrmLiteConfigUtil {

    private static final Class<?>[] classes = new Class[] {
            MessageMeCursor.class, User.class, Room.class, RoomMember.class,
            Message.class, TextMessage.class, DoodlePicMessage.class,
            DoodleMessage.class, PhotoMessage.class, LocationMessage.class,
            SongMessage.class, VideoMessage.class, VoiceMessage.class,
            YoutubeMessage.class, NoticeMessage.class,
            UnsupportedMessage.class, MatchedABRecord.class,
            ABContactInfo.class, Conversation.class, ConversationReader.class };

    /**
     * You must run this method to regenerate ormlite_config.txt any time
     * the database definitions are updated.
     * 
     * You need to update the Run Configuration for this class to set a
     * JRE, and remove the Android bootstrap entry.  Instructions here:
     *   http://ormlite.com/javadoc/ormlite-core/doc-files/ormlite_4.html
     * 
     * If you are changing the database you should also update the 
     * DATABASE_VERSION number in {@link DataBaseHelper}.
     * 
     * If you are adding a new message type class you must add it to the
     * array of classes above.
     */
    public static void main(String[] args) throws Exception {
        writeConfigFile("ormlite_config.txt", classes);
    }
}