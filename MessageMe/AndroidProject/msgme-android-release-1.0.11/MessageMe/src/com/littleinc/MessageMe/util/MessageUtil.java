package com.littleinc.MessageMe.util;

import java.util.LinkedList;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.bo.ContactMessage;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.DoodleMessage;
import com.littleinc.MessageMe.bo.DoodlePicMessage;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.bo.IMessageType;
import com.littleinc.MessageMe.bo.LocationMessage;
import com.littleinc.MessageMe.bo.Message;
import com.littleinc.MessageMe.bo.NoticeMessage;
import com.littleinc.MessageMe.bo.PhotoMessage;
import com.littleinc.MessageMe.bo.Room;
import com.littleinc.MessageMe.bo.NoticeType;
import com.littleinc.MessageMe.bo.SingleImageMessage;
import com.littleinc.MessageMe.bo.SongMessage;
import com.littleinc.MessageMe.bo.TextMessage;
import com.littleinc.MessageMe.bo.UnsupportedMessage;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.bo.VideoMessage;
import com.littleinc.MessageMe.bo.VoiceMessage;
import com.littleinc.MessageMe.bo.YoutubeMessage;
import com.littleinc.MessageMe.chat.Chat;
import com.littleinc.MessageMe.data.DataBaseHelper;
import com.littleinc.MessageMe.protocol.Messages.PBMessageEnvelope;
import com.littleinc.MessageMe.ui.MessagesFragment;

public class MessageUtil {

    public static IMessage newMessageInstance() {
        return new Message();
    }

    public static IMessage newMessageFromPBMessage(
            PBMessageEnvelope pbMessageEnvelope) {
        IMessage newMessage;
        
        if (pbMessageEnvelope.hasType()) {
            // This code can result in an UNSUPPORTED message type if the 
            // client copy of the Protobufs includes a type which is not
            // yet supported in IMessageType.
            newMessage = MessageUtil.newMessageInstanceByType(IMessageType
                    .valueOf(pbMessageEnvelope.getType()
                            .getNumber()));
        } else {            
            // If a new message type has been added to the Protobufs on the
            // server then hasType() returns false above, so we need to 
            // explicitly specify the UNSUPPORTED type here.
            LogIt.d(MessageUtil.class, "Unsupported message type received");
            newMessage = MessageUtil
                    .newMessageInstanceByType(IMessageType.UNSUPPORTED);
        }
        
        return newMessage;
    }
    
    public static IMessage newMessageInstanceByType(IMessageType messageType) {
        if (messageType != null) {
            switch (messageType) {
            case TEXT:
                return new TextMessage();
            case DOODLE:
                return new DoodleMessage();
            case DOODLE_PIC:
                return new DoodlePicMessage();
            case LOCATION:
                return new LocationMessage();
            case PHOTO:
                return new PhotoMessage();
            case SONG:
                return new SongMessage();
            case VIDEO:
                return new VideoMessage();
            case VOICE:
                return new VoiceMessage();
            case YOUTUBE:
                return new YoutubeMessage();
            case NOTICE:
                return new NoticeMessage();
            case CONTACT:
                return new ContactMessage();
            default:
                return new UnsupportedMessage();
            }
        } else {
            LogIt.w(MessageUtil.class,
                    "Null IMessageType passed into newMessageInstanceByType");
            return new Message();
        }
    }

    public static List<IMessage> getChatMessages(Chat chat,
            int maxNumberOfResults) {
        String[] selectionArgs = null;
        StringBuilder whereStringBuilder = new StringBuilder();

        whereStringBuilder.append(Message.CHANNEL_ID_COLUMN);
        whereStringBuilder.append("=?");

        selectionArgs = new String[] { String.valueOf(chat.getChatId()) };

        return getChatMessages(chat, whereStringBuilder.toString(),
                selectionArgs, String.valueOf(maxNumberOfResults));
    }

