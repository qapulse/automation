package com.littleinc.MessageMe.metrics;

import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.data.MMLocalData;

/**
 * MMHourlyTracker
 * 
 * metrics tracker that tracks an event exactly once per hour
 * 
 * @author vivek
 *
 */
public class MMHourlyTracker extends MMTracker {

    private static final String STORAGE_CURRENT_HOUR = "kTrackerHourlyCurrent";

    private static final String STORAGE_HOURLY_EVENTS = "kTrackerHourlyEventsKey";

    private static MMHourlyTracker sInstance;

    private HashSet<String> mCapturedEvents;

    private Integer mCurrentHour;

    protected MMHourlyTracker() {
        super();

        mCapturedEvents = new HashSet<String>();
        JSONArray events = MMLocalData.getInstance().getArrayForKey(
                STORAGE_HOURLY_EVENTS);
        for (int i = 0; i < events.length(); i++) {
            try {
                mCapturedEvents.add(events.getString(i));
            } catch (JSONException e) {
                LogIt.i(this, "error loading hourly captured events from disk");
            }
        }

        mCurrentHour = MMLocalData.getInstance().getIntegerForKey(
                STORAGE_CURRENT_HOUR);
        if (mCurrentHour == null) {
            mCurrentHour = (int) (System.currentTimeMillis() / 1000L);
        }
    }

    public static MMHourlyTracker getInstance() {
        if (sInstance == null) {
            sInstance = new MMHourlyTracker();
        }
        return sInstance;
    }

    @Override
    public void trackEvent(String event, JSONObject properties) {
        tickCurrentHour();
        super.trackEvent(event, properties);
    }

    @Override
    public void trackOnceEvent(String event, JSONObject properties) {
        tickCurrentHour();
        super.trackOnceEvent(event, properties);
    }

    @Override
    public void abacus(String topic, String event, String subtopicOrNull,
            Integer versionOrNull, Integer valueOrNull) {
        tickCurrentHour();
        super.abacus("hourly", event, subtopicOrNull, versionOrNull,
                valueOrNull);
    }

    @Override
    public void abacusOnce(String topic, String event, String subtopicOrNull,
            Integer versionOrNull, Integer valueOrNull) {
        tickCurrentHour();
        super.abacusOnce("hourly", event, subtopicOrNull, versionOrNull,
                valueOrNull);
    }

    @Override
    public boolean canTrack() {
        return checkCurrentHour();
    }

    @Override
    public boolean canTrackOnce(String eventHash) {
        return canTrack() && mCapturedEvents.contains(eventHash);
    }

    @Override
    public void captureEvent(String eventHash) {
        mCapturedEvents.add(eventHash);

        JSONArray json = new JSONArray();
        for (String event : mCapturedEvents) {
            json.put(event);
        }
        MMLocalData.getInstance().updateDataForKey(STORAGE_HOURLY_EVENTS, json);
    }

    private void tickCurrentHour() {
        Integer currentHour = (int) ((System.currentTimeMillis() / 1000L) / 3600);
        if (currentHour > mCurrentHour) {
            mCurrentHour = currentHour;
            mCapturedEvents.clear();

            MMLocalData.getInstance().updateDataForKey(STORAGE_CURRENT_HOUR,
                    currentHour);
            MMLocalData.getInstance().updateDataForKey(STORAGE_HOURLY_EVENTS,
                    new JSONArray());
        }
    }

    private boolean checkCurrentHour() {
        Integer currentHour = (int) ((System.currentTimeMillis() / 1000L) / 3600);
        return currentHour == mCurrentHour;
    }

}
