package com.littleinc.MessageMe.bo;

import java.sql.SQLException;

import android.content.Context;

import com.coredroid.util.LogIt;
import com.google.protobuf.InvalidProtocolBufferException;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.data.DataBaseHelper;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;

@DatabaseTable(tableName = UnsupportedMessage.TABLE_NAME)
public class UnsupportedMessage extends AbstractMessage {

    public static final String TABLE_NAME = "unsupported_message";

    /**
     * This column is now unused, but is left as it already exists in production
     * clients. 
     */
    public static final String PB_MESSAGE_TYPE_COLUMN = "unsupported_pb_message_type";

    /**
     * This field is unused as the Protobuf MessageType does not contain the
     * value of any currently unknown message types.  Instead we need to 
     * reparse the whole PBCommandEnvelope once the client is upgraded to 
     * support the new type.
     */
    @DatabaseField(columnName = PB_MESSAGE_TYPE_COLUMN, dataType = DataType.INTEGER)
    private int mPBMsgType;

    public static final String PB_COMMAND_ENVELOPE_COLUMN = "pb_cmd_envelope";

    @DatabaseField(columnName = UnsupportedMessage.PB_COMMAND_ENVELOPE_COLUMN, dataType = DataType.BYTE_ARRAY)
    protected byte[] mPBCmdEnvelopeAsBytes;

    /**
     * The ORM layer requires a public constructor with no arguments.
     */
    public UnsupportedMessage() {
        super(new Message(IMessageType.UNSUPPORTED));
    }

    public static Dao<UnsupportedMessage, Long> getDao() {
        try {
            return DataBaseHelper.getInstance()
                    .getDao(UnsupportedMessage.class);
        } catch (SQLException e) {
            LogIt.e(UnsupportedMessage.class, e, e.getMessage());
        }

        return null;
    }

    public PBCommandEnvelope getPBCmdEnvelopeData() {
        try {
            return PBCommandEnvelope.parseFrom(mPBCmdEnvelopeAsBytes);
        } catch (InvalidProtocolBufferException e) {
            LogIt.e(this, e, "Error parsing PBCommandEnvelope from database");
        }
        return null;
    }

    public void setPBCmdEnvelopeData(byte[] pbCmdEnvelopeAsBytes) {
        mPBCmdEnvelopeAsBytes = pbCmdEnvelopeAsBytes;
    }

    @Override
    public long save(boolean updateCursor, Conversation conversation) {
        setId(message.save(updateCursor, conversation));

        try {
            Dao<UnsupportedMessage, Long> dao = getDao();
            dao.createOrUpdate(this);
            return dao.extractId(this);
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    @Override
    public PBCommandEnvelope serialize() {
        throw new UnsupportedOperationException(
                "Client should never try to send an UnsupportedMessage");
    }

    @Override
    public IMessageType getType() {
        return IMessageType.UNSUPPORTED;
    }

    @Override
    public boolean load() {
        boolean result = message.load();

        try {
            Dao<UnsupportedMessage, Long> dao = getDao();
            UnsupportedMessage msg = dao.queryForId(getId());

            if (msg != null) {
                setPBCmdEnvelopeData(msg.getPBCmdEnvelopeData().toByteArray());

                return result;
            } else {
                LogIt.w(UnsupportedMessage.class,
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
            Dao<UnsupportedMessage, Long> dao = getDao();
            return dao.deleteById(getId());
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    @Override
    public void parseFrom(PBCommandEnvelope commandEnvelope) {
        message.parseFrom(commandEnvelope);

        setClientId(commandEnvelope.getClientID());
        setCommandId(commandEnvelope.getCommandID());

        setPBCmdEnvelopeData(commandEnvelope.toByteArray());

        LogIt.d(this, "Parsed UnsupportedMessage");
    }

    @Override
    public String getMessagePreview(Context context) {
        if (wasSentByThisUser()) {
            return context
                    .getString(R.string.convo_preview_msg_desc_self_unknown);
        } else {
            return String.format(context
                    .getString(R.string.convo_preview_msg_desc_other_unknown),
                    getSender() != null ? getSender().getDisplayName() : "");
        }
    }
}