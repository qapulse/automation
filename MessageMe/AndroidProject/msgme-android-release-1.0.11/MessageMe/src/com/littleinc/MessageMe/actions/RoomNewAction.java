package com.littleinc.MessageMe.actions;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.MessageMeConstants.InAppNotificationTargetScreen;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.bo.MessageMeCursor;
import com.littleinc.MessageMe.bo.NoticeMessage;
import com.littleinc.MessageMe.bo.NoticeType;
import com.littleinc.MessageMe.bo.Room;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandRoomNew;
import com.littleinc.MessageMe.util.NoticeUtil;

public class RoomNewAction extends BaseAction {

    private RoomNewAction(Builder builder) {
        super(builder);
    }

    /**
     * Process a received ROOM_NEW command.
     */
    public IMessage process() {

        Room room;
        NoticeMessage newMessage = null;
        PBCommandRoomNew commandRoomNew = mCommandEnvelope.getRoomNew();

        // Create a new cursor for the room that we've joined
        MessageMeCursor newRoomCursor = null;

        // Create any users in the room
        Room.createRoomUsers(commandRoomNew.getRoom());

        if (commandRoomNew.getRoom().getRoomID() == 0) {
            long channelId = Room.getChannelIdFromPrivateRoom(commandRoomNew
                    .getRoom());

            // New room cursors should always start at command ID 0, as 
            // otherwise new messages might not be received in certain
            // server ID generation failover scenarios
            newRoomCursor = new MessageMeCursor(channelId, 0);

            if (mConversation == null) {

                // This only happens outside a BATCH
                mConversation = Conversation.newInstance(channelId);
            }

            LogIt.d(MessagingService.class, "Private room with user", channelId);
        } else {
            room = Room.parseFrom(commandRoomNew.getRoom());

            if (mConversation == null) {

                // This only happens outside a BATCH
                mConversation = Conversation.newInstance(room.getContactId());
            }

            room.save();
            LogIt.d(MessagingService.class, "New room", room.getRoomId(),
                    room.getName());

            // New room cursors should always start at command ID 0, as 
            // otherwise new messages might not be received in certain
            // server ID generation failover scenarios
            newRoomCursor = new MessageMeCursor(room.getRoomId(), 0);

            mMessagingService.sendInAppNotification(
                    mMessagingService.getString(R.string.group_addition),
                    room.getName(), null, room.getRoomId(),
                    InAppNotificationTargetScreen.MESSAGE_THREAD);

            // Process the envelop to create the corresponding room notice
            newMessage = NoticeUtil.generateNoticeMessage(mCommandEnvelope,
                    NoticeType.ROOM_NEW, mNextSortedBy);

            if (!newMessage.wasSentByThisUser()) {
                mConversation.incrementUnreadCount();
            } else {
                mConversation.setUnreadCount(0);
            }
            mConversation.setLastMessage(newMessage.getMessage());
        }

        newRoomCursor.create();

        if (mCommandEnvelope.hasCommandID()) {
            // Update the cursor for the user's personal channel to
            // say we've processed everything successfully
            MessageMeCursor personalCursor = new MessageMeCursor(
                    mCurrentUserId, mCommandEnvelope.getCommandID());
            personalCursor.update();
        }

        if (mInBatch) {
            LogIt.d(MessagingService.class, "Get commands for room",
                    newRoomCursor.getChannelId());

            // Inside a BATCH we need to ask for commands in the room
            // we've just joined
            PBCommandEnvelope commandEnvelope = mMessagingService
                    .buildCommandGet(newRoomCursor.getChannelId(),
                            newRoomCursor.getLastCommandId());
            try {
                mMessagingService.sendCommand(commandEnvelope);
            } catch (Exception e) {
                LogIt.e(MessagingService.class, e, e.getMessage());
            }
        } else {

            mConversation.save();

            if (newMessage == null) {
                mMessagingService
                        .notifyChatClient(MessageMeConstants.INTENT_NOTIFY_MESSAGE_LIST);
            } else {
                mMessagingService.notifyChatClient(
                        MessageMeConstants.INTENT_NOTIFY_MESSAGE_LIST,
                        newMessage.getId(), newMessage.getChannelId(), true);
            }
            mMessagingService
                    .notifyChatClient(MessageMeConstants.INTENT_NOTIFY_CONTACT_LIST);

            LogIt.d(MessagingService.class,
                    "Reconnect so the server will give us new room commands",
                    newRoomCursor.getChannelId());

            // Outside of a BATCH we must reconnect so the server updates 
            // its list of channel subscriptions for the user
            mMessagingService.fastReconnect();
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
        public RoomNewAction build() {
            return new RoomNewAction(this);
        }
    }
}
