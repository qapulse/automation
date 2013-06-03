package com.littleinc.MessageMe.util;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Parcelable;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.bo.AddressBook;
import com.littleinc.MessageMe.data.MMLocalData;
import com.littleinc.MessageMe.metrics.MMFirstSessionTracker;
import com.littleinc.MessageMe.ui.ContactListFragment;
import com.littleinc.MessageMe.ui.MultiInviteActivity;

public class FriendsInviteUtil {

    /**
     * Minimum number of no MessageMe users present in our address book in order to display
     * the multi-invite activity
     */
    public static final int MIN_NUM_CONTACTS_TO_DISPLAY_MULTI_INVITE = 5;

    public static final String SMS_SCHEME = "sms:";

    public static final String SMS_BODY_EXTRA = "sms_body";

    public static final String MAIL_MIME_TYPE = "message/rfc822";

    public static final String TEXT_PLAIN_MIME_TYPE = "text/plain";

    public static final String SMS_MIME_TYPE = "vnd.android-dir/mms-sms";

    /**
     * Set of packages to exclude from our custom list.  We exclude any apps
     * that provide a bad sharing user experience.
     */
    private static final String[] EXCLUDED_PACKAGES = new String[] {
            "com.google.android.googlequicksearchbox", // Google Search
            "com.google.android.apps.translate", // Google Translate
            "com.android.bluetooth", // Bluetooth
            "com.sec.android.app.FileShareClient", // Wi-Fi Direct
            "com.sec.android.widgetapp.diotek.smemo", // S Memo
            "la.droid.qr" // QR Droid 
    };

    /**
     * Returns a list of {@link ResolveInfo} of all the SMS apps available in
     * the device
     */
    public static List<ResolveInfo> getSMSOptions(Context context) {

        // Don't include any SMS options if the device can't make 
        // phone calls
        if (!DeviceUtil.canPlacePhoneCall(context)) {
            return new LinkedList<ResolveInfo>();
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(SMS_SCHEME));
        intent.setType(SMS_MIME_TYPE);

        List<ResolveInfo> list = context.getPackageManager()
                .queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);

