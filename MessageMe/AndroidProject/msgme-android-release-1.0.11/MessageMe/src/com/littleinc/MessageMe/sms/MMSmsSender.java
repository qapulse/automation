package com.littleinc.MessageMe.sms;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Calendar;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;

import com.coredroid.util.BackgroundTask;
import com.coredroid.util.LogIt;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.bo.MessageMeAppPreferences;
import com.littleinc.MessageMe.metrics.MMFirstSessionTracker;
import com.littleinc.MessageMe.metrics.MMFirstWeekTracker;
import com.littleinc.MessageMe.metrics.MMTracker;
import com.littleinc.MessageMe.net.RestfulClient;
import com.littleinc.MessageMe.util.DateUtil;
import com.littleinc.MessageMe.util.DeviceUtil;
import com.littleinc.MessageMe.util.FileSystemUtil;
import com.littleinc.MessageMe.util.ImageUtil;

public enum MMSmsSender {

    /**
     * {@link MMSmsSender} uses an Enum Type Singleton
     * Enum Type Singleton is more concise, provides the serialization machinery for free, 
     * and provides an ironclad guarantee against multiple instantiation, 
     * even in the face of sophisticated serialization or reflection attacks.
     */
    INSTANCE;

    private static final int HOURS_INVITE_IS_VALID = 8;

    private static final String INVITE_EXTRA = "invite";

    private static final String SENT_SMS_INTENT = "com.littleinc.MessageMe.sms.sent";

    /**
     * {@link SmsInvite} are written to file as well as the send queue so the
     * app can retry them if it is killed. These variables defines that
     * location.
     */
    public static final String SEND_QUEUE_DIRECTORY = "smsqueue";

    /**
     * This field should only be access through getSendQueueDirectory()
     */
    private static File sQueueDir;

    private File[] mPendingInviteFiles;

    /** The thread that runs the MMSmsSender */
    private Thread mSendThread = null;

    /** Flag for shutting down our thread used to send the invites */
    private volatile boolean mShutdownRequested = false;

    private String mSimCountryCode;

    private SmsManager mSmsManager = SmsManager.getDefault();

    private Context mContext = MessageMeApplication.getInstance();

    private PhoneNumberUtil mPhoneNumberUtil = PhoneNumberUtil.getInstance();

    private Queue<SmsInvite> mSentQueue = new ConcurrentLinkedQueue<SmsInvite>();

    private Queue<SmsInvite> mPendingQueue = new ConcurrentLinkedQueue<SmsInvite>();

    private MMSmsSender() {

        mSimCountryCode = DeviceUtil.getSimCardCountryCode(mContext);

        mContext.registerReceiver(smsSentReceiver, new IntentFilter(
                SENT_SMS_INTENT));
    }

    /**
     * This may need to be done again later if the threads have been stopped
     */
    public synchronized void createThreadsIfRequired() {

        if (mSendThread == null) {
            LogIt.d(MMSmsSender.class, "Create the sms sender thread");
            mShutdownRequested = false;
            mSendThread = new Thread(runnable);
            mSendThread.start();
        }
    }

    /**
     * Add a {@link SmsInvite} to the send queue
     */
    public void addToQueue(SmsInvite invite) {

        if (mPendingQueue.contains(invite) || mSentQueue.contains(invite)) {

            LogIt.d(MMSmsSender.class, "Invite already on queue", invite);
        } else {

            File inviteFile = getFileForInvite(invite);

            if (inviteFile != null && inviteFile.exists()) {

                LogIt.w(MMSmsSender.class, inviteFile.getName(),
                        "is already queued on disk! Ignore write");
            } else {

                writeInviteToDisk(invite);
            }

            if (!mPendingQueue.add(invite)) {
                LogIt.e(MMSmsSender.class, "Failed to add invite to send queue");
            }

            createThreadsIfRequired();
        }
    }

    /**
     * Remove a {@link SmsInvite} from the sent queue as we have
     * received acknowledgment from the {@link SmsManager} that it was sent
     */
    private void removeFromSentQueue(SmsInvite inviteToRemove) {

        // Remove the invite from the in memory sent queue
        if (mSentQueue.remove(inviteToRemove)) {

            LogIt.d(MMSmsSender.class,
                    "Successfully removed invite from sent queue");
        }
    }

