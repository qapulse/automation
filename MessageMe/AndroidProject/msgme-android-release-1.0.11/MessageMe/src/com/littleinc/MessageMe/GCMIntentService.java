package com.littleinc.MessageMe;

import java.util.ArrayList;
import java.util.List;

import org.messageMe.OpenUDID.OpenUDID_manager;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.RemoteViews;

import com.coredroid.util.LogIt;
import com.google.android.gcm.GCMBaseIntentService;
import com.google.gson.Gson;
import com.littleinc.MessageMe.bo.AlertSetting;
import com.littleinc.MessageMe.bo.GCMBundleMessage;
import com.littleinc.MessageMe.bo.GCMMessage;
import com.littleinc.MessageMe.bo.GCMMessageType;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.chat.ChatConnection;
import com.littleinc.MessageMe.chat.WSChatConnection;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope.CommandType;
import com.littleinc.MessageMe.protocol.Commands.PBCommandPushTokenNew;
import com.littleinc.MessageMe.protocol.Objects.PBDevice;
import com.littleinc.MessageMe.ui.MessageMeLauncher;
import com.littleinc.MessageMe.util.StringUtil;

public class GCMIntentService extends GCMBaseIntentService {

    private NotificationManager mNotificationManager;

    private static List<GCMMessage> notificationList = new ArrayList<GCMMessage>();

    private static final String CTYPE_GROUP = "group";

    private static final String PTYPE_MESSAGE = "m";

    public GCMIntentService() {
        super(MessageMeExternalAPI.SENDER_ID);
        LogIt.i(this, "Set sender ID");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogIt.d(this, "GCM onCreate");

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
    }

    @Override
    protected void onError(Context context, String errorId) {
        MessageMeApplication.getPreferences().setGCMNotificationsAvailable(
                false);
        LogIt.w(this, "GCM onError", errorId);
    }

