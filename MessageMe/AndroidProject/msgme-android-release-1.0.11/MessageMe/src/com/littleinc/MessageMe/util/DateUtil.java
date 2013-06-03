package com.littleinc.MessageMe.util;

import java.util.Calendar;
import java.util.Date;

public class DateUtil {

    public static Date now() {
        return new Date();
    }

    /**
     * Get the current time in seconds (not millis).
     */
    public static int getCurrentTimestamp() {
        Calendar calendar = Calendar.getInstance();
        return (int) (calendar.getTimeInMillis() / 1000L);
    }

    /**
     * Convert a Date object into a timestamp in seconds (not millis).
     */
    public static int convertToTimestamp(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return (int) (calendar.getTimeInMillis() / 1000L);
    }

    /**
     * Convert a timestamp in seconds into a Date.
     */
    public static Date convertToDate(int timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp * 1000L);

        return calendar.getTime();
    }
    
    /**
     * Get the current system time in fake microseconds.  This is used
     * when ordering messages in the UI.
     */
    public static double getCurrentTimeMicros() {
        return ((double) System.currentTimeMillis()) * 1000;
    }
}