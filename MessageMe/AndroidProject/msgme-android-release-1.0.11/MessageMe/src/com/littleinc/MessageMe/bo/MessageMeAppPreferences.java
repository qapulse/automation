package com.littleinc.MessageMe.bo;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.location.Location;

import com.coredroid.core.DirtyableCoreObject;
import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.bo.AddressBook.AddressBookState;
import com.littleinc.MessageMe.data.DataBaseHelper;

/**
 * A simple example of handling application preferences
 */
public class MessageMeAppPreferences extends DirtyableCoreObject {

    private User user;

    private String token;

    private String awsAccessKey;

    private String awsSecretKey;

    private String awsSessionToken;

    private String gcmRegisterId;

    // AWS keys expire every 36 hours
    private Date awsExpirationDate;

    private Location lastKnownLocation;

    // Flag to identify if the database need to be upgraded
    private boolean dbNeedUpgrade = false;

    private Map<Long, AlertSetting> alertMap = new HashMap<Long, AlertSetting>();

    /**
     * Preference to store the email or phone of the last logged {@link User}
     * This field will be updated on every log out action.
     * ({@link MessageMeApplication#resetPreferences()})
     */
    private String mLastLoginID = "";

    private String registeredPhoneNumber = "";

    private boolean sentSmsConfirmation = false;

    private String phoneSignature = "";

    private AddressBookState abState;

    private int userCountryCode = -1;

    private boolean soundAlert = true;

    private boolean vibrateAlert = true;

    private boolean isGCMNotificationsAvailable = true;

    private boolean isAppRated;

    private Date lastTimeShowedRatePopup;

    private int launchCount = 0;

    private Date lastTimeInForeground;

    private int rateAppPopupShownCount = 0;
    
    private boolean dbNeedsDowngrade = false;

    /**
     * The timestamp of the last set of UserUpdate actions that we downloaded
     * from the server.
     */
    private int lastModifiedGetUserUpdate = 0;

    private int numSentInvites;

    private int numFailedInvites;

    /**
     * Field to know the current DB version without create a
     * {@link DataBaseHelper}
     * 
     * Users running 1.0.8 (42) and below, don't have this field
     */
    private int mCurrentDBVersion = -1;

    private int mandatoryAppVersion = -1;

    private int optionalAppVersion = -1;

    private long optionalUpgradeDismissedDialogTime = 0;

    @Override
    public boolean isPersistent() {
        return true;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        dirty();
        this.user = user;
    }

    public String getAwsAccessKey() {
        return awsAccessKey;
    }

    public void setAwsAccessKey(String awsAccessKey) {
        dirty();
        this.awsAccessKey = awsAccessKey;
    }

    public String getAwsSecretKey() {
        return awsSecretKey;
    }

    public void setAwsSecretKey(String awsSecretKey) {
        dirty();
        this.awsSecretKey = awsSecretKey;
    }

    public String getAwsSessionToken() {
        return awsSessionToken;
    }

    public void setAwsSessionToken(String awsSessionToken) {
        dirty();
        this.awsSessionToken = awsSessionToken;
    }

    public Date getAwsExpirationDate() {
        return awsExpirationDate;
    }

    public void setAwsExpirationDate(Date awsExpirationDate) {
        dirty();
        this.awsExpirationDate = awsExpirationDate;
    }

