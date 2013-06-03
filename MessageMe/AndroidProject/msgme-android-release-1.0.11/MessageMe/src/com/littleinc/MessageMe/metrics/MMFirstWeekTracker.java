package com.littleinc.MessageMe.metrics;

import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.data.MMLocalData;

/**
 * MMFirstWeekTracker
 * 
 * metrics tracker that only tracks things for the first week
 * after a user sign up.
 * 
 * @author vivek
 *
 */
public class MMFirstWeekTracker extends MMTracker {
    
    private static final String STORAGE_WEEKLY_EVENTS = "kTrackerWeeklyEventsKey";
    private static MMFirstWeekTracker sInstance;
    
    private HashSet<String> mCapturedEvents;
    
    
    protected MMFirstWeekTracker() {
        super();
        
        mCapturedEvents = new HashSet<String>();
        JSONArray json = MMLocalData.getInstance().getArrayForKey(STORAGE_WEEKLY_EVENTS);
        for (int i = 0; i < json.length(); i++) {
            try {
                mCapturedEvents.add(json.getString(i));
            } catch (JSONException e) {
                LogIt.i(this, "error grabbing captured events from disk");
            }
        }
    }
    
    public static MMFirstWeekTracker getInstance() {
        if (sInstance == null) {
            sInstance = new MMFirstWeekTracker();
        }
        return sInstance;
    }
    
    @Override
    public void abacus(String topic, String event, String subtopicOrNull, Integer versionOrNull, Integer valueOrNull) {
        super.abacus("week_one", event, subtopicOrNull, versionOrNull, valueOrNull);
    }
    
    @Override
    public void abacusOnce(String topic, String event, String subtopicOrNull, Integer versionOrNull, Integer valueOrNull) {
        super.abacusOnce("week_one", event, subtopicOrNull, versionOrNull, valueOrNull);
    }
    
    @Override
    public boolean canTrack() {
        return isFirstWeek();
    }
    
    @Override
    public boolean canTrackOnce(String eventHash) {
        return canTrack() && !mCapturedEvents.contains(eventHash);
    }
    
    @Override
    public void captureEvent(String eventHash) {
        mCapturedEvents.add(eventHash);
        
        JSONArray json = new JSONArray();
        for (String event : mCapturedEvents) {
            json.put(event);
        }
        MMLocalData.getInstance().updateDataForKey(STORAGE_WEEKLY_EVENTS, json);
    }
    
    private boolean isFirstWeek() {
        Integer sinceSignup = (int) (System.currentTimeMillis() / 1000L) - MMLocalData.getInstance().getSignupDate();
        return sinceSignup <= MessageMeConstants.ONE_WEEK_SECS;
    }

}
