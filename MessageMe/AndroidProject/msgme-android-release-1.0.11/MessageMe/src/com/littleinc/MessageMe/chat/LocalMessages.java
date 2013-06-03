package com.littleinc.MessageMe.chat;

import java.util.LinkedList;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.bo.IMessageType;
import com.littleinc.MessageMe.bo.Message;
import com.littleinc.MessageMe.bo.Room;
import com.littleinc.MessageMe.bo.TextMessage;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.data.DataBaseHelper;
import com.littleinc.MessageMe.util.MessageUtil;

public class LocalMessages {

    public static List<IMessage> getConversations(String value) {
        LogIt.d(LocalMessages.class, "getConversations", value);

        SQLiteDatabase db = DataBaseHelper.getInstance().getReadableDatabase();
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        queryBuilder.setTables(MessageUtil.getSearchMessagesListJoinedTables());
        queryBuilder.setDistinct(true);

        Cursor cursor = null;
        List<IMessage> messages = new LinkedList<IMessage>();

        try {

            String[] selectionArgs = null;
            StringBuilder whereStringBuilder = new StringBuilder();

            whereStringBuilder.append(TextMessage.BODY_COLUMN);
            whereStringBuilder.append(" LIKE ? OR ");
            whereStringBuilder.append("U." + User.FIRST_NAME_COLUMN + " || ");
            whereStringBuilder.append("?" + " || " + "U."
                    + User.LAST_NAME_COLUMN);
            whereStringBuilder.append(" LIKE ? OR ");
            whereStringBuilder.append("R." + Room.FIRST_NAME_COLUMN);
            whereStringBuilder.append(" LIKE ? AND ");
            whereStringBuilder.append("M." + Message.TYPE_COLUMN);
            whereStringBuilder.append("!=?");

            selectionArgs = new String[] { "%" + value + "%", " ",
                    "%" + value + "%", "%" + value + "%",
                    IMessageType.NOTICE.toString() };

            String sortOrder = Message.CREATED_AT_COLUMN + " DESC";

            cursor = queryBuilder.query(db,
                    MessageUtil.getMessagesListProjection(),
                    whereStringBuilder.toString(), selectionArgs, "M."
                            + Message.CHANNEL_ID_COLUMN, null, sortOrder);

            while (cursor != null && cursor.moveToNext()) {
                IMessage message = MessageUtil.loadMessageFromCursor(cursor);

                messages.add(message);
            }
            cursor.close();

        } catch (Exception e) {
            LogIt.e(LocalMessages.class, e, e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return messages;
    }

    /**
     * Get a minimal IMessage object for message with the provided ID
     * suitable for displaying in the Messages list.  This only contains
     * the fields required for displaying the summary line for the
     * conversation.
     */
    public static IMessage getMessageByID(long id) {
        LogIt.d(MessageUtil.class, "getMessageByID", id);

        SQLiteDatabase db = DataBaseHelper.getInstance().getReadableDatabase();
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(MessageUtil.getMessagesListJoinedTables());

        Cursor cursor = null;

        IMessage message = null;
        try {
            String[] selectionArgs = null;
            StringBuilder whereStringBuilder = new StringBuilder();

            whereStringBuilder.append("M." + Message.ID_COLUMN);
            whereStringBuilder.append("=?");

            selectionArgs = new String[] { String.valueOf(id) };

            cursor = queryBuilder.query(db,
                    MessageUtil.getMessagesListProjection(),
                    whereStringBuilder.toString(), selectionArgs, null, null,
                    null, "1");

            if (cursor != null && cursor.moveToNext()) {
                message = MessageUtil.loadMessageFromCursor(cursor);

                if (message == null) {
                    LogIt.w(LocalMessages.class,
                            "Null message returned for ID", id);
                }
            }
            cursor.close();

        } catch (Exception e) {
            LogIt.e(LocalMessages.class, e, e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return message;
    }

    /**
     * @return the total unread count across all visible conversations.  This
     *         takes less than 10ms on a table with 500 conversations in it.
     */
    public static int loadConversationUnreadCount() {
        SQLiteDatabase db = DataBaseHelper.getInstance().getReadableDatabase();

        Cursor cursor = null;
        int unreadCount = -1;

        try {
            LogIt.d(LocalMessages.class,
                    "Query starting - get total unread count");

            String sql = new StringBuilder().append("SELECT ").append("SUM(")
                    .append(Conversation.UNREAD_COUNT_COLUMN).append(") FROM ")
                    .append(Conversation.TABLE_NAME).append(" WHERE ")
                    .append(Conversation.IS_SHOWN_COLUMN).append("=1")
                    .toString();

            // SELECT SUM(unread_count) FROM conversation WHERE is_shown=1;       
            cursor = db.rawQuery(sql, null);

            if (cursor.moveToFirst()) {
                unreadCount = cursor.getInt(0);
            }
        } catch (Exception e) {
            LogIt.e(LocalMessages.class, e, e.getMessage());
        } finally {
            LogIt.d(LocalMessages.class,
                    "Query finished - get total unread count", unreadCount);

            if (cursor != null) {
                cursor.close();
            }
        }

        return unreadCount;
    }
}
