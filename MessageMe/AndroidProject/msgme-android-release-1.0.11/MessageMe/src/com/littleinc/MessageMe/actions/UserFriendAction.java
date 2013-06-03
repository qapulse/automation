package com.littleinc.MessageMe.actions;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;

public class UserFriendAction extends BaseAction {

    private UserFriendAction(Builder builder) {
        super(builder);
    }

    /**
     * Process a received USER_FRIEND command.
     */
    public IMessage process() {

        User.performFriend(mCommandEnvelope, mInBatch);

        if (!mInBatch) {
            LogIt.d(this, "USER_FRIEND");
            mMessagingService
                    .notifyChatClient(MessageMeConstants.INTENT_NOTIFY_CONTACT_LIST);
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
        public UserFriendAction build() {
            return new UserFriendAction(this);
        }
    }
}
