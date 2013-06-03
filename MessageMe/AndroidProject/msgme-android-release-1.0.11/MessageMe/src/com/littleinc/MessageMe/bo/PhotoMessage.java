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
import com.littleinc.MessageMe.protocol.Messages.PBMessageEnvelope;
import com.littleinc.MessageMe.protocol.Messages.PBMessagePhoto;
import com.littleinc.MessageMe.protocol.Objects.PBImageBundle;

@DatabaseTable(tableName = PhotoMessage.TABLE_NAME)
public class PhotoMessage extends SingleImageMessage implements IMessage {

    public static final String TABLE_NAME = "photo_message";

    /** 
     * The database columns all need to be unique, so we use an alias for
     * the database operations as that allows us to share the code for the 
     * common columns.
     */
    public static final String DB_COLUMN_ALIAS_SUFFIX = "_photo";

    public PhotoMessage() {
        super(new Message(IMessageType.PHOTO));
    }

    public static Dao<PhotoMessage, Long> getDao() {
        try {
            return DataBaseHelper.getInstance().getDao(PhotoMessage.class);
        } catch (SQLException e) {
            LogIt.e(PhotoMessage.class, e, e.getMessage());
        }

        return null;
    }

    @Override
    public int delete() {
        message.delete();

        try {
            Dao<PhotoMessage, Long> dao = getDao();
            return dao.deleteById(getId());
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    @Override
    public IMessageType getType() {
        return IMessageType.PHOTO;
    }

    @Override
    public boolean load() {
        boolean result = message.load();

        try {
            Dao<PhotoMessage, Long> dao = getDao();
            PhotoMessage photoMessage = dao.queryForId(getId());

            if (photoMessage != null) {
                setImageBundle(photoMessage.getImageBundle());
                setImageKey(photoMessage.getImageKey());
                setThumbKey(photoMessage.getThumbKey());

                return result;
            } else {
                LogIt.w(PhotoMessage.class,
                        "Trying to load a non-existing message", getId());
            }
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return false;
    }

    public static PhotoMessage load(long commandId) {
        Message message = Message.loadByCommandId(commandId);

        if (message != null) {
            try {
                Dao<PhotoMessage, Long> dao = PhotoMessage.getDao();
                QueryBuilder<PhotoMessage, Long> queryBuilder = dao
                        .queryBuilder();
                Where<PhotoMessage, Long> where = queryBuilder.where();

                where.eq(Message.ID_COLUMN, message.getId());
                return queryBuilder.queryForFirst();
            } catch (SQLException e) {
                LogIt.e(PhotoMessage.class, e);
            }
        }

        return null;
    }

    @Override
    public void parseFrom(PBCommandEnvelope commandEnvelope) {
        message.parseFrom(commandEnvelope);
        PBMessagePhoto messagePhoto = commandEnvelope.getMessageNew()
                .getMessageEnvelope().getPhoto();

        PBImageBundle pbImageBundle = messagePhoto.getImageBundle();
        parseFrom(commandEnvelope, pbImageBundle, messagePhoto.getImageKey());
    }

    @Override
    public long save(boolean updateCursor, Conversation conversation) {
        setId(message.save(updateCursor, conversation));

        try {
            Dao<PhotoMessage, Long> dao = getDao();
            dao.createOrUpdate(this);
            return dao.extractId(this);
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    @Override
    public PBCommandEnvelope serialize() {
        PBMessagePhoto.Builder messagePhotoBuilder = PBMessagePhoto
                .newBuilder();
        PBMessageEnvelope.Builder messageEnvelopeBuilder = PBMessageEnvelope
                .newBuilder();
        PBCommandMessageNew.Builder commandMessageNewBuilder = PBCommandMessageNew
                .newBuilder();
        PBCommandEnvelope.Builder commandEnvelopeBuilder = PBCommandEnvelope
                .newBuilder();

        if (getImageKey() != null) {
            messagePhotoBuilder.setImageKey(getImageKey());
        }

        // Only set the image bundle if it exists.  It will only exist after 
        // the image has been uploaded to the image server, and only then
        // will this message be sent to the server.
        if (getImageBundle() != null) {
            messagePhotoBuilder.setImageBundle(getImageBundle());
        }

        messageEnvelopeBuilder.setType(getType().toMessageType());
        messageEnvelopeBuilder.setCreatedAt(getCreatedAt());
        messageEnvelopeBuilder.setPhoto(messagePhotoBuilder.build());

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
                    .getString(R.string.convo_preview_msg_desc_self_picture);
        } else {
            return String.format(context
                    .getString(R.string.convo_preview_msg_desc_other_picture),
                    getSender() != null ? getSender().getDisplayName() : "");
        }
    }
}