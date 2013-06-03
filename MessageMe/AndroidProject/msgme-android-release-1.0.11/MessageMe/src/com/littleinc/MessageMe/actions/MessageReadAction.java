package com.littleinc.MessageMe.actions;

import android.os.Bundle;

import com.coredroid.util.LogIt;
import com.crittercism.app.Crittercism;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.ConversationReader;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.bo.Message;
import com.littleinc.MessageMe.bo.MessageMeCursor;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.data.DataBaseHelper;
import com.littleinc.MessageMe.error.UserNotFoundException;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandMessageRead;

public class MessageReadAction extends BaseAction {

    private MessageReadAction(Builder builder) {
        super(builder);
    }

    /**
     * Process a received MESSAGE_READ command.
     */
    public IMessage process() {

        PBCommandMessageRead commandMessageRead = mCommandEnvelope
                .getMessageRead();

        // Only update cursor if the command id is different to 0
        if (!mCommandEnvelope.hasCommandID()) {
            LogIt.e(this, "No command ID in MESSAGE_READ, ignore it");
            return null;
        } else if (mCommandEnvelope.getCommandID() == 0) {
            LogIt.w(MessagingService.class,
                    "Server says this is a duplicate MESSAGE_READ, ignore it",
                    mCommandEnvelope.getClientID());
            mMessagingService.getDurableCommandSender().removeFromQueue(
                    mCommandEnvelope);
            return null;
        }

        MessageMeCursor cursor = null;

        if (mCurrentUserId == mCommandEnvelope.getUserID()) {
            
            mMessagingService.getDurableCommandSender().removeFromQueue(
                    mCommandEnvelope);

            if (mConversation == null) {
                // This only happens outside a BATCH
                mConversation = Conversation.newInstance(commandMessageRead
                        .getRecipientID());
            }

            int count = Message.countUnreadMessagesSince(
                    mConversation.getChannelId(),
                    commandMessageRead.getCommandID(), mCurrentUserId);
            mConversation.setUnreadCount(count);

            if (!mInBatch) {
                // Outside a BATCH we always need to update the 
                // conversation table (when in a BATCH, the conversation 
                // gets saved after the BATCH has been processed).
                mConversation.save();
                
                // The current user may have read the message on another device
                // so we may need to update the message list unread count.
                mMessagingService.notifyChatClient(
                        MessageMeConstants.INTENT_NOTIFY_MESSAGE_LIST,
                        mConversation.getChannelId(), true);
            }

            if (mCommandEnvelope.hasCommandID()) {
                cursor = new MessageMeCursor(
                        commandMessageRead.getRecipientID(),
                        mCommandEnvelope.getCommandID());
            } else {
                LogIt.w(this, "Was expecting a command ID",
                        mCommandEnvelope.getType());
            }
        } else {

            if (mCommandEnvelope.hasCommandID()) {

                long channelId;

                if (mCurrentUserId == commandMessageRead.getRecipientID()) {
                    // If this user is the recipient then this is a private chat, 
                    // and the channel is their user ID 
                    channelId = mCommandEnvelope.getUserID();
                } else {
                    // This is not the recipient so this is a group chat, so the 
                    // channel is the room ID
                    channelId = commandMessageRead.getRecipientID();
                }

                cursor = new MessageMeCursor(channelId,
                        mCommandEnvelope.getCommandID());

                if (mConversation == null) {
                    // This only happens outside a BATCH
                    mConversation = Conversation.newInstance(channelId);
                }

                if (mConversation.getLastSentMessage() != null
                        && mConversation.getLastSentMessage().getCommandId() == commandMessageRead
                                .getCommandID()) {

                    try {                                            
                        ConversationReader reader = new ConversationReader(
                                mConversation, mCommandEnvelope.getUserID());

                        if (!mConversation.getReadUsers().contains(reader)) {
                            mConversation.getReadUsers().add(reader);
                        }
                        mConversation.setReadAt(commandMessageRead.getReadDate());

                    } catch (UserNotFoundException unfe) {
                        LogIt.w(this, "Don't add reader for null user");
                        Crittercism.logHandledException(unfe);
                    }
                }

                mConversation.save();
            } else {
                LogIt.w(this, "Was expecting a command ID",
                        mCommandEnvelope.getType());
            }
        }

        cursor.update();

        if (!mInBatch) {
            if (mCurrentUserId != commandMessageRead.getRecipientID()) {
                mMessagingService.notifyChatClient(
                        MessageMeConstants.INTENT_NOTIFY_MESSAGE_LIST,
                        mConversation.getChannelId(), true);
            }

            Bundle extras = new Bundle();
            extras.putByteArray(PBCommandEnvelope.class.getName(),
                    mCommandEnvelope.toByteArray());

            mMessagingService.notifyChatClient(
                    MessageMeConstants.INTENT_ACTION_MESSAGE_READ, extras);
        }

        return null;
    }

    public static class Builder extends BaseAction.Builder {

        public Builder(MessagingService messagingService,
                PBCommandEnvelope commandEnvelope) {
            super(messagingService, commandEnvelope);
        }

        @Override
        public Builder inBatch(boolean isInBatch) {

            mInBatch = isInBatch;
            return this;
        }

        @Override
        public Builder nextSortedBy(double nextSortedBy) {

            mNextSortedBy = nextSortedBy;
            return this;
        }

        @Override
        public Builder conversation(Conversation conversation) {

            mConversation = conversation;
            return this;
        }

        @Override
        public MessageReadAction build() {
            return new MessageReadAction(this);
        }
    }
}
