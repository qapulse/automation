package com.littleinc.MessageMe.bo;

import android.content.Context;
import android.view.ViewGroup.LayoutParams;

import com.coredroid.util.Dimension;
import com.coredroid.util.LogIt;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.util.ChatAdapter;
import com.littleinc.MessageMe.util.DateUtil;
import com.littleinc.MessageMe.util.ImageUtil;

/**
 * Common abstract class for composition with the Message object.
 */
public class AbstractMessage implements IMessage {

    @DatabaseField(columnName = Message.ID_COLUMN, id = true, dataType = DataType.LONG)
    protected long id;

    protected Message message;

    /**
     * Cache the image message bubble size.
     */
    private static int sImageBubbleHeight = MessageMeApplication.getInstance()
            .getResources()
            .getDimensionPixelSize(R.dimen.image_message_bubble_height);

    public AbstractMessage(Message message) {
        this.message = message;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
        message.setId(id);
    }

    @Override
    public long save(boolean updateCursor, Conversation conversation) {
        return message.save(updateCursor, conversation);
    }

    @Override
    public int delete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean load() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PBCommandEnvelope serialize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void parseFrom(PBCommandEnvelope commandEnvelope) {
        throw new UnsupportedOperationException();
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
    public double getSortedBy() {
        return message.getSortedBy();
    }

    /**
     * The sortedBy time needs to be different depending on where this
     * message came from.  
     * 
     * - It usually is set to the current time, in fake microseconds
     *   from {@link DateUtil#getCurrentTimeMicros()}, possibly plus a
     *   delta if it arrived in a batch.
     *    
     * - If it is received because we did a Load Earlier Messages call 
     *   to the server then it gets a special value.
     */
    @Override
    public void setSortedBy(double sortedBy) {
        message.setSortedBy(sortedBy);
    }

    @Override
    public IMessageType getType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getClientId() {
        return message.getClientId();
    }

    @Override
    public void setClientId(long clientId) {
        message.setClientId(clientId);
    }

    @Override
    public long getSenderId() {
        return message.getSenderId();
    }

    @Override
    public void setSenderId(long senderId) {
        message.setSenderId(senderId);
    }

    @Override
    public long getCommandId() {
        return message.getCommandId();
    }

    @Override
    public void setCommandId(long commandId) {
        message.setCommandId(commandId);
    }

    @Override
    public long getChannelId() {
        return message.getChannelId();
    }

    @Override
    public void setChannelId(long channelId) {
        message.setChannelId(channelId);
    }

    @Override
    public Contact getContact() {
        return message.getContact();
    }

    @Override
    public void setContact(Contact contact) {
        message.setContact(contact);
    }

    @Override
    public boolean wasSentByThisUser() {
        return message.wasSentByThisUser();
    }

    public void setCommonFieldsForSend(long channelId) {
        User currentUser = MessageMeApplication.getCurrentUser();

        // Messages pending a send always start with command ID as -1
        message.setCommandId(-1);

        message.setChannelId(channelId);
        message.setSenderId(currentUser.getUserId());
        message.setClientId(DateUtil.now().getTime());
        message.setCreatedAt(DateUtil.getCurrentTimestamp());
        message.setSortedBy(DateUtil.getCurrentTimeMicros());
    }

    @Override
    public String getMessagePreview(Context context) {
        LogIt.w(this, "No preview for this message");
        return "";
    }

    public void forward(ChatAdapter adapter, MessagingService service) {
        message.forward(adapter, service, this);
    }

    @Override
    public Message getMessage() {
        return message;
    }

    @Override
    public int getViewType() {
        return message.getViewType();
    }

    /**
     * Set the message bubble size in the provided LayoutParams based on the 
     * message type, and return a Dimension of the size it was set to.
     */
    public static Dimension setMessageBubbleSize(IMessage imessage,
            LayoutParams layoutParams) {
        Dimension bubbleSize = new Dimension();

        if (imessage instanceof SingleImageMessage) {
            // Work out how big we want the message bubble to be so we can
            // display it with the correct size immediately, as otherwise
            // the bubble may resize after the image loads, which looks ugly.
            Dimension thumbSize = ((SingleImageMessage) imessage)
                    .getThumbnailSize();

            bubbleSize = ImageUtil.calcResizeToBounds(thumbSize.getWidth(),
                    thumbSize.getHeight(), sImageBubbleHeight);

            // Image messages need their bubble size to be set now, as it is
            // only here that we know the aspect ratio of the image.
            layoutParams.width = bubbleSize.getWidth();
            layoutParams.height = bubbleSize.getHeight();
        } else {
            // All other message bubbles already have the correct size in their
            // XML layouts
            bubbleSize.setWidth(layoutParams.width);
            bubbleSize.setHeight(layoutParams.height);
        }

        return bubbleSize;
    }

    @Override
    public long save(boolean updateCursor) {
        return save(updateCursor, null);
    }
}