    /**
     * Get the next set of messages from earlier than the provided lastMsgInUI,
     * up to a maximum of maxNumberOfResults.
     */
    public static List<IMessage> getChatMessagesBefore(Chat chat,
            IMessage lastMsgInUI, int maxNumberOfResults) {

        LogIt.d(MessageUtil.class, "getChatMessagesBefore",
                String.valueOf(maxNumberOfResults), maxNumberOfResults);

        String[] selectionArgs = null;
        StringBuilder whereStringBuilder = new StringBuilder();

        // We have to search by the "sorted by" time to ensure the
        // messages always show up in a consistent order on a specific
        // device.  We can't use the commandId and messages pending
        // send will not have one yet.
        whereStringBuilder.append(Message.CHANNEL_ID_COLUMN);
        whereStringBuilder.append("=?");
        whereStringBuilder.append(" AND ");
        whereStringBuilder.append(Message.SORTED_BY_COLUMN);
        whereStringBuilder.append("<?");

        selectionArgs = new String[] { String.valueOf(chat.getChatId()),
                String.valueOf(lastMsgInUI.getSortedBy()) };

        return getChatMessages(chat, whereStringBuilder.toString(),
                selectionArgs, String.valueOf(maxNumberOfResults));
    }

    /**
     * Get all the messages including and after the provided 
     * earliestMsgInUISortedBy time, up to a maximum of 
     * maxNumberOfResults.
     */
    public static List<IMessage> getChatMessagesAfter(Chat chat,
            String earliestMsgInUISortedBy) {
        String[] selectionArgs = null;
        StringBuilder whereStringBuilder = new StringBuilder();

        whereStringBuilder.append(Message.CHANNEL_ID_COLUMN);
        whereStringBuilder.append("=?");
        whereStringBuilder.append(" AND ");
        whereStringBuilder.append(Message.SORTED_BY_COLUMN);
        whereStringBuilder.append(">=?");

        selectionArgs = new String[] { String.valueOf(chat.getChatId()),
                earliestMsgInUISortedBy };

        return getChatMessages(chat, whereStringBuilder.toString(),
                selectionArgs, null);
    }

    public static List<IMessage> getChatMessages(Chat chat, String selection,
            String[] selectionArgs, String maxNumberOfResults) {
        LogIt.d(MessageUtil.class, "getChatMessages", chat.getChatId(),
                selection, "limit " + maxNumberOfResults);

        SQLiteDatabase db = DataBaseHelper.getInstance().getReadableDatabase();
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        queryBuilder.setTables(getJoinedTables());

        Cursor readersCursor = null;
        Cursor messagesCursor = null;
        List<IMessage> messages = new LinkedList<IMessage>();

        try {
            messagesCursor = queryBuilder.query(db, getProjection(), selection,
                    selectionArgs, null, null, Message.SORTED_BY_COLUMN
                            + " DESC", // get the newest messages first 
                    maxNumberOfResults);

            while ((messagesCursor != null) && messagesCursor.moveToNext()) {
                IMessage message = loadMessageFromCursor(messagesCursor);

                if (message == null) {
                    LogIt.w(MessageUtil.class,
                            "Ignore message that failed to load from DB");
                } else {
                    messages.add(message);
                }
            }
        } catch (Exception e) {
            LogIt.e(MessageUtil.class, e, e.getMessage());
        } finally {
            if (readersCursor != null)
                readersCursor.close();

            if (messagesCursor != null)
                messagesCursor.close();
        }

        return messages;
    }

