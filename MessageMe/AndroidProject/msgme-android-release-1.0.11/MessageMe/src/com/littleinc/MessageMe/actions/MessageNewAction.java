package com.littleinc.MessageMe.actions;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.bo.AlertSetting;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.bo.Message;
import com.littleinc.MessageMe.chat.DurableCommand;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandMessageNew;
import com.littleinc.MessageMe.util.MessageUtil;

public class MessageNewAction extends BaseAction {

    private MessageNewAction(Builder builder) {
        super(builder);
    }

    /**
     * Process a received MESSAGE_NEW command.
     * 
     * @return the IMessage that was created for this command, or null if a 
     *         problem occurred.
     */
    public IMessage process() {

        PBCommandMessageNew commandMessageNew = mCommandEnvelope
                .getMessageNew();

        final IMessage newMessage = MessageUtil
                .newMessageFromPBMessage(commandMessageNew.getMessageEnvelope());

        if (newMessage == null) {
            // This should never happen
            LogIt.e(MessagingService.class,
                    "Ignore MESSAGE_NEW with null message", commandMessageNew
                            .getMessageEnvelope().getType());
            return null;
        }

        // This does a Message.setChannelId, which results in a DB query
        newMessage.parseFrom(mCommandEnvelope);

        // We need to check for sent messages because the server will return us the
        // final commandId which is the real unique id of each message around the system, 
        // so we are using the clientId to identify which local message we should override
        DurableCommand durableCommand = mMessagingService
                .getDurableCommandSender().getDurableCommand(
                        newMessage.getClientId());
        if (durableCommand != null && durableCommand.getLocalMessage() != null) {
            // This is a command that we sent, it will already have a sortedBy
            LogIt.d(MessagingService.class, "clientID",
                    newMessage.getClientId(), "present on DurableSender");
            newMessage.setId(durableCommand.getLocalMessage().getId());
            newMessage.setSortedBy(durableCommand.getLocalMessage()
                    .getSortedBy());
        } else {
            if (!mInBatch) {
                Message localMessage = Message.loadByClientId(newMessage
                        .getClientId());

                if (localMessage != null) {
                    // The DurableCommandSender can sometimes send a message  
                    // multiple times (e.g. if the server is slow to send the  
                    // command back to the client).  This can result in
                    // duplicate commands from the server.  Those commands should 
                    // be ignored by the getDuplicate() check in CommandReceiver,
                    // but this code is left in as a defensive check.
                    LogIt.w(this, "Set ID of existing local message",
                            mCommandEnvelope.getCommandID());
                    newMessage.setId(localMessage.getId());
                }
            }

            // This is a received command, so set the sortedBy now
            newMessage.setSortedBy(mNextSortedBy);
        }

        if (mConversation == null) {

            // This only happens outside a BATCH
            mConversation = Conversation.newInstance(newMessage);
        }

        // Save the message and update the channel cursor too
        newMessage.save(true, mConversation);

        if (newMessage.getSenderId() == mCurrentUserId) {
            if (!mInBatch) {
                // For performance only log these outside of BATCH commands
                LogIt.d(MessagingService.class,
                        "Message was sent by this user, don't download attachments or generate notifications");
            }

            if (durableCommand != null) {
                mMessagingService.getDurableCommandSender().removeFromQueue(
                        durableCommand);
            }
        } else {
            // Disable in-app notifications for new messages coming from BATCH
            // commands.
            //
            // This matches the iOS implementation.  Batches can arrive at any 
            // time, however they will only contain messages it they are 
            // batches which were generated due to a COMMAND_GET, and the only 
            // time we send those is immediately after the socket connects.  
            // 
            // Another example of a batch that doesn't contain messages would 
            // be a PRESENCE_UPDATE batch  which gets sent in response to a 
            // PRESENCE_GET.  
            //
            // Once the socket is connected and the initial COMMAND_GET is  
            // done, all messages will arrive by themselves, and those
            // individual messages should generate in-app notifications.
            if (!mInBatch) {

                // Checks if the message channel has an alert block setup
                // Also checks if there's a master alert block setup
                if (!AlertSetting.hasAlertBlock(newMessage.getChannelId())
                        && !AlertSetting.hasAlertBlock(mCurrentUserId)) {

                    if (newMessage.getSender() != null) {
                        // Checks if the sender has an alert block setup, if 
                        // that's the case, don't send the in-app notification
                        mMessagingService
                                .generateInAppNotificationNewMessage(newMessage);
                    } else {
                        LogIt.w(MessagingService.class,
                                "Omitting in-app notification because sender doesn't exist yet",
                                newMessage.getSenderId());
                    }
                }
            }
        }

        if (!newMessage.wasSentByThisUser()) {
            mConversation.incrementUnreadCount();
        } else {
            mConversation.setUnreadCount(0);
        }
        mConversation.updateLastMessage(newMessage);

        if (!mInBatch) {
            // Outside a BATCH we always need to update the conversation
            // table (when in a BATCH, the conversation gets saved after 
            // the BATCH has been processed).
            mConversation.save();

            mMessagingService.notifyChatClient(
                    MessageMeConstants.INTENT_NOTIFY_MESSAGE_LIST,
                    newMessage.getId(), newMessage.getChannelId(), true);

            mMessagingService.notifyChatClient(
                    MessageMeConstants.INTENT_ACTION_MESSAGE_NEW,
                    newMessage.getId(), newMessage.getChannelId(),
                    mCommandEnvelope.toByteArray(), newMessage.getSortedBy());
        }

        return newMessage;
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
        public MessageNewAction build() {
            return new MessageNewAction(this);
        }
    }
}