    @Override
    protected void onMessage(Context context, Intent intent) {

        GCMBundleMessage gcmBundleMessage = null;

        User currentUser = MessageMeApplication.getCurrentUser();

        // The key defining the type of the GCM notification. See the
        // GCMMessageType class for the definitions.
        String typeString = intent.getStringExtra("loc_key");

        // The localizable arguments to display in the notification, e.g.
        //
        // loc_args={
        // "artist_name": "Daft Punk",
        // "track_name": "One More Time"
        // }
        //
        // These values are inserted in the localized text for the
        // notification, e.g. for English:
        // <string name="pms">sent you a song: %1$s by %2$s</string>
        String locArgs = intent.getStringExtra("loc_args");

        // The channel ID of the conversation that this notification
        // relates to. This is used to stack notifications for a particular
        // channel, and to take the user to that thread when the notification
        // is touched.
        String channelIdString = intent.getStringExtra("cid");

        // For message notifications, the first name of the person who sent
        // the message. This is only used in group notifications and user
        // join notifications.
        String firstName = intent.getStringExtra("sender_first_name");

        String channelName = intent.getStringExtra("cname");

        // Determines if the message belongs to a group or a
        // private conversation. Returns "user" or "group"
        String channelType = intent.getStringExtra("ctype");

        String addedUserToGroup = intent
                .getStringExtra("added_user_first_name");

        // This is used to determine if the GCM message is for the currently
        // logged in user as there are some edge cases that could involve
        // them being sent to a device for a user that was previously logged
        // in on that device.
        String recipientIdString = intent.getStringExtra("to_user_id");

        // The unread notifications count.
        String badgeString = intent.getStringExtra("badge");

        String senderIdString = intent.getStringExtra("sender_id");

        int badge = -1;

        if (badgeString != null) {
            try {
                badge = Integer.decode(badgeString);
                LogIt.d(this, "Unread badge count", badge);
            } catch (Exception e) {
                // Don't let an error here stop the GCM displaying
                LogIt.e(this, e, "Error parsing badge count as an int",
                        badgeString);
            }
        }

        String ptype = intent.getStringExtra("ptype");

        long recipientId = -1;

        Gson gson = new Gson();

        long channelId = -1;

        long senderId = -1;

        // Whether the notification is for a private conversation. True if it
        // is, otherwise it means the notification relates to a group/room.
        boolean isNotificationForAGroup = false;

        int layout = R.layout.external_notification;

        if (channelType != null) {
            if (channelType.equals(CTYPE_GROUP)) {
                isNotificationForAGroup = true;
            }
        }

        if (channelIdString != null) {
            try {
                channelId = Long.decode(channelIdString);
            } catch (Exception e) {
                LogIt.e(this, e, "Error parsing sender ID as a long",
                        channelIdString);
                return;
            }
        }

        if (senderIdString != null) {
            try {
                senderId = Long.decode(senderIdString);
            } catch (Exception e) {
                LogIt.e(this, e, "Error parsing new sender ID as a long",
                        senderIdString);
                return;
            }
        }

        if (recipientIdString == null) {
            LogIt.w(this, "to_user_id is missing from GCM payload");
        } else {
            try {
                recipientId = Long.decode(recipientIdString);
            } catch (Exception e) {
                LogIt.e(this, e, "Error parsing recipient ID as a long",
                        recipientIdString);
                return;
            }
        }

        String messageContent = "";

        String messageTitle = channelName;

        if (locArgs != null && !StringUtil.isEmpty(locArgs)) {
            try {
                gcmBundleMessage = gson.fromJson(locArgs,
                        GCMBundleMessage.class);
            } catch (Exception e) {
                LogIt.e(this, e, e.getMessage());
            }
        }

        if (currentUser == null) {
            LogIt.w(this, "No current user, ignore GCM notification");
            return;
        }

        if ((recipientIdString != null)
                && (recipientId != currentUser.getContactId())) {
            LogIt.w(this, "Recipient ID is different from the current user ID");
            return;
        }

        if (StringUtil.isEmpty(typeString)) {
            LogIt.w(this, "GCM Notification without typeString, ignoring");
            return;
        }

        GCMMessageType gcmMessageType = GCMMessageType.parseFrom(typeString);

        switch (gcmMessageType) {
            case GCM_TEXT:
                LogIt.i(this, "Text message received");
                if (gcmBundleMessage != null
                        && gcmBundleMessage.getBody() != null) {
                    if (!isNotificationForAGroup) {
                        messageContent = gcmBundleMessage.getBody();
                    } else {
                        messageContent = firstName + ": "
                                + gcmBundleMessage.getBody();
                    }
                } else {
                    LogIt.e(this,
                            "GCMBundleMessage or any of any of its values is null");
                }
                break;
            case GCM_PHOTO:
                LogIt.i(this, "Photo message received");
                if (!isNotificationForAGroup) {
                    messageContent = String.format(getString(R.string.pmp), "")
                            .trim();
                } else {
                    messageContent = String.format(getString(R.string.pmp),
                            firstName);
                }
                break;
            case GCM_DOODLE:
                LogIt.i(this, "Doodle message received");
                if (!isNotificationForAGroup) {
                    messageContent = String.format(getString(R.string.pmd), "")
                            .trim();
                } else {
                    messageContent = String.format(getString(R.string.pmd),
                            firstName);
                }
                break;
            case GCM_DOODLE_PIC:
                LogIt.i(this, "Doodle pic message received");
                if (!isNotificationForAGroup) {
                    messageContent = String
                            .format(getString(R.string.pmdp), "").trim();
                } else {
                    messageContent = String.format(getString(R.string.pmdp),
                            firstName);
                }
                break;
            case GCM_VIDEO:
                LogIt.i(this, "Video message received");
                if (!isNotificationForAGroup) {
                    messageContent = String.format(getString(R.string.pmv), "")
                            .trim();
                } else {
                    messageContent = String.format(getString(R.string.pmv),
                            firstName);
                }
                break;
            case GCM_YOUTUBE:
                LogIt.i(this, "Youtube message received");
                if (gcmBundleMessage != null
                        && gcmBundleMessage.getTitle() != null) {
                    if (!isNotificationForAGroup) {
                        messageContent = String.format(getString(R.string.pmy),
                                "", gcmBundleMessage.getTitle()).trim();
                    } else {
                        messageContent = String.format(getString(R.string.pmy),
                                firstName, gcmBundleMessage.getTitle());
                    }
                } else {
                    LogIt.e(this,
                            "GCMBundleMessage or any of any of its values is null");
                }
                break;
            case GCM_VOICE:
                LogIt.i(this, "Voice message received");
                if (!isNotificationForAGroup) {
                    messageContent = String
                            .format(getString(R.string.pmvc), "").trim();
                } else {
                    messageContent = String.format(getString(R.string.pmvc),
                            firstName);
                }
                break;
            case GCM_SONG:
                LogIt.i(this, "Song message received");
                if (gcmBundleMessage != null
                        && gcmBundleMessage.getTrackName() != null
                        && gcmBundleMessage.getArtistName() != null) {
                    if (!isNotificationForAGroup) {
                        messageContent = String.format(getString(R.string.pms),
                                "", gcmBundleMessage.getTrackName(),
                                gcmBundleMessage.getArtistName()).trim();
                    } else {
                        messageContent = String.format(getString(R.string.pms),
                                firstName, gcmBundleMessage.getTrackName(),
                                gcmBundleMessage.getArtistName());
                    }
                } else {
                    LogIt.e(this,
                            "GCMBundleMessage or any of any of its values is null");
                }
                break;
            case GCM_LOCATION_SPECIFIC:
                LogIt.i(this, "Specific location message received");
                if (gcmBundleMessage != null
                        && gcmBundleMessage.getTitle() != null) {
                    if (!isNotificationForAGroup) {
                        messageContent = String.format(getString(R.string.pml),
                                "", gcmBundleMessage.getTitle()).trim();
                    } else {
                        messageContent = String.format(getString(R.string.pml),
                                firstName, gcmBundleMessage.getTitle());
                    }
                } else {
                    LogIt.e(this,
                            "GCMBundleMessage or any of any of its values is null");
                }
                break;
            case GCM_LOCATION_CURRENT:
                LogIt.i(this, "Current location message received");
                if (!isNotificationForAGroup) {
                    messageContent = String
                            .format(getString(R.string.pmlc), "").trim();
                } else {
                    messageContent = String.format(getString(R.string.pmlc),
                            firstName);
                }
                break;
            case GCM_CONTACT:
                LogIt.i(this, "Forwarded contact message received");
                if (gcmBundleMessage != null
                        && gcmBundleMessage.getContactName() != null) {
                    if (!isNotificationForAGroup) {
                        messageContent = String.format(getString(R.string.pmc),
                                "", gcmBundleMessage.getContactName()).trim();
                    } else {
                        messageContent = String.format(getString(R.string.pmc),
                                firstName, gcmBundleMessage.getContactName());
                    }
                } else {
                    LogIt.e(this,
                            "GCMBundleMessage or any of any of its values is null");
                }
                break;
            case GCM_NEW_GROUP:
                LogIt.i(this, "Added to group message received");
                if (gcmBundleMessage != null
                        && gcmBundleMessage.getGroupName() != null) {
                    messageContent = String.format(getString(R.string.pga),
                            firstName, gcmBundleMessage.getGroupName());
                    messageTitle = gcmBundleMessage.getGroupName();
                } else {
                    LogIt.e(this,
                            "GCMBundleMessage or any of any of its values is null");
                }
                break;
            case GCM_ADDED_TO_GROUP_OTHER:
                LogIt.i(this, "Added to group message received");
                if (gcmBundleMessage != null
                        && gcmBundleMessage.getGroupName() != null) {
                    messageContent = String.format(getString(R.string.pgao),
                            addedUserToGroup, gcmBundleMessage.getGroupName());
                    messageTitle = gcmBundleMessage.getGroupName();
                } else {
                    LogIt.e(this,
                            "GCMBundleMessage or any of any of its values is null");
                }
                break;
            case GCM_GROUP_UPDATE_NAME:
                LogIt.i(this, "Changed group name message received");
                if (gcmBundleMessage != null
                        && gcmBundleMessage.getOldName() != null
                        && gcmBundleMessage.getNewName() != null) {
                    messageContent = String.format(getString(R.string.pgun),
                            firstName, gcmBundleMessage.getOldName(),
                            gcmBundleMessage.getNewName());
                    messageTitle = gcmBundleMessage.getNewName();
                } else {
                    LogIt.e(this,
                            "GCMBundleMessage or any of any of its values is null");
                }
                break;
            case GCM_GROUP_UPDATE_COVER:
                LogIt.i(this, "Group conver picture changed message received");
                if (gcmBundleMessage != null
                        && gcmBundleMessage.getGroupName() != null) {
                    messageContent = String.format(getString(R.string.pguc),
                            firstName, gcmBundleMessage.getGroupName());
                    messageTitle = gcmBundleMessage.getGroupName();
                } else {
                    LogIt.e(this,
                            "GCMBundleMessage or any of any of its values is null");
                }
                break;
            case GCM_GROUP_UPDATE_PROFILE:
                LogIt.i(this, "Group profile picture changed message received");
                if (gcmBundleMessage != null
                        && gcmBundleMessage.getGroupName() != null) {
                    messageContent = String.format(getString(R.string.pgup),
                            firstName, gcmBundleMessage.getGroupName());
                    messageTitle = gcmBundleMessage.getGroupName();
                } else {
                    LogIt.e(this,
                            "GCMBundleMessage or any of any of its values is null");
                }
                break;
            case GCM_USER_GROUP_LEFT:
                LogIt.i(this, "User left group message received");
                if (gcmBundleMessage != null
                        && gcmBundleMessage.getGroupName() != null) {
                    messageContent = String.format(getString(R.string.pgl),
                            firstName, gcmBundleMessage.getGroupName());
                    messageTitle = gcmBundleMessage.getGroupName();
                } else {
                    LogIt.e(this,
                            "GCMBundleMessage or any of any of its values is null");
                }
                break;
            case GCM_USER_JOIN:
                LogIt.i(this, "User joined the app message received");

                String lastName = intent.getStringExtra("sender_last_name");

                StringBuilder newUserName = new StringBuilder();

                if (firstName != null) {
                    newUserName.append(firstName);
                }

                if (lastName != null) {
                    newUserName.append(" ");
                    newUserName.append(lastName);
                }

                if (newUserName.length() > 0) {
                    if ((messageTitle == null) || (messageTitle.length() == 0)) {
                        // The "cname" field is currently not provided on the
                        // GCM_USER_JOIN notification so we have to set it here.
                        messageTitle = newUserName.toString();
                    }

                    messageContent = String.format(getString(R.string.puj),
                            newUserName.toString());

                    // Adding the senderId to the channelId, as the cid is not
                    // provided by the server
                    channelId = senderId;
                } else {
                    LogIt.e(this,
                            "No name provided in GCM_USER_JOIN notification");
                }
                break;
            case GCM_AB_MATCH_SINGLE:
                LogIt.i(this, "Address book single match message received");
                if (gcmBundleMessage != null) {
                    senderId = MessageMeConstants.SYSTEM_NOTICES_CHANNEL_ID;
                    messageTitle = getString(R.string.messageme_system_notif_title);
                    messageContent = getString(R.string.pabmo);
                } else {
                    LogIt.e(this,
                            "GCMBundleMessage or any of any of its values is null");
                }
                break;
            case GCM_AB_MATCH_MULTIPLE:
                LogIt.i(this, "Address book multiple match message received");
                if (gcmBundleMessage != null) {
                    senderId = MessageMeConstants.SYSTEM_NOTICES_CHANNEL_ID;
                    messageTitle = getString(R.string.messageme_system_notif_title);

                    int matchCount = gcmBundleMessage.getMatchCount();

                    if (matchCount <= 1) {
                        LogIt.w(this,
                                "match_count missing or wrong for pabm GCM notification",
                                matchCount);
                        messageContent = getString(R.string.pabm_missing_count_value);
                    } else {
                        messageContent = String.format(
                                getString(R.string.pabm), matchCount);
                    }
                } else {
                    LogIt.e(this,
                            "GCMBundleMessage or any of any of its values is null");
                }
                break;
            case GCM_UNSUPPORTED:
                if (ptype.equals(PTYPE_MESSAGE)) {
                    LogIt.w(this,
                            "New message notification for unknown message type",
                            typeString);
                    messageContent = getString(R.string.pm_unsupported);
                } else {
                    LogIt.w(this, "Ignore unknown GCM notification type",
                            typeString);
                    return;
                }
                break;
            default:
                LogIt.w(this, "Unrecognized message type", typeString);
                break;
        }

        // Sends the notification only if
        // -alerts are enabled for the room, AND
        // -alerts are enabled under the master setting in Settings, AND
        // -the app is not in the foreground
        if ((!AlertSetting.hasAlertBlock(channelId) && !AlertSetting
                .hasAlertBlock(currentUser.getContactId()))
                && !MessageMeApplication.isInForeground(this)) {

            if (!StringUtil.isEmpty(messageTitle)
                    && !StringUtil.isEmpty(messageContent)) {
                displayNotificationFromSender(messageTitle, messageContent,
                        layout, channelId);
            } else {
                LogIt.d(this, "GCM Notification without title, ignoring");
            }
        }
    }