    public static IMessage loadMessageFromCursor(Cursor cursor) {
        int columnIndex;
        IMessage message = null;
        String type = cursor.getString(cursor
                .getColumnIndex(Message.TYPE_COLUMN));

        if (IMessageType.TEXT.toString().equals(type)) {
            message = MessageUtil.newMessageInstanceByType(IMessageType.TEXT);
            TextMessage textMessage = (TextMessage) message;

            columnIndex = cursor.getColumnIndex(TextMessage.BODY_COLUMN);
            if (columnIndex != -1) {
                textMessage.setText(cursor.getString(columnIndex));
            }
        } else if (IMessageType.PHOTO.toString().equals(type)) {
            message = MessageUtil.newMessageInstanceByType(IMessageType.PHOTO);
            PhotoMessage photoMessage = (PhotoMessage) message;

            columnIndex = cursor
                    .getColumnIndex(SingleImageMessage.IMAGE_KEY_COLUMN
                            + PhotoMessage.DB_COLUMN_ALIAS_SUFFIX);
            if (columnIndex != -1) {
                photoMessage.setImageKey(cursor.getString(columnIndex));
            }
            columnIndex = cursor
                    .getColumnIndex(SingleImageMessage.THUMB_KEY_COLUMN
                            + PhotoMessage.DB_COLUMN_ALIAS_SUFFIX);
            if (columnIndex != -1) {
                photoMessage.setThumbKey(cursor.getString(columnIndex));
            }
            columnIndex = cursor
                    .getColumnIndex(SingleImageMessage.IMAGE_BUNDLE_COLUMN
                            + PhotoMessage.DB_COLUMN_ALIAS_SUFFIX);
            if (columnIndex != -1) {
                photoMessage.setImageBundleAsBytes(cursor.getBlob(columnIndex));
            }
        } else if (IMessageType.LOCATION.toString().equals(type)) {
            message = MessageUtil
                    .newMessageInstanceByType(IMessageType.LOCATION);
            LocationMessage locationMessage = (LocationMessage) message;

            columnIndex = cursor.getColumnIndex(LocationMessage.ADDRES_COLUMN);
            if (columnIndex != -1) {
                locationMessage.setAddress(cursor.getString(columnIndex));
            }
            columnIndex = cursor
                    .getColumnIndex(LocationMessage.LATITUDE_COLUMN);
            if (columnIndex != -1) {
                locationMessage.setLatitude(cursor.getFloat(columnIndex));
            }
            columnIndex = cursor
                    .getColumnIndex(LocationMessage.LOCATION_ID_COLUMN);
            if (columnIndex != -1) {
                locationMessage.setLocationId(cursor.getString(columnIndex));
            }
            columnIndex = cursor
                    .getColumnIndex(LocationMessage.LONGITUDE_COLUMN);
            if (columnIndex != -1) {
                locationMessage.setLongitude(cursor.getFloat(columnIndex));
            }
            columnIndex = cursor.getColumnIndex(LocationMessage.NAME_COLUMN);
            if (columnIndex != -1) {
                locationMessage.setName(cursor.getString(columnIndex));
            }
        } else if (IMessageType.SONG.toString().equals(type)) {
            message = MessageUtil.newMessageInstanceByType(IMessageType.SONG);
            SongMessage songMessage = (SongMessage) message;

            columnIndex = cursor.getColumnIndex(SongMessage.ARTIST_NAME_COLUMN);
            if (columnIndex != -1) {
                songMessage.setArtistName(cursor.getString(columnIndex));
            }
            columnIndex = cursor.getColumnIndex(SongMessage.ARTWORK_URL_COLUMN);
            if (columnIndex != -1) {
                songMessage.setArtworkUrl(cursor.getString(columnIndex));
            }
            columnIndex = cursor.getColumnIndex(SongMessage.PREVIEW_URL);
            if (columnIndex != -1) {
                songMessage.setPreviewUrl(cursor.getString(columnIndex));
            }
            columnIndex = cursor.getColumnIndex(SongMessage.TRACK_NAME_COLUMN);
            if (columnIndex != -1) {
                songMessage.setTrackName(cursor.getString(columnIndex));
            }
            columnIndex = cursor.getColumnIndex(SongMessage.TRACK_URL_COLUMN);
            if (columnIndex != -1) {
                songMessage.setTrackUrl(cursor.getString(columnIndex));
            }
        } else if (IMessageType.VOICE.toString().equals(type)) {
            message = MessageUtil.newMessageInstanceByType(IMessageType.VOICE);
            VoiceMessage voiceMessage = (VoiceMessage) message;

            columnIndex = cursor
                    .getColumnIndex(VoiceMessage.SECONDS_KEY_COLUMN);
            if (columnIndex != -1) {
                voiceMessage.setSeconds(cursor.getInt(columnIndex));
            }
            columnIndex = cursor.getColumnIndex(VoiceMessage.SOUND_KEY_COLUMN);
            if (columnIndex != -1) {
                voiceMessage.setSoundKey(cursor.getString(columnIndex));
            }
        } else if (IMessageType.DOODLE.toString().equals(type)) {
            message = MessageUtil.newMessageInstanceByType(IMessageType.DOODLE);
            DoodleMessage doodleMessage = (DoodleMessage) message;

            columnIndex = cursor
                    .getColumnIndex(SingleImageMessage.IMAGE_KEY_COLUMN
                            + DoodleMessage.DB_COLUMN_ALIAS_SUFFIX);
            if (columnIndex != -1) {
                doodleMessage.setImageKey(cursor.getString(columnIndex));
            }
            columnIndex = cursor
                    .getColumnIndex(SingleImageMessage.THUMB_KEY_COLUMN
                            + DoodleMessage.DB_COLUMN_ALIAS_SUFFIX);
            if (columnIndex != -1) {
                doodleMessage.setThumbKey(cursor.getString(columnIndex));
            }
            columnIndex = cursor
                    .getColumnIndex(SingleImageMessage.IMAGE_BUNDLE_COLUMN
                            + DoodleMessage.DB_COLUMN_ALIAS_SUFFIX);
            if (columnIndex != -1) {
                doodleMessage
                        .setImageBundleAsBytes(cursor.getBlob(columnIndex));
            }
        } else if (IMessageType.DOODLE_PIC.toString().equals(type)) {
            message = MessageUtil
                    .newMessageInstanceByType(IMessageType.DOODLE_PIC);
            DoodlePicMessage doodlePicMessage = (DoodlePicMessage) message;

            columnIndex = cursor
                    .getColumnIndex(SingleImageMessage.IMAGE_KEY_COLUMN
                            + DoodlePicMessage.DB_COLUMN_ALIAS_SUFFIX);
            if (columnIndex != -1) {
                doodlePicMessage.setImageKey(cursor.getString(columnIndex));
            }
            columnIndex = cursor
                    .getColumnIndex(SingleImageMessage.THUMB_KEY_COLUMN
                            + DoodlePicMessage.DB_COLUMN_ALIAS_SUFFIX);
            if (columnIndex != -1) {
                doodlePicMessage.setThumbKey(cursor.getString(columnIndex));
            }
            columnIndex = cursor
                    .getColumnIndex(SingleImageMessage.IMAGE_BUNDLE_COLUMN
                            + DoodlePicMessage.DB_COLUMN_ALIAS_SUFFIX);
            if (columnIndex != -1) {
                doodlePicMessage.setImageBundleAsBytes(cursor
                        .getBlob(columnIndex));
            }
        } else if (IMessageType.VIDEO.toString().equals(type)) {
            message = MessageUtil.newMessageInstanceByType(IMessageType.VIDEO);
            VideoMessage videoMessage = (VideoMessage) message;

            columnIndex = cursor
                    .getColumnIndex(VideoMessage.VIDEO_DURATION_COLUMN);
            if (columnIndex != -1) {
                videoMessage.setDuration(cursor.getInt(columnIndex));
            }
            columnIndex = cursor.getColumnIndex(VideoMessage.VIDEO_KEY_COLUMN);
            if (columnIndex != -1) {
                videoMessage.setVideoKey(cursor.getString(columnIndex), true);
            }
            columnIndex = cursor.getColumnIndex(VideoMessage.THUMB_KEY_COLUMN);
            if (columnIndex != -1) {
                videoMessage.setThumbKey(cursor.getString(columnIndex));
            }
        } else if (IMessageType.YOUTUBE.toString().equals(type)) {
            message = MessageUtil
                    .newMessageInstanceByType(IMessageType.YOUTUBE);
            YoutubeMessage youtubeMessage = (YoutubeMessage) message;

            columnIndex = cursor
                    .getColumnIndex(YoutubeMessage.VIDEO_DURATION_COLUMN);
            if (columnIndex != -1) {
                youtubeMessage.setDuration(cursor.getInt(columnIndex));
            }
            columnIndex = cursor
                    .getColumnIndex(YoutubeMessage.THUMB_KEY_COLUMN);
            if (columnIndex != -1) {
                youtubeMessage.setThumbKey(cursor.getString(columnIndex));
            }
            columnIndex = cursor.getColumnIndex(YoutubeMessage.VIDEO_ID_COLUMN);
            if (columnIndex != -1) {
                youtubeMessage.setVideoID(cursor.getString(columnIndex));
            }
            columnIndex = cursor
                    .getColumnIndex(YoutubeMessage.VIDEO_TITLE_COLUMN);
            if (columnIndex != -1) {
                youtubeMessage.setVideoTitle(cursor.getString(columnIndex));
            }
        } else if (IMessageType.NOTICE.toString().equals(type)) {
            message = MessageUtil.newMessageInstanceByType(IMessageType.NOTICE);
            NoticeMessage noticeMessage = (NoticeMessage) message;

            columnIndex = cursor.getColumnIndex(NoticeMessage.NOTICE_COLUMN);
            if (columnIndex != -1) {
                noticeMessage.setNotificationMessage(cursor
                        .getString(columnIndex));
            }
            columnIndex = cursor
                    .getColumnIndex(NoticeMessage.NOTICE_TYPE_COLUMN);
            if (columnIndex != -1) {
                noticeMessage.setNoticeType(NoticeType.valueOf(cursor
                        .getString(columnIndex)));
            }
            columnIndex = cursor
                    .getColumnIndex(NoticeMessage.NOTICE_ACTION_USER_ID);
            if (columnIndex != -1) {
                noticeMessage.setActionUserID(cursor.getLong(columnIndex));
            }
        } else if (IMessageType.CONTACT.toString().equals(type)) {
            message = MessageUtil
                    .newMessageInstanceByType(IMessageType.CONTACT);
            ContactMessage contactMessage = (ContactMessage) message;

            columnIndex = cursor.getColumnIndex(ContactMessage.PB_USER_COLUMN);
            if (columnIndex != -1) {
                contactMessage.setPBUserAsBytes(cursor.getBlob(columnIndex));
            }
        } else if (IMessageType.UNSUPPORTED.toString().equals(type)) {
            message = MessageUtil
                    .newMessageInstanceByType(IMessageType.UNSUPPORTED);
            UnsupportedMessage unsupportedMsg = (UnsupportedMessage) message;

            columnIndex = cursor
                    .getColumnIndex(UnsupportedMessage.PB_COMMAND_ENVELOPE_COLUMN);
            if (columnIndex != -1) {
                unsupportedMsg
                        .setPBCmdEnvelopeData(cursor.getBlob(columnIndex));
            }
        }

        if (message != null) {
            message.setId(cursor.getLong(cursor
                    .getColumnIndex(Message.ID_COLUMN)));
            message.setClientId(cursor.getLong(cursor
                    .getColumnIndex(Message.CLIENT_ID_COLUMN)));
            message.setChannelId(cursor.getLong(cursor
                    .getColumnIndex(Message.CHANNEL_ID_COLUMN)));
            message.setCommandId(cursor.getLong(cursor
                    .getColumnIndex(Message.COMMAND_ID_COLUMN)));
            message.setSenderId(cursor.getLong(cursor
                    .getColumnIndex(Message.SENDER_ID_COLUMN)));
            message.setSortedBy(cursor.getDouble(cursor
                    .getColumnIndex(Message.SORTED_BY_COLUMN)));
            message.setCreatedAt(cursor.getInt(cursor
                    .getColumnIndex(Message.CREATED_AT_COLUMN)));
        } else {
            // Something really bad happened, so return a null message
            // and hope the app can survive by ignoring it
            LogIt.e(MessageUtil.class, "Failed to load message", type);
        }

        return message;
    }

