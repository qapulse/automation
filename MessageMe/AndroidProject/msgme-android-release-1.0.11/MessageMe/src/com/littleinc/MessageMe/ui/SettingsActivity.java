package com.littleinc.MessageMe.ui;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.coredroid.util.LogIt;
import com.coredroid.util.MailUtil;
import com.coredroid.util.StringUtil;
import com.coredroid.util.UIUtil;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.actionbar.ActionBarActivity;
import com.littleinc.MessageMe.bo.AlertSetting;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.util.AlertPickClickListener;
import com.littleinc.MessageMe.util.EditUserProfile;
import com.littleinc.MessageMe.util.LogOutUtil;

/**
 * Settings class for the current user
 * 
 */
@TargetApi(14)
public class SettingsActivity extends ActionBarActivity {

    private User currentUser;

    private GestureDetector gestureDetector;

    private OnTouchListener gestureListener;

    private ImageView logo;

    private TextView userEmail;

    private Button about;

    private Button getHelp;

    private TextView phoneNumber;

    private Button editProfile;

    private TextView version;

    private TextView alertPeriod;

    private Button alertBtn;

    private Button changePassword;

    private Button logout;

    private CheckBox soundAlert;

    private CheckBox vibrateAlert;

    private LinearLayout diagnosticsSection;

    private Button sendLogs;

