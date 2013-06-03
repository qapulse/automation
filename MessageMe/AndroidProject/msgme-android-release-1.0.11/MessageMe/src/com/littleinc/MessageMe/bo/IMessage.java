package com.littleinc.MessageMe.bo;

import android.content.Context;

import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;

public interface IMessage {

    public long getId();

    public void setId(long id);

    public long save(boolean updateCursor);

    public long save(boolean updateCursor, Conversation conversation);

    public int delete();

    public boolean load();

    public PBCommandEnvelope serialize();

    public void parseFrom(PBCommandEnvelope commandEnvelope);

    public User getSender();

    public int getCreatedAt();

    public void setCreatedAt(int createdAt);

    public double getSortedBy();

    public void setSortedBy(double sortedBy);

    public IMessageType getType();

    public long getClientId();

    public void setClientId(long clientId);

    public long getSenderId();

    public void setSenderId(long senderId);

    public long getCommandId();

    public void setCommandId(long commandId);

    public long getChannelId();

    public void setChannelId(long channelId);

    public Contact getContact();

    public void setContact(Contact contact);

    /**
     * @return true if the message was sent by this user, otherwise false.
     */
    public boolean wasSentByThisUser();

    public String getMessagePreview(Context context);

    public Message getMessage();

    /**
     * @return an integer ID representing the layout view for this message.  This
     *         is so views can be reused in the list view.
     */
    public int getViewType();
}