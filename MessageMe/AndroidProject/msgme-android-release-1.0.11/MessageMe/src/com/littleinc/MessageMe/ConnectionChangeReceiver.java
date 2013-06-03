package com.littleinc.MessageMe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.util.NetUtil;

public class ConnectionChangeReceiver extends BroadcastReceiver {

    private MessagingService messagingServiceRef;

    private Context context;

    public ConnectionChangeReceiver(MessagingService messagingServiceRef,
            Context context) {
        this.context = context;
        this.messagingServiceRef = messagingServiceRef;
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (NetUtil.checkInternetConnection(context)) {
            LogIt.d(ConnectionChangeReceiver.class, "Connection restored");
            MessageMeApplication
                    .sendForegroundOnlinePresence(messagingServiceRef);
        } else {
            LogIt.d(ConnectionChangeReceiver.class, "Connection lost");
        }
    }

}
