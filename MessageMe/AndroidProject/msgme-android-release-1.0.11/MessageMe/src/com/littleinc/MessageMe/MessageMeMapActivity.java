package com.littleinc.MessageMe;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.coredroid.util.LogIt;
import com.google.android.maps.MapActivity;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.util.LogOutUtil;
import com.littleinc.MessageMe.util.NetUtil;

public class MessageMeMapActivity extends MapActivity {

    protected MessagingService mMessagingServiceRef;

    /**
     * Flag used to remember if we bound to the MessagingService, so we only
     * unbind if we originally bound to it.  
     */
    private boolean isBound = false;
    
    private LocalBroadcastManager broadcastManager;

    @Override
    protected void onResume() {
        super.onResume();
        
        MessageMeApplication.onResume(this);

        Intent messagingServiceIntent = new Intent(this, MessagingService.class);

		startService(messagingServiceIntent);
		isBound = bindService(messagingServiceIntent,
				messagingServiceConnection, Context.BIND_AUTO_CREATE);
        
		registerGlobalReceivers();
		
        if (MessageMeApplication.getCurrentUser() == null) {
            LogIt.d(this, "finishing the app");
            unregisterReceivers();
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        MessageMeApplication.appIsInactive(MessageMeMapActivity.this, mMessagingServiceRef);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isBound) {
            LogIt.d(this, "Unbind from MessagingService");
            unbindService(messagingServiceConnection);
        }
        
        unregisterReceivers();
        MessageMeApplication.onDestroy(this);
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    /**
     * Local receiver that register network connection changes
     */
    private BroadcastReceiver connectionChangeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (NetUtil.checkInternetConnection(MessageMeMapActivity.this)) {
                LogIt.d(MessageMeMapActivity.class, "Connection restored");
                MessageMeApplication.appIsActive(MessageMeMapActivity.this,
                        mMessagingServiceRef);
            } else {
                LogIt.d(MessageMeMapActivity.class, "Connection lost");
            }
        }
    };

    /**
     * Local receiver that registers the screen ON/OFF changes 
     * to determine when the phone goes to sleep
     */
    private BroadcastReceiver screenReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context ctx, Intent intent) {
            
            boolean isScreenOn = intent.getAction().equalsIgnoreCase(
                    Intent.ACTION_SCREEN_ON);
            
            MessageMeApplication.onScreenChange(isScreenOn,
                    MessageMeMapActivity.this, mMessagingServiceRef);
        }
    };

    /**
     * Local receiver that register an invalid user identifier and proceed with the 
     * logout of the application
     */
    private BroadcastReceiver invalidUserIdentifierReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context ctx, Intent intent) {
            Bundle extras = intent.getExtras();
            if (extras.getBoolean(MessageMeConstants.USER_IDENTIFIER_FAILED,
                    false)) {
                LogIt.d(MessageMeMapActivity.class,
                        "Received USER_IDENTIFIER_FAILED, starting app logout");
                // Execute the logout
                LogOutUtil.logoutUser(MessageMeMapActivity.this,
                        mMessagingServiceRef);
            }
        }
    };

    /**
     * Service that notifies when the activity is connected/binded to the messaging service
     */
    private ServiceConnection messagingServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogIt.d(MessageMeMapActivity.class, "Connected to Messaging Service");
            mMessagingServiceRef = ((MessagingService.MessagingBinder) service)
                    .getService();

            MessageMeApplication.appIsActive(MessageMeMapActivity.this,
                    mMessagingServiceRef);
            
            broadcastManager = LocalBroadcastManager
                    .getInstance(mMessagingServiceRef);

            unregisterReceivers();
            registerReceivers();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mMessagingServiceRef = null;

        }
    };
    
    private void registerGlobalReceivers() {
        registerReceiver(connectionChangeReceiver, new IntentFilter(
                MessageMeConstants.INTENT_CONNECTIVITY_CHANGE));

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);

        LogIt.d(this, "Register for Screen ON/OFF actions");
        registerReceiver(screenReceiver, filter);
    }

    private void registerReceivers() {
        registerGlobalReceivers();
        broadcastManager.registerReceiver(invalidUserIdentifierReceiver,
                new IntentFilter(MessageMeConstants.USER_IDENTIFIER_INVALID));
    }

    private void unregisterReceivers() {
        try {
            LogIt.d(this, "Unregistering receivers");
            if (broadcastManager != null) {
                broadcastManager
                        .unregisterReceiver(invalidUserIdentifierReceiver);
            }
            unregisterReceiver(connectionChangeReceiver);
            unregisterReceiver(screenReceiver);

        } catch (IllegalArgumentException e) {
            // As in the comment above, this can happen if the activity does
            // not complete its initialization
            LogIt.w(this,
                    "Ignore IllegalArgumentException unregistering receivers",
                    e.getMessage());
        }
    }
}