    /**
     * The MessageMe API authentication token, e.g.
     * 
     * 3de0d1f1925e449d864d768fdd6ca876
     */
    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        dirty();
        this.token = token;
    }

    /**
     * The device specific token used to register for GCM notifications, e.g.
     * 
     * APA91bHGApKaWxfjDDLUlwf-wlN8pbeIhKBcZ-qPoq9uyY2bbBxFZklos7sZ-P4-3sAl
     * RyAuj7w7BVtGD5AnjkBfwZTsG_O-3xObDzLkMMGX4hjnFuFfE3jwGHdiu1Jcj7P5TI3p
     * 08HFDTvWNn1XXlaMUXjlSFBnYQ
     */
    public String getGcmRegisterId() {
        return gcmRegisterId;
    }

    public void setGcmRegisterId(String gcmRegisterId) {
        dirty();
        this.gcmRegisterId = gcmRegisterId;
    }

    public Location getLastKnownLocation() {
        return lastKnownLocation;
    }

    public void setLastKnownLocation(Location lastKnownLocation) {
        dirty();
        this.lastKnownLocation = lastKnownLocation;
    }

    public boolean dbNeedUpgrade() {
        return dbNeedUpgrade;
    }

    public void setDbNeedUpgrade(boolean dbNeedUpgrade) {
        dirty();
        this.dbNeedUpgrade = dbNeedUpgrade;
    }

    public boolean dbNeedsDowngrade() {
        return dbNeedsDowngrade;
    }

    public void setDbNeedsDowngrade(boolean dbNeedsDowngrade) {
        dirty();
        this.dbNeedsDowngrade = dbNeedsDowngrade;
    }

    public void setAlertMap(long contactID, AlertSetting item) {
        dirty();
        if (alertMap.containsKey(contactID)) {
            alertMap.remove(contactID);
        }

        alertMap.put(contactID, item);
    }

    public Map<Long, AlertSetting> getAlertMap() {
        return this.alertMap;
    }

    public String getRegisteredPhoneNumber() {
        return registeredPhoneNumber;
    }

    public void setRegisteredPhoneNumber(String registeredPhoneNumber) {
        dirty();
        this.registeredPhoneNumber = registeredPhoneNumber;
    }

    public String getPhoneSignature() {
        return phoneSignature;
    }

    public void setPhoneSignature(String phoneSignature) {
        dirty();
        this.phoneSignature = phoneSignature;
    }

    public boolean hasSentSmsConfirmation() {
        return sentSmsConfirmation;
    }

    public void setSentSmsConfirmation(boolean sentSmsConfirmation) {
        dirty();
        this.sentSmsConfirmation = sentSmsConfirmation;
    }

    public AddressBookState getABState() {
        return abState;
    }

    public void setABState(AddressBookState abState) {
        dirty();
        this.abState = abState;
    }

    /**
     * Get the user country code. This preference is only used if the user does
     * NOT have a phone number saved.
     */
    public int getUserCountryCode() {
        return userCountryCode;
    }

    public void setUserCountryCode(int userCountryCode) {
        dirty();
        this.userCountryCode = userCountryCode;
    }

    public boolean isSoundAlertActive() {
        return soundAlert;
    }

    public void setSoundAlert(boolean soundAlert) {
        dirty();
        this.soundAlert = soundAlert;
    }

    public boolean isVibrateAlertActive() {
        return vibrateAlert;
    }

    public void setVibrateAlert(boolean vibrateAlert) {
        dirty();
        this.vibrateAlert = vibrateAlert;
    }

    public int getUserGetLastModified() {
        return lastModifiedGetUserUpdate;
    }

    /**
     * Update our UserGet last modified timestamp with
     * newLastModifiedGetUserUpdate, but only if the provided one is newer than
     * our existing one.
     */
    public void setUserGetLastModified(int newLastModifiedGetUserUpdate) {
        if (newLastModifiedGetUserUpdate > this.lastModifiedGetUserUpdate) {
            LogIt.i(this, "Set last modified timestamp for UserGet",
                    newLastModifiedGetUserUpdate);
            dirty();
            this.lastModifiedGetUserUpdate = newLastModifiedGetUserUpdate;
        } else {
            LogIt.d(this, "Ignore older last modified timestamp",
                    newLastModifiedGetUserUpdate);
        }
    }

    public boolean isGCMNotificationsAvailable() {
        return isGCMNotificationsAvailable;
    }

    public void setGCMNotificationsAvailable(boolean isGCMNotificationsAvailable) {
        dirty();
        this.isGCMNotificationsAvailable = isGCMNotificationsAvailable;
    }

    public Date getLastTimeShowedRatePopup() {
        return lastTimeShowedRatePopup;
    }

    public void setLastTimeShowedRatePopup(Date lastTimeShowedRatePopup) {
        dirty();
        this.rateAppPopupShownCount++;
        this.lastTimeShowedRatePopup = lastTimeShowedRatePopup;
    }

    /**
     * Get the number of times the app has been launched.
     */
    public int getLaunchCount() {
        return launchCount;
    }

    public void setLaunchCount(int launchCount) {
        dirty();
        this.launchCount = launchCount;
    }

    /**
     * Get the number of times the 'Rate MessageMe' popup has been shown.
     */
    public int getRateAppShownCount() {
        return rateAppPopupShownCount;
    }

    public void setRateAppShownCount(int rateAppPopupShownCount) {
        dirty();
        this.rateAppPopupShownCount = rateAppPopupShownCount;
    }

    public boolean isAppRated() {
        return isAppRated;
    }

    public void setAppRated(boolean isAppRated) {
        dirty();
        this.isAppRated = isAppRated;
    }

    public Date getLastTimeInForeground() {
        LogIt.d(this, "getLastTimeInForeground", lastTimeInForeground);
        return lastTimeInForeground;
    }

    public void setLastTimeInForeground(Date lastTimeInForeground) {
        dirty();
        LogIt.d(this, "setLastTimeInForeground", lastTimeInForeground);
        this.lastTimeInForeground = lastTimeInForeground;
    }
 
    public int getNumSentInvites() {
        return numSentInvites;
    }

    public void setNumSentInvites(int numSentInvites) {
        dirty();
        this.numSentInvites = numSentInvites;
    }

    public int getNumFailedInvites() {
        return numFailedInvites;
    }

    public void setNumFailedInvites(int numFailedInvites) {
        dirty();
        this.numFailedInvites = numFailedInvites;
    }

    public int getCurrentDBVersion() {
        return mCurrentDBVersion;
    }

    public void setCurrentDBVersion(int currentDBVersion) {
        dirty();
        this.mCurrentDBVersion = currentDBVersion;
    }

    public String getLastLoginID() {
        return mLastLoginID;
    }

    public void setLastLoginID(String lastLoginID) {
        dirty();
        mLastLoginID = lastLoginID;
    }

    public int getMandatoryAppVersion() {
        return mandatoryAppVersion;
    }

    public void setMandatoryAppVersion(int mandatoryAppVersion) {
        dirty();
        this.mandatoryAppVersion = mandatoryAppVersion;
    }

    public int getOptionalAppVersion() {
        return optionalAppVersion;
    }

    public void setOptionalAppVersion(int optionalAppVersion) {
        dirty();
        this.optionalAppVersion = optionalAppVersion;
    }

    public long getOptionalUpgradeDismissedDialogTime() {
        return optionalUpgradeDismissedDialogTime;
    }

    public void setOptionalUpgradeDismissedDialogTime(
            long optionalUpgradeDismissedDialogTime) {
        dirty();
        this.optionalUpgradeDismissedDialogTime = optionalUpgradeDismissedDialogTime;
    }
}