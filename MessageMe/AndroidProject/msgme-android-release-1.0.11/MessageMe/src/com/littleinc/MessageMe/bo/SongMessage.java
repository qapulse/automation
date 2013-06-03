package com.littleinc.MessageMe.bo;

import java.sql.SQLException;

import android.content.Context;

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
import com.littleinc.MessageMe.protocol.Messages.PBMessageSong;

@DatabaseTable(tableName = SongMessage.TABLE_NAME)
public class SongMessage extends AbstractMessage {

    public static final String TABLE_NAME = "song_message";

    public static final String TRACK_NAME_COLUMN = "track_name";

    public static final String ARTIST_NAME_COLUMN = "artist_name";

    public static final String ARTWORK_URL_COLUMN = "artwork_url";

    public static final String PREVIEW_URL = "preview_url";

    public static final String TRACK_URL_COLUMN = "track_url";

    @DatabaseField(columnName = TRACK_NAME_COLUMN, dataType = DataType.STRING)
    private String trackName;

    @DatabaseField(columnName = ARTIST_NAME_COLUMN, dataType = DataType.STRING)
    private String artistName;

    @DatabaseField(columnName = ARTWORK_URL_COLUMN, dataType = DataType.STRING)
    private String artworkUrl;

    @DatabaseField(columnName = PREVIEW_URL, dataType = DataType.STRING)
    private String previewUrl;

    @DatabaseField(columnName = TRACK_URL_COLUMN, dataType = DataType.STRING)
    private String trackUrl;

    public SongMessage() {
        super(new Message(IMessageType.SONG));
    }

    public static Dao<SongMessage, Long> getDao() {
        try {
            return DataBaseHelper.getInstance().getDao(SongMessage.class);
        } catch (SQLException e) {
            LogIt.e(SongMessage.class, e, e.getMessage());
        }

        return null;
    }

    @Override
    public long save(boolean updateCursor, Conversation conversation) {
        setId(message.save(updateCursor, conversation));

        try {
            Dao<SongMessage, Long> dao = getDao();
            dao.createOrUpdate(this);
            return dao.extractId(this);
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    @Override
    public PBCommandEnvelope serialize() {
        PBMessageSong.Builder messageSongBuilder = PBMessageSong.newBuilder();
        PBMessageEnvelope.Builder messageEnvelopeBuilder = PBMessageEnvelope
                .newBuilder();
        PBCommandMessageNew.Builder commandMessageNewBuilder = PBCommandMessageNew
                .newBuilder();
        PBCommandEnvelope.Builder commandEnvelopeBuilder = PBCommandEnvelope
                .newBuilder();

        if (getArtistName() != null) {
            messageSongBuilder.setArtistName(getArtistName());
        }

        if (getArtworkUrl() != null) {
            messageSongBuilder.setArtworkURL(getArtworkUrl());
        }

        if (getPreviewUrl() != null) {
            messageSongBuilder.setPreviewURL(getPreviewUrl());
        }

        if (getTrackName() != null) {
            messageSongBuilder.setTrackName(getTrackName());
        }

        if (getTrackUrl() != null) {
            messageSongBuilder.setTrackURL(getTrackUrl());
        }

        messageEnvelopeBuilder.setType(getType().toMessageType());
        messageEnvelopeBuilder.setCreatedAt(getCreatedAt());
        messageEnvelopeBuilder.setSong(messageSongBuilder.build());

        commandMessageNewBuilder.setRecipientID(getChannelId());
        commandMessageNewBuilder.setMessageEnvelope(messageEnvelopeBuilder
                .build());

        commandEnvelopeBuilder.setMessageNew(commandMessageNewBuilder.build());

        return message.serializeWithEnvelopeBuilder(commandEnvelopeBuilder);
    }

    @Override
    public IMessageType getType() {
        return IMessageType.SONG;
    }

    public String getTrackName() {
        return trackName;
    }

    public void setTrackName(String trackName) {
        this.trackName = trackName;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getArtworkUrl() {
        return artworkUrl;
    }

    public void setArtworkUrl(String artworkUrl) {
        this.artworkUrl = artworkUrl;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    public String getTrackUrl() {
        return trackUrl;
    }

    public void setTrackUrl(String trackUrl) {
        this.trackUrl = trackUrl;
    }

    @Override
    public boolean load() {
        boolean result = message.load();

        try {
            Dao<SongMessage, Long> dao = getDao();
            SongMessage songMessage = dao.queryForId(getId());

            if (songMessage != null) {
                setArtistName(songMessage.getArtistName());
                setArtworkUrl(songMessage.getArtworkUrl());
                setPreviewUrl(songMessage.getPreviewUrl());
                setTrackName(songMessage.getTrackName());
                setTrackUrl(songMessage.getTrackUrl());

                return result;
            } else {
                LogIt.w(NoticeMessage.class,
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
            Dao<SongMessage, Long> dao = getDao();
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
        PBMessageSong messageSong = commandEnvelope.getMessageNew()
                .getMessageEnvelope().getSong();

        setArtistName(messageSong.getArtistName());
        setArtworkUrl(messageSong.getArtworkURL());
        setPreviewUrl(messageSong.getPreviewURL());
        setTrackName(messageSong.getTrackName());
        setTrackUrl(messageSong.getTrackURL());
        setCommandId(commandEnvelope.getCommandID());
        setClientId(commandEnvelope.getClientID());
    }

    @Override
    public String getMessagePreview(Context context) {
        if (wasSentByThisUser()) {
            return String.format(context
                    .getString(R.string.convo_preview_msg_desc_self_song),
                    getTrackName());
        } else {
            return String.format(context
                    .getString(R.string.convo_preview_msg_desc_other_song),
                    getSender() != null ? getSender().getDisplayName() : "",
                    getTrackName());
        }
    }
}