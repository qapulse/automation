package com.littleinc.MessageMe.actions;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandUserUnblock;

public class UserUnblockAction extends BaseAction {

    private UserUnblockAction(Builder builder) {
        super(builder);
    }

    /**
     * Process a received USER_UNBLOCK command.
     */
    public IMessage process() {

        PBCommandUserUnblock userUnblock = mCommandEnvelope.getUserUnblock();
        User currentUser = MessageMeApplication.getCurrentUser();

        if ((currentUser == null) || !mCommandEnvelope.hasUserID()
                || !userUnblock.hasUserID()) {
            LogIt.w(this,
                    "Ignore USER_UNBLOCK message due to missing user information",
                    currentUser);
            return null;
        }

        if (currentUser.getContactId() == mCommandEnvelope.getUserID()) {
            // We unblocked somebody
            long userID = userUnblock.getUserID();
            User user = new User(userID);
            user.load();
            user.setIsBlocked(false);
            user.save();

            if (!mInBatch) {
                LogIt.d(this, "USER_UNBLOCK - this user unblocked " + userID);
            }
        } else {
            if (currentUser.getContactId() != userUnblock.getUserID()) {
                LogIt.w(this,
                        "Unexpected user ID inside USER_UNBLOCK command, ignore it");
                return null;
            }

            // Somebody unblocked us
            long userID = mCommandEnvelope.getUserID();
            User user = new User(userID);
            user.load();
            user.setBlockedBy(false);
            user.save();

            if (!mInBatch) {
                LogIt.d(this, "USER_UNBLOCK - " + userID
                        + " unblocked this user");
            }
        }

        if (mCommandEnvelope.hasCommandID()) {
            // Update the current user cursor 
            currentUser.updateCursor(mCommandEnvelope.getCommandID(), mInBatch);
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
        public UserUnblockAction build() {
            return new UserUnblockAction(this);
        }
    }
}
