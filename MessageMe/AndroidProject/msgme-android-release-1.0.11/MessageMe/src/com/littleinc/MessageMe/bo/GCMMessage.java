package com.littleinc.MessageMe.bo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.GCMIntentService;
import com.littleinc.MessageMe.MessageMeApplication;

public class GCMMessage {

    private long senderId;

    private String messageContent;

    private String messageTitle;

    public GCMMessage(long senderId, String messageTitle, String messageContent) {
        this.senderId = senderId;
        this.messageTitle = messageTitle;
        this.messageContent = messageContent;
    }

    public long getSenderId() {
        return senderId;
    }

    public void setSenderId(long senderId) {
        this.senderId = senderId;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public String getMessageTitle() {
        return messageTitle;
    }

    public void setMessageTitle(String messageTitle) {
        this.messageTitle = messageTitle;
    }

    /**
     * Removes all the stacked GCM notifications
     * for a particular sender
     */
    public static void removeGCMMessagesFromSender(long senderId) {
        int deletedMessages = 0;
        List<GCMMessage> gcmList = GCMIntentService.getGCMNotificationList();

        if (senderId == -1) {
            gcmList.clear();
            LogIt.d(MessageMeApplication.getInstance(),
                    "All stacked notifications has been deleted");

        } else {
            Iterator<GCMMessage> it = gcmList.iterator();
            while (it.hasNext()) {

                GCMMessage message = it.next();
                if (message.senderId == senderId) {
                    it.remove();
                    deletedMessages++;
                }
            }
            LogIt.d(GCMMessage.class, "Removed " + deletedMessages
                    + " stacked notifications for user:", senderId);
        }
        GCMIntentService.setGCMNotificationList(gcmList);
    }

    /**
     * Returns all the GCM notifications for a particular
     * sender
     */
    public static List<GCMMessage> getNotificationsFromSender(long senderId) {
        List<GCMMessage> gcmList = new ArrayList<GCMMessage>();
        for (GCMMessage message : GCMIntentService.getGCMNotificationList()) {
            if (message.senderId == senderId) {
                gcmList.add(message);
            }
        }

        return gcmList;
    }
}
