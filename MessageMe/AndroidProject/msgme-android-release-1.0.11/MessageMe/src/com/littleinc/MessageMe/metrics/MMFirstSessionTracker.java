package com.littleinc.MessageMe.metrics;

import java.util.HashSet;

import com.littleinc.MessageMe.data.MMLocalData;


/**
 * MMFirstSessionTracker
 * 
 * metrics tracker that only tracks events that happen
 * in the first session.
 * 
 * @author vivek
 *
 */
public class MMFirstSessionTracker extends MMTracker {

    private static MMFirstSessionTracker sInstance;

    private HashSet<String> mCapturedEvents;

    private boolean mIsFirstSession;

    /**
     * constructor: should NOT be called directly
     */
    protected MMFirstSessionTracker() {
        super();

        mCapturedEvents = new HashSet<String>();
        mIsFirstSession = (MMLocalData.getInstance().getSession() == 1);
    }

    public static MMFirstSessionTracker getInstance() {
        if (sInstance == null) {
            sInstance = new MMFirstSessionTracker();
        }
        return sInstance;
    }

    @Override
    public void abacus(String topic, String event, String subtopicOrNull,
            Integer versionOrNull, Integer valueOrNull) {
        super.abacus("session_one", event, subtopicOrNull, versionOrNull,
                valueOrNull);
    }
    
    @Override
    public void abacusOnce(String topic, String event, String subtopicOrNull,
            Integer versionOrNull, Integer valueOrNull) {
        super.abacusOnce("session_one", event, subtopicOrNull, versionOrNull,
                valueOrNull);
    }

    @Override
    public boolean canTrack() {
        return mIsFirstSession;
    }
    
    @Override
    public boolean canTrackOnce(String eventHash) {
        return canTrack() && !mCapturedEvents.contains(eventHash);
    }
    
    @Override
    public void captureEvent(String eventHash) {
        mCapturedEvents.add(eventHash);
    }

}
