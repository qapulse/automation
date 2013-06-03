package com.littleinc.MessageMe.util;

import android.content.Context;
import android.content.DialogInterface;
import android.widget.TextView;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.bo.AlertSetting;

public class AlertPickClickListener implements DialogInterface.OnClickListener {

    private long contactID;

    private Context context;

    private TextView alertBlockPeriod;

    long currentTime = System.currentTimeMillis();

    private AlertSetting newAlert = new AlertSetting();

    public AlertPickClickListener(long contactID, Context context,
            TextView alertBlockPeriod) {
        this.contactID = contactID;
        this.context = context;
        this.alertBlockPeriod = alertBlockPeriod;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
        case 0:
            AlertSetting.deleteAlertBlock(contactID);
            LogIt.d(this, "Alert removed for user: " + contactID);
            alertBlockPeriod.setText(context.getString(R.string.alerts_on));
            break;
        case 1:
            currentTime += MessageMeConstants.ALERT_SETTINGS_ONE_HOUR;
            newAlert.setContactID(contactID);
            newAlert.setAlertPeriod(currentTime);
            newAlert.setSelectedOption(context
                    .getString(R.string.alerts_off_for_1_hour));
            alertBlockPeriod.setText(AlertSetting
                    .getEndDateBlockingAlert(newAlert));
            MessageMeApplication.getPreferences().setAlertMap(
                    newAlert.getContactID(), newAlert);
            LogIt.d(this,
                    "New alert setup for contact: " + contactID
                            + " setup for: "
                            + context.getString(R.string.alerts_off_for_1_hour));
            break;
        case 2:
            currentTime += MessageMeConstants.ALERT_SETTINGS_THREE_DAYS;
            newAlert.setContactID(contactID);
            newAlert.setAlertPeriod(currentTime);
            newAlert.setSelectedOption(context
                    .getString(R.string.alerts_off_for_3_days));
            alertBlockPeriod.setText(AlertSetting
                    .getEndDateBlockingAlert(newAlert));
            MessageMeApplication.getPreferences().setAlertMap(
                    newAlert.getContactID(), newAlert);
            LogIt.d(this,
                    "New alert setup for contact: " + contactID
                            + " setup for: "
                            + context.getString(R.string.alerts_off_for_3_days));
            break;
        default:
            break;

        }

    }
}
