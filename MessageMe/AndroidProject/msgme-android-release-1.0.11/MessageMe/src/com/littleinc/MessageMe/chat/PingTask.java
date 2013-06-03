package com.littleinc.MessageMe.chat;

import android.os.Handler;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeConstants;

/**
 * Class that regularly sends a ping to keep the web socket 
 * connection alive.
 */
public class PingTask implements Runnable {

    private ChatConnection connection;

    private Handler handler;

    public PingTask(ChatConnection connection, Handler handler) {
        LogIt.d(this, "Create PingTask");
        this.connection = connection;
        this.handler = handler;
        
        // Schedule the first ping
        if ((handler != null)
                && handler.postDelayed(this,
                        MessageMeConstants.PING_RETRY_INTERVAL)) {
            LogIt.d(PingTask.class, "First ping scheduled");
        } else {
            LogIt.w(PingTask.class, "Failed to schedule first ping");
        }
    }

    @Override
    public void run() {
    	LogIt.d(this, "PingTask run()", this);
        connection.sendPing();
        
        if (handler == null) {
            LogIt.w(this, "Handler is null, cannot schedule next ping");
        } else if (!handler.postDelayed(this,
                MessageMeConstants.PING_RETRY_INTERVAL)) {
            LogIt.w(this, "Failed to schedule next ping");
        }
    }
    
    public void shutdown() {
        if (handler != null) {
            LogIt.d(this, "Remove outstanding ping callback", this);
            handler.removeCallbacks(this);
        } else {
            LogIt.d(this, "Null handler, no ping callback to remove");
        }
    }
}