    private CheckBox saveLogsCheckbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings_layout);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            ActionBar actionBar = getActionBar();
            actionBar.setHomeButtonEnabled(true);
            actionBar.setIcon(R.drawable.actionbar_top_icon_back);
        }

        setTitle(getString(R.string.settings_title));

        currentUser = MessageMeApplication.getCurrentUser();

        logo = (ImageView) findViewById(R.id.logo);

        // Make a double tap on the logo show the diagnostics section
        gestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        LogIt.user(SettingsActivity.class,
                                "Double tap to show diagnostics section");
                        diagnosticsSection.setVisibility(View.VISIBLE);

                        return true;
                    }
                });

        gestureListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    return true;
                }
                return false;
            }
        };

        logo.setOnTouchListener(gestureListener);

        userEmail = (TextView) findViewById(R.id.user_email);

        about = (Button) findViewById(R.id.about_btn);

        getHelp = (Button) findViewById(R.id.get_help_btn);

        phoneNumber = (TextView) findViewById(R.id.phone_number);

        editProfile = (Button) findViewById(R.id.edit_profile);

        alertPeriod = (TextView) findViewById(R.id.alert_period);

        changePassword = (Button) findViewById(R.id.change_password);

        version = (TextView) findViewById(R.id.version);

        alertBtn = (Button) findViewById(R.id.settings_btn);

        logout = (Button) findViewById(R.id.logout_btn);

        soundAlert = (CheckBox) findViewById(R.id.alert_sound_checkbox);

        vibrateAlert = (CheckBox) findViewById(R.id.alert_vibrate_checkbox);

        soundAlert.setChecked(MessageMeApplication.getPreferences()
                .isSoundAlertActive());

        vibrateAlert.setChecked(MessageMeApplication.getPreferences()
                .isVibrateAlertActive());

        soundAlert
                .setOnCheckedChangeListener(new SoundAlertCheckedChangeListener());

        vibrateAlert
                .setOnCheckedChangeListener(new VibrateAlertCheckedListener());

        userEmail.setText(currentUser.getEmail());

        phoneNumber
                .setText(StringUtil.isEmpty(currentUser.getPhone()) ? getString(R.string.no_phone)
                        : currentUser.getPhone());

        diagnosticsSection = (LinearLayout) findViewById(R.id.diagnostics_section);

        if (LogIt.isLoggingOn()) {
            LogIt.d(this, "Show diagnostics section as logging is on");
            diagnosticsSection.setVisibility(View.VISIBLE);
        }

        sendLogs = (Button) findViewById(R.id.send_logs_btn);

        sendLogs.setOnClickListener(new SendLogsClickListener());

        saveLogsCheckbox = (CheckBox) findViewById(R.id.logging_checkbox);

        saveLogsCheckbox.setChecked(LogIt.isLoggingOn());

        saveLogsCheckbox
                .setOnCheckedChangeListener(new LoggingCheckedChangeListener());

        getHelp.setOnClickListener(new GetHelpClickListener());

        about.setOnClickListener(new AboutClickListener());

        editProfile.setOnClickListener(new EditProfileClickListener());

        alertBtn.setOnClickListener(new SettingsClickListener());

        changePassword.setOnClickListener(new ChangePasswordClickListener());

        logout.setOnClickListener(new LogoutClickListener());

        setAppVersion();

        if (AlertSetting.hasAlertBlock(currentUser.getContactId())) {

            alertPeriod.setText(AlertSetting
                    .getEndDateBlockingAlert(AlertSetting
                            .getUserAlertBlock(currentUser.getContactId())));
        }

    }

    /**
     * Gets the app version name from the Manifest file.
     */
    private void setAppVersion() {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(
                    getPackageName(), 0);
            version.setText(pi.versionName + " (" + pi.versionCode + ")");
        } catch (NameNotFoundException e) {
            LogIt.e(this, e.getMessage());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private class GetHelpClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            LogIt.d(this, "Clicked Get Help");
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    SettingsActivity.this);

            builder.setTitle(R.string.get_help);
            builder.setItems(R.array.get_help_options,
                    new GetHelpDialogClickListener());

            builder.create().show();
        }
    }

    private class GetHelpDialogClickListener implements
            DialogInterface.OnClickListener {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case 0:
                    openWebViewURL(MessageMeConstants.EXTRA_FAQ_CODE);
                    break;
                case 1:
                    openWebViewURL(MessageMeConstants.EXTRA_SERVICE_STATUS_CODE);
                    break;
            }

            dialog.dismiss();
        }
    }

    private void openWebViewURL(int code) {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(MessageMeConstants.EXTRA_URL, code);
        startActivity(intent);
    }

    private void displayComingSoonMessage() {
        LogIt.user(this, "Get Help option pressed");
        UIUtil.centeredAlert(this, getString(R.string.get_help),
                getString(R.string.coming_soon));
    }

    /**
     * CLickListener that displays the dialog for the About MM button
     * 
     */
    private class AboutClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            LogIt.d(this, "Clicked About MM");
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    SettingsActivity.this);

            builder.setTitle(R.string.about);
            builder.setItems(R.array.about_mm_options,
                    new AboutDialogClickListener());

            builder.create().show();
        }
    }

    /**
     * Clicklistener when an options has been selected
     * on the about dialog
     * 
     */
    private class AboutDialogClickListener implements
            DialogInterface.OnClickListener {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case 0:
                    openWebViewURL(MessageMeConstants.EXTRA_TERMS_OF_SERVICE_CODE);
                    break;
                case 1:
                    openWebViewURL(MessageMeConstants.EXTRA_PRIVACY_POLICY_CODE);
                    break;
            }

            dialog.dismiss();
        }
    }

    /**
     * ClickListener to display the edit user information dialog
     * 
     */
    private class EditProfileClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            LogIt.d(this, "Clicked on open Edit Profile dialog");
            EditUserProfile editUserProfile = new EditUserProfile();
            editUserProfile.createEditProfileDialog(SettingsActivity.this);
        }

    }

    /**
     * ClickListener to display the Alert options
     * 
     */
    private class SettingsClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {

            AlertDialog.Builder builder = new AlertDialog.Builder(
                    SettingsActivity.this);

            // Set the current user ID as master alert blocker
            builder.setTitle(R.string.alerts_dialog_title);
            builder.setItems(R.array.alerts_dialog_options,
                    new AlertPickClickListener(currentUser.getContactId(),
                            SettingsActivity.this, alertPeriod));

            builder.create().show();

        }
    }

    private class ChangePasswordClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            displayComingSoonMessage();
        }

    }

    private class SoundAlertCheckedChangeListener implements
            OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                boolean isChecked) {

            if (isChecked) {
                toast(R.string.sound_alerts_on);
                MessageMeApplication.getPreferences().setSoundAlert(true);
            } else {
                toast(R.string.sound_alerts_off);
                MessageMeApplication.getPreferences().setSoundAlert(false);
            }
        }
    }

    private class VibrateAlertCheckedListener implements
            OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                boolean isChecked) {
            if (isChecked) {
                toast(R.string.vibrate_alerts_on);
                MessageMeApplication.getPreferences().setVibrateAlert(true);
            } else {
                toast(R.string.vibrate_alerts_off);
                MessageMeApplication.getPreferences().setVibrateAlert(false);
            }
        }
    }

    private class LogoutClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {

            AlertDialog.Builder builder = new AlertDialog.Builder(
                    SettingsActivity.this);
            builder.setMessage(getString(R.string.logout_message))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.yes_lbl),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    LogIt.user(SettingsActivity.this,
                                            "Log Out pressed");
                                    LogOutUtil.logoutUser(
                                            SettingsActivity.this,
                                            mMessagingServiceRef);
                                }
                            })
                    .setNegativeButton(getString(R.string.no_lbl),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    dialog.cancel();
                                }
                            });

            builder.create().show();
        }
    }

    private class SendLogsClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            LogIt.user(SettingsActivity.class, "Send logs pressed");

            StringBuilder body = new StringBuilder();
            body.append(getString(R.string.diagnostics_email_body));
            body.append("\n\n");
            body.append(MessageMeApplication.getUserAgent());
            body.append("\n\n");
            body.append(LogIt.getBufferedLogs());

            try {
                // This fails with a TransactionTooLargeException if the log
                // file is too large. Unfortunately we can't catch that
                // exception as it occurs in the ActivityManager instead of
                // our code.
                MailUtil.sendEmail(SettingsActivity.this,
                        getString(R.string.diagnostics_email_recipient), null,
                        getString(R.string.diagnostics_email_subject),
                        body.toString(),
                        getString(R.string.diagnostics_email_reason));
            } catch (Exception e) {
                LogIt.e(SettingsActivity.class, e, "Exception launching email");
                toast(R.string.send_logs_failed);
            }
        }
    }

    private class LoggingCheckedChangeListener implements
            OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                boolean isChecked) {
            if (isChecked) {
                LogIt.user(SettingsActivity.class, "Save logs checked");
                toast(R.string.logging_is_on);
                LogIt.setLoggingOn(true);
            } else {
                LogIt.user(SettingsActivity.class, "Save logs unchecked");
                toast(R.string.logging_is_off);
                LogIt.setLoggingOn(false);
            }
        }
    }
}