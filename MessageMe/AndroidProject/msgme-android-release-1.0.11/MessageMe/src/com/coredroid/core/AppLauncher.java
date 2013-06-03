package com.coredroid.core;

import android.content.Intent;
import android.os.Bundle;

import com.coredroid.ui.CloseActivityClickListener;
import com.coredroid.ui.CoreActivity;
import com.coredroid.util.BackgroundTask;
import com.coredroid.util.LogIt;
import com.crittercism.app.Crittercism;
import com.littleinc.MessageMe.data.DataBaseHelper;

/**
 * Show a splash image for a brief time while the application is initialized.
 * <br>
 * The application should extend this activity and make it the launch/main intent.
 * It is also important to add this to the activity tag in AndroidManifest.xml
 * <code>android:configChanges="keyboard|orientation"</code> 
 */
public abstract class AppLauncher extends CoreActivity {

    private static final int DEFAULT_MIN_DISPLAY_TIME = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new BackgroundTask() {

            @Override
            public void work() {

                long start = System.currentTimeMillis();

                // If app is restarting, clear out old state
                CoreApplication.getState().clear();

                // App specific initialization
                try {
                    init();
                } catch (InitializationException e) {
                    fail(e.getMessage());
                } catch (android.database.SQLException e) {
                    // Raw SQL operations in our database upgrade can throw 
                    // an android.database.SQLException
                    //
                    // Don't fail() here as we'll handle this situation in
                    // MessageMeLauncher.onInitComplete.
                    try {
                        LogIt.e(DataBaseHelper.class, e);
                        Crittercism.logHandledException(e);
                    } catch (Exception ex) {
                        LogIt.e(DataBaseHelper.class, ex,
                                "Unable to log exception because Crittercism is not initialized yet");
                    }
                } catch (Throwable t) {
                    fail(t, "Failure to init");
                    try {
                        Crittercism.logHandledException(t);
                    } catch (Exception ex) {
                        LogIt.e(DataBaseHelper.class, ex,
                                "Unable to log exception because Crittercism is not initialized yet");
                    }
                    return;
                }

                // Ensure the screen stays up long enough
                long duration = System.currentTimeMillis() - start;
                long delay = getMinimumDisplayTime() - duration;
                if (delay > 0) {
                    try {
                        synchronized (this) {
                            Thread.sleep(delay);
                        }
                    } catch (Exception e) {
                        LogIt.w(AppLauncher.class, e,
                                "Exception showing splash screen");
                    }
                }
            }

            @Override
            public void done() {
                if (failed()) {
                    alert(getExceptionMessage(),
                            new CloseActivityClickListener(AppLauncher.this));
                    return;
                }

                Intent intent = onInitComplete();
                if (intent != null) {
                    startActivity(intent);
                    finish();
                } else {
                    LogIt.i(AppLauncher.class, "No activity specified");
                }
            }
        };
    }

    /**
     * Called after init has completed.  Will pass control to the supplied intent
     */
    protected abstract Intent onInitComplete();

    /**
     * Called after the splash has appeared and runs in the background.
     */
    protected void init() {
    }

    protected int getMinimumDisplayTime() {
        return DEFAULT_MIN_DISPLAY_TIME;
    }
}