    /**
     * The device has registered with the Google Cloud Messaging service and
     * has been given a GCM token. That token now needs to be uploaded to
     * the MessageMe servers in a {@link CommandType.PUSH_TOKEN_NEW}.
     */
    @Override
    protected void onRegistered(Context context, String registerId) {
        LogIt.d(this, "GCM onRegistered", registerId);

        ChatConnection wsConnection = WSChatConnection.getInstance(this);

        if (wsConnection != null) {
            MessageMeApplication.getPreferences().setGCMNotificationsAvailable(
                    true);

            PBCommandEnvelope.Builder commandEnvelopeBuilder = PBCommandEnvelope
                    .newBuilder();

            // No need to setDeviceName for now as it is optional
            PBDevice.Builder deviceBuilder = PBDevice.newBuilder();
            deviceBuilder.setDeviceID(OpenUDID_manager.getOpenUDID());
            deviceBuilder.setDeviceType(PBDevice.DeviceType.DEVICE_ANDROID);
            LogIt.i(this,
                    "Send PUSH_TOKEN_NEW to register GCM token with MessageMe servers",
                    registerId, OpenUDID_manager.getOpenUDID());

            PBCommandPushTokenNew.Builder pushTokenNewBuilder = PBCommandPushTokenNew
                    .newBuilder();
            pushTokenNewBuilder.setDevice(deviceBuilder.build());
            pushTokenNewBuilder.setToken(registerId);

            commandEnvelopeBuilder.setPushTokenNew(pushTokenNewBuilder.build());
            commandEnvelopeBuilder.setType(CommandType.PUSH_TOKEN_NEW);

            PBCommandEnvelope envelope = commandEnvelopeBuilder.build();

            wsConnection.sendCommand(envelope);
        } else {
            LogIt.w(this,
                    "Unable to get WSChatConnection to send PUSH_TOKEN_NEW to register for GCM");
        }
    }

