package com.littleinc.MessageMe.ui;

import static com.littleinc.MessageMe.data.DataBaseHelper.DATABASE_NAME;
import static com.littleinc.MessageMe.data.DataBaseHelper.DATABASE_VERSION;

import java.io.IOException;
import java.util.Locale;

import org.json.JSONException;
import org.restlet.resource.ResourceException;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.coredroid.core.AppLauncher;
import com.coredroid.util.BackgroundTask;
import com.coredroid.util.LogIt;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.bo.MessageMeAppPreferences;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.data.DataBaseHelper;
import com.littleinc.MessageMe.data.MMLocalData;
import com.littleinc.MessageMe.net.RestfulClient;
import com.littleinc.MessageMe.util.AlertUtil;
import com.littleinc.MessageMe.util.NetUtil;

public class MessageMeLauncher extends AppLauncher {

    private User mCurrentUser;

    private boolean mDBUpgradeNeeded;

    private boolean mDBCreationNeeded;

    private MessageMeAppPreferences mPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MessageMeApplication.registerWithCrittercism(this);

        mCurrentUser = MessageMeApplication.getCurrentUser();
        mPreferences = MessageMeApplication.getPreferences();

        // Checks if database file already exists on disk.
        // If db should be created, upgraded or downgraded the
        // splashscreen asset will be displayed if not the next
        // screen will be called directly
        if (DataBaseHelper.dbExists(this, DATABASE_NAME)) {

            if (mPreferences.getCurrentDBVersion() != DATABASE_VERSION) {

                mDBUpgradeNeeded = true;
                setContentView(R.layout.splash);

                if (mPreferences.getCurrentDBVersion() < DATABASE_VERSION) {

                    findViewById(R.id.splash_db_spinner).setVisibility(
                            View.VISIBLE);
                    findViewById(R.id.splash_db_message).setVisibility(
                            View.VISIBLE);
                }
            }
        } else {
            mDBCreationNeeded = true;
            setContentView(R.layout.splash);
        }

        if (isMandatoryUpgradeRequired()) {
            setContentView(R.layout.splash);
        }

        // Downloads the required app version from the server
        getRequiredAppVersion();

