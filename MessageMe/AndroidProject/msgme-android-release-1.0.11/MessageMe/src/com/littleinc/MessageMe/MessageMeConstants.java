package com.littleinc.MessageMe;

import com.littleinc.MessageMe.ui.MessagesFragment;

public class MessageMeConstants {

    public static final String FILE_NAME_KEY = "file_name_key";

    public static final String APP_NAME_KEY = "com.littleinc.MessageMe";

    /**
     * The refresh interval, in hours.  Our AWS tokens expire every 36 hours,
     * so we refresh them 6 hours ahead of when they expire.
     */
    public static final int AWS_TOKEN_REFRESH_INTERVAL = 30;

    /* Intent extras */
    public static final String EXTRA_LOCATION = "location";

    public static final String EXTRA_LATITUDE = "lat";

    public static final String EXTRA_LONGITUDE = "lon";

    public static final String EXTRA_MEDIA = "media_item";

    public static final String EXTRA_YOUTUBE = "youtube_item";

    public static final String EXTRA_IMAGE_KEY = "image_key";

    public static final String EXTRA_TYPE = "type";

    public static final String EXTRA_IMAGE_URL = "imageUrl";

    public static final String EXTRA_CONTACT_ID = "contactId";

    /**
     * Extra to hold the screen that a user should be taken to when
     * they touch the in-app notification.
     */
    public static final String EXTRA_SCREEN_TO_SHOW = "in_app_notif_screen_to_show";

    /**
     * Possible values for the {@link #EXTRA_SCREEN_TO_SHOW}.
     */
    public enum InAppNotificationTargetScreen {
        MESSAGE_THREAD, CONTACTS_TAB, CONTACT_PROFILE;
    }

    public static final String EXTRA_TITLE = "title";

    public static final String EXTRA_DESCRIPTION = "description";

    public static final String EXTRA_IS_CALL_CAPABLE = "is_call_capable";

    public static final String EXTRA_PHONE_NUMBER = "phone_number";

    public static final String EXTRA_PHONE_SIGNATURE = "phone_signature";

    public static final String EXTRA_COUNTRY_INITIALS = "country_initials";

    public static final String EXTRA_COUNTRY_NAME = "country_name";

    public static final String EXTRA_CONTRY_CODE = "country_code";

    public static final int EXTRA_FAQ_CODE = 1;

    public static final int EXTRA_SERVICE_STATUS_CODE = 2;

    public static final int EXTRA_TERMS_OF_SERVICE_CODE = 3;

    public static final int EXTRA_PRIVACY_POLICY_CODE = 4;

    public static final String EXTRA_URL = "url";

    public static final String EXTRA_EMAIL = "email";

    public static final String EXTRA_FIRST_NAME = "first_name";

    public static final String EXTRA_LAST_NAME = "last_name";

    public static final String EXTRA_GROUP_LEAVE = "group_leave";

    public static final String EXTRA_PICTURE_CONFIRMATION_TYPE = "confirmation_type";

    public static final String USER_IDENTIFIER_FAILED = "user_identifier_failed";

    public static final String EXTRA_SORTED_BY = "sorted_by";

    public static final String EXTRA_OPTIONAL_UPGRADE = "optional_upgrade";

    public static final String EXTRA_MANDATORY_UPGRADE = "mandatory_upgrade";

    /* Extensions */
    public static final String VOICE_MESSAGE_EXTENSION = ".m4a";

    public static final String PHOTO_MESSAGE_EXTENSION = ".jpg";

    public static final String VIDEO_MESSAGE_EXTENSION = ".mp4";

    public static final String DOODLE_MESSAGE_EXTENSION = ".jpg";

    public static final String GROUP_NAME_KEY = "group_name_key";

    /**
     * The key used to store the Contact ID of a User or Room in an Extra. 
     * This is used to tell activities which message thread to display
     * when a GCM notification is selected.
     */
    public static final String RECIPIENT_ID_KEY = "recipient_id_key";

    public static final String RECIPIENT_USER_KEY = "recipient_user_key";

    public static final String RECIPIENT_IS_SHOWN = "recipient_is_shown";

    public static final String RECIPIENT_FIRST_NAME_KEY = "recipient_user_firstname_key";

    public static final String RECIPIENT_ROOM_KEY = "recipient_room_key";

    public static final String MEDIA_OUTPUT_PATH_KEY = "media_output_path_key";

    public static final long WELCOME_ROOM_ID = 3141592653L;

    public static final int NEW_GROUP_REQUEST_CODE = 55;

    public static final int DETAIL_SCREEN_REQUEST_CODE = 58;

    public static final int DETAIL_REPLY_DOODLE_RESULT_CODE = 59;

    public static final String INTENT_ACTION_SHOW_IN_APP_NOTIFICATION = "com.littleinc.messageme.MESSAGE_RECEIVED";