    @Override
    protected void onUnregistered(Context context, String registerId) {
        LogIt.d(this, "Device unregistered, id:", registerId);
        // TODO inform external server about the unregistered id

    }

    @TargetApi(16)
    private void displayNotificationFromSender(String title,
            String description, int layout, long senderId) {

        LogIt.d(this, "GCM displayNotificationNewMessage", title, description,
                senderId);

        long when = System.currentTimeMillis();

        notificationList.add(new GCMMessage(senderId, title, description));

        RemoteViews contentView = new RemoteViews(getPackageName(), layout);

        contentView.setTextViewText(R.id.notification_title, title);
        contentView.setTextViewText(R.id.notification_description, description);

        // Launch the tab view, it will show the appropriate thread if it is
        // available.
        Intent notificationIntent = new Intent(this, MessageMeLauncher.class);

        // This is the User or Room the notification is from
        notificationIntent.putExtra(MessageMeConstants.RECIPIENT_ID_KEY,
                senderId);

        // Creates an unique intent which will be different from the other
        // intents generated here
        notificationIntent.setData(Uri.parse("content://" + when));

        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // FLAG_UPDATE_CURRENT is required otherwise the previous PendingIntent
        // will be reused for new notifications. In the words of the API docs:
        //
        // "A common mistake people make is to create multiple PendingIntent
        // objects with Intents that only vary in their 'extra' contents,
        // expecting to get a different PendingIntent each time. This does not
        // happen."
        PendingIntent conIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                GCMIntentService.this);

