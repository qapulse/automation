package com.littleinc.MessageMe.actions;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.MessageMeConstants.InAppNotificationTargetScreen;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.bo.NoticeMessage;
import com.littleinc.MessageMe.bo.NoticeType;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandUserJoin;
import com.littleinc.MessageMe.protocol.Objects.PBUser;
import com.littleinc.MessageMe.util.NoticeUtil;

public class UserJoinAction extends BaseAction {

    private UserJoinAction(Builder builder) {
        super(builder);
    }

    /**
     * Process a received USER_JOIN command.
     */
    public IMessage process() {

        PBCommandUserJoin pbCmdUserJoin = mCommandEnvelope.getUserJoin();
        PBUser friendWhoJoined = pbCmdUserJoin.getUser();

        User user = User.parseFrom(friendWhoJoined);
        user.setShown(true);
        user.save();

        LogIt.i(MessagingService.class,
                "Contact in address book joined MessageMe, add to Contacts",
                user.getDisplayName());

        if (mCommandEnvelope.hasCommandID()) {
            MessageMeApplication.getCurrentUser().updateCursor(
                    mCommandEnvelope.getCommandID(), mInBatch);
        } else {
            LogIt.w(this, "Was expecting a command ID",
                    mCommandEnvelope.getType());
        }

        if (mConversation == null) {
            mConversation = Conversation.newInstance(friendWhoJoined
                    .getUserID());
        }

        NoticeMessage message = NoticeUtil.generateNoticeMessage(
                mCommandEnvelope, NoticeType.USER_JOIN, mNextSortedBy);

        if (!mInBatch) {
            mMessagingService
                    .notifyChatClient(MessageMeConstants.INTENT_NOTIFY_CONTACT_LIST);
            mMessagingService.notifyChatClient(
                    MessageMeConstants.INTENT_NOTIFY_MESSAGE_LIST,
                    message.getId(), message.getChannelId());

            mMessagingService.sendInAppNotification(user.getDisplayName(),
                    mMessagingService.getString(R.string.user_banner_desc),
                    user.getProfileImageKey(), user.getContactId(),
                    InAppNotificationTargetScreen.CONTACT_PROFILE);
        } else {

            mConversation.setShown(true);
            mConversation.save();
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
        public UserJoinAction build() {
            return new UserJoinAction(this);
        }
    }
}
