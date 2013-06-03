package com.littleinc.MessageMe;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.messageMe.OpenUDID.OpenUDID_manager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources.NotFoundException;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.text.TextUtils;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;

import com.coredroid.core.CoreApplication;
import com.coredroid.util.Dimension;
import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.crittercism.app.Crittercism;
import com.littleinc.MessageMe.bo.MessageMeAppPreferences;
import com.littleinc.MessageMe.bo.MessageMeAppState;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.chat.DisconnectTask;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.data.MMLocalData;
import com.littleinc.MessageMe.metrics.MMHourlyTracker;
import com.littleinc.MessageMe.metrics.MMTracker;
import com.littleinc.MessageMe.util.DateUtil;
import com.littleinc.MessageMe.util.NetUtil;
import com.littleinc.MessageMe.util.SyncNotificationUtil;

public class MessageMeApplication extends CoreApplication {

    /**
     * Minimum number of times that app has to be launched
     * before showing the rate app popup.
     */
    public final static int RATE_APP_POPUP_MIN_LAUNCHES = 3;

    /**
     * Maximum number of times to show the rate app popup. After this
     * the popup will not be shown again.
     */
    public final static int RATE_APP_POPUP_MAX_LAUNCHES = 4;

    /**
     * Number of days to wait before showing the rate app
     * popup again. This does not apply the first time the
     * app is launched.
     */
    public final static int RATE_APP_POPUP_INTERVAL = 2;

    private static MessageMeApplication instance;

    private static Handler handler;

    private static boolean sIsDisconnectTaskPending = false;

    private static DisconnectTask disconnectTask;

    /**
     * App version in the format v1.0.10-47.  This field should not be accessed
     * directly, use {@link #getAppVersion()} instead.
     */
    private static String sAppVersion = null;
    
    private static String sUserAgent = null;

    private static final JSONObject sCrittercismConfig;