        builder.setContentTitle(title);
        builder.setContentIntent(conIntent);
        builder.setAutoCancel(true);
        builder.setWhen(when);
        builder.setSmallIcon(R.drawable.ic_status_bar);

        // Setup the sound/alert notification status
        int notificationDefaults = Notification.DEFAULT_LIGHTS;
        if (MessageMeApplication.getPreferences().isSoundAlertActive()) {
            notificationDefaults |= Notification.DEFAULT_SOUND;
        }
        if (MessageMeApplication.getPreferences().isVibrateAlertActive()) {
            notificationDefaults |= Notification.DEFAULT_VIBRATE;
        }

        builder.setDefaults(notificationDefaults);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {

            List<GCMMessage> list = GCMMessage
                    .getNotificationsFromSender(senderId);

            // Sets the content text to the notification
            builder.setContentText(list.get(list.size() - 1)
                    .getMessageContent());

            NotificationCompat.InboxStyle inboxNotification = new NotificationCompat.InboxStyle(
                    builder);

            // Only shows the latest 5 notifications
            if (list.size() > 5) {

                for (int i = list.size() - 1; i > (list.size() - 6); i--) {
                    inboxNotification.addLine(list.get(i).getMessageContent());
                }
                inboxNotification.setSummaryText("+"
                        + String.valueOf(list.size() - 5) + " "
                        + getString(R.string.more_lbl));

            } else {

                for (int i = list.size() - 1; i >= 0; i--) {
                    inboxNotification.addLine(list.get(i).getMessageContent());
                }

            }

            mNotificationManager.notify((int) senderId,
                    inboxNotification.build());

        } else {
            // Sets the content text to the notification
            builder.setContentText(description);

            Notification notification = builder.build();

            mNotificationManager.notify((int) senderId, notification);
        }

    }

    public static List<GCMMessage> getGCMNotificationList() {
        return notificationList;
    }

    public static void setGCMNotificationList(List<GCMMessage> list) {
        notificationList = list;
    }
}