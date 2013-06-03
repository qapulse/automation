package com.littleinc.MessageMe.actions;

import android.os.Bundle;

import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;

public class PresenceUpdateAction extends BaseAction {

    private PresenceUpdateAction(Builder builder) {
        super(builder);
    }

    /**
     * Process a received PRESENCE_UPDATE command.
     */
    public IMessage process() {

        /* PBCommandPresenceUpdate commandPresenceUpdate = envelop
        .getPresenceUpdate();

        User user = new User(commandPresenceUpdate.getUserID());
        user.load();

        if (commandPresenceUpdate.getOnlineStatus().ordinal() == 0
        && user.getContactId() != currentUser.getContactId()) {
        generateInnAppNotificationContactOnline(user, commandPresenceUpdate
            .getOnlineStatus().toString().toLowerCase());
        }*/

        if (!mInBatch) {
            Bundle extras = new Bundle();

            extras.putByteArray(PBCommandEnvelope.class.getName(),
                    mCommandEnvelope.toByteArray());

            mMessagingService.notifyChatClient(
                    MessageMeConstants.INTENT_ACTION_MESSAGE_ACTIVITY, extras);
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
        public PresenceUpdateAction build() {
            return new PresenceUpdateAction(this);
        }
    }
}
