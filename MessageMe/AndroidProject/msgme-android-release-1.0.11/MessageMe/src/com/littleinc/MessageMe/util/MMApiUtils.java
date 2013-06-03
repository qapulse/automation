package com.littleinc.MessageMe.util;

import java.io.IOException;

import org.messageMe.OpenUDID.OpenUDID_manager;
import org.restlet.resource.ResourceException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.coredroid.util.BackgroundTask;
import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.facebook.Session;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.bo.MessageMeAppPreferences;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.data.MMLocalData;
import com.littleinc.MessageMe.metrics.MMFunnelTracker;
import com.littleinc.MessageMe.metrics.MMTracker;
import com.littleinc.MessageMe.net.RestfulClient;
import com.littleinc.MessageMe.protocol.Commands.PBCommandAccountFacebookClaim;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandUserSignup;
import com.littleinc.MessageMe.protocol.Objects.PBError;
import com.littleinc.MessageMe.protocol.Objects.PBError.ErrorType;
import com.littleinc.MessageMe.ui.TabsFragmentActivity;

public class MMApiUtils {

    /**
     * Creates sends user information into the server
     * for an account creation
     */
    public static void completeUserRegistration(final Context context,
            final String firstName, final String lastName, final String email,
            final String password, final String normalizedPhoneNumber,
            final String phoneSignature, final boolean isCallCapable,
            final boolean isClaimFBaccount) {

        if (StringUtil.isValid(firstName) && StringUtil.isValid(lastName)
                && StringUtil.isValid(email) && StringUtil.isValid(password)) {

            if (StringUtil.isEmailValid(email)) {
                final ProgressDialog progressDialog = UIUtil
                        .showProgressDialog(context, context
                                .getString(R.string.auth_progress_message));

                new BackgroundTask() {

                    @Override
                    public void work() {
                        if (NetUtil.checkInternetConnection(context)) {

                            PBCommandEnvelope envelope = null;

                            try {

                                if (isCallCapable) {
                                    envelope = RestfulClient.getInstance()
                                            .userSignUp(
                                                    firstName,
                                                    lastName,
                                                    email,
                                                    OpenUDID_manager
                                                            .getOpenUDID(),
                                                    Build.MODEL, password,
                                                    normalizedPhoneNumber,
                                                    phoneSignature);
                                } else {
                                    envelope = RestfulClient.getInstance()
                                            .userSignUp(
                                                    firstName,
                                                    lastName,
                                                    email,
                                                    OpenUDID_manager
                                                            .getOpenUDID(),
                                                    Build.MODEL, password,
                                                    null, null);
                                }

                                if (!envelope.hasError()) {
                                    PBCommandUserSignup userSignUp = envelope
                                            .getUserSignup();

                                    // If you configure an incorrect REST
                                    // endpoint that returns HTTP success (e.g.
                                    // if your DNS redirects you to a webhosting
                                    // provider), then the PBCommandEnvelope
                                    // might parse, and hasError() will return
                                    // false. In this case the current user is
                                    // 0, which tells us something is wrong!
                                    User currentUser = User
                                            .parseFrom(userSignUp.getUser());
                                    currentUser.setEmail(email);
                                    currentUser.save();
                                    currentUser.createCursor(false);

                                    if (currentUser.getUserId() == 0) {
                                        LogIt.e(MMApiUtils.class,
                                                null,
                                                "Current user is 0, something has gone wrong!",
                                                envelope);
                                        fail(context
                                                .getString(R.string.auth_error),
                                                context.getString(R.string.unexpected_error));
                                        return;
                                    }

                                    MessageMeAppPreferences appPrefs = MessageMeApplication
                                            .getPreferences();

                                    appPrefs.setUser(currentUser);
                                    appPrefs.setUserGetLastModified(userSignUp
                                            .getTimestamp());
                                    appPrefs.setToken(userSignUp.getAuthToken());
                                } else {
                                    PBError pbErr = envelope.getError();

                                    LogIt.w(MMApiUtils.class, pbErr.getCode(),
                                            pbErr.getReason());

                                    if (pbErr.getCode() == ErrorType.INVALID_PASSWORD) {
                                        fail(context
                                                .getString(R.string.password_invalid_title),
                                                context.getString(R.string.password_invalid_body));
                                    } else if (pbErr.getCode() == ErrorType.INVALID_EMAIL) {
                                        fail(context
                                                .getString(R.string.signup_email_auth_error_email_title),
                                                context.getString(R.string.signup_email_auth_error_email_message));
                                    } else if (pbErr.getCode() == ErrorType.UNAVAILABLE_EMAIL_UNVERIFIED
                                            || pbErr.getCode() == ErrorType.UNAVAILABLE_EMAIL_VERIFIED) {
                                        fail(context
                                                .getString(R.string.auth_error),
                                                context.getString(R.string.auth_error_email_exists));
                                    } else {
                                        fail(context
                                                .getString(R.string.auth_error),
                                                envelope.getError().getReason());
                                    }
                                }

                            } catch (IOException e) {
                                LogIt.w(MMApiUtils.class, e.getMessage());
                                fail(context.getString(R.string.auth_error),
                                        context.getString(R.string.network_error));
                            } catch (ResourceException e) {
                                LogIt.w(MMApiUtils.class, e.getMessage());
                                fail(context.getString(R.string.auth_error),
                                        context.getString(R.string.network_error));
                            } catch (Exception e) {
                                LogIt.e(MMApiUtils.class, e, e.getMessage());
                                fail(context.getString(R.string.auth_error),
                                        context.getString(R.string.unexpected_error));
                            }
                        } else {
                            fail(context.getString(R.string.auth_error),
                                    context.getString(R.string.network_error));
                        }

                    }

                    @Override
                    public void done() {
                        progressDialog.dismiss();

                        if (failed()) {
                            UIUtil.alert(context, getExceptionTitle(),
                                    getExceptionMessage());
                        } else {
                            if (isClaimFBaccount) {
                                claimFacebookAccount(context);
                            } else {
                                finishSignUp(context, false);
                            }
                        }
                    }
                };
            } else {
                UIUtil.alert(
                        context,
                        context.getString(R.string.required_fields_alert_title),
                        context.getString(R.string.invalid_email));
            }

        } else {
            // Track the missing fields
            if (!StringUtil.isValid(firstName)) {
                MMTracker.getInstance().abacus("signup", "missing", "f_name",
                        null, null);
            }

            if (!StringUtil.isValid(lastName)) {
                MMTracker.getInstance().abacus("signup", "missing", "l_name",
                        null, null);
            }

            if (!StringUtil.isValid(email)) {
                MMTracker.getInstance().abacus("signup", "missing", "email",
                        null, null);
            }

            if (!StringUtil.isValid(password)) {
                MMTracker.getInstance().abacus("signup", "missing", "password",
                        null, null);
            }

            UIUtil.alert(context,
                    context.getString(R.string.required_fields_alert_title),
                    context.getString(R.string.complete_profile_no_data));
        }
    }

