package com.littleinc.MessageMe.bo;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import com.coredroid.util.LogIt;
import com.crittercism.app.Crittercism;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.table.DatabaseTable;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.data.DataBaseHelper;
import com.littleinc.MessageMe.error.InvalidCommandIdException;
import com.littleinc.MessageMe.util.DateUtil;

@DatabaseTable(tableName = MessageMeCursor.TABLE_NAME)
public class MessageMeCursor {

    public static final String TABLE_NAME = "cursor";

    public static final String CHANNEL_ID = "channel_id";

    public static final String LAST_COMMAND_ID = "last_command_id";

    public static final String IS_ENABLED = "is_enabled";

    public static final String DATE_MODIFIED = "date_modified";

    @DatabaseField(columnName = MessageMeCursor.CHANNEL_ID, id = true, dataType = DataType.LONG)
    private long channelId;

    @DatabaseField(columnName = MessageMeCursor.LAST_COMMAND_ID, canBeNull = true, dataType = DataType.LONG)
    private long lastCommandId;

    /**
     * By default all cursors are active use {@link MessageMeCursor#disable()} to disable an
     * specific cursor
     */
    @DatabaseField(columnName = MessageMeCursor.IS_ENABLED, canBeNull = false, dataType = DataType.BOOLEAN)
    private boolean isEnabled = true;

    @DatabaseField(columnName = MessageMeCursor.DATE_MODIFIED, canBeNull = false, dataType = DataType.LONG)
    private long dateModified;

    public static Dao<MessageMeCursor, Long> getDao() {
        try {
            return DataBaseHelper.getInstance().getDao(MessageMeCursor.class);
        } catch (SQLException e) {
            LogIt.e(MessageMeCursor.class, e, e.getMessage());
        }

        return null;
    }

    public MessageMeCursor() {
    }

    public MessageMeCursor(long channelId) {
        this.channelId = channelId;
    }

    public MessageMeCursor(long channelId, long lastCommandId) {
        this.channelId = channelId;
        this.lastCommandId = lastCommandId;
    }

    public void load() {
        Dao<MessageMeCursor, Long> dao = getDao();

        try {
            MessageMeCursor tempCursor = dao.queryForId(getChannelId());
            setLastCommandId(tempCursor.getLastCommandId());
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }
    }

    /**
     * Creates a new cursor into the db
     */
    public long create() {
        Dao<MessageMeCursor, Long> dao = getDao();

        try {
            setDateModified(DateUtil.now().getTime());

            return dao.create(this);
        } catch (SQLException e) {
            LogIt.e(MessageMeCursor.class, e);
        }

        return -1;
    }

    /**
     * Updates the lastCommandId and the dateModified but the isEnable field is never
     * changed by this method
     */
    public long update() {
        Dao<MessageMeCursor, Long> dao = getDao();

        try {
            UpdateBuilder<MessageMeCursor, Long> updateBuilder = dao
                    .updateBuilder();
            Where<MessageMeCursor, Long> where = updateBuilder.where();

            if (getLastCommandId() != 0) {

                if (getLastCommandId() == -1) {

                    User currentUser = MessageMeApplication.getCurrentUser();

                    try {
                        InvalidCommandIdException e = new InvalidCommandIdException(
                                currentUser.getUserId(), getChannelId(),
                                getLastCommandId());

                        Crittercism.logHandledException(e);
                        LogIt.w(MessageMeCursor.class, e);
                    } catch (Exception ex) {
                        LogIt.e(MessageMeCursor.class, ex,
                                "Unable to log exception because Crittercism is not initialized yet");
                    }

                    setLastCommandId(0);
                }

                updateBuilder.updateColumnValue(
                        MessageMeCursor.LAST_COMMAND_ID, getLastCommandId());
            } else {
                updateBuilder.escapeColumnName(MessageMeCursor.LAST_COMMAND_ID);
            }

            updateBuilder.updateColumnValue(MessageMeCursor.DATE_MODIFIED,
                    DateUtil.now().getTime());

            // We should maintain the cursor state after each update
            updateBuilder.escapeColumnName(MessageMeCursor.IS_ENABLED);

            where.eq(MessageMeCursor.CHANNEL_ID, getChannelId());

            return updateBuilder.update();
        } catch (SQLException e) {
            LogIt.e(MessageMeCursor.class, e);
        }

        return -1;
    }

    /**
     * Marks an specific cursor as disable and also updates the lastCommandId and dateModified
     */
    public long disable() {
        Dao<MessageMeCursor, Long> dao = getDao();

        try {
            UpdateBuilder<MessageMeCursor, Long> updateBuilder = dao
                    .updateBuilder();
            Where<MessageMeCursor, Long> where = updateBuilder.where();

            if (getLastCommandId() != 0) {

                if (getLastCommandId() == -1) {

                    User currentUser = MessageMeApplication.getCurrentUser();

                    try {
                        InvalidCommandIdException e = new InvalidCommandIdException(
                                currentUser.getUserId(), getChannelId(),
                                getLastCommandId());

                        Crittercism.logHandledException(e);
                        LogIt.w(MessageMeCursor.class, e);
                    } catch (Exception ex) {
                        LogIt.e(MessageMeCursor.class, ex,
                                "Unable to log exception because Crittercism is not initialized yet");
                    }

                    setLastCommandId(0);
                }

                updateBuilder.updateColumnValue(
                        MessageMeCursor.LAST_COMMAND_ID, getLastCommandId());
            } else {
                updateBuilder.escapeColumnName(MessageMeCursor.LAST_COMMAND_ID);
            }

            updateBuilder.updateColumnValue(MessageMeCursor.DATE_MODIFIED,
                    DateUtil.now().getTime());
            updateBuilder.updateColumnValue(MessageMeCursor.IS_ENABLED, false);

            where.eq(MessageMeCursor.CHANNEL_ID, getChannelId());

            return updateBuilder.update();
        } catch (SQLException e) {
            LogIt.e(MessageMeCursor.class, e);
        }

        return -1;
    }

    public static List<MessageMeCursor> findAllActives() {
        Dao<MessageMeCursor, Long> dao = getDao();

        try {
            return dao.queryForEq(MessageMeCursor.IS_ENABLED, true);
        } catch (SQLException e) {
            LogIt.e(MessageMeCursor.class, e, e.getMessage());
        }

        return new LinkedList<MessageMeCursor>();
    }

    public long getChannelId() {
        return channelId;
    }

    public void setChannelId(long channelId) {
        this.channelId = channelId;
    }

    public long getLastCommandId() {
        return lastCommandId;
    }

    public void setLastCommandId(long lastCommandId) {
        this.lastCommandId = lastCommandId;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public long getDateModified() {
        return dateModified;
    }

    public void setDateModified(long dateModified) {
        this.dateModified = dateModified;
    }
}