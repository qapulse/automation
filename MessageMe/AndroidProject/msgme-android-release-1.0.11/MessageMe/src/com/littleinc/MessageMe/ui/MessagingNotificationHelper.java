package com.littleinc.MessageMe.ui;

import static com.littleinc.MessageMe.MessageMeConstants.INTENT_ACTION_SHOW_IN_APP_NOTIFICATION;

import java.lang.ref.WeakReference;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.FrameLayout;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.MessageMeConstants.InAppNotificationTargetScreen;

public class MessagingNotificationHelper {

    private static final String NOTIFICATION_TAG = "com.litleinc.messagme.NOTIFICATION_TAG";

    private static final int CLOSE_NOTIFICATION = 100;

    BroadcastReceiver mReceiver;

    LocalBroadcastManager mManager;

    WeakReference<FragmentActivity> mActivity;

    /**
     * User id of user application should be showing notifications. Useful on
     * ChatActivity
     */
    private long userId;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case CLOSE_NOTIFICATION:
                closeFragment();
                break;
            default:
                break;
            }
        }
    };

    public MessagingNotificationHelper(Context context) {
        mManager = LocalBroadcastManager.getInstance(context);
    }

    public void attach(FragmentActivity activity) {
        mActivity = new WeakReference<FragmentActivity>(activity);
        mReceiver = new MessageNotificationReceiver(activity);
    }

    public void registerReceiver() {
        mManager.registerReceiver(mReceiver, new IntentFilter(
                INTENT_ACTION_SHOW_IN_APP_NOTIFICATION));
    }

    private class MessageNotificationReceiver extends BroadcastReceiver {
        WeakReference<FragmentActivity> mActivity;

        public MessageNotificationReceiver(FragmentActivity activity) {
            mActivity = new WeakReference<FragmentActivity>(activity);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            LogIt.d(this, "Received broadcast for new in app notification");

            FragmentActivity activity = mActivity.get();
            if (activity != null) {
                if (INTENT_ACTION_SHOW_IN_APP_NOTIFICATION.equals(intent.getAction())) {
                    mHandler.removeMessages(CLOSE_NOTIFICATION);

                    Bundle arguments = intent.getExtras();
                    
                    final int screenToShowOnTouch = arguments.getInt(
                            MessageMeConstants.EXTRA_SCREEN_TO_SHOW,
                            InAppNotificationTargetScreen.MESSAGE_THREAD.ordinal());
                    
                    long senderUserId = arguments.getLong(
                            MessageMeConstants.EXTRA_CONTACT_ID, -1l);

                    // Show the following sorts of in-app notifications:
                    //  -all notifications that will show a contact profile
                    //  -all notifications that will show the contacts tab
                    //  -but only show new message notifications if they were
                    //   sent from somebody other than the current user
                    if ((screenToShowOnTouch == InAppNotificationTargetScreen.CONTACT_PROFILE.ordinal())
                            || (screenToShowOnTouch == InAppNotificationTargetScreen.CONTACTS_TAB.ordinal())
                            || (senderUserId != userId)) {
                        InAppMessageNotification notification = InAppMessageNotification
                                .newInstance(arguments);

                        FragmentManager manager = activity
                                .getSupportFragmentManager();
                        FragmentTransaction transaction = manager
                                .beginTransaction();
                        transaction.setCustomAnimations(R.anim.fade_in,
                                R.anim.fade_out);
                        transaction.add(R.id.notification_container,
                                notification, NOTIFICATION_TAG);

                        // Changed from commit to commitAllowingStateLoss to avoid 
                        // crash if adding the fragment into an Activity that has gone
                        // in background
                        transaction.commitAllowingStateLoss();

                        mHandler.sendEmptyMessageDelayed(CLOSE_NOTIFICATION,
                                5000);
                    }
                }
            }
        }
    }

    public void closeFragment() {
        FragmentActivity activity = mActivity.get();

        if (activity != null) {
            FragmentManager manager = activity.getSupportFragmentManager();
            Fragment f = manager.findFragmentByTag(NOTIFICATION_TAG);

            if (f != null) {
                FragmentTransaction transaction = manager.beginTransaction();
                transaction
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
                transaction.remove(f);
                transaction.commitAllowingStateLoss();

                FrameLayout notificationContainer = (FrameLayout) activity
                        .findViewById(R.id.notification_container);
                notificationContainer.removeAllViewsInLayout();
            }
        }
    }

    public void unRegisterReceiver() {
        mManager.unregisterReceiver(mReceiver);
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }
}