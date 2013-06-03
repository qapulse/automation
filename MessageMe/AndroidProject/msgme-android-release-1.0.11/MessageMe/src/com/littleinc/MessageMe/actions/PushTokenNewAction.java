package com.littleinc.MessageMe.actions;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandPushTokenNew;

public class PushTokenNewAction extends BaseAction {

    private PushTokenNewAction(Builder builder) {
        super(builder);
    }

    /**
     * Process a received PUSH_TOKEN_NEW command.
     */
    public IMessage process() {

        PBCommandPushTokenNew pushTokenNewCommand = mCommandEnvelope
                .getPushTokenNew();
        String gcmToken = pushTokenNewCommand.getToken();

        // Now that the server has acknowledged receipt of the GCM 
        // token we can store it locally
        LogIt.i(MessagingService.class, "Save GCM token", gcmToken);
        MessageMeApplication.getPreferences().setGcmRegisterId(gcmToken);

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
        public PushTokenNewAction build() {
            return new PushTokenNewAction(this);
        }
    }
}
