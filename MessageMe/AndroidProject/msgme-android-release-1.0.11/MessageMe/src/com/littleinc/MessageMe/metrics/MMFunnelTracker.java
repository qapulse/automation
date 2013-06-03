package com.littleinc.MessageMe.metrics;

import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.data.MMLocalData;

/**
 * MMFunnelTracker
 * 
 * metrics tracker for funnels. given a name and a lifetime, the
 * tracker will make sure an event is tracked exactly once and only
 * if it occurs in the lifetime of the funnel.
 * 
 * @author vivek
 *
 */
public class MMFunnelTracker extends MMTracker {

    public static final String SPLASH_FUNNEL = "kFunnelSplash";

    public static final String SIGNUP_FUNNEL = "kFunnelSignup";

    public static final String LOGIN_FUNNEL = "kFunnelLogin";

    private static final String STORAGE_KEY = "kTrackerFunnelStore";

    private String mName;

    private Integer mLifetime;

    private Integer mStartDate;

    private HashSet<String> mCapturedEvents;

    private JSONObject mFunnel;

    private JSONObject mFunnels;

    private static MMFunnelTracker sInstance;

    protected MMFunnelTracker() {
        super();

        mFunnels = MMLocalData.getInstance().getObjectForKey(STORAGE_KEY);
    }

    public static MMFunnelTracker getInstance() {
        if (sInstance == null) {
            sInstance = new MMFunnelTracker();
        }
        return sInstance;
    }

    public void abacusFunnel(String funnel, Integer lifetime, String event,
            String subtopicOrNull, Integer versionOrNull, Integer valueOrNull) {
        funnelFactory(funnel, lifetime);
        super.abacus(funnel, event, subtopicOrNull, versionOrNull, valueOrNull);
    }

    public void abacusOnceFunnel(String funnel, Integer lifetime, String event,
            String subtopicOrNull, Integer versionOrNull, Integer valueOrNull) {
        funnelFactory(funnel, lifetime);
        super.abacusOnce(funnel, event, subtopicOrNull, versionOrNull,
                valueOrNull);
    }

    @Override
    public boolean canTrack() {
        if (mLifetime == null) {
            return true;
        }

        Integer now = (int) (System.currentTimeMillis() / 1000L);
        return now - mStartDate <= mLifetime;
    }

    @Override
    public boolean canTrackOnce(String eventHash) {
        return canTrack() && !mCapturedEvents.contains(eventHash);
    }

    @Override
    public void captureEvent(String eventHash) {
        mCapturedEvents.add(eventHash);
        // store funnel
        JSONArray events = new JSONArray();
        for (String event : mCapturedEvents) {
            events.put(event);
        }

        try {
            mFunnel.put("capturedEvents", events);
            mFunnels.put(mName, mFunnel);
        } catch (JSONException e) {
            LogIt.e(this, "capture event json failure");
        }

        MMLocalData.getInstance().updateDataForKey(STORAGE_KEY, mFunnels);
    }

    private void funnelFactory(String name, Integer lifetime) {
        mName = name;
        try {
            if (mFunnels.has(mName)) {
                mFunnel = mFunnels.getJSONObject(mName);
            } else {
                mFunnel = new JSONObject();
                mFunnel.put("lifetime", lifetime);
                mFunnel.put("startDate",
                        (int) (System.currentTimeMillis() / 1000L));
                mFunnel.put("capturedEvents", new JSONArray());

                mFunnels.put(mName, mFunnel);
                MMLocalData.getInstance().updateDataForKey(STORAGE_KEY,
                        mFunnels);
            }

            mLifetime = mFunnel.getInt("lifetime");
            mStartDate = mFunnel.getInt("startDate");

            mCapturedEvents = new HashSet<String>();
            if (mFunnel.has("capturedEvents")) {
                JSONArray events = mFunnel.getJSONArray("capturedEvents");
                for (int i = 0; i < events.length(); i++) {
                    mCapturedEvents.add(events.getString(i));
                }
            }
        } catch (JSONException e) {
            LogIt.e(this, "funnel factory json failure: " + e.getMessage());
        }
    }

}
