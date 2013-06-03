package com.littleinc.MessageMe;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;

import com.coredroid.ui.CoreFragmentActivity;
import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.littleinc.MessageMe.actionbar.ActionBarFragmentActivity;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.ui.ChatActivity;
import com.littleinc.MessageMe.ui.MMAlertDialogFragment;
import com.littleinc.MessageMe.ui.MMProgressDialogFragment;
import com.littleinc.MessageMe.ui.TabsFragmentActivity;
import com.littleinc.MessageMe.util.AlertUtil;
import com.littleinc.MessageMe.util.AudioUtil;
import com.littleinc.MessageMe.util.LogOutUtil;
import com.littleinc.MessageMe.util.NetUtil;
import com.littleinc.MessageMe.util.StringUtil;

public class MessageMeFragmentActivity extends CoreFragmentActivity {

    public static final String DEFAULT_DIALOG_TAG = "messageme_dialog";

    protected MessagingService mMessagingServiceRef;

    /**
     * Flag used to remember if we bound to the MessagingService, so we only
     * unbind if we originally bound to it.
     */
    private boolean isBound = false;

    protected LocalBroadcastManager mBroadcastManager;

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
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        Fragment dialog = getSupportFragmentManager().findFragmentByTag(
                ActionBarFragmentActivity.DEFAULT_DIALOG_TAG);

        // If a MMAlertDialogFragment is not MMAlertDialogFragment#isAvailable()
        // when the activity is restored then we should dismiss that dialog in
        // order to avoid other problems
        if (dialog != null && dialog instanceof MMAlertDialogFragment) {

            MMAlertDialogFragment alertDialogFragment = (MMAlertDialogFragment) dialog;

            if (!alertDialogFragment.isAvailableToRestore()) {
                alertDialogFragment.dismissAllowingStateLoss();

                LogIt.d(ChatActivity.class,
                        "Unable to restore destroyed dialog",
                        MMAlertDialogFragment.class.getSimpleName());
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (!MessageMeApplication.isInForeground()) {
            AudioUtil.pausePlaying(true);
        }

        MessageMeApplication.appIsInactive(MessageMeFragmentActivity.this,
                mMessagingServiceRef);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isBound) {
            // In some upgrade edge cases the TabsFragmentActivity may exit
            // during onCreate, in which case we would never have bound to
            // this service, and an exception gets thrown unbinding.
            LogIt.d(this, "Unbind from MessagingService");
            unbindService(messagingServiceConnection);
        }

        unregisterReceivers();
        unregisterGlobalReceivers();
        MessageMeApplication.onDestroy(this);
    }

