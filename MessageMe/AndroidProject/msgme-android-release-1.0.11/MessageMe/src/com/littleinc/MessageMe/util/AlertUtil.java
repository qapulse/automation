package com.littleinc.MessageMe.util;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.R;

public class AlertUtil {
    public static void showMandatoryUpgradeAlert(final Activity activity) {
        UIUtil.alert(activity,
                activity.getString(R.string.app_upgrade_mandatory_title),
                activity.getString(R.string.app_upgrade_mandatory_message),
                activity.getString(R.string.app_upgrade_btn_update),
                new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LogIt.user(AlertUtil.class, "Clicked to open Play Store");
                        DeviceUtil.opensPlayStore(activity);

                        dialog.dismiss();
                        activity.finish();
                    }
                });
    }

    public static void showOptionalUpgradeAlert(final Activity activity) {
        UIUtil.alert(activity,
                activity.getString(R.string.app_upgrade_optional_title),
                activity.getString(R.string.app_upgrade_optional_message),
                activity.getString(R.string.app_upgrade_btn_update),
                activity.getString(R.string.app_upgrade_btn_later),
                new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LogIt.user(AlertUtil.class,
                                "Clicked to open the Play Store");
                        DeviceUtil.opensPlayStore(activity);
                        MessageMeApplication.getPreferences()
                                .setOptionalUpgradeDismissedDialogTime(0);
                        dialog.dismiss();
                        activity.finish();
                    }
                }, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LogIt.user(AlertUtil.class,
                                "user dismissed the open play store dialog");
                        MessageMeApplication.getPreferences()
                                .setOptionalUpgradeDismissedDialogTime(
                                        System.currentTimeMillis());
                        dialog.dismiss();
                    }
                });
    }
}
