package com.littleinc.MessageMe.ui;

import java.io.IOException;
import java.util.List;

import org.messageMe.OpenUDID.OpenUDID_manager;
import org.restlet.resource.ResourceException;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.coredroid.ui.CoreActivity;
import com.coredroid.util.BackgroundTask;
import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.facebook.LoginActivity;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.MessageMeAppPreferences;
import com.littleinc.MessageMe.bo.Room;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.data.MMLocalData;
import com.littleinc.MessageMe.metrics.MMFunnelTracker;
import com.littleinc.MessageMe.net.RestfulClient;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandUserLogin;
import com.littleinc.MessageMe.protocol.Objects.PBError;
import com.littleinc.MessageMe.protocol.Objects.PBError.ErrorType;
import com.littleinc.MessageMe.protocol.Objects.PBRoom;
import com.littleinc.MessageMe.protocol.Objects.PBUser;
import com.littleinc.MessageMe.util.DatabaseTask;
import com.littleinc.MessageMe.util.DeviceUtil;
import com.littleinc.MessageMe.util.NetUtil;
import com.littleinc.MessageMe.util.StringUtil;

public class LogInActivity extends CoreActivity {

    private Button logInButton;

    private EditText emailText;

    private EditText passwordTextView;

    private TextView forgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // tracking
        MMFunnelTracker.getInstance().abacusOnceFunnel(
                MMFunnelTracker.LOGIN_FUNNEL, MessageMeConstants.ONE_WEEK_SECS,
                "show", null, null, null);

        logInButton = (Button) findViewById(R.id.log_in_button);
        emailText = (EditText) findViewById(R.id.email_address);
        passwordTextView = (EditText) findViewById(R.id.password);
        forgotPassword = (TextView) findViewById(R.id.forgot_password);

        MessageMeAppPreferences preferences = MessageMeApplication
                .getPreferences();

        // Pre-fill the Email/Phone input with the info of the last logged user
        // if is present in the app preferences
        if (!TextUtils.isEmpty(preferences.getLastLoginID())) {

            if (StringUtil.isEmailValid(preferences.getLastLoginID())) {
                emailText.setText(preferences.getLastLoginID());
            } else {
                String countryCode = DeviceUtil.getSimCardCountryCode(this);
                PhoneNumberUtil numberUtil = PhoneNumberUtil.getInstance();

                try {
                    PhoneNumber phoneNumber = numberUtil.parse(
                            preferences.getLastLoginID(), countryCode);

                    emailText.setText(numberUtil.format(phoneNumber,
                            PhoneNumberFormat.E164));
                } catch (NumberParseException e) {
                    LogIt.w(LoginActivity.class, "Unable to format number",
                            preferences.getRegisteredPhoneNumber(), countryCode);
                }
            }
        }

        if (!TextUtils.isEmpty(emailText.getText().toString())) {
            emailText.selectAll();
        }