        if (list.size() > 0) {
            return list;
        } else {
            return new LinkedList<ResolveInfo>();
        }
    }

    /**
     * Returns a list of {@link ResolveInfo} of all the Mail apps available in
     * the device
     */
    public static List<ResolveInfo> getMailOptions(Context context) {

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(MAIL_MIME_TYPE);

        List<ResolveInfo> list = context.getPackageManager()
                .queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);

        if (list.size() > 0) {
            return list;
        } else {
            return new LinkedList<ResolveInfo>();
        }
    }

    /**
     * Checks if the given package is contained in the given list
     */
    public static boolean containsPackage(List<ResolveInfo> list,
            ResolveInfo appInfo) {

        for (ResolveInfo resolveInfo : list) {
            if (resolveInfo.activityInfo.packageName
                    .equals(appInfo.activityInfo.packageName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the given package should be excluded
     */
    private static boolean shouldBeExcluded(String packageName) {

        for (String excludedPackage : EXCLUDED_PACKAGES) {
            if (packageName.equals(excludedPackage)) {
                LogIt.d(FriendsInviteUtil.class,
                        "Exclude app from invite chooser", packageName);
                return true;
            }
        }
        return false;
    }

    /**
     * Open the {@link MultiInviteActivity} or an Intent chooser, depending
     * on the number of entries in the user's address book, and any prior
     * history of sending SMSes in the background.
     */
    public static void openFriendsInvite(final Activity activity) {

        if (DeviceUtil.canPlacePhoneCall(activity)) {

            new DatabaseTask() {

                int contactsCount;

                @Override
                public void work() {
                    try {
                        contactsCount = AddressBook
                                .countNonMMUsersInAddressBook(activity);
                    } catch (SQLException e) {
                        fail(e);
                    }
                }

                @Override
                public void done() {

                    if (failed()
                            || contactsCount < MIN_NUM_CONTACTS_TO_DISPLAY_MULTI_INVITE) {

                        FriendsInviteUtil.showSendInviteChooser(activity);
                    } else {

                        int successInvites = MessageMeApplication
                                .getPreferences().getNumSentInvites();
                        int failedInvites = MessageMeApplication
                                .getPreferences().getNumFailedInvites();
                        int total = successInvites + failedInvites;

                        if (total == 0) {

                            // No SMS invites sent yet, we are good to should the multi-invite
                            // activity
                            LogIt.d(ContactListFragment.class,
                                    "No history sending sms invites, show multi-invite activity");

                            Intent multiInviteIntent = new Intent(activity,
                                    MultiInviteActivity.class);
                            activity.startActivity(multiInviteIntent);
                        } else if (successInvites > 0) {

                            // If our success invite count is higher than 1 means that we can
                            // send SMS messages then we are good to should the multi-invite

                            LogIt.i(ContactListFragment.class, "Sent",
                                    successInvites,
                                    "invites, show multi-invite activity");
                            Intent multiInviteIntent = new Intent(activity,
                                    MultiInviteActivity.class);
                            activity.startActivity(multiInviteIntent);
                        } else {

                            // We have only failed SMS sends, is safer show the SMS intent to
                            // let user decides what channel use to share the messageMe

                            LogIt.d(ContactListFragment.class,
                                    "No success invites, show invite intent");
                            FriendsInviteUtil.showSendInviteChooser(activity);
                        }
                    }
                }
            };
        } else {
            FriendsInviteUtil.showSendInviteChooser(activity);
        }
    }

    public static void showSendInviteChooser(Activity context) {
        // first session tracking
        Integer order = MMLocalData.getInstance()
                .getSessionOrder();
        MMFirstSessionTracker.getInstance().abacus(null,
                "invite_chooser", "screen", order, null);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(TEXT_PLAIN_MIME_TYPE);

        List<ResolveInfo> smsOptions = getSMSOptions(context);
        List<ResolveInfo> mailOptions = getMailOptions(context);

        if (smsOptions.size() == 0 && mailOptions.size() == 0) {

            LogIt.d(FriendsInviteUtil.class,
                    "No SMS/Mail options, showing general SEND intent");

            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    context.getString(R.string.sms_invite_body));

            context.startActivity(shareIntent);
        } else {
            boolean useMailIntent = mailOptions.size() > 0 ? true : false;

            List<Intent> targetedShareIntents = new LinkedList<Intent>();
            List<ResolveInfo> resolveInfos = context.getPackageManager()
                    .queryIntentActivities(shareIntent,
                            PackageManager.MATCH_DEFAULT_ONLY);

            for (ResolveInfo resolveInfo : resolveInfos) {

                String appPackage = resolveInfo.activityInfo.packageName;

                // Some apps are listed in the ACTION_SEND intent even when they
                // are not able to share content. shouldBeExcluded() will check in our
                // package black list in order to avoid show those apps in the chooser
                if (!shouldBeExcluded(appPackage)) {

                    LogIt.d(FriendsInviteUtil.class,
                            "Include app in invite chooser", appPackage);

                    Intent targetedShareIntent = new Intent(Intent.ACTION_SEND);

                    targetedShareIntent.setType(TEXT_PLAIN_MIME_TYPE);
                    targetedShareIntent.setComponent(new ComponentName(
                            appPackage, resolveInfo.activityInfo.name));
                    targetedShareIntent.putExtra(
                            android.content.Intent.EXTRA_TEXT,
                            context.getString(R.string.sms_invite_body));

                    if (useMailIntent) {

                        if (!containsPackage(mailOptions, resolveInfo)) {
                            targetedShareIntents.add(targetedShareIntent);
                        }
                    } else {

                        if (!containsPackage(smsOptions, resolveInfo)) {
                            targetedShareIntents.add(targetedShareIntent);
                        }
                    }
                }
            }

            if (useMailIntent) {

                LogIt.d(FriendsInviteUtil.class,
                        "Are Mail options, showing chooser based on Mail intent");

                shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType(MAIL_MIME_TYPE);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT,
                        context.getString(R.string.email_invite_subject));
                shareIntent.putExtra(Intent.EXTRA_TEXT,
                        context.getString(R.string.email_invite_body));
            } else {

                LogIt.d(FriendsInviteUtil.class,
                        "Are SMS options, showing chooser based on SMS intent");

                shareIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(SMS_SCHEME));
                shareIntent.setType(SMS_MIME_TYPE);
                shareIntent.putExtra(SMS_BODY_EXTRA,
                        context.getString(R.string.sms_invite_body));
            }

            // The Intent.createChooser needs a base intent to be able of
            // display the chooser so we going to use the Mail/SMS specific 
            // intent as base to avoid duplicates in the list
            Intent openInChooser = Intent.createChooser(shareIntent,
                    context.getString(R.string.invite_friends_label));

            // EXTRA_INITIAL_INTENTS help us to add additional activities to
            // place a the front of the list of choices, so the chooser will
            // have the targetedShareIntents + Mail/SMS options
            openInChooser.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                    targetedShareIntents.toArray(new Parcelable[] {}));

            context.startActivity(openInChooser);
        }
    }
}