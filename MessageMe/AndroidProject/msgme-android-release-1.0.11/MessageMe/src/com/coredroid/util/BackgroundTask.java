package com.coredroid.util;

import android.os.Handler;

public abstract class BackgroundTask {

    protected Handler handler;

    private Throwable error;

    private String errorTitle;

    private String errorMessage;

    public BackgroundTask() {
        handler = new Handler();

        new BackgroundThread().start();
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

    protected void fail(Throwable t) {
        fail(t, null, null);
    }

    protected void fail(Throwable t, String message) {
        fail(t, null, message);
    }

    protected void fail(Throwable t, String tittle, String message) {
        error = t;
        errorTitle = tittle;
        errorMessage = message;
        LogIt.e(this, t, "BackgroundTask has failed", message);
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

    private class BackgroundThread extends Thread {

        @Override
        public void run() {

            try {
                work();
            } catch (Throwable t) {
                fail(t, null, null);
            }

            handler.post(new Runnable() {

                @Override
                public void run() {
                    try {
                        done();
                    } catch (Throwable t) {
                        LogIt.e(this, t,
                                "BackgroundThread hit an exception in run()",
                                t.getMessage());
                    }
                }
            });
        }
    }
}
