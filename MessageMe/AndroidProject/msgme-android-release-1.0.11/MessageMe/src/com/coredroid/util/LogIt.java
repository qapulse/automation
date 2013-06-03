package com.coredroid.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;

import android.util.Log;

public class LogIt {

    /** Global tag to define all logs made by the app */
    public static final String TAG = "MessageMe";
  
    /**
     * Whether to also log to our internal logBuffer or not. 
     */
    private static boolean sSaveLogsToBuffer = false;
    
    /**
     * The maximum number of lines to buffer up for emailing to our support
     * email address.  This needs to be small enough that the logs do not
     * get too close to 1Mb, otherwise we'll hit a TransactionTooLargeException
     * when launching the email application with our logs in the intent.
     */
    private static final int BUFFER_SIZE = 400;
    
    /**
     * Buffer for holding our logcat logs in case the user turns on diagnostics
     * and wants to email them to us.
     */
    private static ArrayBlockingQueue<String> logBuffer = new ArrayBlockingQueue<String>(
            BUFFER_SIZE);
    
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    /**
     * User level logs to indicate that the user has carried out an action 
     * in the UI, e.g. touching a button.
     */
    public static void user(Object src, Object... message) {
        StringBuilder builder = new StringBuilder();
        
        builder.append("USER: ");
        appendClassName(src, builder);
        appendObjects(builder, message);
        
        String log = builder.toString();
        
        if (sSaveLogsToBuffer) {
            saveToBuffer(log);
        }
        
        Log.i(TAG, log);
    }
    
    /**
     * Error level logs to indicate an Exception has occurred.
     */
    public static void e(Object src, Throwable t, Object... message) {
        StringBuilder builder = new StringBuilder();
        
        if (t != null) {
            builder.append(t.getMessage()).append(": ");
        } else {
            builder.append("ERROR: ");
        }
        
        appendClassName(src, builder);
        appendObjects(builder, message);

        String log = builder.toString();
        
        if (sSaveLogsToBuffer) {
            saveToBuffer(log);
        }
        
        Log.e(TAG, log, t);
    }
    
    /**
     * Error level logs to indicate a serious error, or a programming
     * mistake has occurred.
     */
    public static void e(Object src, Object... message) {
        StringBuilder builder = new StringBuilder();
        
        appendClassName(src, builder);
        appendObjects(builder, message);

        String log = builder.toString();
        
        if (sSaveLogsToBuffer) {
            saveToBuffer(log);
        }
        
        Log.e(TAG, log);
    }

    /**
     * Warning level logs to indicate that a recoverable condition has occurred
     * which significantly impacts the app, e.g. the network connection has been
     * lost, or a message upload has failed.
     */
    public static void w(Object src, Object... message) {
        StringBuilder builder = new StringBuilder();
        
        appendClassName(src, builder);
        appendObjects(builder, message);
        
        String log = builder.toString();
        
        if (sSaveLogsToBuffer) {
            saveToBuffer(log);
        }
        
        Log.w(TAG, log);
    }
    
    /**
     * Info level logs for important diagnostics, e.g. a message upload starts
     * or completes.
     */
    public static void i(Object src, Object... message) {
        StringBuilder builder = new StringBuilder();
        
        appendClassName(src, builder);
        appendObjects(builder, message);
        
        String log = builder.toString();
        
        if (sSaveLogsToBuffer) {
            saveToBuffer(log);
        }
        
        Log.i(TAG, log);
    }
    
    /**
     * Debug level logs for low level diagnostics.
     */
    public static void d(Object src, Object... message) {
        //		if (!Log.isLoggable(src.getClass().getSimpleName(), Log.DEBUG)) {
        //			return;
        //		}
        StringBuilder builder = new StringBuilder();
        
        appendClassName(src, builder);
        appendObjects(builder, message);
        
        String log = builder.toString();
        
        if (sSaveLogsToBuffer) {
            saveToBuffer(log);
        }
        
        Log.d(TAG, log);
    }
    
    /**
     * Verbose level logs for extremely low level diagnostics that could 
     * otherwise create performance issues or spam Logcat with too many logs. 
     * 
     * This will not be logged to Logcat unless verbose logging has been turned
     * on for this app on the device.
     */
    public static void v(Object src, Object... message) {
        StringBuilder builder = new StringBuilder();
        
        appendClassName(src, builder);
        appendObjects(builder, message);
        
        // Never save verbose logs to our logBuffer
        Log.v(TAG, builder.toString());
    }
    
    /**
     * This method can be used to get the time suitable for logging. This is
     * only used in our internal log buffer as Logcat already lets you see the 
     * time for all logs:
     *   adb logcat -v threadtime
     */
    private static String getTime() {        
        StringBuilder builder = new StringBuilder();
        builder.append(DATE_FORMAT.format(new Date()));
        builder.append(" ");
        return builder.toString();
    }
    
    private static void appendObjects(StringBuilder builder, Object... message) {
        for (Object o : message) {
            builder.append(o).append(", ");
        }
    }
    
    private static void appendClassName(Object src, StringBuilder builder) {
        Class c = src instanceof Class ? (Class) src : src.getClass();
        builder.append(c.getSimpleName());
        builder.append(": ");
    }
    
    public static String getBufferedLogs() {
        
        // Do a direct Logcat log so we know what size the logs are
        Log.i(TAG, "getBufferedLogs called with " + logBuffer.size() + " lines");
        
        StringBuilder output = new StringBuilder();
        
        for (String log : logBuffer) {            
            output.append(log);
            output.append("\n");
        }
        
        return output.toString();
    }
    
    private static void saveToBuffer(String log) {
        // Check if we need to remove an element before adding the next 
        // one.  We give ourselves a safety margin to allow for 
        // concurrent access, as we never actually want to block when
        // writing to our buffer.
        if (logBuffer.size() >= (BUFFER_SIZE - 10)) {
            // Remove the head
            logBuffer.poll();
        }
        
        logBuffer.offer(getTime() + log);
    }
    
    /**
     * Return whether the app should save logs so the user can
     * email them to us.
     */
    public static boolean isLoggingOn() {
        return sSaveLogsToBuffer;
    }

    public static void setLoggingOn(boolean isLoggingOn) {
        sSaveLogsToBuffer = isLoggingOn;
    }
}
