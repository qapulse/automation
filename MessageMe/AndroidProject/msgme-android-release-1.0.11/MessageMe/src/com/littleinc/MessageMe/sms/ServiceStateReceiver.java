package com.littleinc.MessageMe.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.ServiceState;

import com.coredroid.util.LogIt;

public class ServiceStateReceiver extends BroadcastReceiver {

    public static final String SERVICE_STATE_STATE_EXTRA = "state";

    @Override
    public void onReceive(Context context, Intent intent) {

        ServiceState serviceState = buildServiceState(intent);

        if (serviceState.getState() == ServiceState.STATE_IN_SERVICE) {

            LogIt.i(ServiceStateReceiver.class,
                    "Service state changed to 'In Service'",
                    serviceState.getState());

            // Check for pending invites
            MMSmsSender.INSTANCE.loadPendingInvitesFromFile();
        } else {

            LogIt.i(ServiceStateReceiver.class, "Service state changed to",
                    serviceState.getState());
            MMSmsSender.INSTANCE.shutDown();
        }
    }

    /**
     * Fills a {@link ServiceState} instance with the given intent extras
     */
    private ServiceState buildServiceState(Intent intent) {

        ServiceState serviceState = new ServiceState();

        if (intent.hasExtra(SERVICE_STATE_STATE_EXTRA)) {
            serviceState.setState(intent.getIntExtra(SERVICE_STATE_STATE_EXTRA,
                    0));
        }

        return serviceState;
    }
}