    /**
     * Remove a {@link SmsInvite} from the pending queue directory as we have
     * received acknowledgment from the {@link SmsManager} that it was processed
     */
    public void removeFromPendingQueue(SmsInvite inviteToRemove) {

        // Remove the invite from the in memory pending queue
        if (mPendingQueue.remove(inviteToRemove)) {

            LogIt.d(MMSmsSender.class,
                    "Successfully removed invite from pending queue");
        }
        removeFromSentQueue(inviteToRemove);

        // The invite was removed from the in-memory send queue,
        // so check whether any file on disk also needs to be deleted.
        File cmdFile = getFileForInvite(inviteToRemove);

        if (cmdFile != null && cmdFile.exists()) {

            if (cmdFile.delete()) {

                LogIt.d(MMSmsSender.class, "Invite file deleted successfully",
                        cmdFile);
            } else {

                LogIt.w(MMSmsSender.class, "Failed to delete Invite", cmdFile);
            }
        } else {

            LogIt.d(MMSmsSender.class, "No Invite on disk to remove");
            return;
        }
    }

    /**
     * This checks if the invite already exists on disk as callers should
     * be able to use this to overwrite existing {@link SmsInvite} objects
     */
    static void writeInviteToDisk(SmsInvite invite) {

        ObjectOutputStream objOutputStream = null;
        File inviteFile = getFileForInvite(invite);

        try {

            if (inviteFile == null
                    || (inviteFile != null && !inviteFile.exists())) {

                inviteFile = File.createTempFile(INVITE_EXTRA, null,
                        getSendQueueDirectory());
                invite.setTempFileName(inviteFile.getAbsolutePath());

                LogIt.d(MMSmsSender.class, "Writing sms invite to disk",
                        inviteFile.getName());
            } else {

                LogIt.d(MMSmsSender.class, "Updating sms invite on disk",
                        inviteFile.getName());
            }

            // Write the invite to disk
            //
            // Use a buffered output stream as apparently performance is better
            // if your data is smaller than 8KB
            // http://stackoverflow.com/a/8713017/112705
            objOutputStream = new ObjectOutputStream(new BufferedOutputStream(
                    new FileOutputStream(inviteFile)));
            objOutputStream.writeObject(invite);
        } catch (Exception e) {

            LogIt.w(MMSmsSender.class, "Error writing sms invite to file", e);
        } finally {
            FileSystemUtil.closeOutputStream(objOutputStream);
        }
    }

    /**
     * Get the File where the {@link SmsInvite} is being stored
     */
    private static File getFileForInvite(SmsInvite invite) {

        if (invite.getTempFileName() == null) {

            return null;
        } else {

            return new File(invite.getTempFileName());
        }
    }

    /**
     * Load any pending {@link SmsInvite} objects from file and add them to the
     * mQueue for processing.
     */
    public synchronized void loadPendingInvitesFromFile() {

        LogIt.d(MMSmsSender.class, "Load any pending invites from file");

        // Get files that need to be processed
        mPendingInviteFiles = getSendQueueDirectory().listFiles();

        if ((mPendingInviteFiles == null) || (mPendingInviteFiles.length == 0)) {

            LogIt.d(MMSmsSender.class, "No pending invites");
        } else {

            for (File inviteFile : mPendingInviteFiles) {

                ObjectInputStream objInputStream = null;

                try {

                    objInputStream = new ObjectInputStream(new FileInputStream(
                            inviteFile));
                    Object obj = objInputStream.readObject();

                    if (obj instanceof SmsInvite) {

                        SmsInvite invite = (SmsInvite) obj;

                        if (mPendingQueue.contains(invite)
                                || mSentQueue.contains(invite)) {

                            LogIt.d(MMSmsSender.class,
                                    "Invite already on queue",
                                    invite.getTempFileName());
                        } else {

                            mPendingQueue.add(invite);

                            LogIt.d(MMSmsSender.class,
                                    "Added SMSInvite to process queue", invite);
                        }
                    } else {

                        LogIt.w(MMSmsSender.class,
                                "Trying to load a non SMSInvite file");
                    }
                } catch (Exception e) {
                    LogIt.w(MMSmsSender.class,
                            "Error loading invite from file", inviteFile, e);
                } finally {
                    FileSystemUtil.closeInputStream(objInputStream);
                }
            }

            createThreadsIfRequired();
        }
    }