    public static String[] getProjection() {
        // The column IDs must all be unique in this projection
        return new String[] {
                "M." + Message.ID_COLUMN,
                "M." + Message.CLIENT_ID_COLUMN,
                "M." + Message.CHANNEL_ID_COLUMN,
                "M." + Message.COMMAND_ID_COLUMN,
                "M." + Message.CREATED_AT_COLUMN,
                "M." + Message.SENDER_ID_COLUMN,
                "M." + Message.SORTED_BY_COLUMN,
                "M." + Message.TYPE_COLUMN,
                "TM." + TextMessage.BODY_COLUMN,
                "PM." + SingleImageMessage.IMAGE_KEY_COLUMN + " AS "
                        + SingleImageMessage.IMAGE_KEY_COLUMN
                        + PhotoMessage.DB_COLUMN_ALIAS_SUFFIX,
                "PM." + SingleImageMessage.THUMB_KEY_COLUMN + " AS "
                        + SingleImageMessage.THUMB_KEY_COLUMN
                        + PhotoMessage.DB_COLUMN_ALIAS_SUFFIX,
                "PM." + SingleImageMessage.IMAGE_BUNDLE_COLUMN + " AS "
                        + SingleImageMessage.IMAGE_BUNDLE_COLUMN
                        + PhotoMessage.DB_COLUMN_ALIAS_SUFFIX,
                "LM." + LocationMessage.ADDRES_COLUMN,
                "LM." + LocationMessage.LATITUDE_COLUMN,
                "LM." + LocationMessage.LOCATION_ID_COLUMN,
                "LM." + LocationMessage.LONGITUDE_COLUMN,
                "LM." + LocationMessage.NAME_COLUMN,
                "SM." + SongMessage.ARTIST_NAME_COLUMN,
                "SM." + SongMessage.ARTWORK_URL_COLUMN,
                "SM." + SongMessage.PREVIEW_URL,
                "SM." + SongMessage.TRACK_NAME_COLUMN,
                "SM." + SongMessage.TRACK_URL_COLUMN,
                "VM." + VoiceMessage.SOUND_KEY_COLUMN,
                "VM." + VoiceMessage.SECONDS_KEY_COLUMN,
                "DM." + SingleImageMessage.IMAGE_KEY_COLUMN + " AS "
                        + SingleImageMessage.IMAGE_KEY_COLUMN
                        + DoodleMessage.DB_COLUMN_ALIAS_SUFFIX,
                "DM." + SingleImageMessage.THUMB_KEY_COLUMN + " AS "
                        + SingleImageMessage.THUMB_KEY_COLUMN
                        + DoodleMessage.DB_COLUMN_ALIAS_SUFFIX,
                "DM." + SingleImageMessage.IMAGE_BUNDLE_COLUMN + " AS "
                        + SingleImageMessage.IMAGE_BUNDLE_COLUMN
                        + DoodleMessage.DB_COLUMN_ALIAS_SUFFIX,
                "VID." + VideoMessage.VIDEO_KEY_COLUMN,
                "VID." + VideoMessage.THUMB_KEY_COLUMN,
                "VID." + VideoMessage.VIDEO_DURATION_COLUMN,
                "YM." + YoutubeMessage.THUMB_KEY_COLUMN,
                "YM." + YoutubeMessage.VIDEO_DURATION_COLUMN,
                "YM." + YoutubeMessage.VIDEO_ID_COLUMN,
                "YM." + YoutubeMessage.VIDEO_TITLE_COLUMN,
                "DPM." + SingleImageMessage.IMAGE_KEY_COLUMN + " AS "
                        + SingleImageMessage.IMAGE_KEY_COLUMN
                        + DoodlePicMessage.DB_COLUMN_ALIAS_SUFFIX,
                "DPM." + SingleImageMessage.THUMB_KEY_COLUMN + " AS "
                        + SingleImageMessage.THUMB_KEY_COLUMN
                        + DoodlePicMessage.DB_COLUMN_ALIAS_SUFFIX,
                "DPM." + SingleImageMessage.IMAGE_BUNDLE_COLUMN + " AS "
                        + SingleImageMessage.IMAGE_BUNDLE_COLUMN
                        + DoodlePicMessage.DB_COLUMN_ALIAS_SUFFIX,
                "NM." + NoticeMessage.NOTICE_COLUMN,
                "NM." + NoticeMessage.NOTICE_TYPE_COLUMN,
                "NM." + NoticeMessage.NOTICE_ACTION_USER_ID,
                "UM." + UnsupportedMessage.PB_MESSAGE_TYPE_COLUMN,
                "UM." + UnsupportedMessage.PB_COMMAND_ENVELOPE_COLUMN,
                "CM." + ContactMessage.PB_USER_COLUMN };
    }

