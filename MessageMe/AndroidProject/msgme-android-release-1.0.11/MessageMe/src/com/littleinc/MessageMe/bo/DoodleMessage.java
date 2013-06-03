package com.littleinc.MessageMe.bo;

import java.sql.SQLException;

import android.content.Context;

import com.coredroid.util.LogIt;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.table.DatabaseTable;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.data.DataBaseHelper;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandMessageNew;
import com.littleinc.MessageMe.protocol.Messages.PBMessageDoodle;
import com.littleinc.MessageMe.protocol.Messages.PBMessageEnvelope;
import com.littleinc.MessageMe.protocol.Objects.PBImageBundle;

@DatabaseTable(tableName = DoodleMessage.TABLE_NAME)
public class DoodleMessage extends SingleImageMessage {

    public static final String TABLE_NAME = "doodle_message";

    /** 
     * The database columns all need to be unique, so we use an alias for
     * the database operations as that allows us to share the code for the 
     * common columns.
     */
    public static final String DB_COLUMN_ALIAS_SUFFIX = "_doodle";

    public DoodleMessage() {
        super(new Message(IMessageType.DOODLE));
    }

    public static Dao<DoodleMessage, Long> getDao() {
        try {
            return DataBaseHelper.getInstance().getDao(DoodleMessage.class);
        } catch (SQLException e) {
            LogIt.e(DoodleMessage.class, e, e.getMessage());
        }

        return null;
    }

    @Override
    public int delete() {
        message.delete();

        try {
            Dao<DoodleMessage, Long> dao = getDao();
            return dao.deleteById(getId());
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    @Override
    public IMessageType getType() {
        return IMessageType.DOODLE;
    }

    @Override
    public boolean load() {
        boolean result = message.load();

        try {
            Dao<DoodleMessage, Long> dao = getDao();
            DoodleMessage doodleMessage = dao.queryForId(getId());

            if (doodleMessage != null) {
                setImageBundle(doodleMessage.getImageBundle());
                setImageKey(doodleMessage.getImageKey());
                setThumbKey(doodleMessage.getThumbKey());

                return result;
            } else {
                LogIt.w(Message.class, "Trying to load a non-existing message",
                        getId());
            }
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return false;
    }

    public static DoodleMessage loadByCommandId(long commandId) {
        Message message = Message.loadByCommandId(commandId);

        if (message != null) {
            try {
                Dao<DoodleMessage, Long> dao = DoodleMessage.getDao();
                QueryBuilder<DoodleMessage, Long> queryBuilder = dao
                        .queryBuilder();
                Where<DoodleMessage, Long> where = queryBuilder.where();

                where.eq(Message.ID_COLUMN, message.getId());
                return queryBuilder.queryForFirst();
            } catch (SQLException e) {
                LogIt.e(DoodleMessage.class, e);
            }
        }

        return null;
    }

    @Override
    public void parseFrom(PBCommandEnvelope commandEnvelope) {
        message.parseFrom(commandEnvelope);
        PBMessageDoodle messageDoodle = commandEnvelope.getMessageNew()
                .getMessageEnvelope().getDoodle();

        PBImageBundle pbImageBundle = messageDoodle.getImageBundle();
        parseFrom(commandEnvelope, pbImageBundle, messageDoodle.getImageKey());
    }

    @Override
    public long save(boolean updateCursor, Conversation conversation) {
        setId(message.save(updateCursor, conversation));

        try {
            Dao<DoodleMessage, Long> dao = getDao();
            dao.createOrUpdate(this);
            return dao.extractId(this);
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    @Override
    public PBCommandEnvelope serialize() {
        PBMessageDoodle.Builder messageDoodleBuilder = PBMessageDoodle
                .newBuilder();
        PBMessageEnvelope.Builder messageEnvelopeBuilder = PBMessageEnvelope
                .newBuilder();
        PBCommandMessageNew.Builder commandMessageNewBuilder = PBCommandMessageNew
                .newBuilder();
        PBCommandEnvelope.Builder commandEnvelopeBuilder = PBCommandEnvelope
                .newBuilder();

        if (getImageKey() != null) {
            messageDoodleBuilder.setImageKey(getImageKey());
        }

        // Only set the image bundle if it exists.  It will only exist after 
        // the image has been uploaded to the image server, and only then
        // will this message be sent to the server.
        if (getImageBundle() != null) {
            messageDoodleBuilder.setImageBundle(getImageBundle());
        }

        messageEnvelopeBuilder.setType(getType().toMessageType());
        messageEnvelopeBuilder.setCreatedAt(getCreatedAt());
        messageEnvelopeBuilder.setDoodle(messageDoodleBuilder.build());

        commandMessageNewBuilder.setRecipientID(getChannelId());
        commandMessageNewBuilder.setMessageEnvelope(messageEnvelopeBuilder
                .build());

        commandEnvelopeBuilder.setMessageNew(commandMessageNewBuilder.build());

        return message.serializeWithEnvelopeBuilder(commandEnvelopeBuilder);
    }

    @Override
    public String getMessagePreview(Context context) {
        if (wasSentByThisUser()) {
            return context
                    .getString(R.string.convo_preview_msg_desc_self_doodle);
        } else {
            return String.format(context
                    .getString(R.string.convo_preview_msg_desc_other_doodle),
                    getSender() != null ? getSender().getDisplayName() : "");
        }
    }
}