    /**
     * When passed without any extras this intent will reload the entire 
     * {@link MessagesFragment} from the database.
     */
    public static final String INTENT_NOTIFY_MESSAGE_LIST = "com.littleinc.messageme.NOTIFY_MESSAGE_LIST";

    public static final String EXTRA_UPDATE_UNREAD_COUNT = "extra_update_unread_count";

    public static final String INTENT_ACTION_MESSAGE_NEW = "com.littleinc.messageme.MESSAGE_NEW";

    public static final String INTENT_ACTION_MESSAGE_READ = "com.littleinc.messageme.MESSAGE_READ";

    public static final String INTENT_ACTION_EARLIER_MESSAGES_AVAILABLE = "com.littleinc.messageme.EARLIER_MESSAGES_AVAILABLE";

    public static final String INTENT_ACTION_MESSAGE_ACTIVITY = "com.littleinc.messageme.MESSAGE_ACTIVITY";

    public static final String USER_IDENTIFIER_INVALID = "com.littleinc.MessageMe.USER_IDENTIFIER_INVALID";

    public static final String INTENT_CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE";

    public static final String INTENT_NOTIFY_CONTACT_LIST = "com.littleinc.messageme.NOTIFY_CONTACT_LIST";

    public static final String INTENT_NOTIFY_TABS_MESSAGE = "com.littleinc.messageme.NOTIFY_TABS_MESSAGE";

    public static final String INTENT_NOTIFY_APP_LOADING = "com.littleinc.messageme.NOTIFY_APP_LOADING";

    public static final String EXTRA_LOADING = "is_loading";

    /**
     * In future this one should probably be combined with 
     * INTENT_NOTIFY_CONTACT_LIST.
     */
    public static final String INTENT_NOTIFY_USER_CHANGED = "com.littleinc.messageme.NOTIFY_USER_CHANGED";

    public static final String SEARCHED_MESSAGE_SORTED_BY = "searched_message_sorted_by";

    public static String PROFILE_PIC = "profile_pic";

    public static String COVER_PIC = "cover_pic";

    public static String USER_NAME = "user_name";

    public static final int GROUP_INVITE_REQUEST_CODE = 60;

    public static final int GROUP_PROFILE_REQUEST_CODE = 66;

    public static final String IS_GROUP_INVITE = "is_group_invite";

    public static final int COUNTRY_SELECTION_REQUEST_CODE = 61;

    public static final int SEARCH_CONTACT_REQUEST_CODE = 65;

    public static final int PHOTO_REQUEST_CODE = 1;

    public static final int VIDEO_REQUEST_SELECT_EXISTING_CODE = 5;

    public static final int CONFIRMATION_PAGE_REQUEST_CODE = 9;

    /* Time Intervals */

    public static final long PING_RETRY_INTERVAL = 15000; // 15 seconds

    public static final int DISCONNECT_THRESHOLD_LIMIT = 300000; // 5 minutes

    public static final long ALERT_SETTINGS_ONE_HOUR = 3600000; // 1 hour

    public static final long ALERT_SETTINGS_THREE_DAYS = 259200000; // 3 days  

    public static final long ONE_DAY_MILLIS = 86400000; // 1 day

    public static final long TEXT_CHANGE_THRESHOLD = 150; //milliseconds

    public static final int UPDATE_SEARCH_MESSAGE = 100;

    public static final long DATE_DIVIDER_INTERVAL = 600; //10 minutes in seconds  

    public static final long COUNTDOWN_THRESHOLD = 1000; // 1 seconds in milliseconds

    public static final long COUNTDOWN_TIMER_LIMIT = 90000; // 90 seconds in milliseconds

    public static final long VIBRATION_SHORT_MILLIS = 80;

    public static final long VIBRATION_LONG_MILLIS = 200;

    public static final int ONE_DAY_SECS = 86400;

    public static final int ONE_WEEK_SECS = 604800;

    public static final long OPTIONAL_UPGRADE_INTERVAL_MILLIS = 86400000;

    /**
     * This special ID is used to show system notices under a single 
     * stacked GCM notification.  This value won't clash with normal 
     * channel IDs (this is "messageme" on a phone keypad).
     */
    public static final long SYSTEM_NOTICES_CHANNEL_ID = 637724363L;

    public static final String SELECTED_AB_CONTACT = "selected_ab_contact";

    /**
     * Advertising API Keys
     * 
     * TapJoy, Apsalar and InMobi are not used.
     */
    public static final String APSALAR_KEY = "littleinc";

    public static final String APSALAR_SECRET = "0Y0v23cy";

    public static final String TAPJOY_KEY = "b75e363a-f930-4c08-a726-e29f8277e832";

    public static final String TAPJOY_SECRET = "w3CRyBvGMEU2IurdL9eg";

    public static final String INMOBI_KEY = "a40f4fd3a2824ca4862a82967e8d19f1";

    public static final String JAMPP_ADVERTISER_ID = "7436";

    public static final String JAMPP_ADVERTISER_KEY = "0b1b2b0d05fa9e0d661c82a2ff968ded";
}