    /**
     * Reduced projection with only the columns necessary on the messages list
     */
    public static String[] getMessagesListProjection() {
        // The column IDs must all be unique in this projection
        return new String[] { "M." + Message.ID_COLUMN,
                "M." + Message.CLIENT_ID_COLUMN,
                "M." + Message.CHANNEL_ID_COLUMN,
                "M." + Message.COMMAND_ID_COLUMN,
                "M." + Message.CREATED_AT_COLUMN,
                "M." + Message.SENDER_ID_COLUMN, "M." + Message.TYPE_COLUMN,
                "M." + Message.SORTED_BY_COLUMN,
                "TM." + TextMessage.BODY_COLUMN,
                "CM." + ContactMessage.PB_USER_COLUMN,
                "LM." + LocationMessage.LOCATION_ID_COLUMN,
                "LM." + LocationMessage.NAME_COLUMN,
                "SM." + SongMessage.TRACK_NAME_COLUMN,
                "YM." + YoutubeMessage.VIDEO_TITLE_COLUMN,
                "NM." + NoticeMessage.NOTICE_COLUMN };
    }

    public static String getJoinedTables() {
        return getJoinedTables(false);
    }

    public static String getJoinedTables(boolean excludeNotices) {
        StringBuilder tablesStringBuilder = new StringBuilder();

        tablesStringBuilder.append(Message.TABLE_NAME);

        // TEXT MESSAGE
        tablesStringBuilder.append(" AS M LEFT JOIN ");
        tablesStringBuilder.append(TextMessage.TABLE_NAME);
        tablesStringBuilder.append(" AS TM ON M.");
        tablesStringBuilder.append(Message.ID_COLUMN);
        tablesStringBuilder.append(" = TM.");
        tablesStringBuilder.append(Message.ID_COLUMN);

        // PHOTO MESSAGE
        tablesStringBuilder.append(" LEFT JOIN ");
        tablesStringBuilder.append(PhotoMessage.TABLE_NAME);
        tablesStringBuilder.append(" AS PM ON M.");
        tablesStringBuilder.append(Message.ID_COLUMN);
        tablesStringBuilder.append(" = PM.");
        tablesStringBuilder.append(Message.ID_COLUMN);

        // LOCATION MESSAGE
        tablesStringBuilder.append(" LEFT JOIN ");
        tablesStringBuilder.append(LocationMessage.TABLE_NAME);
        tablesStringBuilder.append(" AS LM ON M.");
        tablesStringBuilder.append(Message.ID_COLUMN);
        tablesStringBuilder.append(" = LM.");
        tablesStringBuilder.append(Message.ID_COLUMN);

        // SONG MESSAGE
        tablesStringBuilder.append(" LEFT JOIN ");
        tablesStringBuilder.append(SongMessage.TABLE_NAME);
        tablesStringBuilder.append(" AS SM ON M.");
        tablesStringBuilder.append(Message.ID_COLUMN);
        tablesStringBuilder.append(" = SM.");
        tablesStringBuilder.append(Message.ID_COLUMN);

        // VOICE MESSAGE
        tablesStringBuilder.append(" LEFT JOIN ");
        tablesStringBuilder.append(VoiceMessage.TABLE_NAME);
        tablesStringBuilder.append(" AS VM ON M.");
        tablesStringBuilder.append(Message.ID_COLUMN);
        tablesStringBuilder.append(" = VM.");
        tablesStringBuilder.append(Message.ID_COLUMN);

        // DOODLE MESSAGE
        tablesStringBuilder.append(" LEFT JOIN ");
        tablesStringBuilder.append(DoodleMessage.TABLE_NAME);
        tablesStringBuilder.append(" AS DM ON M.");
        tablesStringBuilder.append(Message.ID_COLUMN);
        tablesStringBuilder.append(" = DM.");
        tablesStringBuilder.append(Message.ID_COLUMN);

        // VIDEO MESSAGE
        tablesStringBuilder.append(" LEFT JOIN ");
        tablesStringBuilder.append(VideoMessage.TABLE_NAME);
        tablesStringBuilder.append(" AS VID ON M.");
        tablesStringBuilder.append(Message.ID_COLUMN);
        tablesStringBuilder.append(" = VID.");
        tablesStringBuilder.append(Message.ID_COLUMN);

        // YOUTUBE MESSAGE
        tablesStringBuilder.append(" LEFT JOIN ");
        tablesStringBuilder.append(YoutubeMessage.TABLE_NAME);
        tablesStringBuilder.append(" AS YM ON M.");
        tablesStringBuilder.append(Message.ID_COLUMN);
        tablesStringBuilder.append(" = YM.");
        tablesStringBuilder.append(YoutubeMessage.ID_COLUMN);

        // DOODLE PIC MESSAGE
        tablesStringBuilder.append(" LEFT JOIN ");
        tablesStringBuilder.append(DoodlePicMessage.TABLE_NAME);
        tablesStringBuilder.append(" AS DPM ON M.");
        tablesStringBuilder.append(Message.ID_COLUMN);
        tablesStringBuilder.append(" = DPM.");
        tablesStringBuilder.append(Message.ID_COLUMN);

        if (!excludeNotices) {
            //NOTICE MESSAGE
            tablesStringBuilder.append(" LEFT JOIN ");
            tablesStringBuilder.append(NoticeMessage.TABLE_NAME);
            tablesStringBuilder.append(" AS NM ON M.");
            tablesStringBuilder.append(Message.ID_COLUMN);
            tablesStringBuilder.append(" = NM.");
            tablesStringBuilder.append(Message.ID_COLUMN);
        }

        // UNSUPPORTED MESSAGE
        tablesStringBuilder.append(" LEFT JOIN ");
        tablesStringBuilder.append(UnsupportedMessage.TABLE_NAME);
        tablesStringBuilder.append(" AS UM ON M.");
        tablesStringBuilder.append(Message.ID_COLUMN);
        tablesStringBuilder.append(" = UM.");
        tablesStringBuilder.append(Message.ID_COLUMN);

        // CONTACT MESSAGE
        tablesStringBuilder.append(" LEFT JOIN ");
        tablesStringBuilder.append(ContactMessage.TABLE_NAME);
        tablesStringBuilder.append(" AS CM ON M.");
        tablesStringBuilder.append(Message.ID_COLUMN);
        tablesStringBuilder.append(" = CM.");
        tablesStringBuilder.append(Message.ID_COLUMN);

        return tablesStringBuilder.toString();
    }

