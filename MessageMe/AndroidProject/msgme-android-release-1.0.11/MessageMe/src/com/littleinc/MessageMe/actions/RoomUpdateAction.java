package com.littleinc.MessageMe.actions;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.bo.NoticeMessage;
import com.littleinc.MessageMe.bo.Room;
import com.littleinc.MessageMe.bo.NoticeType;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandRoomUpdate;
import com.littleinc.MessageMe.protocol.Objects.PBRoom;
import com.littleinc.MessageMe.util.NoticeUtil;

public class RoomUpdateAction extends BaseAction {

    private RoomUpdateAction(Builder builder) {
        super(builder);
    }

    /**
     * Process a received ROOM_UPDATE command.
     * 
     * @return the NoticeMessage that was created for this command, or null
     *         if none needed to be created.
     */
    public IMessage process() {

        PBCommandRoomUpdate commandRoomUpdate = mCommandEnvelope
                .getRoomUpdate();

        PBRoom pbRoom = commandRoomUpdate.getRoom();
        LogIt.d(MessagingService.class, "Update room", pbRoom.getRoomID());

        NoticeMessage noticeMessage = null;

        if (Room.exists(pbRoom.getRoomID())) {
            Room existingRoom = new Room(pbRoom.getRoomID());
            existingRoom.load();

            // Process the envelop to create the corresponding room notice
            noticeMessage = NoticeUtil
                    .generateNoticeMessage(mCommandEnvelope,
                            NoticeType.ROOM_UPDATE, mNextSortedBy);

            existingRoom.updateFromPBRoom(pbRoom);
            existingRoom.save();

            // Checks for duplicate message
            if (noticeMessage != null) {

                if (mConversation == null) {
                    // This only happens outside a BATCH
                    mConversation = Conversation.newInstance(noticeMessage);
                }

                if (mCommandEnvelope.getUserID() != MessageMeApplication
                        .getCurrentUser().getUserId()) {
                    mConversation.incrementUnreadCount();
                }

                if (mInBatch) {
                    // When in a BATCH, the conversation gets saved after 
                    // the BATCH has been processed
                    //
                    // The unread count was already updated above
                    mConversation.updateLastMessage(noticeMessage);
                } else {
                    // Outside a BATCH we save the conversation table immediately 
                    mConversation.save();

                    mMessagingService.notifyChatClient(
                            MessageMeConstants.INTENT_ACTION_MESSAGE_NEW,
                            noticeMessage.getId(),
                            noticeMessage.getChannelId(),
                            noticeMessage.getSortedBy());

                    mMessagingService
                            .notifyChatClient(
                                    MessageMeConstants.INTENT_NOTIFY_MESSAGE_LIST,
                                    noticeMessage.getId(),
                                    noticeMessage.getChannelId(), true);
                }
            }

            if (mCommandEnvelope.hasCommandID()) {
                existingRoom.updateCursor(mCommandEnvelope.getCommandID(),
                        mInBatch);
            } else {
                LogIt.w(this, "Was expecting a command ID",
                        mCommandEnvelope.getType());
            }

            if (!mInBatch) {
                mMessagingService
                        .notifyChatClient(MessageMeConstants.INTENT_NOTIFY_CONTACT_LIST);
            }
        } else {
            LogIt.w(MessagingService.class,
                    "ROOM_UPDATE received for unknown room", pbRoom.getRoomID());
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
        public RoomUpdateAction build() {
            return new RoomUpdateAction(this);
        }
    }
}
