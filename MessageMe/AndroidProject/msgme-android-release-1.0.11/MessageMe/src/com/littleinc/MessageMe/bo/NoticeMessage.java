package com.littleinc.MessageMe.bo;

import java.sql.SQLException;

import android.content.Context;

import com.coredroid.util.LogIt;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.littleinc.MessageMe.data.DataBaseHelper;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.ui.EmojiUtils;
import com.littleinc.MessageMe.ui.EmojiUtils.EmojiSize;

@DatabaseTable(tableName = NoticeMessage.TABLE_NAME)
public class NoticeMessage extends AbstractMessage {

    public static final String TABLE_NAME = "notice_message";

    public static final String NOTICE_COLUMN = "notice";

    public static final String NOTICE_TYPE_COLUMN = "notice_type";

    public static final String NOTICE_ACTION_USER_ID = "action_user_id";

    @DatabaseField(columnName = NoticeMessage.NOTICE_TYPE_COLUMN, dataType = DataType.ENUM_STRING)
    private NoticeType noticeType;

    @DatabaseField(columnName = NoticeMessage.NOTICE_COLUMN, dataType = DataType.STRING)
    private String notificationMessage;

    @DatabaseField(columnName = NoticeMessage.NOTICE_ACTION_USER_ID, dataType = DataType.LONG)
    private long actionUserID = -1;

    /**
     * A local cache of the notification text, with emoji characters converted
     * to HTML for display as local images.
     */
    private CharSequence mNotificationEmojiText = null;

    public static Dao<NoticeMessage, Long> getDao() {
        try {
            return DataBaseHelper.getInstance().getDao(NoticeMessage.class);
        } catch (SQLException e) {
            LogIt.e(TextMessage.class, e, e.getMessage());
        }

        return null;
    }

    public NoticeMessage() {
        super(new Message(IMessageType.NOTICE));
    }

    @Override
    public long save(boolean updateCursor, Conversation conversation) {
        setId(message.save(updateCursor, conversation));

        try {
            Dao<NoticeMessage, Long> dao = getDao();
            dao.createOrUpdate(this);
            return dao.extractId(this);
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    @Override
    public int delete() {
        message.delete();

        try {
            Dao<NoticeMessage, Long> dao = getDao();
            return dao.deleteById(getId());
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    @Override
    public boolean load() {
        boolean result = message.load();

        try {
            Dao<NoticeMessage, Long> dao = getDao();
            NoticeMessage noticeMessage = dao.queryForId(getId());

            if (noticeMessage != null) {
                setNotificationMessage(noticeMessage.getNotificationMessage());
                setNoticeType(noticeMessage.getNoticeType());
                setActionUserID(noticeMessage.getActionUserID());

                return result;
            } else {
                LogIt.w(NoticeMessage.class,
                        "Trying to load a non-existing message", getId());
            }
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return false;
    }

    @Override
    public PBCommandEnvelope serialize() {
        LogIt.d(this, "No need to serialize, this message is managed locally");
        return null;
    }

    @Override
    public void parseFrom(PBCommandEnvelope commandEnvelope) {
        message.parseFrom(commandEnvelope);
    }

    @Override
    public User getSender() {
        return message.getSender();
    }

    @Override
    public int getCreatedAt() {
        return message.getCreatedAt();
    }

    @Override
    public void setCreatedAt(int createdAt) {
        message.setCreatedAt(createdAt);
    }

    @Override
    public IMessageType getType() {
        return IMessageType.NOTICE;
    }

    public CharSequence getNotificationMessageWithEmojis() {

        if (mNotificationEmojiText == null) {
            mNotificationEmojiText = EmojiUtils.convertToEmojisIfRequired(
                    notificationMessage, EmojiSize.SMALL);
        }

        return mNotificationEmojiText;
    }

    public String getNotificationMessage() {
        return notificationMessage;
    }

    public void setNotificationMessage(String notificationMessage) {
        this.notificationMessage = notificationMessage;

        // Force the emojis to be converted again
        mNotificationEmojiText = null;
    }

    public NoticeType getNoticeType() {
        return noticeType;
    }

    public void setNoticeType(NoticeType noticeType) {
        this.noticeType = noticeType;
    }

    public long getActionUserID() {
        return actionUserID;
    }

    public void setActionUserID(long actionUserID) {
        this.actionUserID = actionUserID;
    }

    @Override
    public String getMessagePreview(Context context) {
        return getNotificationMessage();
    }
}