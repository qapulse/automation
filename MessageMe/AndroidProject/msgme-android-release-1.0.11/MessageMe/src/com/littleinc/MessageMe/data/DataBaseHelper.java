package com.littleinc.MessageMe.data;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDiskIOException;

import com.coredroid.util.LogIt;
import com.crittercism.app.Crittercism;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.bo.ABContactInfo;
import com.littleinc.MessageMe.bo.Contact;
import com.littleinc.MessageMe.bo.ContactMessage;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.ConversationReader;
import com.littleinc.MessageMe.bo.DoodleMessage;
import com.littleinc.MessageMe.bo.DoodlePicMessage;
import com.littleinc.MessageMe.bo.LocationMessage;
import com.littleinc.MessageMe.bo.MatchedABRecord;
import com.littleinc.MessageMe.bo.Message;
import com.littleinc.MessageMe.bo.MessageMeCursor;
import com.littleinc.MessageMe.bo.NoticeMessage;
import com.littleinc.MessageMe.bo.PhotoMessage;
import com.littleinc.MessageMe.bo.Room;
import com.littleinc.MessageMe.bo.RoomMember;
import com.littleinc.MessageMe.bo.SongMessage;
import com.littleinc.MessageMe.bo.TextMessage;
import com.littleinc.MessageMe.bo.UnsupportedMessage;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.bo.VideoMessage;
import com.littleinc.MessageMe.bo.VoiceMessage;
import com.littleinc.MessageMe.bo.YoutubeMessage;
import com.littleinc.MessageMe.error.UserNotFoundException;
import com.littleinc.MessageMe.util.BatchTask;
import com.littleinc.MessageMe.util.DatabaseTask;

public class DataBaseHelper extends OrmLiteSqliteOpenHelper implements Runnable {

    public static final String DATABASE_NAME = "message-me.db";

    /**
     * Database version history:
     *   2 - Add thumbnail key and PBImageBundle to PhotoMessage
     *   3 - Add thumbnail key and PBImageBundle to DoodleMessage and DoodlePicMessage
     *   4 - Commonize database field names for PhotoMessage, DoodleMessage and 
     *       DoodlePicMessage, but create unique aliases in database operations
     *   5 - Add isShown to User and Room
     *   6 - Changed datatype of dateCreated to INTEGER in room entity
     *   7 - Added new table to handle room notices
     *   8 - Add an Unsupported message type for forward compatibility
     *   9 - Add ContactMessage for showing forwarded contacts
     *   10 - Added new entity MatchedABRecord for AB sync
     *   11 - Not using clientId as primary anymore
     *   12 - Add sortedBy field to messages, and commonize some IMessage methods
     *   13 - Added ABContactInfo table
     *   14 - Bump database version to force login again for channel jump changes
     *   15 - Added is_enable and date_modified on cursor entity
     *   
     * === v0.7 to v1.0.6 releases all published ===
     *   
     *   16 - Add isBlocked, blockedBy, localFirstName and localLastName to User
     *   
     * === v1.0.7 release published ===  
     *   
     *   17 - Add unnamed field to Room
     *   
     * === v1.0.8 to v1.0.9 releases all published ===
     * 
     *   18 - Add Conversation, ConversationReader and remove MessageReader
     *   
     */
    public static final int DATABASE_VERSION = 18;

    private static DataBaseHelper sInstance;

    /** 
     * Thread to execute every {@link DatabaseTask} or {@link BatchTask} task.
     * 
     * We can't use a single thread Executor as we need to prioritize BatchTask
     * objects over DatabaseTasks, so we need to implement run() ourselves. 
     */
    private Thread dedicatedDataBaseThread;

    private Queue<BatchTask> batchTasks = new ConcurrentLinkedQueue<BatchTask>();

    /**
     * blockingQueue is just to maintain the total amount of tasks to be processed and to block
     * the current thread when it doesn't have anything to do, the values in this queue 
     * should not be used 
     */
    private BlockingQueue<Object> blockingTaskCountQueue = new LinkedBlockingQueue<Object>();

    private Queue<DatabaseTask> databaseTasks = new ConcurrentLinkedQueue<DatabaseTask>();

    /** Flag for shutting down our database thread */
    private boolean shutdownRequested = false;

