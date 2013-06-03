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
import com.littleinc.MessageMe.protocol.Commands.PBCommandMessageNew;
import com.littleinc.MessageMe.protocol.Messages.PBMessageContact;
import com.littleinc.MessageMe.protocol.Messages.PBMessageEnvelope;
import com.littleinc.MessageMe.protocol.Objects.PBUser;

@DatabaseTable(tableName = ContactMessage.TABLE_NAME)
public class ContactMessage extends AbstractMessage {

    public static final String TABLE_NAME = "contact_message";

    public static final String PB_USER_COLUMN = "pb_user";

    @DatabaseField(columnName = ContactMessage.PB_USER_COLUMN, dataType = DataType.BYTE_ARRAY)
    protected byte[] mPBUserAsBytes;

    /**
     * The PBUser object for the forwarded contact. 
     */
    protected PBUser mPBUser = null;

    protected String mDisplayName = null;

    public static Dao<ContactMessage, Long> getDao() {
        try {
            return DataBaseHelper.getInstance().getDao(ContactMessage.class);
        } catch (SQLException e) {
            LogIt.e(ContactMessage.class, e, e.getMessage());
        }

        return null;
    }

    public ContactMessage() {
        super(new Message(IMessageType.CONTACT));
    }

    public String getDisplayName() {

        if (mDisplayName == null) {
            StringBuilder name = new StringBuilder();

            PBUser pbUser = getPBUser();

            if (pbUser != null) {
                name.append(pbUser.getFirstName());
                name.append(" ");
                name.append(pbUser.getLastName());
            }

            mDisplayName = name.toString();
        }

        return mDisplayName;
    }

    public PBUser getPBUser() {

        if ((mPBUser == null) && (mPBUserAsBytes != null)) {
            try {
                mPBUser = PBUser.parseFrom(mPBUserAsBytes);
            } catch (InvalidProtocolBufferException e) {
                LogIt.e(this, e, "Error parsing PBUser from database");
            }
        }

        return mPBUser;
    }

    public void setPBUser(PBUser pbUser) {

        this.mPBUser = pbUser;
        this.mPBUserAsBytes = pbUser.toByteArray();
    }

    /**
     * Only to be used by database methods.  Other callers should use
     * {@link #setPBUser(PBUser)}
     */
    public void setPBUserAsBytes(byte[] pbUserAsBytes) {

        this.mPBUserAsBytes = pbUserAsBytes;
    }

    @Override
    public long save(boolean updateCursor, Conversation conversation) {
        setId(message.save(updateCursor, conversation));

        // Forwarded ContactMessages may contain unknown users
        // so we should verify if that contact is already in our
        // local db and if not here is a good point to add it
        try {
            if (mPBUser != null && !User.getDao().idExists(mPBUser.getUserID())) {

                LogIt.d(ContactMessage.class, "saving unknown user",
                        mPBUser.getUserID());
                User unknownUser = User.parseFrom(mPBUser);
                unknownUser.save();
            }

            Dao<ContactMessage, Long> dao = getDao();
            dao.createOrUpdate(this);
            return dao.extractId(this);
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    @Override
    public PBCommandEnvelope serialize() {
        PBMessageContact.Builder messageContactBuilder = PBMessageContact
                .newBuilder();
        PBMessageEnvelope.Builder messageEnvelopeBuilder = PBMessageEnvelope
                .newBuilder();
        PBCommandMessageNew.Builder commandMessageNewBuilder = PBCommandMessageNew
                .newBuilder();
        PBCommandEnvelope.Builder commandEnvelopeBuilder = PBCommandEnvelope
                .newBuilder();

        messageContactBuilder.setUser(getPBUser());

        messageEnvelopeBuilder.setType(getType().toMessageType());
        messageEnvelopeBuilder.setCreatedAt(getCreatedAt());
        messageEnvelopeBuilder.setContact(messageContactBuilder.build());

        commandMessageNewBuilder.setRecipientID(getChannelId());
        commandMessageNewBuilder.setMessageEnvelope(messageEnvelopeBuilder
                .build());

        commandEnvelopeBuilder.setMessageNew(commandMessageNewBuilder.build());

        return message.serializeWithEnvelopeBuilder(commandEnvelopeBuilder);
    }

    @Override
    public IMessageType getType() {
        return IMessageType.CONTACT;
    }

    @Override
    public boolean load() {
        boolean result = message.load();

        try {
            Dao<ContactMessage, Long> dao = getDao();
            ContactMessage msg = dao.queryForId(getId());

            if (msg != null) {
                setPBUser(msg.getPBUser());
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

    @Override
    public int delete() {
        message.delete();

        try {
            Dao<ContactMessage, Long> dao = getDao();
            return dao.deleteById(getId());
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    @Override
    public void parseFrom(PBCommandEnvelope commandEnvelope) {
        message.parseFrom(commandEnvelope);

        PBMessageContact msg = commandEnvelope.getMessageNew()
                .getMessageEnvelope().getContact();

        setPBUser(msg.getUser());
        setCommandId(commandEnvelope.getCommandID());
        setClientId(commandEnvelope.getClientID());
    }

    @Override
    public String getMessagePreview(Context context) {
        if (wasSentByThisUser()) {
            return String.format(context
                    .getString(R.string.convo_preview_msg_desc_self_contact),
                    getDisplayName());
        } else {
            return String.format(context
                    .getString(R.string.convo_preview_msg_desc_other_contact),
                    getSender() != null ? getSender().getDisplayName() : "",
                    getDisplayName());
        }
    }
}