package com.littleinc.MessageMe.util;

import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.ui.CompleteProfileActivity;
import com.littleinc.MessageMe.ui.PhoneEntryActivity;

public class DeviceUtil extends com.coredroid.util.DeviceUtil {

    public static String GOOGLE_ACCOUNT = "com.google";

    private static final String PLAY_STORE_PACKET_NAME_OLD = "com.google.market";

    private static final String PLAY_STORE_PACKET_NAME_NEW = "com.android.vending";

    /**
     * Checks the current state of the device SIM card
     * and returns an Intent for the screen to go to
     */
    public static Intent checkSimState(Context context) {

        Intent intent = null;

        TelephonyManager telMgr = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        int simState = telMgr.getSimState();
        switch (simState) {
            case TelephonyManager.SIM_STATE_ABSENT:
                intent = new Intent(context, CompleteProfileActivity.class);
                intent.putExtra(MessageMeConstants.EXTRA_IS_CALL_CAPABLE, false);
                break;

            case TelephonyManager.SIM_STATE_READY:
                if (MessageMeApplication.getPreferences()
                        .hasSentSmsConfirmation()) {
                    intent = new Intent(context, CompleteProfileActivity.class)
                            .putExtra(MessageMeConstants.EXTRA_IS_CALL_CAPABLE,
                                    true);

                } else {
                    intent = new Intent(context, PhoneEntryActivity.class);
                }

                break;
            default:
                intent = new Intent(context, CompleteProfileActivity.class);
                intent.putExtra(MessageMeConstants.EXTRA_IS_CALL_CAPABLE, false);
                break;
        }

        return intent;
    }

    /**
     * @return whether the device seems to be capable of making phone calls.
     *
     * This is an approximation based on the state of the SIM card. This doesn't
     * attempt to handle situations where a user might not have a cellular plan,
     * but uses a data based telephony service instead (e.g. Skype).
     */
    public static boolean canPlacePhoneCall(Context context) {
        TelephonyManager telMgr = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        return telMgr.getSimState() == TelephonyManager.SIM_STATE_READY;
    }

    /**
     * Get the text country code from the SIM card, e.g. "US" or "DE".
     */
    public static String getSimCardCountryCode(Context context) {
        TelephonyManager telMgr = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);

        String countryCode = telMgr.getSimCountryIso().toUpperCase();
        LogIt.d(DeviceUtil.class, "SIM card country code", countryCode);
        return countryCode;
    }

    /**
     * Checks if a google account has been registered into the device
     */
    public static boolean checkIsGoogleAccountRegistered(Context context) {
        AccountManager accountManager = AccountManager.get(context);
        if (accountManager.getAccountsByType(GOOGLE_ACCOUNT).length == 0) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Checks if the Google Play Store application is installed
     * Can't check for an Intent, because we specifically want the Play Store,
     * and checking for an Intent may return other market applications we don't
     * care
     */
    public static boolean checkIfPlayMarketInstalled(Context context) {
        PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> packages = packageManager.getInstalledPackages(0);
        for (PackageInfo packageInfo : packages) {
            if (packageInfo.packageName.equals(PLAY_STORE_PACKET_NAME_OLD)
                    || packageInfo.packageName
                            .equals(PLAY_STORE_PACKET_NAME_NEW)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the device only have one account setup, if that's
     * the case, then we're sure that's the primary email, and returns it
     */
    public static String getDevicePrimaryEmail(Context context) {
        String primaryEmail = "";

        if (checkIsGoogleAccountRegistered(context)) {
            AccountManager accountManager = AccountManager.get(context);
            Account[] accounts = accountManager
                    .getAccountsByType(GOOGLE_ACCOUNT);
            LogIt.d(context, "Accounts found: ", accounts.length);
            if (accounts.length == 1) {
                primaryEmail = accounts[0].name;
            }
        }

        return primaryEmail;
    }

    /**
     * Check if at least one app is registered to handle the
     * ACTION_IMAGE_CAPTURE intent.
     */
    public static boolean isImageCaptureAppAvailable(Context context) {

        Intent intent = new Intent(
                android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

        List<ResolveInfo> matchedApps = context.getPackageManager()
                .queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);

        // No app installed that can capture images
        if (matchedApps.size() == 0) {
            LogIt.w(ChatAdapter.class,
                    "No app available that can capture images");
            return false;
        } else {
            return true;
        }
    }

    /**
     * Checks if the device has camera (back or front)
     */
    public static boolean isCameraAvailable(Context context) {
        PackageManager pm = context.getPackageManager();

        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)
                || pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
            LogIt.d(DeviceUtil.class, "Camera detected");
            return true;
        } else {
            LogIt.w(DeviceUtil.class, "Can't find camera");
            return false;
        }
    }

    /**
     * Checks if the device has a physical keyboard attached and available
     * for use (i.e. a slide out keyboard must already be slid out).
     */
    public static boolean isPhysicalKeyboardAvailable(Context context) {
        Configuration config = context.getResources().getConfiguration();

        if ((config.keyboard == Configuration.KEYBOARD_QWERTY)
                && (config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO)) {
            LogIt.d(DeviceUtil.class, "Physical keyboard exposed");
            return true;
        } else {
            LogIt.d(DeviceUtil.class, "No physical keyboard exposed right now");
            return false;
        }
    }

    /**
     * Return whether the user submitted the action for the provided TextView.
     *
     * This checks whether Enter was pressed on the keyboard as well as looking
     * for all IME_ACTION values that we use.
     */
    public static boolean wasActionPressed(TextView v, int actionId,
            KeyEvent event) {

        if ((event != null) && (event.getAction() == KeyEvent.ACTION_DOWN)) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                LogIt.user(DeviceUtil.class, "Pressed Enter");
                return true;
            } else {
                LogIt.d(DeviceUtil.class, "Ignore key down event");
                return false;
            }
        } else if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            LogIt.user(DeviceUtil.class, "Pressed Search");
            return true;
        } else if (actionId == EditorInfo.IME_ACTION_NEXT) {
            LogIt.user(DeviceUtil.class, "Pressed Next");
            return true;
        } else {
            LogIt.d(DeviceUtil.class, "Other condition");
            return false;
        }
    }

    /**
     * Opens the play store, and redirects the user to the
     * messageMe application in the store
     */
    public static void opensPlayStore(Context context) {
        try {
            LogIt.d(DeviceUtil.class, "opens google play store");
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                    .parse("market://details?id="
                            + MessageMeConstants.APP_NAME_KEY)));
        } catch (ActivityNotFoundException ex) {
            LogIt.w(DeviceUtil.class,
                    "user doesn't have play store, opens browser");
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                    .parse("http://play.google.com/store/apps/details?id="
                            + MessageMeConstants.APP_NAME_KEY)));
        }
    }
}
