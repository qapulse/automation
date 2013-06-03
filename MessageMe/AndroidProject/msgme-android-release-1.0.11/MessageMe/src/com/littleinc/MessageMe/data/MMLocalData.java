package com.littleinc.MessageMe.data;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.littleinc.MessageMe.MessageMeApplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;


/**
 * MMLocalData
 * 
 * holds miscellaneous local data such as the data user signed up/logged in
 * and how many 'sessions' have happened since then. should call 'clear()'
 * when user is logging out.
 * 
 * @author vivek
 */
public class MMLocalData {

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private Integer mSession;

    private Integer mSignupDate;

    private Integer mLoginDate;
    
    // do NOT clear this on logout.
    private Integer mLaunchCount;

    private SharedPreferences preferences;
    
    // ticks up every time it's accessed. it's used
    // to track the order in which events happen in
    // a session.
    private Integer mSessionOrder;

    private static MMLocalData sInstance;

    private MMLocalData() {
        super();

        Context context = MessageMeApplication.getInstance()
                .getApplicationContext();
        preferences = context.getSharedPreferences("MMLocalData",
                Context.MODE_PRIVATE);
        mSessionOrder = 0;
    }

    public static MMLocalData getInstance() {
        if (sInstance == null) {
            sInstance = new MMLocalData();
        }
        return sInstance;
    }
    
    public void clear() {
        setSession(null);
        setSignupDate(null);
        setLoginDate(null);
    }

    public Integer getSession() {
        if (mSession == null) {
            mSession = getIntegerForKey("session");
        }
        return mSession;
    }
    
    private void setSession(Integer session) {
        mSession = session;
        updateDataForKey("session", mSession);
    }
    
    public synchronized Integer getSessionOrder() {
        mSessionOrder += 1;
        return mSessionOrder;
    }

    public void tickSession() {
        setSession(getSession() + 1);
        mSessionOrder = 0;
    }

    public Integer getSignupDate() {
        if (mSignupDate == null) {
            mSignupDate = getIntegerForKey("signup_date");
        }
        return mSignupDate;
    }

    public void setSignupDate(Integer signupDate) {
        mSignupDate = signupDate;
        updateDataForKey("signup_date", signupDate);
    }

    public Integer getLoginDate() {
        if (mLoginDate == null) {
            mLoginDate = getIntegerForKey("login_date");
        }
        return mLoginDate;
    }

    public void setLoginDate(Integer loginDate) {
        mLoginDate = loginDate;
        updateDataForKey("login_date", loginDate);
    }
    
    public void tickLaunchCount() {
        setLaunchCount(getLaunchCount() + 1);
    }
    
    public Integer getLaunchCount() {
        if (mLaunchCount == null) {
            mLaunchCount = getIntegerForKey("mm_launch_count");
        }
        return mLaunchCount;
    }
    
    private void setLaunchCount(Integer launchCount) {
        mLaunchCount = launchCount;
        updateDataForKey("mm_launch_count", mLaunchCount);
    }

    public Integer getIntegerForKey(String key) {
        return preferences.getInt(key, 0);
    }
    
    public JSONObject getObjectForKey(String key) {
        try {
            String json = preferences.getString(key, null);
            if (json == null) {
                return new JSONObject();
            }
            return new JSONObject(json);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }
    
    public JSONArray getArrayForKey(String key) {
        try {
            String json = preferences.getString(key, null);
            if (json == null) {
                return new JSONArray();
            }
            return new JSONArray(json);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    public void updateDataForKey(String key, Integer value) {
        class IntegerWriter implements Runnable {
            private String mKey;
            private Integer mValue;
            
            public IntegerWriter(String key, Integer value) {
                super();
                mKey = key;
                mValue = value;
            }
            
            public void run() {
                Editor editor = preferences.edit();
                if (mValue == null) {
                    editor.remove(mKey);
                } else {
                    editor.putInt(mKey, mValue);
                }
                editor.commit();                
            }
        }
        
        IntegerWriter writer = new IntegerWriter(key, value);
        mExecutor.execute(writer);
    }
    
    public void updateDataForKey(String key, JSONArray value) {
        class ArrayWriter implements Runnable {
            private String mKey;
            private JSONArray mValue;
            
            public ArrayWriter(String key, JSONArray value) {
                super();
                mKey = key;
                mValue = value;
            }
            
            public void run() {
                Editor editor = preferences.edit();
                if (mValue == null) {
                    editor.remove(mKey);
                } else {
                    editor.putString(mKey, mValue.toString());
                }
                editor.commit();
            }
        }
        
        ArrayWriter writer = new ArrayWriter(key, value);
        mExecutor.execute(writer);
    }
    
    public void updateDataForKey(String key, JSONObject value) {
        class ObjectWriter implements Runnable {
            private String mKey;
            private JSONObject mValue;
            
            public ObjectWriter(String key, JSONObject value) {
                super();
                mKey = key;
                mValue = value;
            }
            
            public void run() {
                Editor editor = preferences.edit();
                if (mValue == null) {
                    editor.remove(mKey);
                } else {
                    editor.putString(mKey, mValue.toString());
                }
                editor.commit();
            }
        }
        
        ObjectWriter writer = new ObjectWriter(key, value);
        mExecutor.execute(writer);
    }
}
