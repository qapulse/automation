package com.littleinc.MessageMe.actions;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.bo.Contact;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandRoomClear;

public class RoomClearAction extends BaseAction {

    private RoomClearAction(Builder builder) {
        super(builder);
    }

    /**
     * Process a received ROOM_CLEAR command
     */
    public IMessage process() {

        PBCommandRoomClear commandRoomClear = mCommandEnvelope.getRoomClear();

        Contact channel = Contact
                .newInstance(commandRoomClear.getRecipientID());
        channel.clear();

        User currentUser = MessageMeApplication.getCurrentUser();

        if (mCommandEnvelope.getUserID() == mCurrentUserId) {
            mMessagingService.getDurableCommandSender().removeFromQueue(
                    mCommandEnvelope);
        }

        if (mCommandEnvelope.hasCommandID()) {
            // ROOM_CLEAR has to be sent on the current user channel,
            // otherwise other devices using the same account won't see it.  
            //
            // It is only sent on the current user channel to avoid sending 
            // these commands to all users in the room.
            currentUser.updateCursor(mCommandEnvelope.getCommandID(), mInBatch);
        } else {
            LogIt.w(this, "Was expecting a command ID",
                    mCommandEnvelope.getType());
        }

        if (mConversation == null) {
            // This only happens outside a BATCH
            mConversation = Conversation.newInstance(channel.getContactId());
        }

        mConversation.setUnreadCount(0);
        mConversation.setShown(false);

        if (!mInBatch) {
            // Outside a BATCH we always need to update the conversation
            // table (when in a BATCH, the conversation gets saved after 
            // the BATCH has been processed).
            mConversation.save();

            mMessagingService
                    .notifyChatClient(MessageMeConstants.INTENT_NOTIFY_MESSAGE_LIST);
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
        public RoomClearAction build() {
            return new RoomClearAction(this);
        }
    }
}