    static {
        sCrittercismConfig = new JSONObject();
        try {
            // Send logcat data for devices with API Level 16 and higher
            sCrittercismConfig.put("shouldCollectLogcat", true);
        } catch (JSONException je) {
            LogIt.w(MessageMeApplication.class,
                    "Error creating Crittercism config");
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
        OpenUDID_manager.sync(this);
        handler = new Handler();

        if (getCurrentUser() != null) {
            MMHourlyTracker.getInstance().abacusOnce(null, "active", "user",
                    null, null);
        }

        MMTracker.getInstance().abacus("app", "launch", null, null, null);
    }

    /**
     * It is safe to call this multiple times.
     */
    public static void registerWithCrittercism(Context context) {

        // Initialize our crash reporting library
        // 5164f946558d6a5f89000008 - Development app ID
        // 50b3f4d789ea744cd5000003 - Beta app ID
        // 50b3f45863d9524037000004 - Staging app ID
        Crittercism.init(context.getApplicationContext(),
                "5164f946558d6a5f89000008", sCrittercismConfig);

        User user = MessageMeApplication.getPreferences().getUser();

        if (user == null) {
            LogIt.d(context, "Initializing Crittercism");
        } else {
            // If the user has already signed up then put their name and user
            // ID into the crash reports
            Crittercism.setUsername(user.getDisplayName());
            LogIt.d(context, "Initializing Crittercism for user",
                    user.getDisplayName(), user.getContactId());

            try {
                JSONObject metadata = new JSONObject();
                metadata.put("userID", user.getContactId());
                metadata.put("appVersion", getAppVersion());
                Crittercism.setMetadata(metadata);
            } catch (Exception e) {
                LogIt.w(context,
                        "Error adding userID into Crittercism metadata", e);
            }
        }
    }

    public static User getCurrentUser() {
        return getPreferences().getUser();
    }

    public static void setCurrentUser(User user) {
        getPreferences().setUser(user);
    }

    public static MessageMeApplication getInstance() {
        return instance;
    }

    public static String getAppVersion() {
        if (instance == null) {
            LogIt.e(MessageMeApplication.class,
                    "Let application initialize before calling getAppVersion()");
            return "error-error";
        }
        
        if (sAppVersion == null) {            
            PackageManager pm = getInstance().getPackageManager();
            String packageName = getInstance().getPackageName();
            
            try {
                LogIt.d(MessageMeApplication.class, "Get package info",
                        packageName);
                PackageInfo packageInfo = pm.getPackageInfo(packageName, 0);
                
                if (packageInfo == null) {
                    sAppVersion = "unknown-unknown";
                } else {
                    sAppVersion = packageInfo.versionName + "-"
                            + packageInfo.versionCode;
                    LogIt.d(MessageMeApplication.class, "App version",
                            sAppVersion);
                }
            } catch (NameNotFoundException e) {
                LogIt.w(MessageMeApplication.class, e,
                        "Failed to get package info");
            }
        }
        
        return sAppVersion;
    }
    
    
    public static MessageMeAppState getAppState() {
        MessageMeAppState state = getState().get(MessageMeAppState.class);

        if (state == null) {
            state = new MessageMeAppState();
            getState().set(MessageMeAppState.class, state);
        }

        return state;
    }

    public static MessageMeAppPreferences getPreferences() {
        MessageMeAppPreferences prefs = getState().get(
                MessageMeAppPreferences.class);

        if (prefs == null) {
            prefs = new MessageMeAppPreferences();
            getState().set(MessageMeAppPreferences.class, prefs);
        }

        return prefs;
    }

    /**
     * Cleans everything in the app state and app preferences
     */
    public static void resetPreferences() {

        String lastLoginID = null;
        String registeredPhone = getPreferences().getRegisteredPhoneNumber();
        if (TextUtils.isEmpty(registeredPhone)) {
            registeredPhone = getCurrentUser().getPhone();
        }

        // Phone number have precedence over email so only set email as
        // lastLoginID if the user account doesn't have a phone number
        // registered
        if (TextUtils.isEmpty(registeredPhone)) {
            lastLoginID = getCurrentUser().getEmail();
        } else {
            lastLoginID = registeredPhone;
        }

        LogIt.d(MessageMeApplication.class, "Registered last log in term",
                lastLoginID);

        getState().clear();

        MessageMeAppState state = new MessageMeAppState();
        getState().set(MessageMeAppState.class, state);
        state.dirty();

        MessageMeAppPreferences prefs = new MessageMeAppPreferences();
        getState().set(MessageMeAppPreferences.class, prefs);

        // In order to improve user experience after a log out we need
        // to maintain the email or phone of the last logged user to be
        // displayed in the log in activity
        prefs.setLastLoginID(lastLoginID);

        // Force the update of the current app and persistent state
        getState().sync();
    }

    @TargetApi(13)
    @SuppressWarnings("deprecation")
    public static Dimension getScreenSize() {

        Dimension screenSize = new Dimension();

        WindowManager wm = (WindowManager) getInstance().getSystemService(
                Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        try {
            Point size = new Point();
            display.getSize(size);
            screenSize.setWidth(size.x);
            screenSize.setHeight(size.y);
        } catch (NoSuchMethodError e) {
            // These methods were deprecated in API level 13
            screenSize.setWidth(display.getWidth());
            screenSize.setHeight(display.getHeight());
        }

        return screenSize;
    }

    private static HashMap<String, String> sTargetConfig = null;

    /**
     * Get the MessageMeConfig HashMap for the target system defined
     * in res/raw/config.properties
     */
    public static synchronized HashMap<String, String> getTargetConfig() {
        if (sTargetConfig == null) {
            LogIt.d(MessageMeApplication.class, "Loading target configuration");

            Context context = getInstance().getApplicationContext();

            try {
                // Load res/raw/config.properties
                InputStream rawResource = context.getResources()
                        .openRawResource(R.raw.config);
                Properties properties = new Properties();
                properties.load(rawResource);

                // Get the defined "target"
                String target = properties.getProperty("target");

                LogIt.d(MessageMeApplication.class,
                        "Loaded target configuration", properties);

                if (target == null) {
                    LogIt.e(MessageMeApplication.class,
                            "You must define a target in res/raw/config.properties");
                    return null;
                }

                sTargetConfig = MessageMeConfig.getConfigMap().get(target);

            } catch (NotFoundException e) {
                LogIt.e("Did not find res/raw/config.properties", e);
            } catch (IOException e) {
                LogIt.e("Failed to open res/raw/config.properties", e);
            }
        }

        return sTargetConfig;
    }

    /**
     * Get a String suitable for using as the User-Agent in all our requests
     * to the server, in the format:
     * MessageMe/<version> (<device>; <OS>; <screeninfo>)
     * e.g.
     * MessageMe/0.4 (samsung-takju; Android 4.1.2; Scale/720x1184-320dpi)
     * MessageMe/30 (iPhone; iOS 6.0.1; Scale/2.00)
     */
    public static String getUserAgent() {
        if (sUserAgent == null) {
            StringBuilder agent = new StringBuilder();
            
            Dimension screenSize = getScreenSize();

            agent.append("MessageMe");
            agent.append("/");
            agent.append(getAppVersion());
            agent.append(" (");
            agent.append(android.os.Build.MANUFACTURER);
            agent.append("-");
            agent.append(android.os.Build.PRODUCT);
            agent.append("; ");
            agent.append("Android ");
            agent.append(android.os.Build.VERSION.RELEASE);
            agent.append("; Scale/");
            agent.append(screenSize.getWidth());
            agent.append("x");
            agent.append(screenSize.getHeight());
            agent.append("-");
            agent.append(getInstance().getResources().getDisplayMetrics().densityDpi);
            agent.append("dpi");
            agent.append(")");

            sUserAgent = agent.toString();
                
            LogIt.i(MessageMeApplication.class, "Created User-Agent string",
                    sUserAgent);
        }

        return sUserAgent;
    }

    private enum ScreenStatus {
        ON, OFF, UNKNOWN
    };

    /**
     * Utility method to check the current status of the screen.
     */
    public static ScreenStatus getScreenStatus(Context context) {
        ScreenStatus status = ScreenStatus.UNKNOWN;

        if (context != null) {
            PowerManager pm = (PowerManager) context
                    .getSystemService(Context.POWER_SERVICE);

            if (pm != null) {
                if (pm.isScreenOn()) {
                    status = ScreenStatus.ON;
                } else {
                    status = ScreenStatus.OFF;
                }
            } else {
                LogIt.w(MessageMeApplication.class,
                        "PowerManager is null, can't check if screen is on");
            }
        } else {
            LogIt.w(MessageMeApplication.class,
                    "Context is null, can't check if screen is on");
        }

        LogIt.d(MessageMeApplication.class, "Screen status is", status);

        return status;
    }

    /**
     * Utility method for callers who do not already have a Context.
     */
    public static boolean isInForeground() {
        return isInForeground(getInstance());
    }

    /**
     * Test whether the MessageMe application is currently in the foreground,
     * by checking if the topmost activity is our package name.
     */
    public static boolean isInForeground(Context context) {

        boolean isInForeground = false;

        // The app may still be on top of the activity stack when the
        // screen is off, so always check the screen state first
        if (getScreenStatus(context) == ScreenStatus.OFF) {
            LogIt.d(MessageMeApplication.class,
                    "Screen is off, MessageMe is not in foreground");
            return false;
        }

        String topmostActivityClass = UIUtil.getTopActivity(context);

        // The CoreDroid package doesn't need to be checked as MessageMe never
        // uses it to show any activities
        if (topmostActivityClass.contains("com.littleinc.MessageMe")) {
            LogIt.d(MessageMeApplication.class, "MessageMe is in foreground",
                    topmostActivityClass);

            // Remember that we were just in the foreground
            getPreferences().setLastTimeInForeground(DateUtil.now());
            getState().sync();

            isInForeground = true;
        } else {
            LogIt.d(MessageMeApplication.class, "MessageMe is in background",
                    topmostActivityClass);
        }

        return isInForeground;
    }

    /**
     * Send an OFFLINE presence update
     */
    public static void sendOfflinePresence(Context context,
            MessagingService mMessagingServiceRef) {

        if (mMessagingServiceRef == null) {
            LogIt.w(MessageMeApplication.class,
                    "MessagingService is null, cannot send offline presence");
        } else {
            User currentUser = getCurrentUser();

            if (mMessagingServiceRef.isConnected() && (currentUser != null)
                    && currentUser.isOnline()) {
                currentUser.setOnlinePresence(false);
                mMessagingServiceRef.sendOnlineStatus(currentUser.isOnline());
            }
        }
    }

    /**
     * Sends an ONLINE presence update command when the application
     * comes back to the foreground
     */
    public static void sendForegroundOnlinePresence(
            MessagingService mMessagingServiceRef) {

        if (mMessagingServiceRef == null) {
            LogIt.w(MessageMeApplication.class,
                    "MessagingService is null, cannot send online presence");
        } else {
            User currentUser = getCurrentUser();

            if (mMessagingServiceRef.isConnected() && (currentUser != null)
                    && !currentUser.isOnline()) {
                currentUser.setOnlinePresence(true);
                mMessagingServiceRef.sendOnlineStatus(currentUser.isOnline());
            }
        }
    }

    /**
     * All MessageMe Activities and Fragments call this method so we
     * can update the last time the application came into the foreground.
     */
    public static void onResume(Context context) {

        if (getScreenStatus(context) == ScreenStatus.ON) {
            getPreferences().setLastTimeInForeground(DateUtil.now());
            MessageMeApplication.getState().sync();
        }
    }

    /**
     * All MessageMe Activities and Fragments call this method so we
     * can unbind drawables to avoid memory leaks.
     */
    public static void onDestroy(Activity activity) {
        View rootView = null;

        try {
            rootView = ((ViewGroup) activity.findViewById(android.R.id.content))
                    .getChildAt(0);
        } catch (Exception e) {
            LogIt.w(MessageMeApplication.class,
                    "Cannot find root view to call unbindDrawables on",
                    activity);
        }

        if (rootView != null) {
            LogIt.d(MessageMeApplication.class, "unbindDrawables", activity,
                    rootView);
            unbindDrawables(rootView);
        }
    }

    /**
     * All MessageMe Activities and Fragments call this method so we
     * can detect when the application has moved into the foreground.
     */
    public static void appIsActive(Context context,
            MessagingService messagingService) {

        if (messagingService != null) {
            // Tell the MessagingService the UI is running again so if the
            // network was shut down it can reopen.
            //
            // The MessagingService can be null, e.g. if a connection change
            // is received before the activity has finished binding to the
            // MessagingService.
            LogIt.d(MessageMeApplication.class,
                    "appIsActive - tell MessagingService to connect if needed",
                    context);
            messagingService.connectWebSocketIfNeeded();
        }

        boolean isInForeground = MessageMeApplication.isInForeground(context);

        // We need to check if we are in the foreground as we get called
        // for events that might not have brought us to the foreground (e.g.
        // a network connection change).
        if (isInForeground) {
            MessageMeApplication.stopDisconnectTimer(messagingService, context,
                    isInForeground);

            MessageMeApplication.sendForegroundOnlinePresence(messagingService);
        }
    }

    /**
     * All MessageMe Activities call this method so we can detect when the
     * application has moved into the background.
     */
    public static void appIsInactive(Context context,
            MessagingService messagingService) {

        // If the app is now in the background, send an offline presence
        // update and start the disconnect timer
        if (MessageMeApplication.isInForeground(context)) {
            LogIt.d(MessageMeApplication.class,
                    "appIsInactive - in foreground, don't do anything");
        } else {
            LogIt.d(MessageMeApplication.class,
                    "appIsInactive - app is not in foreground");
            MessageMeApplication.sendOfflinePresence(context, messagingService);
            MessageMeApplication
                    .startDisconnectTimer(messagingService, context);

            if (MessageMeApplication.getCurrentUser() != null) {
                // tick our session when we go to the background
                MMLocalData.getInstance().tickSession();
            }
        }
    }

    public static void onScreenChange(boolean isScreenOn, Context context,
            MessagingService messagingService) {

        boolean isInForeground = MessageMeApplication.isInForeground(context);

        if (isScreenOn) {
            stopDisconnectTimer(messagingService, context, isInForeground);
        } else {
            LogIt.d(MessageMeApplication.class,
                    "Screen OFF - start a disconnect timer", context);
            startDisconnectTimer(messagingService, context);
        }

        // Send any presence changes required, if we have a connection
        if (NetUtil.checkInternetConnection(context)) {
            if (isScreenOn) {
                if (isInForeground) {
                    LogIt.d(MessageMeApplication.class,
                            "Screen ON - send online presence", context);
                    MessageMeApplication
                            .sendForegroundOnlinePresence(messagingService);
                }
            } else {
                LogIt.d(MessageMeApplication.class,
                        "Screen OFF - send offline presence", context);
                MessageMeApplication.sendOfflinePresence(context,
                        messagingService);
            }
        }
    }

    /**
     * Start the disconnect task if it hasn't been started already.
     * 
     * We do not check if the app is in the foreground as this code
     * is also used when the screen turns off.
     */
    public static synchronized void startDisconnectTimer(
            MessagingService mMessagingServiceRef, Context context) {

        if (mMessagingServiceRef == null) {
            // XXX Not sure if this is a normal condition...
            LogIt.w(MessageMeApplication.class,
                    "startDisconnectTimer - MessagingService is null so no need to start disconnect timer",
                    context);
        } else {
            if (sIsDisconnectTaskPending) {
                LogIt.d(MessageMeApplication.class,
                        "startDisconnectTimer - disconnect task is already pending",
                        context);
            } else {
                LogIt.d(MessageMeApplication.class,
                        "startDisconnectTimer - start disconnect timer",
                        context);
                disconnectTask = DisconnectTask
                        .getInstance(mMessagingServiceRef);
                handler.postDelayed(disconnectTask,
                        MessageMeConstants.DISCONNECT_THRESHOLD_LIMIT);
                sIsDisconnectTaskPending = true;
            }
        }
    }

    /**
     * Stop the disconnect timer if the app is in the foreground
     */
    public static synchronized void stopDisconnectTimer(
            MessagingService mMessagingServiceRef, Context context,
            boolean isInForeground) {

        // Only stop the disconnect timer if we are in
        // the foreground
        if (isInForeground) {
            if (sIsDisconnectTaskPending) {
                LogIt.d(MessageMeApplication.class,
                        "stopDisconnectTimer - app in foreground, disable disconnect task",
                        context);
                handler.removeCallbacks(disconnectTask);
                sIsDisconnectTaskPending = false;
            } else {
                LogIt.d(MessageMeApplication.class,
                        "stopDisconnectTimer - app in foreground, no disconnect task to disable",
                        context);
            }
        } else {
            LogIt.d(MessageMeApplication.class,
                    "stopDisconnectTimer - app in background, don't touch disconnect timer",
                    context);
        }
    }

    public static void updateLastTimeShowedRatePopup() {
        getPreferences().setLastTimeShowedRatePopup(DateUtil.now());
        getState().sync();
    }

    /**
     * @return whether the 'Rate MessageMe' popup should be shown now.
     */
    public static boolean shouldShowRateAppPopup() {

        // Evidence shows we get much lower ratings on older devices
        // so we don't show the rating popup on them
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            LogIt.i(MessageMeApplication.class,
                    "Don't show app rating popup on devices earlier than OS4.0");
            return false;
        }

        if (getPreferences().isAppRated()) {
            LogIt.i(MessageMeApplication.class, "App is already rated");
            return false;
        } else {
            if (getPreferences().getRateAppShownCount() >= RATE_APP_POPUP_MAX_LAUNCHES) {
                LogIt.d(MessageMeApplication.class,
                        "Don't show Rate App popup as already showed it "
                                + RATE_APP_POPUP_MAX_LAUNCHES + " times");
                return false;
            }

            int launchCount = getPreferences().getLaunchCount();

            Date lastTimeShowedRatePopup = getPreferences()
                    .getLastTimeShowedRatePopup();

            if (lastTimeShowedRatePopup == null) {
                LogIt.d(MessageMeApplication.class,
                        "Rate App popup never been shown, app launched "
                                + RATE_APP_POPUP_MIN_LAUNCHES + " times");

                if (launchCount < RATE_APP_POPUP_MIN_LAUNCHES) {
                    return false;
                } else {
                    return true;
                }
            } else {
                LogIt.d(MessageMeApplication.class,
                        "Rate App popup last shown", lastTimeShowedRatePopup);

                Calendar nextSyncDate = Calendar.getInstance();
                nextSyncDate.setTime(lastTimeShowedRatePopup);
                nextSyncDate
                        .add(Calendar.DAY_OF_MONTH, RATE_APP_POPUP_INTERVAL);

                Calendar currentDate = Calendar.getInstance();
                currentDate.setTime(DateUtil.now());

                if (currentDate.before(nextSyncDate)) {
                    LogIt.d(MessageMeApplication.class,
                            "Don't show rate app popup until",
                            nextSyncDate.getTime());
                    return false;
                } else {
                    LogIt.d(MessageMeApplication.class, "Show rate app popup");
                    return true;
                }
            }
        }
    }

    /**
     * Check whether if DISCONNECT_THRESHOLD_LIMIT time has passed
     * since the app was last in the foreground.
     */
    public static boolean shouldDisconnectNow() {
        Date lastTimeInForeground = getPreferences().getLastTimeInForeground();

        if (lastTimeInForeground == null) {
            LogIt.d(MessageMeApplication.class,
                    "lastTimeInForeground has never been set");
            return false;
        } else {
            Calendar desiredDisconnectTime = Calendar.getInstance();
            desiredDisconnectTime.setTime(lastTimeInForeground);
            desiredDisconnectTime.add(Calendar.MILLISECOND,
                    MessageMeConstants.DISCONNECT_THRESHOLD_LIMIT);

            Calendar currentTime = Calendar.getInstance();
            currentTime.setTime(DateUtil.now());

            if (currentTime.after(desiredDisconnectTime)) {
                LogIt.d(MessageMeApplication.class, "We should disconnect now");

                // Dismiss the Sync notification from the status bar
                SyncNotificationUtil.INSTANCE.cancelSyncNotification();

                return true;
            } else {
                LogIt.d(MessageMeApplication.class, "No need to disconnect yet");
                return false;
            }
        }
    }

    /**
     * Utility method to unbind drawables when an activity is destroyed. This
     * ensures the drawables can be garbage collected.
     */
    public static void unbindDrawables(View view) {
        if (view.getBackground() != null) {
            view.getBackground().setCallback(null);
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                unbindDrawables(((ViewGroup) view).getChildAt(i));
            }

            try {
                // AdapterView objects do not support the removeAllViews method
                if (!(view instanceof AdapterView)) {
                    ((ViewGroup) view).removeAllViews();
                }
            } catch (Exception e) {
                LogIt.w(MessageMeApplication.class,
                        "Ignore Exception in unbindDrawables", e);
            }
        }
    }
}