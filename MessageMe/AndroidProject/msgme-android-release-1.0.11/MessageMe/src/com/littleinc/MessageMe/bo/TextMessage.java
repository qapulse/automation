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
import com.littleinc.MessageMe.protocol.Commands.PBCommandMessageNew;
import com.littleinc.MessageMe.protocol.Messages.PBMessageEnvelope;
import com.littleinc.MessageMe.protocol.Messages.PBMessageText;
import com.littleinc.MessageMe.ui.EmojiUtils;
import com.littleinc.MessageMe.ui.EmojiUtils.EmojiSize;

@DatabaseTable(tableName = TextMessage.TABLE_NAME)
public class TextMessage extends AbstractMessage {

    public static final String TABLE_NAME = "text_message";

    public static final String BODY_COLUMN = "body";

    private boolean hasLinks;

    @DatabaseField(columnName = TextMessage.BODY_COLUMN, dataType = DataType.STRING)
    private String text;
    
    /**
     * A local cache of the text message, with emoji characters converted to
     * HTML for display as local images.
     */
    private CharSequence mEmojiText = null;

    public static Dao<TextMessage, Long> getDao() {
        try {
            return DataBaseHelper.getInstance().getDao(TextMessage.class);
        } catch (SQLException e) {
            LogIt.e(TextMessage.class, e, e.getMessage());
        }

        return null;
    }

    public TextMessage() {
        super(new Message(IMessageType.TEXT));
    }

    @Override
    public long save(boolean updateCursor, Conversation conversation) {
        setId(message.save(updateCursor, conversation));

        try {
            Dao<TextMessage, Long> dao = getDao();
            dao.createOrUpdate(this);
            return dao.extractId(this);
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    @Override
    public PBCommandEnvelope serialize() {
        PBMessageText.Builder messageTextBuilder = PBMessageText.newBuilder();
        PBMessageEnvelope.Builder messageEnvelopeBuilder = PBMessageEnvelope
                .newBuilder();
        PBCommandMessageNew.Builder commandMessageNewBuilder = PBCommandMessageNew
                .newBuilder();
        PBCommandEnvelope.Builder commandEnvelopeBuilder = PBCommandEnvelope
                .newBuilder();

        messageTextBuilder.setBody(getText());

        messageEnvelopeBuilder.setType(getType().toMessageType());
        messageEnvelopeBuilder.setCreatedAt(getCreatedAt());
        messageEnvelopeBuilder.setText(messageTextBuilder.build());

        commandMessageNewBuilder.setRecipientID(getChannelId());
        commandMessageNewBuilder.setMessageEnvelope(messageEnvelopeBuilder
                .build());

        commandEnvelopeBuilder.setMessageNew(commandMessageNewBuilder.build());

        return message.serializeWithEnvelopeBuilder(commandEnvelopeBuilder);
    }

    public boolean hasLinks() {
        return hasLinks;
    }

    public void setHasLinks(boolean hasLinks) {
        this.hasLinks = hasLinks;
    }

    public CharSequence getEmojiText() {
        if (mEmojiText == null) {
            mEmojiText = EmojiUtils.convertToEmojisIfRequired(text,
                    EmojiSize.NORMAL);
        }
        
        return mEmojiText;
    }
    
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        
        // Force the text message emojis to be converted again
        mEmojiText = null;
    }

    @Override
    public IMessageType getType() {
        return IMessageType.TEXT;
    }

    @Override
    public boolean load() {
        boolean result = message.load();

        try {
            Dao<TextMessage, Long> dao = getDao();
            TextMessage textMessage = dao.queryForId(getId());

            if (textMessage != null) {
                setText(textMessage.getText());

                return result;
            } else {
                LogIt.w(TextMessage.class,
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
            Dao<TextMessage, Long> dao = getDao();
            return dao.deleteById(getId());
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    @Override
    public void parseFrom(PBCommandEnvelope commandEnvelope) {
        message.parseFrom(commandEnvelope);

        PBMessageText messageText = commandEnvelope.getMessageNew()
                .getMessageEnvelope().getText();

        setText(messageText.getBody());
        setCommandId(commandEnvelope.getCommandID());
        setClientId(commandEnvelope.getClientID());
    }

    @Override
    public String getMessagePreview(Context context) {
        return getText();
    }
}