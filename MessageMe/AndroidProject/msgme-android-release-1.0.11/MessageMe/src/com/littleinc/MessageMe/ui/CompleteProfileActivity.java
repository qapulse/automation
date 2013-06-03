package com.littleinc.MessageMe.ui;

import java.io.IOException;
import java.util.Arrays;

import org.restlet.resource.ResourceException;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.coredroid.util.BackgroundTask;
import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.facebook.Request;
import com.facebook.RequestAsyncTask;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionDefaultAudience;
import com.facebook.SessionLoginBehavior;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.actionbar.ActionBarNoServiceActivity;
import com.littleinc.MessageMe.metrics.MMFunnelTracker;
import com.littleinc.MessageMe.metrics.MMTracker;
import com.littleinc.MessageMe.net.RestfulClient;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.util.DeviceUtil;
import com.littleinc.MessageMe.util.MMApiUtils;
import com.littleinc.MessageMe.util.NetUtil;
import com.littleinc.MessageMe.util.StringUtil;

@TargetApi(14)
public class CompleteProfileActivity extends ActionBarNoServiceActivity {

    private String phoneSignature;

    private String normalizedPhoneNumber;

    private Button submitButton;

    private EditText firstNameText;

    private EditText lastNameText;

    private EditText emailAddressText;

    private EditText passwordText;

    private boolean isCallCapable;

    private GraphUser fbUser;

    private RequestAsyncTask mFBUserIfoRequestTask;

    private static final String FB_CHECK_ERROR = "FB_CHECK_ERROR";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // tracking
        String subtopic = (DeviceUtil.canPlacePhoneCall(this.getBaseContext())) ? "phone"
                : "email";
        MMFunnelTracker.getInstance().abacusOnceFunnel(
                MMFunnelTracker.SIGNUP_FUNNEL,
                MessageMeConstants.ONE_WEEK_SECS, "profile", subtopic, null,
                null);

