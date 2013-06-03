package com.littleinc.MessageMe.actions;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.util.DateUtil;

public abstract class BaseAction {

    protected boolean mInBatch;

    protected double mNextSortedBy;

    protected long mCurrentUserId = -1;

    protected Conversation mConversation;

    protected MessagingService mMessagingService;

    protected PBCommandEnvelope mCommandEnvelope;

    protected BaseAction(Builder builder) {

        mInBatch = builder.mInBatch;
        mNextSortedBy = builder.mNextSortedBy;
        mConversation = builder.mConversation;
        mCommandEnvelope = builder.mCommandEnvelope;
        mMessagingService = builder.mMessagingService;

        User currentUser = MessageMeApplication.getCurrentUser();

        if (currentUser == null) {
            LogIt.w(this, "Null current user");
        } else {
            mCurrentUserId = currentUser.getUserId();
        }

        if (mNextSortedBy == -1) {
            mNextSortedBy = DateUtil.getCurrentTimeMicros();
        }
    }

    /**
     * Run all processing for the PBCommand action.
     */
    abstract public IMessage process();

    protected static abstract class Builder {

        // Required
        protected MessagingService mMessagingService;

        protected PBCommandEnvelope mCommandEnvelope;

        // Optional
        protected double mNextSortedBy = -1;

        protected boolean mInBatch = false;

        protected Conversation mConversation = null;

        public Builder(MessagingService messagingService,
                PBCommandEnvelope commandEnvelope) {

            mMessagingService = messagingService;
            mCommandEnvelope = commandEnvelope;
        }

        public abstract Builder inBatch(boolean isInBatch);

        public abstract Builder nextSortedBy(double nextSortedBy);

        public abstract Builder conversation(Conversation conversation);

        public abstract BaseAction build();
    }
}