    /**
     * Reduced set of tables to be used in {@link MessagesFragment}
     */
    public static String getMessagesListJoinedTables() {
        StringBuilder tablesStringBuilder = new StringBuilder();

        tablesStringBuilder.append(Conversation.TABLE_NAME);

        // MESSAGE
        tablesStringBuilder.append(" AS C LEFT JOIN ");
        tablesStringBuilder.append(Message.TABLE_NAME);
        tablesStringBuilder.append(" AS M ON C.");
        tablesStringBuilder.append(Conversation.LAST_MESSAGE_ID_COLUMN);
        tablesStringBuilder.append(" = M.");
        tablesStringBuilder.append(Message.ID_COLUMN);

        // TEXT MESSAGE
        tablesStringBuilder.append(" LEFT JOIN ");
        tablesStringBuilder.append(TextMessage.TABLE_NAME);
        tablesStringBuilder.append(" AS TM ON C.");
        tablesStringBuilder.append(Conversation.LAST_MESSAGE_ID_COLUMN);
        tablesStringBuilder.append(" = TM.");
        tablesStringBuilder.append(Message.ID_COLUMN);

        // CONTACT MESSAGE
        tablesStringBuilder.append(" LEFT JOIN ");
        tablesStringBuilder.append(ContactMessage.TABLE_NAME);
        tablesStringBuilder.append(" AS CM ON C.");
        tablesStringBuilder.append(Conversation.LAST_MESSAGE_ID_COLUMN);
        tablesStringBuilder.append(" = CM.");
        tablesStringBuilder.append(Message.ID_COLUMN);

        // LOCATION MESSAGE
        tablesStringBuilder.append(" LEFT JOIN ");
        tablesStringBuilder.append(LocationMessage.TABLE_NAME);
        tablesStringBuilder.append(" AS LM ON C.");
        tablesStringBuilder.append(Conversation.LAST_MESSAGE_ID_COLUMN);
        tablesStringBuilder.append(" = LM.");
        tablesStringBuilder.append(Message.ID_COLUMN);

        // SONG MESSAGE
        tablesStringBuilder.append(" LEFT JOIN ");
        tablesStringBuilder.append(SongMessage.TABLE_NAME);
        tablesStringBuilder.append(" AS SM ON C.");
        tablesStringBuilder.append(Conversation.LAST_MESSAGE_ID_COLUMN);
        tablesStringBuilder.append(" = SM.");
        tablesStringBuilder.append(Message.ID_COLUMN);

        // YOUTUBE MESSAGE
        tablesStringBuilder.append(" LEFT JOIN ");
        tablesStringBuilder.append(YoutubeMessage.TABLE_NAME);
        tablesStringBuilder.append(" AS YM ON C.");
        tablesStringBuilder.append(Conversation.LAST_MESSAGE_ID_COLUMN);
        tablesStringBuilder.append(" = YM.");
        tablesStringBuilder.append(Message.ID_COLUMN);

        //NOTICE MESSAGE
        tablesStringBuilder.append(" LEFT JOIN ");
        tablesStringBuilder.append(NoticeMessage.TABLE_NAME);
        tablesStringBuilder.append(" AS NM ON C.");
        tablesStringBuilder.append(Conversation.LAST_MESSAGE_ID_COLUMN);
        tablesStringBuilder.append(" = NM.");
        tablesStringBuilder.append(Message.ID_COLUMN);

        return tablesStringBuilder.toString();
    }

    /**
     * Added User/Room set of tables to be used in SearchMessages
     */
    public static String getSearchMessagesListJoinedTables() {

        // Add the TEXT MESSAGE table first
        StringBuilder tablesStringBuilder = new StringBuilder(
                getMessagesListJoinedTables());

        // USER MESSAGE
        tablesStringBuilder.append(" LEFT JOIN ");
        tablesStringBuilder.append(User.TABLE_NAME);
        tablesStringBuilder.append(" AS U ON M.");
        tablesStringBuilder.append(Message.CHANNEL_ID_COLUMN);
        tablesStringBuilder.append(" = U.");
        tablesStringBuilder.append(User.ID_COLUMN);

        // ROOM MESSAGE
        tablesStringBuilder.append(" LEFT JOIN ");
        tablesStringBuilder.append(Room.TABLE_NAME);
        tablesStringBuilder.append(" AS R ON M.");
        tablesStringBuilder.append(Message.CHANNEL_ID_COLUMN);
        tablesStringBuilder.append(" = R.");
        tablesStringBuilder.append(Room.ID_COLUMN);

        return tablesStringBuilder.toString();

    }
}