        setContentView(R.layout.complete_profile_layout);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            ActionBar actionBar = getActionBar();
            actionBar.setHomeButtonEnabled(true);
            actionBar.setIcon(R.drawable.actionbar_top_icon_back);
        }

        firstNameText = (EditText) findViewById(R.id.first_name);

        lastNameText = (EditText) findViewById(R.id.last_name);

        emailAddressText = (EditText) findViewById(R.id.email_address);

        passwordText = (EditText) findViewById(R.id.password);

        isCallCapable = getIntent().getBooleanExtra(
                MessageMeConstants.EXTRA_IS_CALL_CAPABLE, false);

        if (isCallCapable) {

            if (getIntent().hasExtra(MessageMeConstants.EXTRA_PHONE_SIGNATURE)) {
                phoneSignature = getIntent().getStringExtra(
                        MessageMeConstants.EXTRA_PHONE_SIGNATURE);

                normalizedPhoneNumber = getIntent().getStringExtra(
                        MessageMeConstants.EXTRA_PHONE_NUMBER);
            } else {
                phoneSignature = MessageMeApplication.getPreferences()
                        .getPhoneSignature();
                normalizedPhoneNumber = MessageMeApplication.getPreferences()
                        .getRegisteredPhoneNumber();
            }

        }

        submitButton = (Button) findViewById(R.id.submit_btn);
        submitButton.setOnClickListener(new SubmitButtonClickListener());

        passwordText.setOnEditorActionListener(new DoneKeyListener());

        Session session = Session.getActiveSession();
        if (session == null) {
            if (savedInstanceState != null) {
                session = Session.restoreSession(this, null, null,
                        savedInstanceState);
            }
            if (session == null) {
                session = new Session(this);
            }
            Session.setActiveSession(session);
            if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED)) {
                session.openForRead(setFBPermissions());
            }
        }

        String possibleUserEmail = DeviceUtil.getDevicePrimaryEmail(this);

        if (!StringUtil.isEmpty(possibleUserEmail)) {
            LogIt.d(this, "Found user email address:", possibleUserEmail);
            emailAddressText.setText(possibleUserEmail);
        }

        createFacebookConfirmationDialog();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.login_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void startLandingPageActivity() {
        Intent intent = new Intent(this, LandingActivity.class);

        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        finish();
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            if (!isCallCapable) {
                startLandingPageActivity();
            }
            break;
        case R.id.next_btn:
            UIUtil.hideKeyboard(firstNameText);
            LogIt.user(this, "Pressed Next button");

            String firstName = firstNameText.getText().toString();
            String lastName = lastNameText.getText().toString();
            String email = emailAddressText.getText().toString();
            String password = passwordText.getText().toString();

            MMApiUtils
                    .completeUserRegistration(CompleteProfileActivity.this,
                            firstName, lastName, email, password,
                            normalizedPhoneNumber, phoneSignature,
                            isCallCapable, false);

            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        cancelUserRequestTask();

        if (!isCallCapable) {
            startLandingPageActivity();
        }
    }

    @Override
    public void finish() {
        cancelUserRequestTask();
        super.finish();
    }

    @Override
    protected void onDestroy() {
        cancelUserRequestTask();
        super.onDestroy();
    }

    /**
     * Interrupts a FB user info request if its running
     */
    private void cancelUserRequestTask() {
        if (mFBUserIfoRequestTask != null) {
            LogIt.d(CompleteProfileActivity.class,
                    "Canceling FB user info request");
            mFBUserIfoRequestTask.cancel(true);
        }
    }

    private class DoneKeyListener implements EditText.OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                UIUtil.hideKeyboard(v);
                LogIt.user(this, "User pressed Done key");

                String firstName = firstNameText.getText().toString();
                String lastName = lastNameText.getText().toString();
                String email = emailAddressText.getText().toString();
                String password = passwordText.getText().toString();

                MMApiUtils.completeUserRegistration(
                        CompleteProfileActivity.this, firstName, lastName,
                        email, password, normalizedPhoneNumber, phoneSignature,
                        isCallCapable, false);

                return true;
            }
            return false;
        }
    }

    private class SubmitButtonClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            UIUtil.hideKeyboard(v);
            LogIt.user(this, "Complete Profile submit button pressed");

            String firstName = firstNameText.getText().toString();
            String lastName = lastNameText.getText().toString();
            String email = emailAddressText.getText().toString();
            String password = passwordText.getText().toString();

            MMApiUtils
                    .completeUserRegistration(CompleteProfileActivity.this,
                            firstName, lastName, email, password,
                            normalizedPhoneNumber, phoneSignature,
                            isCallCapable, false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Session.getActiveSession().onActivityResult(this, requestCode,
                resultCode, data);

        getFacebookUserInformation();

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Creates Facebook confirmation dialog
     */
    private void createFacebookConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(getString(R.string.complete_profile_fb))
                .setTitle(R.string.connect_with_fb)
                .setCancelable(true)
                .setPositiveButton(getString(R.string.yes_lbl),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                LogIt.user(CompleteProfileActivity.class,
                                        "Pressed Yes - connect with FB");

                                // Tracking
                                MMTracker.getInstance().abacus("signup",
                                        "accept", "fb_auth", null, null);

                                Session session = Session.getActiveSession();
                                // Session.getActiveSession() can return null, so we need validation for it
                                // http://developers.facebook.com/docs/reference/android/3.0/Session#getActiveSession()
                                if (session != null && !session.isOpened()
                                        && !session.isClosed()) {
                                    session.openForRead(setFBPermissions());
                                } else {
                                    getFacebookUserInformation();
                                }
                            }
                        })
                .setNegativeButton(getString(R.string.no_lbl),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                LogIt.user(CompleteProfileActivity.class,
                                        "Pressed No - don't connect with FB");

                                // Tracking
                                String subtopic = DeviceUtil
                                        .canPlacePhoneCall(getBaseContext()) ? "phone"
                                        : "email";
                                MMFunnelTracker.getInstance().abacusOnceFunnel(
                                        MMFunnelTracker.SIGNUP_FUNNEL,
                                        MessageMeConstants.ONE_WEEK_SECS,
                                        "nofb", subtopic, null, null);
                                MMTracker.getInstance().abacusOnce("signup",
                                        "deny", "fb_auth", null, null);

                                dialog.cancel();
                            }
                        });

        builder.create().show();
    }

    /**
     * Adds custom permissions to the Facebook authentication
     * Email and User location
     */
    private Session.OpenRequest setFBPermissions() {
        Session.OpenRequest openRequest = new Session.OpenRequest(
                CompleteProfileActivity.this);

        openRequest.setDefaultAudience(SessionDefaultAudience.ONLY_ME);
        openRequest.setPermissions(Arrays.asList("email", "user_location"));
        openRequest.setLoginBehavior(SessionLoginBehavior.SSO_WITH_FALLBACK);

        return openRequest;

    }

    private void showAlertForLoginOrSignup() {

        String title = String.format(
                getString(R.string.facebook_auth_collision),
                fbUser.getFirstName());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(title)

                .setCancelable(true)
                .setPositiveButton(getString(R.string.log_in_label),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent intent = new Intent(
                                        CompleteProfileActivity.this,
                                        LogInActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        })
                .setNegativeButton(getString(R.string.signup),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                firstNameText.setText(fbUser.getFirstName());
                                lastNameText.setText(fbUser.getLastName());
                                emailAddressText.setText((String) fbUser
                                        .asMap().get("email"));
                            }
                        });

        builder.create().show();
    }

    private void getFacebookUserInformation() {
        if (Session.getActiveSession() != null
                && Session.getActiveSession().getState()
                        .equals(SessionState.OPENED)) {

            mFBUserIfoRequestTask = Request.executeMeRequestAsync(
                    Session.getActiveSession(),
                    new Request.GraphUserCallback() {
                        // callback after Graph API response with user object
                        @Override
                        public void onCompleted(GraphUser user,
                                Response response) {

                            if (user != null) {
                                fbUser = user;

                                // It seems that the cancel() of mFBUserIfoRequestTask is not 100%
                                // reliable so we going to check also if the Activity is finishing
                                // before call to check the FB tokens
                                if (!isFinishing()) {
                                    checkFacebookToken();
                                } else {
                                    LogIt.w(CompleteProfileActivity.class,
                                            "Activity is in process of be destroyed ignore FB check");
                                }
                            } else {
                                LogIt.e(this,
                                        "Error getting Facebook user information");
                            }
                        }
                    });
        } else {
            LogIt.d(this, "Clearing FB session");
            Session.setActiveSession(null);
        }
    }

    /**
     * Checks if the Facebook Authorization token
     * is already assigned to a MM account
     */
    private void checkFacebookToken() {

        final ProgressDialog progressDialog = showProgressDialog(getString(R.string.loading));

        new BackgroundTask() {

            @Override
            public void work() {
                if (NetUtil
                        .checkInternetConnection(CompleteProfileActivity.this)) {

                    try {
                        PBCommandEnvelope envelope = RestfulClient
                                .getInstance().facebookCheck(
                                        Session.getActiveSession()
                                                .getAccessToken());

                        if (envelope.hasError()) {
                            // Track the error
                            MMTracker.getInstance().abacus("signup",
                                    "pb_error", "fb", null,
                                    envelope.getError().getCode().getNumber());

                            LogIt.w(CompleteProfileActivity.class,
                                    "User FB signup failed", envelope
                                            .getError().getCode(), envelope
                                            .getError().getReason());

                            fail(FB_CHECK_ERROR);
                        }
                    } catch (ResourceException e) {
                        MMTracker.getInstance().abacus("signup", "error", "fb",
                                null, e.getStatus().getCode());
                        LogIt.e(CompleteProfileActivity.this, e, e.getMessage());
                        fail(getString(R.string.auth_error),
                                getString(R.string.network_error));
                    } catch (IOException e) {
                        MMTracker.getInstance().abacus("signup", "error", "fb",
                                null, null);
                        LogIt.e(CompleteProfileActivity.this, e, e.getMessage());
                        fail(getString(R.string.auth_error),
                                getString(R.string.network_error));
                    } catch (Exception e) {
                        MMTracker.getInstance().abacus("signup", "error_u",
                                "fb", null, null);
                        LogIt.e(CompleteProfileActivity.this, e, e.getMessage());
                        fail(getString(R.string.unexpected_error_title),
                                getString(R.string.unexpected_error));
                    }
                } else {
                    fail(getString(R.string.auth_error),
                            getString(R.string.network_error));
                }
            }

            @Override
            public void done() {

                progressDialog.dismiss();

                if (!failed()) {
                    // track FB auth success
                    MMTracker.getInstance().abacus("signup", "authed", "fb",
                            null, null);
                    String subtopic = DeviceUtil
                            .canPlacePhoneCall(getBaseContext()) ? "phone"
                            : "email";
                    MMFunnelTracker.getInstance().abacusOnceFunnel(
                            MMFunnelTracker.SIGNUP_FUNNEL,
                            MessageMeConstants.ONE_WEEK_SECS, "fb", subtopic,
                            null, null);

                    Intent intent = new Intent(CompleteProfileActivity.this,
                            CreateLoginActivity.class);

                    intent.putExtra(MessageMeConstants.EXTRA_EMAIL,
                            (String) fbUser.asMap().get("email"));
                    intent.putExtra(MessageMeConstants.EXTRA_FIRST_NAME,
                            fbUser.getFirstName());
                    intent.putExtra(MessageMeConstants.EXTRA_LAST_NAME,
                            fbUser.getLastName());
                    intent.putExtra(MessageMeConstants.EXTRA_IS_CALL_CAPABLE,
                            isCallCapable);
                    intent.putExtra(MessageMeConstants.EXTRA_PHONE_SIGNATURE,
                            phoneSignature);
                    intent.putExtra(MessageMeConstants.EXTRA_PHONE_NUMBER,
                            normalizedPhoneNumber);

                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    startActivity(intent);
                    finish();
                } else if (getExceptionMessage().equals(FB_CHECK_ERROR)) {
                    // track that fb account is already claimed
                    MMTracker.getInstance().abacus("signup", "exists", "fb",
                            null, null);

                    showAlertForLoginOrSignup();
                } else {
                    LogIt.e(this, "Facebook Sign Up error");

                    alert(getExceptionTitle(), getExceptionMessage());
                }
            }
        };
    }
}