        logInButton.setOnClickListener(logInButtonClickListener);
        forgotPassword.setOnClickListener(new ForgotPasswordClickListener());
    }

    @Override
    protected int getContentViewId() {
        return R.layout.log_in;
    }

    private View.OnClickListener logInButtonClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            UIUtil.hideKeyboard(view);
            LogIt.user(this, "Log in button pressed");

            // TODO refactor variable to "emailOrPhone"
            final String email = emailText.getText().toString();
            final String password = passwordTextView.getText().toString();
            final int countryCode;

            // If the emailOrPhone looks like an email then do an email login,
            // otherwise try a phone login
            final boolean doEmailLogin = isEmailAddress(email);

            if (doEmailLogin) {
                countryCode = -1;
            } else {
                String countryInitials = DeviceUtil.getSimCardCountryCode(view
                        .getContext());

                if (StringUtil.isEmpty(countryInitials)) {
                    countryCode = -1;
                } else {
                    countryCode = PhoneNumberUtil.getInstance()
                            .getCountryCodeForRegion(countryInitials);
                }
            }

            if (!NetUtil.checkInternetConnection(view.getContext())) {
                LogIt.w(this, "No connection at login");
                alert(R.string.network_error_title, R.string.network_error);
            } else if (!StringUtil.isValid(email)
                    || !StringUtil.isValid(password)) {
                LogIt.w(this, "Invalid required fields");
                alert(R.string.required_fields_alert_title,
                        R.string.required_fields_alert_body);
            } else if (doEmailLogin && !StringUtil.isEmailValid(email)) {
                LogIt.w(this, "Invalid email field");
                alert(R.string.required_fields_alert_title,
                        R.string.invalid_email);
            } else {
                LogIt.i(this, "Starting login process", email);
                final ProgressDialog progressDialog = showProgressDialog(R.string.auth_progress_message);

                new BackgroundTask() {

                    User currentUser;

                    PBCommandUserLogin commandUserLogin;

                    MessageMeAppPreferences appPreferences = MessageMeApplication
                            .getPreferences();

                    @Override
                    public void work() {
                        try {
                            PBCommandEnvelope envelope;
                            if (doEmailLogin) {
                                envelope = RestfulClient.getInstance()
                                        .userLoginWithEmail(email,
                                                OpenUDID_manager.getOpenUDID(),
                                                Build.MODEL, password);
                            } else {
                                envelope = RestfulClient.getInstance()
                                        .userLoginWithPhone(email, countryCode,
                                                OpenUDID_manager.getOpenUDID(),
                                                Build.MODEL, password);
                            }

                            if (!envelope.hasError()) {
                                commandUserLogin = envelope.getUserLogin();

                                // If you configure an incorrect REST endpoint
                                // that returns HTTP success (e.g. if your DNS
                                // redirects you to a webhosting provider), then
                                // the PBCommandEnvelope might parse, and
                                // hasError() will return false. In this case
                                // the current user is 0, which tells us
                                // something is wrong!
                                currentUser = User.parseFrom(commandUserLogin
                                        .getUser());

                                // Always get the email from the login envelope
                                // as the user may have logged in with a phone
                                // number instead of an email
                                if (commandUserLogin.getVerifiedEmailsCount() > 0) {
                                    currentUser.setEmail(commandUserLogin
                                            .getVerifiedEmails(0));
                                } else if (commandUserLogin
                                        .getUnverifiedEmailsCount() > 0) {
                                    currentUser.setEmail(commandUserLogin
                                            .getUnverifiedEmails(0));
                                } else {
                                    LogIt.e(LogInActivity.class,
                                            "No email in user login envelope",
                                            email);
                                    currentUser.setEmail(email);
                                }

                                if (currentUser.getUserId() == 0) {
                                    LogIt.e(LogInActivity.class,
                                            "Current user is 0, something has gone wrong!",
                                            envelope);
                                    fail(getString(R.string.auth_error),
                                            getString(R.string.unexpected_error));
                                    return;
                                }

                                if (commandUserLogin.getPhonesCount() > 0) {
                                    String registeredPhone = commandUserLogin
                                            .getPhones(0);
                                    LogIt.d(LogInActivity.class,
                                            "Registered phone",
                                            registeredPhone,
                                            commandUserLogin.getPhonesCount());

                                    appPreferences
                                            .setRegisteredPhoneNumber(registeredPhone);
                                    currentUser.setPhone(registeredPhone);
                                } else {

                                    // If user doesn't have any registered phone
                                    // number we manually
                                    // setRegisteredPhoneNumber as empty
                                    appPreferences.setRegisteredPhoneNumber("");
                                }

                                LogIt.i(LogInActivity.this, "Processing Log in");

                                appPreferences.setUser(currentUser);
                                appPreferences.setToken(commandUserLogin
                                        .getAuthToken());
                                appPreferences
                                        .setUserGetLastModified(commandUserLogin
                                                .getTimestamp());
                            } else {
                                PBError pbErr = envelope.getError();

                                // The reason field usually seems to be blank on
                                // a USER_LOGIN_NOT_FOUND error
                                LogIt.w(LogInActivity.class,
                                        "User login failed", email, envelope
                                                .getError().getCode(), envelope
                                                .getError().getReason());

                                if ((pbErr.getCode() == ErrorType.NOT_FOUND)
                                        || (pbErr.getCode() == ErrorType.INVALID_PHONE)) {
                                    if (doEmailLogin) {
                                        fail(getString(R.string.login_incorrect_email),
                                                getString(R.string.login_incorrect_email_message));
                                    } else {
                                        fail(getString(R.string.login_incorrect_phone),
                                                getString(R.string.login_incorrect_phone_message));
                                    }
                                } else if (pbErr.getCode() == ErrorType.INVALID_EMAIL) {
                                    fail(getString(R.string.signup_email_auth_error_email_title),
                                            getString(R.string.signup_email_auth_error_email_message));
                                } else {
                                    // This includes ErrorType.UNAUTHORIZED
                                    if (doEmailLogin) {
                                        fail(getString(R.string.auth_error),
                                                getString(R.string.login_error));
                                    } else {
                                        fail(getString(R.string.login_invalid_phone_title),
                                                getString(R.string.login_invalid_phone_body));
                                    }
                                }
                            }
                        } catch (IOException e) {
                            LogIt.e(LogInActivity.this, e, e.getMessage());
                            fail(getString(R.string.auth_error),
                                    getString(R.string.network_error));
                        } catch (ResourceException e) {
                            LogIt.e(LogInActivity.this, e, e.getMessage());
                            fail(getString(R.string.auth_error),
                                    getString(R.string.network_error));
                        } catch (Exception e) {
                            LogIt.e(LogInActivity.this, e, e.getMessage());
                            fail(getString(R.string.auth_error),
                                    getString(R.string.unexpected_error));
                        }
                    }

                    @Override
                    public void done() {
                        if (failed()) {
                            progressDialog.dismiss();
                            LogIt.w(LogInActivity.this, "Log in failed");

                            if (appPreferences != null) {
                                // Cleaning currentUser info if the login fails
                                appPreferences.setUser(null);
                            }

                            alert(getExceptionTitle(), getExceptionMessage());
                        } else {
                            new DatabaseTask() {

                                @Override
                                public void work() {
                                    processSuccessLogIn(currentUser,
                                            commandUserLogin);
                                }

                                @Override
                                public void done() {

                                    LogIt.i(LogInActivity.this,
                                            "Log in succeeded");

                                    if (appPreferences.dbNeedUpgrade()) {
                                        LogIt.i(LogInActivity.this,
                                                "Database upgraded, setting DbNeedUpgrade false");
                                        appPreferences.setDbNeedUpgrade(false);
                                    }

                                    // Tick session and save login date
                                    // we tick twice to prevent first session
                                    // tracking
                                    // from happening on login
                                    MMLocalData.getInstance().tickSession();
                                    MMLocalData.getInstance().tickSession();
                                    MMLocalData
                                            .getInstance()
                                            .setLoginDate(
                                                    (int) System
                                                            .currentTimeMillis() / 1000);

                                    startService(new Intent(LogInActivity.this,
                                            MessagingService.class));

                                    Intent intent = new Intent(
                                            LogInActivity.this,
                                            TabsFragmentActivity.class);

                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            | Intent.FLAG_ACTIVITY_NEW_TASK);

                                    progressDialog.dismiss();
                                    startActivity(intent);
                                    finish();
                                }
                            };
                        }
                    }
                };
            }
        }
    };

    private void processSuccessLogIn(User currentUser,
            PBCommandUserLogin commandUserLogin) {

        currentUser.save();

        if (commandUserLogin.hasLastCommandID()) {
            currentUser
                    .createCursor(commandUserLogin.getLastCommandID(), false);
        } else {
            currentUser.createCursor(false);
        }

        roomCreation(commandUserLogin.getRoomsList());
        friendCreation(commandUserLogin.getContactsList());
    }

    /**
     * Create all friends
     * 
     * @param contacts Represents the entire set of contacts for the
     * authenticated user
     */
    private void friendCreation(final List<PBUser> contacts) {
        for (PBUser pbUser : contacts) {
            User user = User.parseFrom(pbUser);
            user.setShown(true);

            LogIt.d(this, "Adding friend", user.getUserId(),
                    user.getDisplayName());
            user.save();
        }
    }

    /**
     * Create Rooms & Conversations
     */
    private void roomCreation(final List<PBRoom> rooms) {
        for (PBRoom pbRoom : rooms) {
            if (pbRoom.getRoomID() != 0) {
                // These are normal room/group chats
                Room room = Room.parseFrom(pbRoom);
                Room.createRoomUsers(pbRoom);

                LogIt.d(LoginActivity.class, "Adding room", pbRoom.getRoomID(),
                        pbRoom.getName());

                room.save();
                room.createCursor(false);
                Conversation.newInstance(room.getRoomId()).save();
            } else {
                // These are private chats
                User currentUser = MessageMeApplication.getCurrentUser();

                // Every private chat includes a list of
                // users, which is the current user, plus
                // the person they are talking to.
                for (PBUser pbUser : pbRoom.getUsersList()) {
                    if (currentUser.getUserId() != pbUser.getUserID()) {
                        User user = User.parseFrom(pbUser);

                        // Add the User if they aren't already in the database
                        if (!User.exists(pbUser.getUserID())) {
                            LogIt.d(LoginActivity.class, "Adding user",
                                    user.getUserId(), user.getDisplayName());
                            user.save();
                        }

                        user.createCursor(false);
                        Conversation.newInstance(user.getUserId()).save();
                    }
                }
            }
        }
    }

    /**
     * @return true if the input string appears to be an email. This is a
     * rough approximation, using the same rules as iOS.
     */
    public static boolean isEmailAddress(String emailOrPhone) {
        return !StringUtil.isEmpty(emailOrPhone) && emailOrPhone.contains("@");
    }

    private class ForgotPasswordClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            createResetPasswordDialog();
        }

    }

    /**
     * Creates a dialog for password reset
     */
    private void createResetPasswordDialog() {

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText emailInput = new EditText(this);
        emailInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.setSingleLine(true);
        emailInput.setHint(getString(R.string.email_hint));

        // If the user has already entered an email address then prefill
        // the popup with it
        if (emailText.length() > 0) {
            String emailOrPhone = emailText.getText().toString();

            // Only do this if the field looks like an email
            if (isEmailAddress(emailOrPhone)) {
                emailInput.setText(emailOrPhone);
            }
        }

        alert.setView(emailInput);
        alert.setTitle(R.string.login_forgot_password);
        alert.setMessage(R.string.login_forgot_enter_email);
        alert.setPositiveButton(getString(R.string.reset),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String email = emailInput.getText().toString().trim();
                        resetPassword(email);
                    }
                });

        alert.setNegativeButton(getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                });
        alert.show();

    }

    private void unregisteredEmailDialog(String email) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.login_account_not_found);
        alert.setMessage(String.format(
                getString(R.string.login_account_not_found_message), email));
        alert.setPositiveButton(getString(R.string.signup),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        startActivity(DeviceUtil
                                .checkSimState(LogInActivity.this));
                        finish();
                    }
                });

        alert.setNegativeButton(getString(R.string.retry),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                });
        alert.show();

    }

    private void resetPassword(final String email) {
        if (!NetUtil.checkInternetConnection(LogInActivity.this)) {
            LogIt.w(this, "No connection at forgot password");
            alert(R.string.network_error_title, R.string.network_error);
        } else if (!StringUtil.isEmailValid(email.toString())) {
            LogIt.w(this, "Invalid email field");
            alert(R.string.required_fields_alert_title, R.string.invalid_email);
        } else {
            LogIt.d(this, "Starting forgot password process");
            final ProgressDialog progressDialog = showProgressDialog(R.string.loading);

            new BackgroundTask() {

                ErrorType error;

                @Override
                public void work() {
                    String userEmail = email;

                    try {
                        PBCommandEnvelope envelope = RestfulClient
                                .getInstance().forgotPassword(userEmail);

                        if (envelope.hasError()) {

                            PBError pbError = envelope.getError();

                            error = pbError.getCode();

                            fail(getString(R.string.login_forgot_password),
                                    getString(R.string.login_contact_support_message));
                        }
                    } catch (IOException e) {
                        LogIt.e(LogInActivity.this, e, e.getMessage());
                        fail(getString(R.string.network_error),
                                getString(R.string.network_error));
                    } catch (ResourceException e) {
                        LogIt.e(LogInActivity.this, e, e.getMessage());
                        fail(getString(R.string.network_error),
                                getString(R.string.network_error));
                    } catch (Exception e) {
                        LogIt.e(LogInActivity.class, e, e.getMessage());
                        fail(getString(R.string.auth_error),
                                getString(R.string.unexpected_error));
                    }
                }

                @Override
                public void done() {
                    progressDialog.dismiss();
                    if (!failed()) {
                        LogIt.d(this,
                                "Sent email with instructions to reset password");
                        alert(R.string.login_forgot_password,
                                R.string.login_password_reset_message);
                    } else {
                        if (error == ErrorType.NOT_FOUND) {
                            LogIt.d(this, "Email is not registered");
                            unregisteredEmailDialog(email);
                        } else {
                            LogIt.w(this, "Forgot password failed");
                            alert(getExceptionTitle(), getExceptionMessage());
                        }
                    }
                }
            };
        }
    }
}