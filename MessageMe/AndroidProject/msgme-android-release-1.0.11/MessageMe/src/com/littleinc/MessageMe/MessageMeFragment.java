package com.littleinc.MessageMe;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.app.Fragment;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.chat.MessagingService;

public class MessageMeFragment extends Fragment {

    protected MessagingService mMessagingServiceRef;

    @Override
    public void onResume() {
        super.onResume();

        Intent messagingServiceIntent = new Intent(getActivity(),
				MessagingService.class);

		getActivity().startService(messagingServiceIntent);
		getActivity().bindService(messagingServiceIntent,
				messagingServiceConnection, Context.BIND_AUTO_CREATE);

        if (MessageMeApplication.getCurrentUser() == null) {            
            getActivity().finish();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unbindService(messagingServiceConnection);
    }

    /**
     * Service that notifies when the activity is connected/binded to the messaging service
     */
    protected ServiceConnection messagingServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogIt.d(MessageMeFragment.class, "Connected to Messaging Service");
            mMessagingServiceRef = ((MessagingService.MessagingBinder) service)
                    .getService();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mMessagingServiceRef = null;
        }
    };
}