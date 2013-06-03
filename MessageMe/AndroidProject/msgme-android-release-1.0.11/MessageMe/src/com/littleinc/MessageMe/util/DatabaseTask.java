package com.littleinc.MessageMe.util;

import android.os.Handler;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.data.DataBaseHelper;

/**
 * Class to perform interactions with the database.
 * These tasks has priority over {@link BatchTask} and notifies the caller thread when is done. 
 * These tasks will be added into a queue in the {@link DataBaseHelper} to be processed in the background.
 */
public abstract class DatabaseTask {

    protected Handler handler;

    private Throwable error;

    private String errorTitle;

    private String errorMessage;

    public DatabaseTask() {
        handler = new Handler();
        DataBaseHelper.getInstance().addTask(this);
    }

    public DatabaseTask(Handler handler) {
        this.handler = handler;
        DataBaseHelper.getInstance().addTask(this);
    }

    public abstract void work();

    public abstract void done();

    /**
     * Whether there was an error while working, typically checked in done()
     */
    protected boolean failed() {
        return error != null || errorTitle != null || errorMessage != null;
    }

    protected void fail(String title, String message) {
        fail(null, title, message);
    }

    protected void fail(String message) {
        fail(null, null, message);
    }

    public void fail(Throwable t) {
        fail(t, null, null);
    }

    protected void fail(Throwable t, String message) {
        fail(t, null, message);
    }

    protected void fail(Throwable t, String tittle, String message) {
        error = t;
        errorTitle = tittle;
        errorMessage = message;
        LogIt.e(this, t, "DatabaseTask has failed", message);
    }

    protected Throwable getException() {
        return error;
    }

    protected String getExceptionMessage() {
        StringBuilder builder = new StringBuilder();

        if (!StringUtil.isEmpty(errorMessage)) {
            builder.append(errorMessage);
        }

        if (error != null) {
            if (builder.length() > 0) {
                builder.append(": ");
            }

            builder.append(error.toString());
        }

        return builder.toString();
    }

    protected String getExceptionTitle() {
        StringBuilder builder = new StringBuilder();

        if (!StringUtil.isEmpty(errorTitle)) {
            builder.append(errorTitle);
        }

        return builder.toString();
    }

    public void notifyCallerThread() {
        handler.post(new Runnable() {

            @Override
            public void run() {
                done();
            }
        });
    }
}
