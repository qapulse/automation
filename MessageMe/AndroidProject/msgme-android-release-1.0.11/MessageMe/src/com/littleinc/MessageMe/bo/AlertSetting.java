package com.littleinc.MessageMe.bo;

import java.util.Date;
import java.util.Map;

import android.text.format.DateFormat;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;

/**
 * Auxiliary object that will keep track of the 
 * alert settings for each contact/group.
 *
 */
public class AlertSetting {

    private long contactID;

    private long alertPeriod;// period of time expressed in milliseconds in which the alerts will be disabled.

    private String selectedOption;// name of the alert selected option.

    public long getContactID() {
        return contactID;
    }

    public void setContactID(long contactID) {
        this.contactID = contactID;
    }

    public long getAlertPeriod() {
        return alertPeriod;
    }

    public void setAlertPeriod(long alertPeriod) {
        this.alertPeriod = alertPeriod;
    }

    public String getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(String selectedOption) {
        this.selectedOption = selectedOption;
    }

    /**
     * Removes the alert for a contactID
     * returns TRUE if the alert was removed.
     */
    public static boolean deleteAlertBlock(long contactID) {
        Map<Long, AlertSetting> alerts = MessageMeApplication
                .getPreferences().getAlertMap();

        if (alerts.containsKey(contactID)) {
            alerts.remove(contactID);
            return true;
        }

        return false;

    }

    /**
     * Checks if a particular contact has an alert setup, and removes
     * any alert block for them that has expired
     */
    public static boolean hasAlertBlock(long contactID) {
        
        if (contactID == -1) {
            return false;
        }
        
        Map<Long, AlertSetting> alertMap = 
                MessageMeApplication.getPreferences().getAlertMap();
        
        if (alertMap.containsKey(contactID)) {
            
            long currentSystemTime = System.currentTimeMillis();

            if (currentSystemTime >= getUserAlertBlock(contactID)
                    .getAlertPeriod()) {
                deleteAlertBlock(contactID);
                LogIt.d(MessageMeApplication.getInstance(),
                        "Alert block removed for user: " + contactID);
                return false;
            } else {                
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the alert setup for a particular contact.
     */
    public static AlertSetting getUserAlertBlock(long contactID) {
        AlertSetting userAlert = new AlertSetting();

        Map<Long, AlertSetting> alertMap = 
                MessageMeApplication.getPreferences().getAlertMap();
        
        if (alertMap.containsKey(contactID)) {
            userAlert = alertMap.get(contactID);
        }

        return userAlert;
    }
    
    /**
     * Returns the remaining block time of the alert in the following format:
     * it it's less than 1 day then "Off until 8:15 am" else "Off until 01/21/2013"
     */
    public static String getEndDateBlockingAlert(AlertSetting alertSetting) {

        long remainingTime = alertSetting.getAlertPeriod()
                - System.currentTimeMillis();

        String time;

        Date blockDate = new Date(alertSetting.getAlertPeriod());

        if (remainingTime > MessageMeConstants.ONE_DAY_MILLIS) {
            time = DateFormat.format("MM/dd/yyyy", blockDate).toString();
        } else {
            time = DateFormat.format("hh:mm aa", blockDate).toString();
        }
        return String.format(
                MessageMeApplication.getInstance()
                        .getString(R.string.off_until), time);

    }
}
