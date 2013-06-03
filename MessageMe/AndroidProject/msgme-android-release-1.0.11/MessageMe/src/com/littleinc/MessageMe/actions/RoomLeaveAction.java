package com.littleinc.MessageMe.actions;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.bo.NoticeMessage;
import com.littleinc.MessageMe.bo.Room;
import com.littleinc.MessageMe.bo.RoomMember;
import com.littleinc.MessageMe.bo.NoticeType;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandRoomLeave;
import com.littleinc.MessageMe.util.NoticeUtil;

public class RoomLeaveAction extends BaseAction {

    private RoomLeaveAction(Builder builder) {
        super(builder);
    }

    /**
     * Process a received ROOM_LEAVE command.
     * 
     * @return the NoticeMessage that was created for this command, or null
     *         if none needed to be created.
     */
    public IMessage process() {

        PBCommandRoomLeave commandRoomLeave = mCommandEnvelope.getRoomLeave();

        User user = new User(mCommandEnvelope.getUserID());
        Room room = new Room(commandRoomLeave.getRoomID());

        if (mCommandEnvelope.getUserID() == mCurrentUserId) {
            LogIt.d(MessagingService.class,
                    "Removing room and disabling cursor",
                    commandRoomLeave.getRoomID());

            Conversation.delete(room.getRoomId());
            room.disableCursor(mCommandEnvelope.getCommandID(), mInBatch);
            room.delete();

            // We want to reset the connection with the server ONLY to the user
            // that has just left the room, not for all the members of the room
            if (mMessagingService.isConnected()) {
                mMessagingService.fastReconnect();
            }
            return null;
        }

        NoticeMessage noticeMessage = null;
        RoomMember roomMember = new RoomMember(user, room);

        if (roomMember.exists()) {

            if (user.getUserId() != mCurrentUserId) {

                // Process the envelop to create the corresponding room notice
                noticeMessage = NoticeUtil.generateNoticeMessage(
                        mCommandEnvelope, NoticeType.ROOM_LEAVE,
                        mNextSortedBy);

                if (mConversation == null) {
                    // This only happens outside a BATCH
                    mConversation = Conversation.newInstance(noticeMessage);
                }

                mConversation.incrementUnreadCount();

                LogIt.d(MessagingService.class, "Remove user",
                        user.getContactId(), " from room ",
                        commandRoomLeave.getRoomID());
                Room.removeMember(user, room);

                if (mCommandEnvelope.hasCommandID()) {
                    room.updateCursor(mCommandEnvelope.getCommandID(), mInBatch);
                } else {
                    LogIt.w(this, "Was expecting a command ID",
                            mCommandEnvelope.getType());
                }
            }

            // Checks for duplicate messages
            if (noticeMessage != null) {

                // Outside of a BATCH we must reconnect so the server updates its list
                // of channels the client needs to receive commands for.  Otherwise the
                // client will receive NEW_MESSAGE commands for the room that it just 
                // left.
                if (mInBatch) {
                    mConversation.updateLastMessage(noticeMessage);
                } else {
                    // Only notify when another user has left the group
                    // If the current user is the one that has left the group there's no point in
                    // notify, as the whole room conversation has been already deleted from the local DB
                    if (user.getContactId() != mCurrentUserId) {

                        // Outside a BATCH we always need to update the conversation table 
                        mConversation.save();

                        mMessagingService.notifyChatClient(
                                MessageMeConstants.INTENT_ACTION_MESSAGE_NEW,
                                noticeMessage.getId(),
                                noticeMessage.getChannelId(),
                                noticeMessage.getSortedBy());                        
                    }
                    
                    mMessagingService.notifyChatClient(
                            MessageMeConstants.INTENT_NOTIFY_MESSAGE_LIST,
                            noticeMessage.getId(),
                            noticeMessage.getChannelId(), true);
                }
            }
        } else {
            // The room cursor will have already been deleted when leaving
            // the group, so this condition is normal.
            LogIt.d(MessagingService.class,
                    "User is no longer part of the group",
                    commandRoomLeave.getRoomID());
        }

        return noticeMessage;
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
        public RoomLeaveAction build() {
            return new RoomLeaveAction(this);
        }
    }
}
