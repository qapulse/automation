package com.littleinc.MessageMe.bo;

import java.sql.SQLException;
import java.util.Comparator;

import android.content.Context;
import android.provider.SyncStateContract.Columns;

import com.coredroid.util.LogIt;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.table.DatabaseTable;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.chat.DurableCommand;
import com.littleinc.MessageMe.chat.DurableCommandSender;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.data.DataBaseHelper;
import com.littleinc.MessageMe.metrics.MMHourlyTracker;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope.CommandType;
import com.littleinc.MessageMe.protocol.Commands.PBCommandMessageNew;
import com.littleinc.MessageMe.protocol.Messages.PBMessageEnvelope;
import com.littleinc.MessageMe.util.ChatAdapter;
import com.littleinc.MessageMe.util.DateUtil;

@DatabaseTable(tableName = Message.TABLE_NAME)
public class Message implements IMessage {

    public static final String TABLE_NAME = "message";

    public static final String ID_COLUMN = Columns._ID;

    public static final String CLIENT_ID_COLUMN = "client_id";

    public static final String SENDER_ID_COLUMN = "sender_id";

    public static final String COMMAND_ID_COLUMN = "command_id";

    public static final String CHANNEL_ID_COLUMN = "channel_id";

    public static final String CREATED_AT_COLUMN = "created_at";

    public static final String SORTED_BY_COLUMN = "sorted_by";

    public static final String TYPE_COLUMN = "type";

    /**
     * Auto-increment id, all the Message sub-classes should copy this value
     */
    @DatabaseField(columnName = ID_COLUMN, generatedId = true, dataType = DataType.LONG)
    private long id;

    @DatabaseField(columnName = Message.CLIENT_ID_COLUMN, dataType = DataType.LONG)
    private long clientId;

    @DatabaseField(columnName = Message.SENDER_ID_COLUMN, canBeNull = false, dataType = DataType.LONG)
    private long senderId;

    @DatabaseField(columnName = Message.COMMAND_ID_COLUMN, canBeNull = true, dataType = DataType.LONG)
    private long commandId;

    @DatabaseField(columnName = Message.CHANNEL_ID_COLUMN, canBeNull = false, dataType = DataType.LONG)
    private long channelId;

    /** 
     * The time in seconds that the message was created on the server. 
     */
    @DatabaseField(columnName = Message.CREATED_AT_COLUMN, canBeNull = false, dataType = DataType.INTEGER)
    private int createdAt;

    /** 
     * A field used for ordering messages in the UI.  This ensures messages 
     * always appear in the same order on a particular device.  We can't
     * sort by the "createdAt" field as messages pending send do not have
     * a server creation time yet, and using the temporary client creation 
     * time can cause messages to reorder.
     * 
     * This field stores a fake timestamp simulating the "createdAt" time, in 
     * fake microseconds.  
     */
    @DatabaseField(columnName = Message.SORTED_BY_COLUMN, canBeNull = false, dataType = DataType.DOUBLE)
    private double sortedBy;

    @DatabaseField(columnName = Message.TYPE_COLUMN, canBeNull = false, dataType = DataType.ENUM_STRING)
    private IMessageType type;

    public static final String READED_COLUMN = "readed";

    /**
     * This column is now retired.  Its values will be null for all clients who
     * installed after it was retired.  SQLite does not allow us to delete the
     * column, so we just relax the constraint so its values can be null.
     */
    @DatabaseField(columnName = Message.READED_COLUMN, canBeNull = true, dataType = DataType.BOOLEAN)
    private boolean readed;

    private User sender;

    private Contact contact;

    public static Dao<Message, Long> getDao() {
        try {
            return DataBaseHelper.getInstance().getDao(Message.class);
        } catch (SQLException e) {
            LogIt.e(Message.class, e, e.getMessage());
        }

        return null;
    }

    public Message() {
    }

    public Message(IMessageType messageType) {
        type = messageType;
    }

    @Override
    public void parseFrom(PBCommandEnvelope commandEnvelope) {
        User currentUser = MessageMeApplication.getCurrentUser();
        PBCommandMessageNew commandMessageNew = commandEnvelope.getMessageNew();
        PBMessageEnvelope messageEnvelope = commandEnvelope.getMessageNew()
                .getMessageEnvelope();

        setSenderId(commandEnvelope.getUserID());

        if (currentUser.getUserId() == commandMessageNew.getRecipientID()) {
            setChannelId(commandEnvelope.getUserID());
        } else {
            setChannelId(commandMessageNew.getRecipientID());
        }

        setClientId(commandEnvelope.getClientID());
        setCommandId(commandEnvelope.getCommandID());
        setCreatedAt(messageEnvelope.getCreatedAt());
    }