    private static File getSendQueueDirectory() {

        if (sQueueDir == null) {
            sQueueDir = ImageUtil.getInternalFilesDir(SEND_QUEUE_DIRECTORY);

            if (!sQueueDir.exists()) {

                // Create parent folders if required
                if (sQueueDir.mkdirs()) {

                    LogIt.d(MMSmsSender.class, "Creating sms invite directory",
                            sQueueDir);
                } else {
                    LogIt.w(MMSmsSender.class,
                            "Error creating sms invite directory", sQueueDir);
                }
            }
        }

        return sQueueDir;
    }

    public synchronized void shutDown() {

        if (mSendThread == null) {
            LogIt.i(MMSmsSender.class, "No send thread to shut down");
        } else {
            LogIt.i(MMSmsSender.class, "Shut down the SMS send thread");
            mShutdownRequested = true;

            // If the thread is blocked this will make it wake up
            mSendThread.interrupt();
            mSendThread = null;
        }
    }

    private void consume(SmsInvite invite) {

        if (isInviteTooOld(invite)) {

            LogIt.d(MMSmsSender.class, "Ingore old invite", invite);
            removeFromPendingQueue(invite);
        } else {

            PhoneNumber phoneNumber = null;
            try {
                phoneNumber = mPhoneNumberUtil.parse(invite.getPhoneNumber(),
                        mSimCountryCode);

                if (mPhoneNumberUtil.isValidNumber(phoneNumber)) {

                    invite.setDateLastAttempt(DateUtil.now());

                    // We should override the invite on disk to update the last attempt date
                    writeInviteToDisk(invite);

                    // This intent will be added into the sentPendingIntent below.
                    // The current invite will be added as an extra to be restored and processed when the 
                    // pending intent is fired
                    Intent intent = new Intent(SENT_SMS_INTENT);
                    intent.putExtra(INVITE_EXTRA, invite);

                    // The SmsManager#sendTextMessage uses a PendingIntent to notify us when a SMS have been
                    // sent or delivered providing the result code of the action.
                    PendingIntent sentPendingIntent = PendingIntent
                            .getBroadcast(mContext, invite.hashCode(), intent,
                                    PendingIntent.FLAG_UPDATE_CURRENT);

                    // We need to track the sent invites to avoid add them again in
                    // the pending queue if a service state change is received
                    mSentQueue.add(invite);

                    mSmsManager.sendTextMessage(invite.getPhoneNumber(), null,
                            invite.getMessageBody(), sentPendingIntent, null);
                } else {

                    LogIt.d(MMSmsSender.class,
                            "Unable to send invite, phone is invalid",
                            phoneNumber);
                    removeFromPendingQueue(invite);
                }
            } catch (NumberParseException e) {

                LogIt.w(MMSmsSender.class, "Unable to parse number",
                        invite.getPhoneNumber());
                removeFromPendingQueue(invite);
            }
        }
    }

    /**
     * Checks if an invite was initial sent more than 8 hours ago
     */
    private boolean isInviteTooOld(SmsInvite invite) {

        if (invite.getDateLastAttempt() == null) {

            return false;
        } else {

            Calendar inviteCalendar = Calendar.getInstance();
            inviteCalendar.setTime(invite.getDateCreated());
            inviteCalendar.add(Calendar.HOUR, HOURS_INVITE_IS_VALID);

            Calendar nowCalendar = Calendar.getInstance();
            nowCalendar.setTime(DateUtil.now());

            if (nowCalendar.after(inviteCalendar)) {

                return true;
            } else {

                return false;
            }
        }
    }

