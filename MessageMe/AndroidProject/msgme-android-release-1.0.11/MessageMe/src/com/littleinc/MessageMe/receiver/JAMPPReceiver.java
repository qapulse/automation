package com.littleinc.MessageMe.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mobileapptracker.*;

/**
 * This class acts a receiver for the JAMPP advertising SDK integration.
 * 
 * @author vivek
 * 
 */
public class JAMPPReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Tracker tracker = new Tracker();
        tracker.onReceive(context, intent);
    }

}
