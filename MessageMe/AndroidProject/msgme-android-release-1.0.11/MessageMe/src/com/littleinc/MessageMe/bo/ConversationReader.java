package com.littleinc.MessageMe.bo;

import java.sql.SQLException;

import android.provider.SyncStateContract.Columns;

import com.coredroid.util.LogIt;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.error.UserNotFoundException;

@DatabaseTable(tableName = ConversationReader.TABLE_NAME)
public class ConversationReader {

    public static final String ID_COLUMN = Columns._ID;

    public static final String USER_ID_COLUMN = "user_id";

    public static final String TABLE_NAME = "conversation_reader";

    public static final String CONVERSATION_ID_COLUMN = "conversation_id";

    @DatabaseField(columnName = ID_COLUMN, generatedId = true, dataType = DataType.LONG)
    private long mId;

    /** 
     * This combination foreign = true, foreignAutoRefresh = true
     * will save on table only the user id and when a query to the
     * ConversationReader table is executed ORM will load the user object automatically
     */
    @DatabaseField(columnName = USER_ID_COLUMN, foreign = true, foreignAutoRefresh = true, uniqueCombo = true)
    private User mUser;

    /** 
     * This combination foreign = true, foreignAutoRefresh = true
     * will save on table only the conversation id and when a query to the
     * ConversationReader table is executed ORM will load the conversation object automatically
     */
    @DatabaseField(columnName = CONVERSATION_ID_COLUMN, foreign = true, foreignAutoRefresh = true, uniqueCombo = true)
    private Conversation mConversation;

    public ConversationReader() {
    }

    public ConversationReader(Conversation conversation, long readerId)
            throws UserNotFoundException {

        mConversation = conversation;
        try {
            mUser = User.getDao().queryForId(readerId);
            
            if (mUser == null) {
                LogIt.e(ConversationReader.class, "Reader does not exist",
                        readerId);
                throw new UserNotFoundException(conversation.getChannelId(),
                        readerId);                
            }
        } catch (SQLException e) {
            LogIt.e(ConversationReader.class, e, "SQLException loading reader",
                    readerId);
        }
    }

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        this.mId = id;
    }

    public User getUser() {
        return mUser;
    }

    public void setUser(User user) {
        this.mUser = user;
    }

    public Conversation getConversation() {
        return mConversation;
    }

    public void setConversation(Conversation conversation) {
        this.mConversation = conversation;
    }

    @Override
    public int hashCode() {

        if (this == null) {
            return 0;
        } else {

            // Start with a non-zero constant.
            int result = 17;

            // Include a hash for each field.
            result = 31 * result
                    + (mConversation == null ? 0 : mConversation.hashCode());
            result = 31 * result + (mUser == null ? 0 : mUser.hashCode());

            return result;
        }
    }

    @Override
    public boolean equals(Object o) {

        if (this == null) {
            return false;
        }

        // Return true if the objects are identical.
        // (This is just an optimization, not required for correctness.)
        if (this == o) {
            return true;
        }

        // Return false if the other object has the wrong type.
        // This type may be an interface depending on the interface's specification.
        if (!(o instanceof ConversationReader)) {
            return false;
        }

        // Cast to the appropriate type.
        // This will succeed because of the instanceof, and lets us access private fields.
        ConversationReader lhs = (ConversationReader) o;

        // Check each field. Primitive fields, reference fields, and nullable reference
        // fields are all treated differently.
        boolean result = true;
        
        if (mConversation == null) {
            result = lhs.mConversation == null;
        } else if (lhs.mConversation == null) {
            result = false;
        } else {
            result = mConversation.getChannelId() == lhs.mConversation
                    .getChannelId();
        }
 
        if (mUser == null) {
            result = result && lhs.mUser == null;
        } else if (lhs.mUser == null) {
            result = false;
        } else {
            result = result && mUser.getUserId() == lhs.mUser.getUserId();
        }
 
        return result;
    }
}
