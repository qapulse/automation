package com.littleinc.MessageMe.actions;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandUserUpdate;
import com.littleinc.MessageMe.protocol.Objects.PBUser;

public class UserUpdateAction extends BaseAction {

    private UserUpdateAction(Builder builder) {
        super(builder);
    }

    /**
     * Process a received USER_UPDATE command.
     */
    public IMessage process() {

        PBCommandUserUpdate commandUserUpdate = mCommandEnvelope
                .getUserUpdate();
        PBUser pbUser = commandUserUpdate.getUser();

        if (User.exists(pbUser.getUserID())) {
            User existingUser = new User(pbUser.getUserID());
            existingUser.load();

            existingUser.updateFromPBUser(pbUser);
            existingUser.save();

            if (mCommandEnvelope.hasCommandID()) {
                LogIt.d(MessagingService.class, "Update user and cursor",
                        pbUser.getUserID());
                // USER_UPDATE commands always happen on the user's personal
                // channel, so the channelID is the user's ID.
                MessageMeApplication.getCurrentUser().updateCursor(
                        mCommandEnvelope.getCommandID(), mInBatch);
            } else {
                // This condition is normal as USER_UPDATE commands sent in
                // response to our UserGet will not contain a command ID.
                LogIt.d(MessagingService.class, "Update user",
                        pbUser.getUserID());
            }

            if (commandUserUpdate.hasDateModified()) {
                MessageMeApplication.getPreferences().setUserGetLastModified(
                        commandUserUpdate.getDateModified());
            }

            if (!mInBatch) {
                mMessagingService
                        .notifyChatClient(MessageMeConstants.INTENT_NOTIFY_CONTACT_LIST);
                mMessagingService
                        .notifyChatClient(MessageMeConstants.INTENT_NOTIFY_MESSAGE_LIST);
            }
        } else {
            LogIt.w(MessagingService.class,
                    "USER_UPDATE received for unknown user", pbUser.getUserID());
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
        public UserUpdateAction build() {
            return new UserUpdateAction(this);
        }
    }
}
