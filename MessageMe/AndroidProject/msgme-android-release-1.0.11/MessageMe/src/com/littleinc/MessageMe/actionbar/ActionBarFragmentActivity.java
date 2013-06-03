package com.littleinc.MessageMe.actionbar;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.MessageMeFragmentActivity;
import com.littleinc.MessageMe.util.SyncNotificationUtil;

public class ActionBarFragmentActivity extends MessageMeFragmentActivity {

    final ActionBarHelper mActionBarHelper = ActionBarHelper
            .createInstance(this);

    /**
     * Returns the {@link ActionBarHelper} for this activity.
     */
    protected ActionBarHelper getActionBarHelper() {
        return mActionBarHelper;
    }

    /** {@inheritDoc} */
    @Override
    public MenuInflater getMenuInflater() {
        return mActionBarHelper.getMenuInflater(super.getMenuInflater());
    }

    /** {@inheritDoc} */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionBarHelper.onCreate(savedInstanceState);
    }

    /** {@inheritDoc} */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mActionBarHelper.onPostCreate(savedInstanceState);
    }

    /**
     * Base action bar-aware implementation for
     * {@link Activity#onCreateOptionsMenu(android.view.Menu)}.
     * 
     * Note: marking menu items as invisible/visible is not currently supported.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        boolean retValue = false;

        retValue |= mActionBarHelper.onCreateOptionsMenu(menu);
        retValue |= super.onCreateOptionsMenu(menu);

        // INTENT_NOTIFY_APP_LOADING is fire only at some moments so this check
        // is necessary in order to maintain the spinner up when new chat
        // activities are opened
        if (SyncNotificationUtil.INSTANCE.isRefreshing()) {
            getActionBarHelper().setRefreshActionItemState(true);
        }

        return retValue;
    }

    /** {@inheritDoc} */
    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        mActionBarHelper.onTitleChanged(title, color);
        super.onTitleChanged(title, color);
    }
    
    /**
     * Show or hide the provided menuItem, checking first if it is null to
     * avoid crashes (which happens during activity recreation).
     */
    public void setMenuItemVisible(MenuItem menuItem, boolean showItem) {
        if (menuItem == null) {
            LogIt.w(this, "Ignore setMenuItemVisible for null menu item");
        } else {
            if (showItem) {
                showMenuItem(menuItem);
            } else {
                hideMenuItem(menuItem);
            }
        }
    }

    public void hideMenuItem(MenuItem item) {
        mActionBarHelper.hideMenuItem(item);
    }

    public void showMenuItem(MenuItem item) {
        mActionBarHelper.showMenuitem(item);
    }

    public void setHomeIcon(int resID) {
        mActionBarHelper.setHomeIcon(resID);
    }

    public void disableHomeButton() {
        mActionBarHelper.disableHomeButton();
    }

    @Override
    protected void registerReceivers() {
        super.registerReceivers();

        if (mBroadcastManager != null) {
            mBroadcastManager.registerReceiver(mLoadingReceiver,
                    new IntentFilter(
                            MessageMeConstants.INTENT_NOTIFY_APP_LOADING));
        }
    }

    @Override
    protected void unregisterReceivers() {
        super.unregisterReceivers();

        try {
            if (mBroadcastManager != null) {
                mBroadcastManager.unregisterReceiver(mLoadingReceiver);
            }
        } catch (IllegalArgumentException e) {

            // As in the comment above, this can happen if the activity does
            // not complete its initialization
            LogIt.w(ActionBarFragmentActivity.class,
                    "Ignore IllegalArgumentException unregistering receivers",
                    e.getMessage());
        }
    }

    /**
     * Service that notifies actionbar refresh spinner
     */
    protected BroadcastReceiver mLoadingReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent != null) {

                boolean isRefreshing = intent.getBooleanExtra(
                        MessageMeConstants.EXTRA_LOADING, false);
                getActionBarHelper().setRefreshActionItemState(isRefreshing);
            } else {
                LogIt.w(ActionBarFragmentActivity.this,
                        "Can't process message, intent is null");
            }
        }
    };
}