    public DataBaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION,
                R.raw.ormlite_config);
    }

    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        createDatabaseTables(connectionSource);
    }

    private void createDatabaseTables(ConnectionSource connectionSource) {

        try {
            LogIt.i(this, "Creating DB tables, version " + DATABASE_VERSION);

            TableUtils.createTable(connectionSource, MessageMeCursor.class);
            TableUtils.createTable(connectionSource, User.class);
            TableUtils.createTable(connectionSource, Room.class);
            TableUtils.createTable(connectionSource, RoomMember.class);
            TableUtils.createTable(connectionSource, Message.class);
            TableUtils.createTable(connectionSource, TextMessage.class);
            TableUtils.createTable(connectionSource, PhotoMessage.class);
            TableUtils.createTable(connectionSource, LocationMessage.class);
            TableUtils.createTable(connectionSource, VoiceMessage.class);
            TableUtils.createTable(connectionSource, SongMessage.class);
            TableUtils.createTable(connectionSource, VideoMessage.class);
            TableUtils.createTable(connectionSource, DoodleMessage.class);
            TableUtils.createTable(connectionSource, YoutubeMessage.class);
            TableUtils.createTable(connectionSource, DoodlePicMessage.class);
            TableUtils.createTable(connectionSource, NoticeMessage.class);
            TableUtils.createTable(connectionSource, UnsupportedMessage.class);
            TableUtils.createTable(connectionSource, ContactMessage.class);
            TableUtils.createTable(connectionSource, MatchedABRecord.class);
            TableUtils.createTable(connectionSource, ABContactInfo.class);
            TableUtils.createTable(connectionSource, Conversation.class);
            TableUtils.createTable(connectionSource, ConversationReader.class);
        } catch (java.sql.SQLException e) {
            LogIt.e(this, e, e.getMessage());
            throw new RuntimeException();
        }

        MessageMeApplication.getPreferences().setCurrentDBVersion(
                DATABASE_VERSION);
    }

    /**
     * This method is only supported in Android API level 11 and above.  On 
     * older OSes the onUpgrade method is called instead.
     */
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This should never happen in Production
        LogIt.w(this, "DB downgrade required from " + oldVersion + " to "
                + newVersion + " (onDowngrade)");
        MessageMeApplication.getPreferences().setDbNeedsDowngrade(true);
        MessageMeApplication.getState().sync();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource,
            int oldVersion, int newVersion) {

        if (newVersion < oldVersion) {
            // This should never happen in Production
            LogIt.w(this, "DB downgrade required from " + oldVersion + " to "
                    + newVersion + " (onUpgrade)");
            MessageMeApplication.getPreferences().setDbNeedsDowngrade(true);
            MessageMeApplication.getState().sync();
            return;
        }

        LogIt.i(this, "Upgrading DB from " + oldVersion + " to " + newVersion);

        if (oldVersion < 15) {
            LogIt.w(this,
                    "Client is upgrading from v0.6 or older, they will have to login again");
            deleteAndRecreateDatabase(db);
        } else {
            if (oldVersion < 16) {
                LogIt.d(DataBaseHelper.class,
                        "Starting migration to DB version 16");
                
                addColumn(db, User.TABLE_NAME, User.BLOCKED_BY, "BOOLEAN");
                addColumn(db, User.TABLE_NAME, User.IS_BLOCKED, "BOOLEAN");
                addColumn(db, User.TABLE_NAME, User.LOCAL_FIRST_NAME, "STRING");
                addColumn(db, User.TABLE_NAME, User.LOCAL_LAST_NAME, "STRING");

                LogIt.d(DataBaseHelper.class,
                        "Finished migration to DB version 16");
            }

            if (oldVersion < 17) {
                LogIt.d(DataBaseHelper.class,
                        "Starting migration to DB version 17");
                
                addColumn(db, Room.TABLE_NAME, Room.UNNAMED, "BOOLEAN");

                // In previous releases the isShown field was defaulted to 
                // false (0) for both User and Room objects.  Rooms should
                // have been defaulted to true (1), so fix them up now.
                setColumnValueInteger(db, Room.TABLE_NAME,
                        Contact.IS_SHOWN_COLUMN, 1);
                
                LogIt.d(DataBaseHelper.class,
                        "Finished migration to DB version 17");
            }

            if (oldVersion < 18) {
                
                Cursor cursor = null;
                db.beginTransaction();

                try {
                    LogIt.d(DataBaseHelper.class,
                            "Starting migration to DB version 18");

                    TableUtils
                            .createTable(connectionSource, Conversation.class);
                    TableUtils.createTable(connectionSource,
                            ConversationReader.class);

                    User currentUser = MessageMeApplication.getCurrentUser();

                    if (currentUser == null) {
                        LogIt.w(DataBaseHelper.class,
                                "No current user, data migration no needed");
                    } else {

                        long currentUserID = MessageMeApplication
                                .getCurrentUser().getUserId();

                        String sql = new StringBuilder()
                                .append("SELECT ")
                                .append(Message.CHANNEL_ID_COLUMN)
                                .append(", MR.")
                                .append(Message.COMMAND_ID_COLUMN)
                                .append(", read_date, ")
                                .append(Message.ID_COLUMN)
                                .append(" FROM ")
                                .append(Message.TABLE_NAME)
                                .append(" AS M LEFT JOIN message_reader AS MR ON ")
                                .append("M.").append(Message.COMMAND_ID_COLUMN)
                                .append("=MR.command_id WHERE ")
                                .append(Message.SENDER_ID_COLUMN).append("=")
                                .append(currentUserID).append(" GROUP BY ")
                                .append(Message.CHANNEL_ID_COLUMN)
                                .append(" ORDER BY ")
                                .append(Message.SORTED_BY_COLUMN)
                                .append(" DESC;").toString();

                        // e.g SELECT 
                        //         channel_id, 
                        //         command_id,
                        //         _id,
                        //         read_date
                        //     FROM 
                        //         message AS M LEFT JOIN message_reader AS MR ON M.command_id = MR.command_id
                        //     WHERE
                        //         sender_id=XXXXXXXX
                        //     GROUP BY 
                        //         channel_id 
                        //     ORDER BY 
                        //         sorted_by DESC;

                        cursor = db.rawQuery(sql, null);

                        // Map of last sent message ID, command ID and readAt for each channel ID
                        Map<Long, Object[]> sentMsgs = new HashMap<Long, Object[]>();

                        final int LAST_SENT_MSG_ID = 0;
                        final int LAST_SENT_READ_AT = 1;
                        final int LAST_SENT_MSG_READERS = 3;
                        final int LAST_SENT_MSG_COMMAND_ID = 2;

                        while ((cursor != null) && cursor.moveToNext()) {

                            long channelId = cursor
                                    .getColumnIndex(Message.CHANNEL_ID_COLUMN) != -1 ? cursor
                                    .getLong(cursor
                                            .getColumnIndex(Message.CHANNEL_ID_COLUMN))
                                    : -1;
                            long lastSentMsgId = cursor
                                    .getColumnIndex(Message.ID_COLUMN) != -1 ? cursor
                                    .getLong(cursor
                                            .getColumnIndex(Message.ID_COLUMN))
                                    : -1;
                            long lastSentMsgCommandId = cursor
                                    .getColumnIndex(Message.COMMAND_ID_COLUMN) != -1 ? cursor
                                    .getLong(cursor
                                            .getColumnIndex(Message.COMMAND_ID_COLUMN))
                                    : -1;
                            int lastSentMsgReatAt = cursor
                                    .getColumnIndex("read_date") != -1 ? cursor
                                    .getInt(cursor.getColumnIndex("read_date"))
                                    : -1;

                            if (channelId != -1) {
                                Object[] value = new Object[] { lastSentMsgId,
                                        lastSentMsgReatAt,
                                        lastSentMsgCommandId, null };
                                sentMsgs.put(channelId, value);
                            }
                        }

                        if (cursor != null) {
                            cursor.close();
                        }

                        StringBuilder builder = new StringBuilder()
                                .append("SELECT MR.reader_id, M.")
                                .append(Message.CHANNEL_ID_COLUMN)
                                .append(" FROM message_reader AS MR, ")
                                .append(Message.TABLE_NAME)
                                .append(" AS M")
                                .append(" WHERE M.")
                                .append(Message.COMMAND_ID_COLUMN)
                                .append("=MR.command_id AND MR.command_id IN (");

                        boolean isFirst = true;
                        for (Object[] sentMsgInfo : sentMsgs.values()) {

                            long commandId = (Long) sentMsgInfo[LAST_SENT_MSG_COMMAND_ID];

                            if (commandId != 0) {
                                if (isFirst) {
                                    builder.append(commandId);
                                    isFirst = false;
                                } else {
                                    builder.append(", ").append(commandId);
                                }
                            }
                        }

                        // e.g SELECT 
                        //         M.channel_id, 
                        //         MR.reader_id
                        //     FROM 
                        //         message_reader AS MR, message AS M
                        //     WHERE
                        //         M.command_id = MR.command_id AND MR.command_id IN (XXXX,XXXX,XXXX);
                        sql = builder.append(");").toString();
                        cursor = db.rawQuery(sql, null);

                        while (cursor != null && cursor.moveToNext()) {

                            long readerId = cursor.getLong(cursor
                                    .getColumnIndex("reader_id"));
                            long channelId = cursor.getLong(cursor
                                    .getColumnIndex(Message.CHANNEL_ID_COLUMN));

                            LinkedList<Long> readersId = (LinkedList<Long>) sentMsgs
                                    .get(channelId)[LAST_SENT_MSG_READERS];

                            if (readersId == null) {
                                readersId = new LinkedList<Long>();
                                sentMsgs.get(channelId)[LAST_SENT_MSG_READERS] = readersId;
                            }
                            readersId.add(readerId);
                        }

                        if (cursor != null) {
                            cursor.close();
                        }

                        // e.g SELECT 
                        //         channel_id, 
                        //         _id, 
                        //         sorted_by, 
                        //         sender_id,
                        //         SUM(CASE WHEN readed=0 AND sender_id<>XXXXXXX THEN 1 ELSE 0 END) AS unread_count 
                        //     FROM 
                        //         message 
                        //     GROUP BY 
                        //         channel_id 
                        //     ORDER BY 
                        //         sorted_by DESC;

                        sql = new StringBuilder().append("SELECT ")
                                .append(Message.CHANNEL_ID_COLUMN).append(", ")
                                .append(Message.ID_COLUMN).append(", ")
                                .append(Message.SORTED_BY_COLUMN).append(", ")
                                .append(Message.SENDER_ID_COLUMN).append(", ")
                                .append("SUM(CASE WHEN ")
                                .append(Message.READED_COLUMN).append("=0 AND ")
                                .append(Message.SENDER_ID_COLUMN).append("<>")
                                .append(currentUser.getUserId())
                                .append(" THEN 1 ELSE 0 END) AS ")
                                .append(Conversation.UNREAD_COUNT_COLUMN)
                                .append(" FROM ").append(Message.TABLE_NAME)
                                .append(" GROUP BY ")
                                .append(Message.CHANNEL_ID_COLUMN)
                                .append(" ORDER BY ")
                                .append(Message.SORTED_BY_COLUMN)
                                .append(" DESC;").toString();

                        // e.g SELECT 
                        //         channel_id, 
                        //         _id, 
                        //         sorted_by, 
                        //         sender_id,
                        //         SUM(CASE WHEN readed=0 AND sender_id<>XXXXXXX THEN 1 ELSE 0 END) AS unread_count 
                        //     FROM 
                        //         message 
                        //     GROUP BY 
                        //         channel_id 
                        //     ORDER BY 
                        //         sorted_by DESC;

                        cursor = db.rawQuery(sql, null);
                        List<Conversation> conversations = new LinkedList<Conversation>();

                        // Do all the work we need to on the conversations 
                        // before saving the updates to the database, as we 
                        // don't want to lock up the DB unnecessarily
                        while ((cursor != null) && cursor.moveToNext()) {

                            long mostRecentMsgSenderID = -1;

                            if (cursor.getColumnIndex(Message.SENDER_ID_COLUMN) != -1) {
                                mostRecentMsgSenderID = cursor
                                        .getLong(cursor
                                                .getColumnIndex(Message.SENDER_ID_COLUMN));
                            }

                            Message lastSentMsg = new Message();
                            Conversation conversation = new Conversation(cursor);

                            Object[] lastSentMsgInfo = sentMsgs
                                    .get(conversation.getChannelId());

                            if (lastSentMsgInfo != null) {

                                lastSentMsg
                                        .setId((Long) lastSentMsgInfo[LAST_SENT_MSG_ID]);

                                conversation
                                        .setReadAt((Integer) lastSentMsgInfo[LAST_SENT_READ_AT]);
                                conversation.setLastSentMessage(lastSentMsg);

                                LinkedList<Long> readers = (LinkedList<Long>) lastSentMsgInfo[LAST_SENT_MSG_READERS];

                                if (readers != null) {
                                    for (Long readerId : readers) {

                                        try {                                            
                                            ConversationReader reader = new ConversationReader(
                                                    conversation, readerId);
                                            
                                            if (conversation.getReadUsers() != null) {
                                                
                                                // ForeignCollection#add will throw
                                                // an SQLiteConstraintException if
                                                // the object to be added already
                                                // exists so ignore already added
                                                // readers
                                                if (!conversation.getReadUsers()
                                                        .contains(reader)) {
                                                    
                                                    conversation.getReadUsers()
                                                    .add(reader);
                                                } else {
                                                    LogIt.d(DataBaseHelper.class,
                                                            "Ignore already added reader",
                                                            readerId);
                                                }
                                            }
                                        } catch (UserNotFoundException unfe) {
                                            LogIt.w(this, "Don't add reader for null user", readerId);
                                            Crittercism.logHandledException(unfe);
                                        }
                                    }
                                }
                            }

                            int unreadCount = conversation.getUnreadCount();
                            long channelID = conversation.getChannelId();

                            LogIt.d(this, "Migrating conversation for",
                                    channelID, "unread count " + unreadCount);

                            // Fix up any obviously incorrect unread counts
                            // (which older clients won't have noticed as 
                            // previously no unread count was displayed).
                            if ((unreadCount > 0)
                                    && (mostRecentMsgSenderID == currentUserID)) {
                                LogIt.w(this,
                                        "Most recent message was sent by this user, set unread count to zero",
                                        channelID);
                                conversation.setUnreadCount(0);
                                unreadCount = 0;
                            }

                            conversations.add(conversation);
                        }

                        for (Conversation conv : conversations) {
                            conv.save();
                        }
//                        throw new SQLiteConstraintException("DAJtest");
                        db.setTransactionSuccessful();

                        // Ensure the MessageReadCounterUtil updates are saved
                        MessageMeApplication.getState().sync();
                    }
                } catch (java.sql.SQLException e) {
                    // The ORMLite tools like createTable can throw a 
                    // java.sql.SQLException.
                    //
                    // Raw SQL operations can throw a 
                    // android.database.SQLException.  We don't try to handle
                    // those as we'd prefer this onUpgrade method to fail so
                    // it will get called again each time the user launches
                    // the app.
                    try {
                        LogIt.e(DataBaseHelper.class, e);
                        Crittercism.logHandledException(e);
                    } catch (Exception ex) {
                        LogIt.e(DataBaseHelper.class, ex,
                                "Unable to log exception because Crittercism is not initialized yet");
                    }
                    
                    // Ideally we would want to rethrow the exception here, 
                    // otherwise onUpgrade will never be called again.  
                    // Unfortunately this method doesn't let us :-(
                } finally {

                    if (cursor != null) {
                        cursor.close();
                    }
                    db.endTransaction();
                }
                
                db.beginTransaction();
                try {
                    LogIt.d(DataBaseHelper.class,
                            "Dropping message_reader table");
                    db.execSQL("DROP TABLE message_reader;");
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                            
                MessageMeApplication.getPreferences().setCurrentDBVersion(18);
                LogIt.d(DataBaseHelper.class,
                        "Finished migration to DB version 18");
            }
        }
    }

    /**
     * Completely delete and recreate the tables in the database.  
     * 
     * This should never be called for Productions clients.  Instead they 
     * should be upgraded without losing data or being forced to login again. 
     */
    private void deleteAndRecreateDatabase(SQLiteDatabase db) {
        LogIt.w(this, "Delete and recreate database");
        MessageMeApplication.getPreferences().setDbNeedUpgrade(true);
        MessageMeApplication.getState().sync();
        dropDatabaseTables(db);
        createDatabaseTables(connectionSource);
    }

    private boolean doesColumnExistsInTable(SQLiteDatabase db, String table,
            String column) {
        boolean columnExists;

        Cursor cursor = null;
        try {
            // Get 1 row from the table
            cursor = db.rawQuery("SELECT * FROM " + table + " LIMIT 0", null);

            if (cursor.getColumnIndex(column) == -1) {
                columnExists = false;
            } else {
                columnExists = true;
            }
        } catch (Exception e) {
            // Something else went wrong, e.g. missing the table
            LogIt.e(this, e, "doesColumnExistsInTable error", table, column);
            columnExists = false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return columnExists;
    }

    /**
     * Add the provided columnName to the specified tableName if it does not
     * exist already.
     * 
     * @param tableName the table to add the column to
     * @param columnName the column name to add
     * @param columnType is a String describing the data type
     *                   http://ormlite.com/data_types.shtml
     */
    private void addColumn(SQLiteDatabase db, String tableName,
            String columnName, String columnType) {
        // This safety check is mostly useful during development,
        // but if an upgrade is aborted partway (e.g. due to a Force 
        // Stop or a reboot), then through this should ensure it 
        // succeeds the second time.
        if (doesColumnExistsInTable(db, User.TABLE_NAME, columnName)) {
            LogIt.w(this, "Column already exists, don't add it again",
                    columnName);
        } else {
            String sql = "ALTER TABLE '" + tableName + "' ADD COLUMN '"
                    + columnName + "' " + columnType;

            LogIt.d(this, "Add new column", sql);

            try {
                db.beginTransaction();
                db.execSQL(sql);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    /**
     * Set the columnName to the provided columnValue for all rows in the 
     * provided tableName, using this SQL:
     * 
     *  UPDATE 'tableName' SET 'columnName' = 'columnValue'
     */
    private void setColumnValueInteger(SQLiteDatabase db, String tableName,
            String columnName, int columnValue) {
        String sql = "UPDATE '" + tableName + "' SET '" + columnName + "' = '"
                + columnValue + "'";

        LogIt.d(this, "Set column " + columnName + " to " + columnValue, sql);

        try {
            db.beginTransaction();
            db.execSQL(sql);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Drop the database tables.  Ideally this should never be used in 
     * Production as upgrades should preserve existing data. 
     * 
     * We have to use raw SQL instead of TableUtils.dropTable method, as 
     * calling a utility method from outside an overridden interface (like 
     * onDowngrade) fails with this error:
     *   java.lang.IllegalStateException: getDatabase called recursively
     */
    private void dropDatabaseTables(SQLiteDatabase db) {

        final String sqlStart = "DROP TABLE '";
        final String sqlEnd = "'";

        try {
            LogIt.w(this, "Dropping the DB tables");

            db.beginTransaction();

            db.execSQL(sqlStart + MessageMeCursor.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + User.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + Room.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + RoomMember.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + Message.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + TextMessage.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + PhotoMessage.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + LocationMessage.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + VoiceMessage.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + SongMessage.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + VideoMessage.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + DoodleMessage.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + YoutubeMessage.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + DoodlePicMessage.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + NoticeMessage.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + UnsupportedMessage.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + ContactMessage.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + MatchedABRecord.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + ABContactInfo.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + Conversation.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + ConversationReader.TABLE_NAME + sqlEnd);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void clearDataBase() {
        clearDataBase(getWritableDatabase());
    }

    public void clearDataBase(SQLiteDatabase db) {
        final String sqlStart = "DELETE FROM '";
        final String sqlEnd = "'";

        try {
            LogIt.i(this, "Clearing the DB tables");

            db.beginTransaction();

            db.execSQL(sqlStart + MessageMeCursor.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + User.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + Room.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + RoomMember.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + Message.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + TextMessage.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + PhotoMessage.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + LocationMessage.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + VoiceMessage.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + SongMessage.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + VideoMessage.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + DoodleMessage.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + YoutubeMessage.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + DoodlePicMessage.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + NoticeMessage.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + UnsupportedMessage.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + ContactMessage.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + MatchedABRecord.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + ABContactInfo.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + Conversation.TABLE_NAME + sqlEnd);
            db.execSQL(sqlStart + ConversationReader.TABLE_NAME + sqlEnd);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Checks if Database file exist already on disk
     */
    public static boolean dbExists(Context context, String dbName) {

        File dbFile = context.getDatabasePath(dbName);

        if (dbFile == null) {
            return false;
        } else {
            return dbFile.exists();
        }
    }

    public static synchronized DataBaseHelper getInstance() {

        if (sInstance == null) {
            sInstance = new DataBaseHelper(MessageMeApplication.getInstance());
        }

        return sInstance;
    }

    public synchronized void addBatchTask(BatchTask batchTask) {
        if (batchTasks.add(batchTask)) {
            blockingTaskCountQueue.add(batchTask);
            createThreadsIfRequired();
        } else {
            LogIt.e(DataBaseHelper.class,
                    "Failed to add database task to queue");
        }
    }

    public synchronized void addTask(DatabaseTask databaseTask) {
        if (databaseTasks.add(databaseTask)) {
            blockingTaskCountQueue.add(databaseTask);
            createThreadsIfRequired();
        } else {
            LogIt.e(DataBaseHelper.class,
                    "Failed to add database task to queue");
        }
    }

    private synchronized void createThreadsIfRequired() {
        if (dedicatedDataBaseThread == null) {
            LogIt.d(DataBaseHelper.class, "Create dedicated database thread");

            shutdownRequested = false;

            dedicatedDataBaseThread = new Thread(this);
            dedicatedDataBaseThread.start();
        }
    }

    public synchronized void shutDown() {
        if (dedicatedDataBaseThread == null) {
            LogIt.i(this, "No database thread to shut down");
        } else {
            LogIt.i(this, "Shut down the database thread");

            shutdownRequested = true;

            // If the thread is blocked this will make it wake up
            dedicatedDataBaseThread.interrupt();
            dedicatedDataBaseThread = null;

            // Discard pending tasks if the shutdown is requested
            batchTasks.clear();
            blockingTaskCountQueue.clear();
            databaseTasks.clear();
        }
    }

    @Override
    public void run() {

        LogIt.d(DataBaseHelper.class, "Starting DB thread");

        while (!shutdownRequested && !Thread.currentThread().isInterrupted()) {
            try {
                // This queue is only used to maintain our count of how
                // many tasks need to be processed.
                blockingTaskCountQueue.take();
                consumeNextTask();
            } catch (InterruptedException ex) {
                // A thread will only receive an InterruptedException if it was 
                // blocking at the time of interrupt
                LogIt.i(DataBaseHelper.class, "InterruptedException");
            }
        }

        LogIt.i(DataBaseHelper.class, "Shutting down DB thread");
    }

    private void consumeNextTask() {

        SQLiteDatabase db = getWritableDatabase();

        try {
            // Only starts a new transaction if there is not another one running
            if (!db.inTransaction()) {
                db.beginTransaction();
            }
        } catch (SQLiteDiskIOException e) {
            try {
                LogIt.e(DataBaseHelper.class, e);
                Crittercism.logHandledException(e);
            } catch (Exception ex) {
                LogIt.e(DataBaseHelper.class, ex,
                        "Unable to log exception because Crittercism is not initialized yet");
            }
        }

        try {
            // DatabaseTasks have priority over BatchTasks
            if (!databaseTasks.isEmpty()) {
                DatabaseTask databaseTask = databaseTasks.poll();

                try {
                    databaseTask.work();
                } catch (Exception e) {
                    databaseTask.fail(e);
                } finally {
                    databaseTask.notifyCallerThread();
                }
            } else if (!batchTasks.isEmpty()) {
                BatchTask batchTask = batchTasks.poll();

                batchTask.work();
            }
        } finally {
            try {
                // Only finish transaction if a transaction is running and if 
                // there is nothing more to process
                if (db.inTransaction() && blockingTaskCountQueue.isEmpty()) {
                    // We use transactions here to improve write speed not to ensure data integrity
                    // for that reason we going to set all the transactions as successful to avoid lose
                    // big amounts of data if for some reason just few transactions fails
                    db.setTransactionSuccessful();
                    db.endTransaction();
                }
            } catch (SQLiteDiskIOException e) {
                try {
                    LogIt.e(DataBaseHelper.class, e);
                    Crittercism.logHandledException(e);
                } catch (Exception ex) {
                    LogIt.e(DataBaseHelper.class, ex,
                            "Unable to log exception because Crittercism is not initialized yet");
                }
            }
        }
    }
}