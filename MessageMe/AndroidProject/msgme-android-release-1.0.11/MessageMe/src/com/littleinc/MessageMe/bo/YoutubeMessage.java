package com.littleinc.MessageMe.bo;

import java.sql.SQLException;

import android.content.Context;
import android.provider.SyncStateContract.Columns;

import com.coredroid.util.LogIt;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.chat.DurableCommand;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.data.DataBaseHelper;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandMessageNew;
import com.littleinc.MessageMe.protocol.Messages.PBMessageEnvelope;
import com.littleinc.MessageMe.protocol.Messages.PBMessageYoutube;

@DatabaseTable(tableName = YoutubeMessage.TABLE_NAME)
public class YoutubeMessage extends AbstractMessage {

    public static final String TABLE_NAME = "youtube_message";

    public static final String ID_COLUMN = Columns._ID;

    public static final String VIDEO_ID_COLUMN = "video_id";

    public static final String THUMB_KEY_COLUMN = "youtube_thumb";

    public static final String VIDEO_DURATION_COLUMN = "youtube_duration";

    public static final String VIDEO_TITLE_COLUMN = "video_title";

    @DatabaseField(columnName = VIDEO_ID_COLUMN, dataType = DataType.STRING)
    private String videoID;

    @DatabaseField(columnName = THUMB_KEY_COLUMN, dataType = DataType.STRING)
    private String thumbKey;

    @DatabaseField(columnName = VIDEO_DURATION_COLUMN, dataType = DataType.INTEGER)
    private int duration;

    @DatabaseField(columnName = VIDEO_TITLE_COLUMN, dataType = DataType.STRING)
    private String videoTitle;

    public YoutubeMessage() {
        super(new Message(IMessageType.YOUTUBE));
    }

    public static Dao<YoutubeMessage, Long> getDao() {
        try {
            return DataBaseHelper.getInstance().getDao(YoutubeMessage.class);
        } catch (SQLException e) {
            LogIt.e(YoutubeMessage.class, e, e.getMessage());
        }

        return null;
    }

    @Override
    public long save(boolean updateCursor, Conversation conversation) {
        setId(message.save(updateCursor, conversation));

        try {
            Dao<YoutubeMessage, Long> dao = getDao();
            dao.createOrUpdate(this);
            return dao.extractId(this);
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    public void setVideoID(String videoID) {
        this.videoID = videoID;
    }

    public String getVideoID() {
        return this.videoID;
    }

    public void setThumbKey(String thumbKey) {
        this.thumbKey = thumbKey;
    }

    public String getThumbKey() {
        return this.thumbKey;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getDuration() {
        return this.duration;
    }

    public void setVideoTitle(String title) {
        this.videoTitle = title;
    }

    public String getVideoTitle() {
        return this.videoTitle;
    }

    @Override
    public PBCommandEnvelope serialize() {
        PBMessageYoutube.Builder messageYoutubeBuilder = PBMessageYoutube
                .newBuilder();
        PBMessageEnvelope.Builder messageEnvelopeBuilder = PBMessageEnvelope
                .newBuilder();
        PBCommandMessageNew.Builder commandMessageNewBuilder = PBCommandMessageNew
                .newBuilder();
        PBCommandEnvelope.Builder commandEnvelopeBuilder = PBCommandEnvelope
                .newBuilder();

        messageYoutubeBuilder.setSeconds(getDuration());

        if (getThumbKey() != null) {
            messageYoutubeBuilder.setThumbURL(getThumbKey());
        }

        if (getVideoID() != null) {
            messageYoutubeBuilder.setVideoID(getVideoID());
        }

        if (getVideoTitle() != null) {
            messageYoutubeBuilder.setTitle(getVideoTitle());
        }

        messageEnvelopeBuilder.setType(getType().toMessageType());
        messageEnvelopeBuilder.setCreatedAt(getCreatedAt());
        messageEnvelopeBuilder.setYoutube(messageYoutubeBuilder.build());

        commandMessageNewBuilder.setRecipientID(getChannelId());
        commandMessageNewBuilder.setMessageEnvelope(messageEnvelopeBuilder
                .build());

        commandEnvelopeBuilder.setMessageNew(commandMessageNewBuilder.build());

        return message.serializeWithEnvelopeBuilder(commandEnvelopeBuilder);
    }

    @Override
    public IMessageType getType() {
        return IMessageType.YOUTUBE;
    }

    @Override
    public boolean load() {
        boolean result = message.load();

        try {
            Dao<YoutubeMessage, Long> dao = getDao();
            YoutubeMessage youtubeMessage = dao.queryForId(getId());

            if (youtubeMessage != null) {
                setDuration(youtubeMessage.getDuration());
                setThumbKey(youtubeMessage.getThumbKey());
                setVideoID(youtubeMessage.getVideoID());
                setVideoTitle(youtubeMessage.getVideoTitle());

                return result;
            } else {
                LogIt.w(YoutubeMessage.class,
                        "Trying to load a non-existing message", getId());
            }
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return false;
    }

    @Override
    public int delete() {
        message.delete();

        try {
            Dao<YoutubeMessage, Long> dao = getDao();
            return dao.deleteById(getId());
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    public void send(MessagingService service) {
        DurableCommand durableCmd = new DurableCommand(this);
        message.durableSend(service, durableCmd);
    }

    @Override
    public void parseFrom(PBCommandEnvelope commandEnvelope) {
        message.parseFrom(commandEnvelope);
        PBMessageYoutube messageYoutube = commandEnvelope.getMessageNew()
                .getMessageEnvelope().getYoutube();

        setDuration(messageYoutube.getSeconds());
        setThumbKey(messageYoutube.getThumbURL());
        setVideoTitle(messageYoutube.getTitle());
        setVideoID(messageYoutube.getVideoID());
        setCommandId(commandEnvelope.getCommandID());
        setClientId(commandEnvelope.getClientID());
    }

    @Override
    public String getMessagePreview(Context context) {
        if (wasSentByThisUser()) {
            return String.format(context
                    .getString(R.string.convo_preview_msg_desc_self_youtube),
                    getVideoTitle());
        } else {
            return String.format(context
                    .getString(R.string.convo_preview_msg_desc_other_youtube),
                    getSender() != null ? getSender().getDisplayName() : "",
                    getVideoTitle());
        }
    }
}