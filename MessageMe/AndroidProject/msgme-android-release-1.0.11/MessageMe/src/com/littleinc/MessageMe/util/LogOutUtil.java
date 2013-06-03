package com.littleinc.MessageMe.util;

import java.io.IOException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;

import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.data.DataBaseHelper;
import com.littleinc.MessageMe.data.MMLocalData;
import com.littleinc.MessageMe.ui.LandingActivity;

public class LogOutUtil {

    public static void logoutUser(final Context context,
            final MessagingService messagingServiceRef) {

        LogIt.d(context, "Starting logout process");

        final ProgressDialog progressDialog = UIUtil.showProgressDialog(
                context,
                context.getString(R.string.log_out_progress_dialog_message));

        new DatabaseTask() {

            @Override
            public void work() {

                // Clears all the tables of the local DB
                DataBaseHelper.getInstance().shutDown();
                DataBaseHelper.getInstance().clearDataBase();

                // Remove local data
                MMLocalData.getInstance().clear();

                try {
                    FileSystemUtil.deleteFiles(MessageMeApplication
                            .getInstance().getFilesDir());
                } catch (IOException e) {
                    LogIt.e(this, e, e.getMessage());
                }
            }

            @Override
            public void done() {

                progressDialog.dismiss();
                LogIt.d(LogOutUtil.class, "Successfully logged out");

                // Disconnects from the server
                messagingServiceRef.shutdownNetworkAndThreads();

                MessageMeApplication.resetPreferences();
                ImageLoader.getInstance().clearCache();

                Intent intent = new Intent(context, LandingActivity.class);

                // FLAG_ACTIVITY_CLEAR_TASK doesn't work on old devices so we
                // need to use FLAG_ACTIVITY_CLEAR_TOP in combination with
                // FLAG_ACTIVITY_NEW_TASK to achieve a similar behavior
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
                        | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                context.startActivity(intent);

                Activity activity = (Activity) context;
                activity.finish();
            }
        };
    }
}
