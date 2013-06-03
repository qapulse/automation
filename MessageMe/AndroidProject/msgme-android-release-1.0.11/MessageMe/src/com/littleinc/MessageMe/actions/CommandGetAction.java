package com.littleinc.MessageMe.actions;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.chat.CommandReceiver;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandGet;
import com.littleinc.MessageMe.util.SyncNotificationUtil;

public class CommandGetAction extends BaseAction {

    private CommandGetAction(Builder builder) {
        super(builder);
    }

    /**
     * Process a GET response.  This callback only happens when our GET is in
     * STALE_ONLY mode.
     */
    public IMessage process() {

        PBCommandGet commandGetResponse = mCommandEnvelope.getGet();

        LogIt.d(CommandReceiver.class, "GET response for STALE_ONLY mode",
                commandGetResponse.getStaleCursorsCount() + " stale cursors");
        SyncNotificationUtil.INSTANCE
                .setPendingBatchCommandsCount(commandGetResponse
                        .getStaleCursorsCount());

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
        public CommandGetAction build() {
            return new CommandGetAction(this);
        }
    }
}
