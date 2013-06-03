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
import com.littleinc.MessageMe.chat.ChatManager;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.metrics.MMFirstSessionTracker;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandRoomJoin;
import com.littleinc.MessageMe.util.NoticeUtil;

public class RoomJoinAction extends BaseAction {

    private RoomJoinAction(Builder builder) {
        super(builder);
    }

    /**
     * Process a received ROOM_JOIN command.
     * 
     * @return the NoticeMessage that was created for this command, or null
     *         if none needed to be created.
     */
    public IMessage process() {

        PBCommandRoomJoin commandGroupJoin = mCommandEnvelope.getRoomJoin();

        Room room = new Room(commandGroupJoin.getRoomID());
        User user = User.parseFrom(commandGroupJoin.getUser());
        ChatManager.addParticipantIfChatExists(room.getRoomId(), user);

        if (!User.exists(user.getUserId())) {
            if (!mInBatch) {
                LogIt.d(MessagingService.class, "Adding user", user.getUserId());
            }
            user.save();
        }

        NoticeMessage noticeMessage = null;

        RoomMember roomMember = new RoomMember(user, room);
        if (!roomMember.exists()) {
            Room.addMember(user, room);

            // Checks if the user added to the new room is the same of the application user
            // This to avoid duplicate room notices, e.g.
            // [X] added you to the room (This message comes from ROOM_NEW)
            // [X] added [Y] to the room (having "Y" as the current app user, generated in ROOM_JOIN)
            if (user.getContactId() != mCurrentUserId) {

                // Process the envelop to create the corresponding room notice
                noticeMessage = NoticeUtil.generateNoticeMessage(
                        mCommandEnvelope, NoticeType.ROOM_JOIN,
                        mNextSortedBy);

                if (mConversation == null) {
                    // This only happens outside a BATCH
                    mConversation = Conversation.newInstance(noticeMessage);
                }

                mConversation.incrementUnreadCount();

                // Validation to avoid duplicate room notices

                if (noticeMessage != null) {
                    if (mInBatch) {
                        // When in a BATCH, the conversation gets saved after 
                        // the BATCH has been processed
                        mConversation.updateLastMessage(noticeMessage);
                    } else {
                        // Outside a BATCH we always need to update the 
                        // conversation table 
                        mConversation.save();

                        mMessagingService.notifyChatClient(
                                MessageMeConstants.INTENT_ACTION_MESSAGE_NEW,
                                noticeMessage.getId(),
                                noticeMessage.getChannelId(),
                                noticeMessage.getSortedBy());

                        // Call update of the message list to notify the following notice "X has added Y to the group"
                        mMessagingService.notifyChatClient(
                                MessageMeConstants.INTENT_NOTIFY_MESSAGE_LIST,
                                noticeMessage.getId(),
                                noticeMessage.getChannelId(), true);
                    }
                } else {
                    LogIt.w(MessagingService.class,
                            "Received commandID == 0, ignoring...");
                }
            } else {
                // track if we're added to a room in the first session
                MMFirstSessionTracker.getInstance().abacusOnce(null, "join",
                        "room", null, null);
            }
        } else {
            LogIt.d(MessagingService.class,
                    "User already belongs to the group",
                    commandGroupJoin.getRoomID());
        }

        if (mCommandEnvelope.hasCommandID()) {
            room.updateCursor(mCommandEnvelope.getCommandID(), mInBatch);
        } else {
            LogIt.w(this, "Was expecting a command ID",
                    mCommandEnvelope.getType());
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
        public RoomJoinAction build() {
            return new RoomJoinAction(this);
        }
    }
}
