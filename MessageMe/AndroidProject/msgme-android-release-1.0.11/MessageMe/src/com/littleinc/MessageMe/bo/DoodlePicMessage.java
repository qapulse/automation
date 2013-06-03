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
import com.littleinc.MessageMe.protocol.Messages.PBMessageDoodlePic;
import com.littleinc.MessageMe.protocol.Messages.PBMessageEnvelope;
import com.littleinc.MessageMe.protocol.Objects.PBImageBundle;

@DatabaseTable(tableName = DoodlePicMessage.TABLE_NAME)
public class DoodlePicMessage extends SingleImageMessage implements IMessage {

    public static final String TABLE_NAME = "doodlepic_message";

    /** 
     * The database columns all need to be unique, so we use an alias for
     * the database operations as that allows us to share the code for the 
     * common columns.
     */
    public static final String DB_COLUMN_ALIAS_SUFFIX = "_doodlepic";

    public DoodlePicMessage() {
        super(new Message(IMessageType.DOODLE_PIC));
    }

    public static Dao<DoodlePicMessage, Long> getDao() {
        try {
            return DataBaseHelper.getInstance().getDao(DoodlePicMessage.class);
        } catch (SQLException e) {
            LogIt.e(DoodlePicMessage.class, e, e.getMessage());
        }

        return null;
    }

    @Override
    public int delete() {
        message.delete();

        try {
            Dao<DoodlePicMessage, Long> dao = getDao();
            return dao.deleteById(getId());
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    @Override
    public IMessageType getType() {
        return IMessageType.DOODLE_PIC;
    }

    @Override
    public boolean load() {
        boolean result = message.load();

        try {
            Dao<DoodlePicMessage, Long> dao = getDao();
            DoodlePicMessage doodlePicMessage = dao.queryForId(getId());

            if (doodlePicMessage != null) {
                setImageBundle(doodlePicMessage.getImageBundle());
                setImageKey(doodlePicMessage.getImageKey());
                setThumbKey(doodlePicMessage.getThumbKey());

                return result;
            } else {
                LogIt.w(DoodleMessage.class,
                        "Trying to load a non-existing message", getId());
            }
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return false;
    }

    public static DoodlePicMessage loadByCommandId(long commandId) {
        Message message = Message.loadByCommandId(commandId);

        if (message != null) {
            try {
                Dao<DoodlePicMessage, Long> dao = DoodlePicMessage.getDao();
                QueryBuilder<DoodlePicMessage, Long> queryBuilder = dao
                        .queryBuilder();
                Where<DoodlePicMessage, Long> where = queryBuilder.where();

                where.eq(Message.ID_COLUMN, message.getId());
                return queryBuilder.queryForFirst();
            } catch (SQLException e) {
                LogIt.e(DoodlePicMessage.class, e);
            }
        }

        return null;
    }

    @Override
    public void parseFrom(PBCommandEnvelope commandEnvelope) {
        message.parseFrom(commandEnvelope);
        PBMessageDoodlePic messageDoodlePic = commandEnvelope.getMessageNew()
                .getMessageEnvelope().getDoodlePic();

        PBImageBundle pbImageBundle = messageDoodlePic.getImageBundle();

        parseFrom(commandEnvelope, pbImageBundle,
                messageDoodlePic.getImageKey());
    }

    @Override
    public long save(boolean updateCursor, Conversation conversation) {
        setId(message.save(updateCursor, conversation));

        try {
            Dao<DoodlePicMessage, Long> dao = getDao();
            dao.createOrUpdate(this);
            return dao.extractId(this);
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    @Override
    public PBCommandEnvelope serialize() {
        PBMessageDoodlePic.Builder messageDoodlePicBuilder = PBMessageDoodlePic
                .newBuilder();
        PBMessageEnvelope.Builder messageEnvelopeBuilder = PBMessageEnvelope
                .newBuilder();
        PBCommandMessageNew.Builder commandMessageNewBuilder = PBCommandMessageNew
                .newBuilder();
        PBCommandEnvelope.Builder commandEnvelopeBuilder = PBCommandEnvelope
                .newBuilder();

        if (getImageKey() != null) {
            messageDoodlePicBuilder.setImageKey(getImageKey());
        }

        // Only set the image bundle if it exists.  It will only exist after 
        // the image has been uploaded to the image server, and only then
        // will this message be sent to the server.
        if (getImageBundle() != null) {
            messageDoodlePicBuilder.setImageBundle(getImageBundle());
        }

        messageEnvelopeBuilder.setType(getType().toMessageType());
        messageEnvelopeBuilder.setCreatedAt(getCreatedAt());
        messageEnvelopeBuilder.setDoodlePic(messageDoodlePicBuilder.build());

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
                    .getString(R.string.convo_preview_msg_desc_self_doodlepic);
        } else {
            return String
                    .format(context
                            .getString(R.string.convo_preview_msg_desc_other_doodlepic),
                            getSender() != null ? getSender().getDisplayName()
                                    : "");
        }
    }
}