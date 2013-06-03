package com.littleinc.MessageMe.bo;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import android.database.Cursor;

import com.coredroid.util.LogIt;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.table.DatabaseTable;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.chat.LocalMessages;
import com.littleinc.MessageMe.data.DataBaseHelper;
import com.littleinc.MessageMe.protocol.Commands.PBCommandRoomNew;
import com.littleinc.MessageMe.protocol.Objects.PBRoom;
import com.littleinc.MessageMe.protocol.Objects.PBUser;
import com.littleinc.MessageMe.util.BatchTask;
import com.littleinc.MessageMe.util.DatabaseTask;

@DatabaseTable(tableName = Conversation.TABLE_NAME)
public class Conversation {

    public static final String READ_USERS = "read_users";

    public static final String TABLE_NAME = "conversation";

    public static final String READ_AT_COLUMN = "read_at";

    public static final String IS_SHOWN_COLUMN = "is_shown";

    public static final String SORTED_BY_COLUMN = "sorted_by";

    public static final String CHANNEL_ID_COLUMN = "channel_id";

    public static final String UNREAD_COUNT_COLUMN = "unread_count";

    public static final String LAST_MESSAGE_ID_COLUMN = "last_message_id";

    public static final String LAST_SENT_MESSAGE_ID_COLUMN = "last_sent_message_id";

    private static Dao<Conversation, Long> sDao;

    @DatabaseField(columnName = CHANNEL_ID_COLUMN, id = true, dataType = DataType.LONG)
    private long mChannelId;

    @DatabaseField(columnName = READ_AT_COLUMN, canBeNull = false, dataType = DataType.INTEGER)
    private int mReadAt;

    @DatabaseField(columnName = SORTED_BY_COLUMN, canBeNull = false, dataType = DataType.DOUBLE)
    private double mSortedBy;

    @DatabaseField(columnName = UNREAD_COUNT_COLUMN, canBeNull = false, dataType = DataType.INTEGER)
    private int mUnreadCount;

    @DatabaseField(columnName = IS_SHOWN_COLUMN, canBeNull = false, dataType = DataType.BOOLEAN)
    private boolean mIsShown;

