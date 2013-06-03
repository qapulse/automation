package com.littleinc.MessageMe.metrics;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.metrics.mixpanel.MixpanelAPI;


/**
 * MMTracker
 * 
 * metrics tracking base class that exposes methods to track events
 * 
 * @author vivek
 */
public class MMTracker extends Object {

    private static MMTracker sInstance;

    protected MixpanelAPI mMixpanel;

    /**
     * constructor: should NOT be called directly.
     */
    protected MMTracker() {
        LogIt.i(this, "MMTracker constructor");
        Context context = MessageMeApplication.getInstance()
                .getApplicationContext();
        mMixpanel = MixpanelAPI.getInstance(context, "");
    }

    /**
     * singleton (unsafe because it's not synchronized)
     */
    public static MMTracker getInstance() {
        if (sInstance == null) {
            sInstance = new MMTracker();
        }
        return sInstance;
    }

    /**
     * track an event with no meta data attached
     */
    public void track(String event) {
        trackEvent(event, null);
    }

    /**
     * wrapper around mixpanel's trackEvent function.
     */
    public void trackEvent(String event, JSONObject properties) {
        if (!canTrack()) {
            return;
        }

        User user = MessageMeApplication.getCurrentUser();
        if (mMixpanel.getDistinctId() == null && user != null) {
            mMixpanel.identify(String.valueOf(user.getUserId()));
        }

        mMixpanel.track(event, properties);
    }

    /**
     * track an event exactly once with no meta data attached.
     */
    public void trackOnce(String event) {
        trackOnceEvent(event, null);
    }

    /**
     * interface to track an event exactly once.
     */
    public void trackOnceEvent(String event, JSONObject properties) {
        if (!canTrackOnce(event)) {
            return;
        }

        captureEvent(event);
        mMixpanel.track(event, properties);
    }

    public void abacus(String topic, String event, String subtopicOrNull,
            Integer versionOrNull, Integer valueOrNull) {
        abacusEvent(topic, event, subtopicOrNull, versionOrNull, valueOrNull,
                null, null, null);
    }

    public void abacusOnce(String topic, String event, String subtopicOrNull,
            Integer versionOrNull, Integer valueOrNull) {
        String hash = topic + ":" + event
                + (subtopicOrNull == null ? "" : subtopicOrNull) + ":"
                + (versionOrNull == null ? "" : versionOrNull);
        if (!canTrackOnce(hash)) {
            LogIt.i(this, "already tracked: " + hash);
            return;
        }
        LogIt.i(this, "tracking event: " + hash);
        captureEvent(hash);
        abacusEvent(topic, event, subtopicOrNull, versionOrNull, valueOrNull,
                null, null, null);
    }

    protected void abacusEvent(String topic, String event,
            String subtopicOrNull, Integer versionOrNull, Integer valueOrNull,
            Long userIdOrNull, Long roomIdOrNull, Long messageIdOrNull) {
        if (!canTrack()) {
            return;
        }
        
        LogIt.i(this, "Tracking ", topic, event, subtopicOrNull);
        
        JSONObject properties = new JSONObject();
        try {
            properties.put("topic", topic);
            properties.put("event", event);

            if (subtopicOrNull != null) {
                properties.put("subtopic", subtopicOrNull);
            }

            if (versionOrNull != null) {
                properties.put("version", versionOrNull);
            }

            if (valueOrNull != null) {
                properties.put("value", valueOrNull);
            }

            if (userIdOrNull != null) {
                properties.put("user2_id", userIdOrNull);
            }

            if (roomIdOrNull != null) {
                properties.put("room_id", roomIdOrNull);
            }

            if (messageIdOrNull != null) {
                properties.put("message_id", messageIdOrNull);
            }

            User user = MessageMeApplication.getCurrentUser();
            if (user != null) {
                properties.put("user_id", user.getUserId());
            }

            mMixpanel.track("abacus", properties);
        } catch (JSONException e) {
            LogIt.e(this, "tracking error");
        }

    }

    /**
     * always returns true here. subclasses can override.
     */
    public boolean canTrack() {
        return true;
    }

    /**
     * always returns true here. subclasses can override
     */
    public boolean canTrackOnce(String eventHash) {
        return true;
    }

    /**
     * does nothing. subclasses should implement their own version.
     */
    public void captureEvent(String eventHash) {
        // subclasses should override
    }

}