    public PBCommandEnvelope serializeWithEnvelopeBuilder(
            PBCommandEnvelope.Builder builder) {

        builder.setUserID(getSenderId());
        builder.setClientID(getClientId());

        // Send -1 in the commandId can cause problems in the server
        // and also in the server responses so to avoid that we going to avoid
        // serialize this value
        if (getCommandId() != -1) {
            builder.setCommandID(getCommandId());
        }

        builder.setType(CommandType.MESSAGE_NEW);

        return builder.build();
    }

    @Override
    public int getCreatedAt() {
        return createdAt;
    }

    @Override
    public void setCreatedAt(int createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public double getSortedBy() {
        return sortedBy;
    }

    @Override
    public void setSortedBy(double sortedBy) {
        this.sortedBy = sortedBy;
    }

    @Override
    public long save(boolean updateCursor, Conversation conversation) {
        try {
            Dao<Message, Long> dao = Message.getDao();
            dao.createOrUpdate(this);

            if (updateCursor) {
                // Some of these defensive checks shouldn't necessary now that
                // we have an "updateCursor" parameter, but we leave them in 
                // anyway for extra protection
                if ((getCommandId() == 0)
                        || (getCommandId() == -1)
                        || (getChannelId() == MessageMeConstants.WELCOME_ROOM_ID)) {
                    LogIt.d(this, "Don't update cursor", getCommandId(),
                            getChannelId());
                } else if (type == IMessageType.NOTICE) {
                    // Even if we did update the cursor here it probably wouldn't 
                    // cause any problems as the room notices have the same
                    // commandId as the command that triggered them.
                    LogIt.d(this, "Don't update cursor for room notice");
                } else {
                    MessageMeCursor cursor = new MessageMeCursor(
                            getChannelId(), getCommandId());
                    cursor.update();
                }
            }

            if (conversation == null) {
                // Whether was sent by this user set it as last sent message
                // of the conversation
                conversation = Conversation.newInstance(this);
            }

            if (wasSentByThisUser()) {

                conversation.setLastSentMessage(this);

                // In the ConversationReader table will be only stored the readers of the
                // last sent message for that reason previous reader should be removed at
                // this point
                conversation.getReadUsers().removeAll(
                        conversation.getReadUsers());
            }

            conversation.setLastMessage(this);
            conversation.save();

            return dao.extractId(this);
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    @Override
    public boolean load() {
        try {
            Dao<Message, Long> dao = Message.getDao();
            Message message = dao.queryForId(getId());

            if (message != null) {
                setClientId(message.getClientId());
                setChannelId(message.getChannelId());
                setCommandId(message.getCommandId());
                setCreatedAt(message.getCreatedAt());
                setSenderId(message.getSenderId());
                setSortedBy(message.getSortedBy());

                return true;
            } else {
                LogIt.w(Message.class, "Trying to load a non-existing message",
                        getId());
            }
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return false;
    }

    public static Message loadByCommandId(long commandId) {
        LogIt.d(Message.class, "Loading message by commandId " + commandId);

        try {
            Dao<Message, Long> dao = Message.getDao();
            QueryBuilder<Message, Long> queryBuilder = dao.queryBuilder();
            Where<Message, Long> where = queryBuilder.where();

            where.eq(Message.COMMAND_ID_COLUMN, commandId);
            return queryBuilder.queryForFirst();
        } catch (SQLException e) {
            LogIt.e(Message.class, e, e.getMessage());
        }

        return null;
    }

    public static Message loadByClientId(long clientId) {
        LogIt.d(Message.class, "Loading message by clientId " + clientId);

        try {
            Dao<Message, Long> dao = Message.getDao();
            QueryBuilder<Message, Long> queryBuilder = dao.queryBuilder();
            Where<Message, Long> where = queryBuilder.where();

            where.eq(Message.CLIENT_ID_COLUMN, clientId);
            return queryBuilder.queryForFirst();
        } catch (SQLException e) {
            LogIt.e(Message.class, e, e.getMessage());
        }

        return null;
    }

    public static class SortMessages implements Comparator<IMessage> {

        @Override
        public int compare(IMessage lhs, IMessage rhs) {
            
            // We need to be careful to respect the contract that a compare
            // method must obey, which includes transitivity.
            if ((lhs == null) && (rhs == null)) {
                LogIt.w(Message.class, "Both messages are null");
                return 0;
            } else if (lhs == null) {
                LogIt.w(Message.class, "LHS message is null");
                return 1;
            } else if (rhs == null) {
                LogIt.w(Message.class, "RHS message is null");
                return -1;
            }

            return String.valueOf(lhs.getCreatedAt())
                    .compareToIgnoreCase(
                            String.valueOf(rhs.getCreatedAt()));
        }
    }

    @Override
    public int delete() {
        try {
            Dao<Message, Long> dao = Message.getDao();
            return dao.deleteById(getId());
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    @Override
    public long getClientId() {
        return clientId;
    }

    @Override
    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    @Override
    public long getCommandId() {
        return commandId;
    }

    @Override
    public void setCommandId(long commandId) {
        this.commandId = commandId;
    }

    @Override
    public long getChannelId() {
        return channelId;
    }

    @Override
    public void setChannelId(long channelId) {
        this.channelId = channelId;
        User currentUser = MessageMeApplication.getCurrentUser();

        try {
            // Optimization, If channelId is the current user we don't need to query
            // that information from the DB because it is present in the shared preferences
            if (channelId == currentUser.getUserId()) {
                setContact(currentUser);
            } else if (Room.getDao().idExists(channelId)) {
                setContact(Room.getDao().queryForId(channelId));
            } else {
                setContact(User.getDao().queryForId(channelId));
            }

            if (getContact() == null) {
                // This normal for ROOM_NEW commands
                LogIt.d(Message.class, "Channel doesn't exist yet", channelId);
            }
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }
    }

    @Override
    public long getSenderId() {
        return senderId;
    }

    @Override
    public void setSenderId(long senderId) {
        this.senderId = senderId;
        User currentUser = MessageMeApplication.getCurrentUser();

        try {
            // Optimization, If senderId is the current user we don't need to query
            // that information from the DB because it is present in the shared preferences
            if (senderId == currentUser.getUserId()) {
                setSender(currentUser);
            } else {
                setSender(User.getDao().queryForId(senderId));
            }

            if (getSender() == null) {
                LogIt.w(Message.class, "Sender doesn't exist yet", senderId);
            }
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }
    }

    @Override
    public PBCommandEnvelope serialize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public IMessageType getType() {
        return type;
    }

    public void send(MessagingService service, PBCommandEnvelope envelop) {
        try {
            setCommandId(-1);
            service.sendCommand(envelop);
            MMHourlyTracker.getInstance().abacusOnce(null, "active", "user",
                    null, null);
        } catch (Exception e) {
            setCommandId(0);
            LogIt.e(this, e, e.getMessage());
        }
    }

    public void durableSend(MessagingService service, DurableCommand durableCmd) {
        try {
            setCommandId(-1);
            service.notifyChatClient(
                    MessageMeConstants.INTENT_NOTIFY_MESSAGE_LIST, getId(),
                    getChannelId());
            service.addToDurableSendQueue(durableCmd);
        } catch (Exception e) {
            setCommandId(0);
            LogIt.e(this, e, e.getMessage());
        }
    }

    @Override
    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public static boolean isDelayed(IMessage msg) {
        int currentTimestampInSeconds = DateUtil.getCurrentTimestamp();

        int timeSinceSendInSeconds = currentTimestampInSeconds
                - msg.getCreatedAt();

        if (timeSinceSendInSeconds > DurableCommandSender.CMD_DELAYED_INTERVAL_SECONDS) {
            LogIt.d(Message.class, "Message is delayed", timeSinceSendInSeconds);
            return true;
        }

        return false;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public boolean wasSentByThisUser() {
        User currentUser = MessageMeApplication.getCurrentUser();

        if (currentUser == null) {
            LogIt.w(this, "No current user");
            return false;
        } else {
            return senderId == currentUser.getUserId();
        }
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String getMessagePreview(Context context) {
        throw new UnsupportedOperationException();
    }

    public void forward(ChatAdapter adapter, MessagingService service,
            IMessage message) {
        LogIt.d(this, "Forwarding Message");

        // The media for this message has already been uploaded, so 
        // don't upload the media file again
        DurableCommand durableCmd = new DurableCommand(message, null);

        service.addToDurableSendQueue(durableCmd);
        service.notifyChatClient(MessageMeConstants.INTENT_NOTIFY_MESSAGE_LIST,
                message.getId(), message.getChannelId());

        // Show the message in the UI
        adapter.addSendMessage(message);
        adapter.notifyDataSetChanged();
    }

    @Override
    public Message getMessage() {
        return this;
    }

    private final static int sMsgTypeCount = IMessageType.values().length;

    /**
     * Get an ID representing the UI layout for this message.  Use a  
     * combination of message type, and whether the message was sent 
     * or received (as they use different layouts).
     */
    public int getViewType() {
        if (wasSentByThisUser()) {
            return getType().ordinal();
        } else {
            return getType().ordinal() + sMsgTypeCount;
        }
    }

    /**
     * Count received messages since given commandId of an specific conversation
     */
    public static int countUnreadMessagesSince(long channelId, long commandId,
            long currentUserId) {

        try {
            Dao<Message, Long> dao = Message.getDao();
            QueryBuilder<Message, Long> queryBuilder = dao.queryBuilder();

            return (int) dao.countOf(queryBuilder.setCountOf(true).where()
                    .eq(Message.CHANNEL_ID_COLUMN, channelId).and()
                    .gt(Message.COMMAND_ID_COLUMN, commandId).and()
                    .ne(Message.SENDER_ID_COLUMN, currentUserId).prepare());
        } catch (SQLException e) {
            LogIt.e(Message.class, e);
        }

        return 0;
    }

    @Override
    public long save(boolean updateCursor) {
        return save(updateCursor, null);
    }
}