    private BroadcastReceiver smsSentReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.hasExtra(INVITE_EXTRA)) {

                final SmsInvite invite = (SmsInvite) intent
                        .getSerializableExtra(INVITE_EXTRA);

                switch (getResultCode()) {
                case Activity.RESULT_OK:
                    
                    new BackgroundTask() {
                        
                        @Override
                        public void work() {
                         // make POST call to server to record invite
                            JSONObject postData = new JSONObject();
                            JSONArray phones = new JSONArray();
                            JSONObject phone = new JSONObject();
                            try {
                                phone.put("first_name", invite.getFirstName());
                                phone.put("last_name", invite.getLastName());
                                phone.put("phone_number", invite.getPhoneNumber());
                                
                                phones.put(phone);
                                postData.put("phone_numbers", phones);
                            } catch (JSONException e) {}
                            
                            RestfulClient.getInstance().contactInvite(postData);
                        }
                        
                        @Override
                        public void done() {}
                    };
                    
                    // 3840 = ZW
                    MMTracker.getInstance().abacus("invite", "send",
                            "sms_unselected0", 3840, null);
                    MMFirstSessionTracker.getInstance().abacus(null, "send",
                            "sms_unselected0", 3840, null);
                    MMFirstWeekTracker.getInstance().abacus(null, "send",
                            "sms_unselected0", 3840, null);

                    // The invite have been sent
                    LogIt.i(MMSmsSender.class, "Sent", invite);
                    increaseSentSmsInvites();
                    removeFromPendingQueue(invite);
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:

                    // The invite fails, e.g unreachable number, land line number, unexpected error
                    // We don't want to retry invites with this kind of error so we going to
                    // remove it from the queue to avoid send it for ever
                    //
                    // LIMITATION
                    // Android SmsManager returns GENERIC_FAILURE error for messages that have been
                    // added while the phone have connection but for some reason at the moment of send
                    // the connection have been lost or the signal is extremely low. For that reason is possible
                    // that in this case some invites get lost because we remove invites with this result code
                    // from memory and disk queue

                    LogIt.i(MMSmsSender.class, "Fail", invite);
                    increaseFailSmsInvites();
                    removeFromPendingQueue(invite);
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:

                    // We don't remove the invite from queue in this case because we want to
                    // retry as soon as we get a service state change
                    LogIt.i(MMSmsSender.class, "No service, can't send", invite);
                    removeFromSentQueue(invite);
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:

                    // We don't remove the invite from queue in this case because we want to
                    // retry as soon as we get a service state change
                    LogIt.i(MMSmsSender.class, "Service off, can't send",
                            invite);
                    removeFromSentQueue(invite);
                    break;
                default:

                    LogIt.w(MMSmsSender.class, "Unknow error, can't send",
                            invite);
                    increaseFailSmsInvites();
                    removeFromPendingQueue(invite);
                    break;
                }
            }
        }
    };

    /**
     * Increases global count of invites successfully sent
     */
    private synchronized void increaseSentSmsInvites() {

        MessageMeAppPreferences appPreferences = MessageMeApplication
                .getPreferences();
        appPreferences
                .setNumSentInvites(appPreferences.getNumSentInvites() + 1);
    }

    /**
     * Increase global count of failed invites
     */
    private synchronized void increaseFailSmsInvites() {

        MessageMeAppPreferences appPreferences = MessageMeApplication
                .getPreferences();
        appPreferences
                .setNumFailedInvites(appPreferences.getNumFailedInvites() + 1);
    }

    private Runnable runnable = new Runnable() {

        @Override
        public void run() {

            LogIt.d(MMSmsSender.class, "Starting MMSmsSender processing thread");

            while (!mShutdownRequested
                    && !Thread.currentThread().isInterrupted()
                    && !mPendingQueue.isEmpty()) {

                consume(mPendingQueue.poll());

                // Added a short delay between each invite to help android SmsManager
                // to get service state updates and avoid send SMSs if the service unavailable
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {

                    LogIt.e(MMSmsSender.class, e,
                            "Exception sleeping MMSmsSender thread");
                }
            }

            shutDown();
        }
    };
}
