package com.littleinc.MessageMe.util;

import android.os.Bundle;

import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.sms.MMSmsSender;

public enum SyncNotificationUtil {

    /**
     * {@link MMSmsSender} uses an Enum Type Singleton
     * Enum Type Singleton is more concise, provides the serialization machinery for free, 
     * and provides an ironclad guarantee against multiple instantiation, 
     * even in the face of sophisticated serialization or reflection attacks.
     */
    INSTANCE;

    /**
     * Flag to maintain state of the sync notification
     */
    private boolean isRefreshing;

    private int mPendingBatchCommandCount = 0;

    private MessagingService mMessagingService;

    public int getPendingBatchCommandCount() {
        return mPendingBatchCommandCount;
    }

    public void setPendingBatchCommandsCount(int pendingBatchCommandCount) {
        mPendingBatchCommandCount = pendingBatchCommandCount;
    }

    public void removePendingBatch() {
        mPendingBatchCommandCount--;
    }

    public void init(MessagingService messagingService) {
        mMessagingService = messagingService;
    }

    /**
     * Notify UI to display the refresh spinner into the action bar 
     * to let the user know the app is connecting or loading batch commands
     */
    public void showSyncNotification() {

        isRefreshing = true;
        Bundle extras = new Bundle();
        extras.putBoolean(MessageMeConstants.EXTRA_LOADING, true);

        mMessagingService.notifyChatClient(
                MessageMeConstants.INTENT_NOTIFY_APP_LOADING, extras);
    }

    /**
     * Notify UI to hide the refresh spinner from the action bar
     */
    public void cancelSyncNotification() {

        isRefreshing = false;
        Bundle extras = new Bundle();
        extras.putBoolean(MessageMeConstants.EXTRA_LOADING, false);

        mMessagingService.notifyChatClient(
                MessageMeConstants.INTENT_NOTIFY_APP_LOADING, extras);
    }

    public boolean isRefreshing() {
        return isRefreshing;
    }
}