    /**
     * Local receiver that register network connection changes
     */
    private BroadcastReceiver connectionChangeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (NetUtil.checkInternetConnection(MessageMeFragmentActivity.this)) {
                LogIt.d(MessageMeFragmentActivity.class, "Connection restored");
                MessageMeApplication.appIsActive(
                        MessageMeFragmentActivity.this, mMessagingServiceRef);
            } else {
                LogIt.d(MessageMeFragmentActivity.class, "Connection lost");
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
                    MessageMeFragmentActivity.this, mMessagingServiceRef);
        }
    };

    /**
     * Local receiver that register an invalid user identifier and proceed with
     * the
     * logout of the application
     */
    private BroadcastReceiver invalidUserIdentifierReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context ctx, Intent intent) {
            Bundle extras = intent.getExtras();
            if (extras.getBoolean(MessageMeConstants.USER_IDENTIFIER_FAILED,
                    false)) {
                Dialog alert = TabsFragmentActivity.getDialog();
                if (alert != null && alert.isShowing()) {
                    alert.dismiss();
                    alert = null;
                }
                LogIt.d(MessageMeFragmentActivity.class,
                        "Received USER_IDENTIFIER_FAILED, starting app logout");
                // Execute the logout
                LogOutUtil.logoutUser(MessageMeFragmentActivity.this,
                        mMessagingServiceRef);
            }
        }
    };

    /**
     * Service that notifies when the activity is connected/binded to the
     * messaging service
     */
    protected ServiceConnection messagingServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogIt.d(MessageMeFragmentActivity.class,
                    "Connected to Messaging Service");
            mMessagingServiceRef = ((MessagingService.MessagingBinder) service)
                    .getService();

            MessageMeApplication.appIsActive(MessageMeFragmentActivity.this,
                    mMessagingServiceRef);

            mBroadcastManager = LocalBroadcastManager
                    .getInstance(mMessagingServiceRef);

            unregisterReceivers();
            registerReceivers();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mMessagingServiceRef = null;

        }
    };

    /**
     * Service that notifies when a message is generated to be displayed in the
     * TabsFragmentActivity
     */
    private BroadcastReceiver notificationMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent != null) {

                if (intent.getBooleanExtra(
                        MessageMeConstants.EXTRA_OPTIONAL_UPGRADE, false)) {

                    // Removes the sticky broadcast
                    context.removeStickyBroadcast(intent);

                    long currentTime = System.currentTimeMillis();
                    long dismissedTime = MessageMeApplication.getPreferences()
                            .getOptionalUpgradeDismissedDialogTime();

                    if ((currentTime - dismissedTime) > MessageMeConstants.OPTIONAL_UPGRADE_INTERVAL_MILLIS) {
                        AlertUtil
                                .showOptionalUpgradeAlert(MessageMeFragmentActivity.this);
                    }

                } else if (intent.getBooleanExtra(
                        MessageMeConstants.EXTRA_MANDATORY_UPGRADE, false)) {

                    // Removes the sticky broadcast
                    context.removeStickyBroadcast(intent);

                    AlertUtil
                            .showMandatoryUpgradeAlert(MessageMeFragmentActivity.this);
                } else {
                    String messageTitle = intent
                            .getStringExtra(MessageMeConstants.EXTRA_TITLE);

                    String messageDescription = intent
                            .getStringExtra(MessageMeConstants.EXTRA_DESCRIPTION);

                    if (!StringUtil.isEmpty(messageTitle)
                            && !StringUtil.isEmpty(messageDescription)) {

                        UIUtil.alert(MessageMeFragmentActivity.this,
                                messageTitle, messageDescription);

                    } else {
                        LogIt.w(MessageMeFragmentActivity.this,
                                "Unrecognized message content", messageTitle,
                                messageTitle);
                    }
                }

            } else {
                LogIt.w(MessageMeFragmentActivity.this,
                        "Can't process message, intent is null");
            }
        }

    };

    private void registerGlobalReceivers() {
        registerReceiver(connectionChangeReceiver, new IntentFilter(
                MessageMeConstants.INTENT_CONNECTIVITY_CHANGE));

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);

        LogIt.d(this, "Register for Screen ON/OFF actions");
        registerReceiver(screenReceiver, filter);

        registerReceiver(notificationMessageReceiver, new IntentFilter(
                MessageMeConstants.INTENT_NOTIFY_TABS_MESSAGE));
    }

    protected void registerReceivers() {

        if (mBroadcastManager != null) {
            mBroadcastManager
                    .registerReceiver(invalidUserIdentifierReceiver,
                            new IntentFilter(
                                    MessageMeConstants.USER_IDENTIFIER_INVALID));
        }
    }

    private void unregisterGlobalReceivers() {
        try {
            LogIt.d(this, "Unregistering receivers");

            unregisterReceiver(connectionChangeReceiver);
            unregisterReceiver(screenReceiver);
            unregisterReceiver(notificationMessageReceiver);

        } catch (IllegalArgumentException e) {
            // As in the comment above, this can happen if the activity does
            // not complete its initialization
            LogIt.w(this,
                    "Ignore IllegalArgumentException unregistering receivers",
                    e.getMessage());
        }
    }

    protected void unregisterReceivers() {
        try {
            LogIt.d(this, "Unregistering receivers");
            if (mBroadcastManager != null) {
                mBroadcastManager
                        .unregisterReceiver(invalidUserIdentifierReceiver);
            }

        } catch (IllegalArgumentException e) {
            // As in the comment above, this can happen if the activity does
            // not complete its initialization
            LogIt.w(this,
                    "Ignore IllegalArgumentException unregistering receivers",
                    e.getMessage());
        }
    }

    protected MMAlertDialogFragment showAlertFragment(int titleResId,
            int messageResId, boolean allowStateLoss) {
        MMAlertDialogFragment dialogFragment = MMAlertDialogFragment
                .newInstance(titleResId, messageResId);

        dialogFragment.show(getSupportFragmentManager(), DEFAULT_DIALOG_TAG,
                allowStateLoss);
        return dialogFragment;
    }

    protected MMAlertDialogFragment showAlertFragment(String title,
            String message, boolean allowStateLoss) {
        MMAlertDialogFragment dialogFragment = MMAlertDialogFragment
                .newInstance(title, message);

        dialogFragment.show(getSupportFragmentManager(), DEFAULT_DIALOG_TAG,
                allowStateLoss);
        return dialogFragment;
    }

    protected MMProgressDialogFragment showProgressDialogFragment(
            int messageResId, boolean cancelable, boolean allowStateLoss) {
        MMProgressDialogFragment dialogFragment = MMProgressDialogFragment
                .newInstance(messageResId, cancelable);

        dialogFragment.show(getSupportFragmentManager(), DEFAULT_DIALOG_TAG,
                allowStateLoss);
        return dialogFragment;
    }

    protected MMProgressDialogFragment showProgressDialogFragment(
            String message, boolean cancelable, boolean allowStateLoss) {
        MMProgressDialogFragment dialogFragment = MMProgressDialogFragment
                .newInstance(message, cancelable);

        dialogFragment.show(getSupportFragmentManager(), DEFAULT_DIALOG_TAG,
                allowStateLoss);
        return dialogFragment;
    }
}