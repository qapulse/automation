package com.littleinc.MessageMe.util;

import com.littleinc.MessageMe.MessageMeApplication;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetUtil {

    /**
     * Helper method for callers who do not already have a Context.
     */
    public static boolean checkInternetConnection() {
        return checkInternetConnection(MessageMeApplication.getInstance());
    }
    
    public static boolean checkInternetConnection(Context context) {
        ConnectivityManager conMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = conMgr.getActiveNetworkInfo();
        
        if ((networkInfo != null)
                && networkInfo.isAvailable()
                && networkInfo.isConnected()) {
            return true;
        }

        return false;
    }
}