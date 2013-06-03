package com.littleinc.MessageMe.actions;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandUserBlock;

public class UserBlockAction extends BaseAction {

    private UserBlockAction(Builder builder) {
        super(builder);
    }

    /**
     * Process a received USER_BLOCK command.
     */
    public IMessage process() {

        PBCommandUserBlock userBlock = mCommandEnvelope.getUserBlock();
        User currentUser = MessageMeApplication.getCurrentUser();

        if ((currentUser == null) || !mCommandEnvelope.hasUserID()
                || !userBlock.hasUserID()) {
            LogIt.w(this,
                    "Ignore USER_BLOCK message due to missing user information",
                    currentUser);
            return null;
        }

        if (currentUser.getContactId() == mCommandEnvelope.getUserID()) {
            // We blocked somebody
            long userID = userBlock.getUserID();
            User user = new User(userID);
            user.load();
            user.setIsBlocked(true);
            user.save();

            if (!mInBatch) {
                LogIt.d(this, "USER_BLOCK - this user blocked " + userID);
            }
        } else {
            if (currentUser.getContactId() != userBlock.getUserID()) {
                LogIt.w(this,
                        "Unexpected user ID inside USER_BLOCK command, ignore it");
                return null;
            }

            // Somebody blocked us
            long userID = mCommandEnvelope.getUserID();
            User user = new User(userID);
            user.load();
            user.setBlockedBy(true);
            user.save();

            if (!mInBatch) {
                LogIt.d(this, "USER_BLOCK - " + userID + " blocked this user");
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
        public UserBlockAction build() {
            return new UserBlockAction(this);
        }
    }
}
