package com.littleinc.MessageMe.chat;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeConstants;

/**
 * Singleton class that manages the disconnect thread
 * Starts to run as soon as the application goes to the background
 * 
 * After {@link MessageMeConstants#DISCONNECT_THRESHOLD_LIMIT} 
 * of being in the background, disables the ping task.
 */
public class DisconnectTask implements Runnable {

    private static DisconnectTask instance;

    private MessagingService mMessagingServiceRef;

    private DisconnectTask() { 
        LogIt.i(DisconnectTask.class, "Create DisconnectTask");
    };

    public static synchronized DisconnectTask getInstance(
            MessagingService messagingService) {
    
        if (instance == null) {
            instance = new DisconnectTask();
        }
        
        instance.setMessagingService(messagingService);

        return instance;
    }
    
    private void setMessagingService(MessagingService messagingService) {
        this.mMessagingServiceRef = messagingService;
    }

    @Override
    public void run() {
        
        if (mMessagingServiceRef == null) {
            // This would mean the MessagingService has already shutdown, so
            // we don't need to do it
            LogIt.i(DisconnectTask.class, 
                    "MessagingService is null, no need to disable resources");
        } else {                
            // The DISCONNECT_THRESHOLD_LIMIT has passed, so disable any 
            // threads and shut down the web socket connection.
            //
            // If we don't disable the web socket now then Autobahn will  
            // close it after 3 minutes, which automatically triggers a 
            // reconnect by us.
            LogIt.i(DisconnectTask.class, 
                    "Been in background for a long time, disable resources");
            mMessagingServiceRef.shutdownNetworkAndThreads();
        }
    }
}