    /** 
     * This combination foreign = true, foreignAutoRefresh = true
     * will save on table only the message id and when a query to the
     * Conversation table is executed ORM will load the message object automatically
     */
    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = LAST_MESSAGE_ID_COLUMN)
    private Message mLastMessage;

    private IMessage mLastIMessage = null;

    /** 
     * This combination foreign = true, foreignAutoRefresh = true
     * will save on table only the message id and when a query to the
     * Conversation table is executed ORM will load the message object automatically
     */
    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = LAST_SENT_MESSAGE_ID_COLUMN)
    private Message mLastSentMessage;

    /**
     * There are two different types of foreign collections: eager or lazy. 
     * Eager true will do a separate query immediately and the results are stored as a list within the collection.
     */
    @ForeignCollectionField(eager = true, columnName = READ_USERS)
    private ForeignCollection<ConversationReader> mReadUsers;

    public Conversation() {

        try {
            mReadUsers = getDao().getEmptyForeignCollection(READ_USERS);
        } catch (SQLException e) {
            LogIt.e(Conversation.class,
                    "Unable to create empty read users ForeignCollection");
        }
    }

    public Conversation(Cursor cursor) {

        if (cursor.getColumnIndex(Message.CHANNEL_ID_COLUMN) != -1) {

            setChannelId(cursor.getLong(cursor
                    .getColumnIndex(Message.CHANNEL_ID_COLUMN)));
        }

        if (cursor.getColumnIndex(Message.ID_COLUMN) != -1) {

            long messageId = (cursor.getLong(cursor
                    .getColumnIndex(Message.ID_COLUMN)));

            Message lastMessage = new Message();
            lastMessage.setId(messageId);

            setLastMessage(lastMessage);
        }

        if (cursor.getColumnIndex(Message.SORTED_BY_COLUMN) != -1) {
            setSortedBy(cursor.getDouble(cursor
                    .getColumnIndex(Message.SORTED_BY_COLUMN)));
        }

        if (cursor.getColumnIndex(Conversation.UNREAD_COUNT_COLUMN) != -1) {

            setUnreadCount(cursor.getInt(cursor
                    .getColumnIndex(Conversation.UNREAD_COUNT_COLUMN)));
        }

        try {
            mReadUsers = getDao().getEmptyForeignCollection(READ_USERS);
        } catch (SQLException e) {
            LogIt.e(Conversation.class,
                    "Unable to create empty read users ForeignCollection");
        }
    }

    /**
     * Factory method to create a new Conversation or load an existing one from
     * database
     * 
     * IMPORTANT don't call this method outside of a {@link DatabaseTask} or {@link BatchTask}
     */
    public static Conversation newInstance(long channelId) {

        Conversation newInstance = null;

        try {
            newInstance = getDao().queryForId(channelId);
        } catch (SQLException e) {
            LogIt.e(Conversation.class, e);
        }

        if (newInstance == null) {
            newInstance = new Conversation();

            // The Conversation is not shown until it contains at least one
            // message (by calling setLastMessage).
            newInstance.setShown(false);
            newInstance.setChannelId(channelId);
        }

        return newInstance;
    }

    /**
     * Factory method to create a new Conversation or load an existing one from
     * database and set the lastMessage and sortedBy properties
     * 
     * IMPORTANT don't call this method outside of a {@link DatabaseTask} or {@link BatchTask}
     */
    public static Conversation newInstance(IMessage iMessage) {

        Conversation newInstance = newInstance(iMessage.getChannelId());

        newInstance.setLastMessage(iMessage.getMessage());
        newInstance.setSortedBy(iMessage.getSortedBy());

        return newInstance;
    }

    /**
     * Factory method to create a new Conversation or load an existing one from
     * database depending on the channel ID inside of the {@link PBCommandRoomNew}
     * 
     * IMPORTANT don't call this method outside of a {@link DatabaseTask} or {@link BatchTask}
     */
    public static Conversation newInstance(PBCommandRoomNew commandRoomNew,
            double nextSortedBy) {

        Conversation newInstance = null;
        PBRoom room = commandRoomNew.getRoom();

        if (room.hasRoomID()) {
            newInstance = newInstance(room.getRoomID());
        } else {

            User currentUser = MessageMeApplication.getCurrentUser();

            if (currentUser != null) {
                for (PBUser user : room.getUsersList()) {
                    if (user.getUserID() != currentUser.getContactId()) {
                        newInstance = newInstance(user.getUserID());
                        break;
                    }
                }
            } else {
                LogIt.w(Conversation.class, "Current user is null");
            }
        }

        newInstance.setSortedBy(nextSortedBy);

        return newInstance;
    }

    public void save() {

        try {
            getDao().createOrUpdate(this);
        } catch (SQLException e) {
            LogIt.e(Conversation.class, e);
        }
    }

    public int delete() {

        try {
            return getDao().delete(this);
        } catch (SQLException e) {
            LogIt.e(Conversation.class, e);
        }

        return 0;
    }

    public static int delete(long conversationId) {

        try {
            return getDao().deleteById(conversationId);
        } catch (SQLException e) {
            LogIt.e(Conversation.class, e);
        }

        return 0;
    }

    public static Dao<Conversation, Long> getDao() {

        if (sDao == null) {
            try {
                sDao = DataBaseHelper.getInstance().getDao(Conversation.class);

                // Check here for more information about ORMLite cache documentation
                // http://ormlite.com/javadoc/ormlite-core/doc-files/ormlite_5.html#Object-Caches
                sDao.setObjectCache(true);

            } catch (SQLException e) {
                LogIt.e(Conversation.class, e, e.getMessage());
            }
        }

        return sDao;
    }

    public static List<Conversation> getVisibleConversations() {
        List<Conversation> conversations = new ArrayList<Conversation>();

        try {
            Dao<Conversation, Long> dao = getDao();

            QueryBuilder<Conversation, Long> queryBuilder = dao.queryBuilder();
            Where<Conversation, Long> where = queryBuilder.where();

            where.eq(Conversation.IS_SHOWN_COLUMN, true);
            queryBuilder.orderBy(Conversation.SORTED_BY_COLUMN, false);

            conversations.addAll(queryBuilder.query());
        } catch (SQLException e) {
            LogIt.e(Room.class, e, e.getMessage());
        }

        return conversations;
    }

    public long getChannelId() {
        return mChannelId;
    }

    public void setChannelId(long channelId) {
        this.mChannelId = channelId;
    }

    public boolean isCachedIMessageAvailable() {
        return (mLastIMessage != null);
    }

    /**
     * @return a minimal message suitable for displaying a summary in the list
     */
    public IMessage getLastMessageForMessagesList() {
        if (mLastIMessage == null) {
            if (mLastMessage == null) {
                LogIt.e(this, "Last message is null, this should never happen");
            } else {
                mLastIMessage = LocalMessages.getMessageByID(mLastMessage
                        .getId());
            }
        }

        return mLastIMessage;
    }

    public Message getLastMessage() {
        return mLastMessage;
    }

    public void updateLastMessage(IMessage lastMessage) {
        setLastMessage(lastMessage.getMessage());
        setSortedBy(lastMessage.getSortedBy());

        mLastIMessage = lastMessage;
    }

    public void setLastMessage(Message lastMessage) {
        this.mLastMessage = lastMessage;

        // Remove any cached IMessage
        mLastIMessage = null;

        // If last message is different to null this
        // conversation should be shown
        mIsShown = mLastMessage != null;
    }

    public Message getLastSentMessage() {
        return mLastSentMessage;
    }

    public void setLastSentMessage(Message lastSentMessage) {
        this.mLastSentMessage = lastSentMessage;
    }

    public int getReadAt() {
        return mReadAt;
    }

    public void setReadAt(int readAt) {
        this.mReadAt = readAt;
    }

    public double getSortedBy() {
        return mSortedBy;
    }

    public void setSortedBy(double sortedBy) {
        this.mSortedBy = sortedBy;
    }

    public int getUnreadCount() {
        return mUnreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.mUnreadCount = unreadCount;
    }

    public boolean isShown() {
        return mIsShown;
    }

    public void setShown(boolean isShown) {
        this.mIsShown = isShown;
    }

    public void removeUnreadMessages(int messagestoRemove) {
        if ((mUnreadCount - messagestoRemove) < 0) {
            LogIt.w(this, "Unread count less than zero, set to zero",
                    mUnreadCount, messagestoRemove);
            this.mUnreadCount = 0;
        } else {
            this.mUnreadCount = mUnreadCount - messagestoRemove;
        }
    }

    public void incrementUnreadCount() {
        mUnreadCount++;
    }

    public ForeignCollection<ConversationReader> getReadUsers() {
        return mReadUsers;
    }

    public static class SortConversation implements Comparator<Conversation> {

        @Override
        public int compare(Conversation lhs, Conversation rhs) {

            // We need to be careful to respect the contract that a compare
            // method must obey, which includes transitivity.
            //
            // These are unexpected cases but it is best not to crash when they
            // occur.
            if ((lhs == null) && (rhs == null)) {
                LogIt.e(Conversation.class, "Both conversations are null");
                return 0;
            } else if (lhs == null) {
                LogIt.e(Conversation.class, "LHS conversation is null");
                return 1;
            } else if (rhs == null) {
                LogIt.e(Conversation.class, "RHS conversation is null");
                return -1;
            } else if (lhs.getLastMessage() == null) {
                LogIt.e(Conversation.class, "Last message LHS is null",
                        lhs.getLastMessage(), lhs.getChannelId());
                return 1;
            } else if (rhs.getLastMessage() == null) {
                LogIt.e(Conversation.class, "Last message RHS is null",
                        rhs.getLastMessage(), rhs.getChannelId());
                return -1;
            }

            int leftCreatedAt = lhs.getLastMessage().getCreatedAt();
            int rightCreatedAt = rhs.getLastMessage().getCreatedAt();

            if (leftCreatedAt == rightCreatedAt) {
                return 0;
            } else if (leftCreatedAt < rightCreatedAt) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj == this) {
            return true;
        } else if (obj.getClass() != getClass()) {
            return false;
        }

        Conversation conversation = (Conversation) obj;

        return (mChannelId == conversation.getChannelId());
    }

    @Override
    public String toString() {
        return "Conversation " + mChannelId + " (" + mUnreadCount + " unread)";
    }
}