    private static void finishSignUp(Context context, boolean claimedFacebook) {
        context.startService(new Intent(context, MessagingService.class));

        // Tracking
        String subtopic = (DeviceUtil.canPlacePhoneCall(context)) ? "phone"
                : "email";
        String event = claimedFacebook ? "success_fb" : "success";
        MMFunnelTracker.getInstance().abacusOnceFunnel(
                MMFunnelTracker.SIGNUP_FUNNEL,
                MessageMeConstants.ONE_WEEK_SECS, event, subtopic, null, null);
        MMTracker.getInstance().abacus("signup", event, null, null, null);

        // Generates the Welcome Messages
        WelcomeMessageUtil.createWelcomeInformation();

        // Tick the session.
        MMLocalData.getInstance().tickSession();
        MMLocalData.getInstance().setSignupDate(
                (int) System.currentTimeMillis() / 1000);

        Intent intent = new Intent(context, TabsFragmentActivity.class);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);
        Activity activity = (Activity) context;
        activity.finish();
    }

    /**
     * Links a Facebook Authorization token to a MM account
     */
    private static void claimFacebookAccount(final Context context) {

        final ProgressDialog progressDialog = UIUtil.showProgressDialog(
                context, context.getString(R.string.loading));

        new BackgroundTask() {

            @Override
            public void work() {
                if (NetUtil.checkInternetConnection(context)) {

                    try {
                        PBCommandEnvelope envelope = RestfulClient
                                .getInstance().facebookClaim(
                                        Session.getActiveSession()
                                                .getAccessToken());

                        if (!envelope.hasError()) {

                            PBCommandAccountFacebookClaim commandFBClaim = envelope
                                    .getAccountFacebookClaim();

                        } else {
                            LogIt.w(MMApiUtils.class, "User FB signup failed",
                                    envelope.getError().getCode(), envelope
                                            .getError().getReason());

                            fail(context.getString(R.string.auth_error),
                                    context.getString(R.string.fb_account_link_error));
                        }

                    } catch (ResourceException e) {
                        LogIt.w(context, e.getMessage());
                        fail(context.getString(R.string.auth_error),
                                context.getString(R.string.network_error));
                    } catch (IOException e) {
                        LogIt.w(context, e.getMessage());
                        fail(context.getString(R.string.auth_error),
                                context.getString(R.string.network_error));
                    } catch (Exception e) {
                        LogIt.e(context, e.getMessage());
                        fail(context.getString(R.string.auth_error),
                                context.getString(R.string.unexpected_error));
                    }

                } else {
                    fail(context.getString(R.string.auth_error),
                            context.getString(R.string.network_error));
                }

            }

            @Override
            public void done() {
                progressDialog.dismiss();
                if (!failed()) {
                    finishSignUp(context, true);
                } else {
                    LogIt.e(this, "Facebook account link error");

                    UIUtil.alert(context, getExceptionTitle(),
                            getExceptionMessage());
                }
            }
        };

    }

}