        // tick our session when we launch
        if (MessageMeApplication.getCurrentUser() != null) {
            MMLocalData.getInstance().tickSession();
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void init() {

        // CoreDroid AppLauncher#onCreate will execute CoreApplication.getState().clear()
        // that method will provoke that the preferences instance change for that reason we
        // need to update the member fields here again
        mCurrentUser = MessageMeApplication.getCurrentUser();
        mPreferences = MessageMeApplication.getPreferences();

        if (mDBCreationNeeded || mDBUpgradeNeeded) {

            // getWritableDatabase will force a db creation, upgrade or downgrade
            DataBaseHelper.getInstance().getWritableDatabase();

            // If after perform a DataBaseHelper#getWritableDatabase() the db version on the preferences
            // is -1 this means that user don't have currentDBVersion in his preferences and he has the latest 
            // db version,  if this happens the preferences currentDBVersion should be set manually
            if (mPreferences.getCurrentDBVersion() == -1) {

                LogIt.d(MessageMeLauncher.class,
                        "Database is up to date, updating preferences manually to version",
                        DATABASE_VERSION);
                mPreferences.setCurrentDBVersion(DATABASE_VERSION);
            }
        }
    }

    @Override
    protected Intent onInitComplete() {

        try {
            // For testing purposes log the locale
            Locale locale = getResources().getConfiguration().locale;
            LogIt.i(this, "Current locale", locale);
        } catch (Exception e) {
            LogIt.w(this, "Ignore error when accessing locale");
        }

        Intent intent = null;

        if (mCurrentUser != null) {
            if (mPreferences.dbNeedUpgrade()) {
                // This should not happen in Production as any DB upgrades
                // should be handled automatically in DatabaseHelper.onUpgrade
                LogIt.w(this,
                        "onInitComplete - force user to login again for database upgrade");
                intent = new Intent(this, LogInActivity.class);
            } else if (mPreferences.dbNeedsDowngrade()) {
                // This should never happen in Production but is useful during 
                // development and testing
                LogIt.w(this, "onInitComplete - database downgrade required");
                intent = new Intent(this, ShowAlertAndCloseAppActivity.class);
                intent.putExtra(ShowAlertAndCloseAppActivity.EXTRA_ALERT_TITLE,
                        getString(R.string.db_downgrade_title));
                intent.putExtra(ShowAlertAndCloseAppActivity.EXTRA_ALERT_MSG,
                        getString(R.string.db_downgrade_message));
            } else if (mPreferences.getCurrentDBVersion() < DATABASE_VERSION) {
                LogIt.w(this,
                        "onInitComplete - database upgrade must have failed");
                intent = new Intent(this, ShowAlertAndCloseAppActivity.class);
                intent.putExtra(ShowAlertAndCloseAppActivity.EXTRA_ALERT_TITLE,
                        getString(R.string.app_upgrade_error_title));
                intent.putExtra(ShowAlertAndCloseAppActivity.EXTRA_ALERT_MSG,
                        getString(R.string.app_upgrade_error_message));
            } else {
                LogIt.i(this, "Start the MessagingService");
                startService(new Intent(this, MessagingService.class));
                
                LogIt.v(this, "onInitComplete - show tabs");
                
                intent = new Intent(this, TabsFragmentActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                if (isMandatoryUpgradeRequired()) {

                    // Prompts the user to download upgrade the app using the play store,
                    // as the app version code is less than the mandatory version
                    AlertUtil.showMandatoryUpgradeAlert(MessageMeLauncher.this);

                    // Do NOT launch any activity
                    return null;
                }

                if (getIntent() != null) {

                    if (!TextUtils.isEmpty(getIntent().getAction())) {

                        // Adding the action of the caller, this will preserve the action send
                        // from gallery pictures share functions
                        intent.setAction(getIntent().getAction());
                    }

                    if (!TextUtils.isEmpty(getIntent().getType())) {

                        // Adding the type of the caller, this will preserve the action send
                        // from gallery pictures share functions
                        intent.setType(getIntent().getType());
                    }

                    if (getIntent().getExtras() != null) {

                        // Adding the entire bundle of extras into the intent to preserve the state
                        // of the user action when the TabsFragmentActivity is loaded
                        intent.putExtras(getIntent().getExtras());

                    }

                    if (isOptionalUpgradeRequired()) {
                        Intent broadCastIntent = new Intent();
                        broadCastIntent
                                .setAction(MessageMeConstants.INTENT_NOTIFY_TABS_MESSAGE);
                        broadCastIntent
                                .putExtra(
                                        MessageMeConstants.EXTRA_OPTIONAL_UPGRADE,
                                        true);
                        sendStickyBroadcast(broadCastIntent);
                    }
                }
            }
        } else {
            LogIt.v(this, "onInitComplete - show landing page");
            intent = new Intent(this, LandingActivity.class);
        }

        return intent;
    }

    @Override
    protected int getMinimumDisplayTime() {
        return 0;
    }

    public class AppVersion {

        @SerializedName("optional_version")
        private int optionalVersion;

        @SerializedName("mandatory_version")
        private int mandatoryVersion;

        public int getOptionalVersion() {
            return optionalVersion;
        }

        public void setOptionalVersion(int optionalVersion) {
            this.optionalVersion = optionalVersion;
        }

        public int getMandatoryVersion() {
            return mandatoryVersion;
        }

        public void setMandatoryVersion(int mandatoryVersion) {
            this.mandatoryVersion = mandatoryVersion;
        }
    }

    /**
     * Checks against the server the 
     * mandatory app version and the optional
     * app version
     */
    private void getRequiredAppVersion() {
        if (NetUtil.checkInternetConnection()) {

            new BackgroundTask() {

                AppVersion appVersion = null;

                @Override
                public void work() {
                    try {
                        appVersion = RestfulClient.getInstance()
                                .checkApplicationVersion();
                    } catch (JsonSyntaxException e) {
                        fail(e);
                    } catch (IOException e) {
                        fail(e);
                    } catch (JSONException e) {
                        fail(e);
                    } catch (ResourceException e) {
                        fail(e);
                    }
                }

                @Override
                public void done() {
                    if (!failed()) {
                        if (appVersion != null) {
                            MessageMeApplication.getPreferences()
                                    .setMandatoryAppVersion(
                                            appVersion.mandatoryVersion);
                            MessageMeApplication.getPreferences()
                                    .setOptionalAppVersion(
                                            appVersion.optionalVersion);

                            if (isMandatoryUpgradeRequired()) {
                                Intent broadCastIntent = new Intent();
                                broadCastIntent
                                        .setAction(MessageMeConstants.INTENT_NOTIFY_TABS_MESSAGE);
                                broadCastIntent
                                        .putExtra(
                                                MessageMeConstants.EXTRA_MANDATORY_UPGRADE,
                                                true);
                                sendStickyBroadcast(broadCastIntent);
                            }
                        }
                    }
                }
            };
        } else {
            LogIt.w(MessageMeLauncher.this,
                    "Can't verify app version againts the server, no network connection");
        }
    }

    /**
     * Checks if a mandatory application update is required
     */
    private boolean isMandatoryUpgradeRequired() {
        int appVersionCode = -1;
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(
                    getPackageName(), 0);

            appVersionCode = pi.versionCode;
        } catch (NameNotFoundException e) {
            LogIt.e(this, e.getMessage());
        }

        return appVersionCode < MessageMeApplication.getPreferences()
                .getMandatoryAppVersion();
    }

    /**
     * Checks if an optional application update is required
     */
    private boolean isOptionalUpgradeRequired() {
        int appVersionCode = -1;
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(
                    getPackageName(), 0);

            appVersionCode = pi.versionCode;
        } catch (NameNotFoundException e) {
            LogIt.e(this, e.getMessage());
        }

        return appVersionCode < MessageMeApplication.getPreferences()
                .getOptionalAppVersion();
    }

}