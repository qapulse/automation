package com.littleinc.MessageMe.ui;

import java.io.IOException;

import org.restlet.resource.ResourceException;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.coredroid.util.BackgroundTask;
import com.coredroid.util.LogIt;
import com.coredroid.util.MailUtil;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.actionbar.ActionBarNoServiceActivity;
import com.littleinc.MessageMe.metrics.MMFunnelTracker;
import com.littleinc.MessageMe.metrics.MMTracker;
import com.littleinc.MessageMe.net.RestfulClient;
import com.littleinc.MessageMe.protocol.Commands.PBCommandAccountPhoneNew;
import com.littleinc.MessageMe.protocol.Commands.PBCommandAccountPhoneVerify;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Objects.PBError;
import com.littleinc.MessageMe.util.NetUtil;
import com.littleinc.MessageMe.util.StringUtil;

@TargetApi(14)
public class VerifyNumberActivity extends ActionBarNoServiceActivity {

    private EditText confirmationCode;

    private String normalizedPhoneNumber;

    private String phoneSignature;

    private TextView verifyNumberLegend;

    private TextView resendCode;

    private String countryInitials;

    private TextView voiceSms;

    private PhoneNumberUtil phoneUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // tracking
        MMFunnelTracker.getInstance().abacusOnceFunnel(
                MMFunnelTracker.SIGNUP_FUNNEL,
                MessageMeConstants.ONE_WEEK_SECS, "pin", "phone", null, null);

        setContentView(R.layout.verify_number_layout);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            ActionBar actionBar = getActionBar();
            actionBar.setHomeButtonEnabled(true);
            actionBar.setIcon(R.drawable.actionbar_top_icon_back);
        }

        confirmationCode = (EditText) findViewById(R.id.confirmation_code);
        confirmationCode.setOnEditorActionListener(new DoneKeyListener());

        voiceSms = (TextView) findViewById(R.id.resend_voice_sms);

        normalizedPhoneNumber = getIntent().getStringExtra(
                MessageMeConstants.EXTRA_PHONE_NUMBER);

        countryInitials = getIntent().getStringExtra(
                MessageMeConstants.EXTRA_COUNTRY_INITIALS);

        verifyNumberLegend = (TextView) findViewById(R.id.verify_number_legend);
        verifyNumberLegend.setText(verifyNumberLegend.getText().toString()
                + " " + normalizedPhoneNumber);

        resendCode = (TextView) findViewById(R.id.resend_code);
        resendCode.setOnClickListener(new ResendCodeClickListener());
        confirmationCode.addTextChangedListener(new RegisterPhoneWatcher());

        phoneUtil = PhoneNumberUtil.getInstance();

    }

    /**
     * starts a countdown timer
     */
    private void initCountDownTimer() {
        countdownTimer.cancel();
        countdownTimer.start();
    }

    private CountDownTimer countdownTimer = new CountDownTimer(
            MessageMeConstants.COUNTDOWN_TIMER_LIMIT,
            MessageMeConstants.COUNTDOWN_THRESHOLD) {

        @Override
        public void onTick(long millisUntilFinished) {
            voiceSms.setText(String.format(
                    getString(R.string.signup_pin_call_pending),
                    DateUtils.formatElapsedTime(millisUntilFinished
                            / MessageMeConstants.COUNTDOWN_THRESHOLD)));
        }

        @Override
        public void onFinish() {
            LogIt.d(this, "Countdown finished");
            voiceSms.setText(R.string.signup_pin_call_ready);
            voiceSms.setOnClickListener(new VoiceSMSClickListener());
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        initCountDownTimer();
    }

    private class VoiceSMSClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            LogIt.user(this, "User clicked in send voice sms");
            voiceSms.setText(R.string.signup_pin_callin);
            resendSmsCode(true);
            voiceSms.setClickable(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.login_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            openPreviousActivity();
            break;
        case R.id.next_btn:
            LogIt.user(this, "Pressed Next button");
            verifyPhone();
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        openPreviousActivity();
    }

    private void openPreviousActivity() {
        Intent intent = new Intent(this, PhoneEntryActivity.class);
        startActivity(intent);
    }

    /**
     * Sends a POST to the Restful API with the verification code     
     */
    private void verifyPhone() {
        final String verificationCode = confirmationCode.getText().toString();

        if (StringUtil.isValid(verificationCode)) {

            final ProgressDialog progressDialog = showProgressDialog(VerifyNumberActivity.this
                    .getString(R.string.verify_number_dialog_message));

            new BackgroundTask() {

                @Override
                public void work() {
                    if (NetUtil
                            .checkInternetConnection(VerifyNumberActivity.this)) {

                        try {

                            PBCommandEnvelope envelope = RestfulClient
                                    .getInstance().verifyPhone(
                                            normalizedPhoneNumber,
                                            Integer.parseInt(verificationCode));

                            if (!envelope.hasError()) {
                                PBCommandAccountPhoneVerify phoneVerify = envelope
                                        .getAccountPhoneVerify();

                                phoneSignature = phoneVerify.getSignature();

                            } else {
                                LogIt.e(VerifyNumberActivity.class, null,
                                        envelope.getError().getReason());
                                fail(getString(R.string.signup_error),
                                        getString(R.string.verify_number_error));
                            }
                        } catch (IOException e) {
                            LogIt.e(VerifyNumberActivity.class, e,
                                    e.getMessage());
                            fail(getString(R.string.network_error_title),
                                    getString(R.string.network_error));
                        } catch (ResourceException e) {
                            LogIt.e(VerifyNumberActivity.class, e,
                                    e.getMessage());
                            fail(getString(R.string.network_error_title),
                                    getString(R.string.network_timed_out));
                        } catch (Exception e) {
                            LogIt.e(VerifyNumberActivity.class, e,
                                    e.getMessage());
                            fail(getString(R.string.signup_error),
                                    getString(R.string.unexpected_error));
                        }
                    } else {
                        LogIt.w(VerifyNumberActivity.class, "No network");
                        fail(getString(R.string.network_error_title),
                                getString(R.string.network_error));
                    }
                }

                @Override
                public void done() {
                    progressDialog.dismiss();

                    if (failed()) {
                        alert(getExceptionTitle(), getExceptionMessage());
                    } else {
                        LogIt.d(VerifyNumberActivity.class, "Phone number: "
                                + normalizedPhoneNumber);
                        LogIt.d(VerifyNumberActivity.class, "Phone signature: "
                                + phoneSignature);

                        Intent intent = new Intent(VerifyNumberActivity.this,
                                CompleteProfileActivity.class);
                        intent.putExtra(
                                MessageMeConstants.EXTRA_PHONE_SIGNATURE,
                                phoneSignature);
                        intent.putExtra(MessageMeConstants.EXTRA_PHONE_NUMBER,
                                normalizedPhoneNumber);
                        intent.putExtra(
                                MessageMeConstants.EXTRA_IS_CALL_CAPABLE, true);

                        MessageMeApplication.getPreferences()
                                .setSentSmsConfirmation(true);
                        MessageMeApplication.getPreferences()
                                .setPhoneSignature(phoneSignature);
                        startActivity(intent);
                        finish();
                    }
                }
            };

        } else {
            alert(VerifyNumberActivity.this
                    .getString(R.string.required_fields_alert_title),
                    VerifyNumberActivity.this
                            .getString(R.string.required_fields_alert_body));
        }
    }

    private class DoneKeyListener implements EditText.OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                LogIt.user(this, "User pressed Next Key");
                verifyPhone();
                return true;
            }
            return false;
        }

    }

    private class RegisterPhoneWatcher implements TextWatcher {

        /* 
         * Checks if the verification code length is 3
         * if that's the case, then automatically verifies the code.
         */
        @Override
        public void afterTextChanged(Editable s) {
            if (s.toString().length() == 3) {
                LogIt.user(this, "User entered the third verification number");
                verifyPhone();
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                int count) {
        }

    }

    /**
     * Sends a POST to the Restful server requesting a
     * new SMS code, if isCalling is true, then ask the server
     * for a Voice SMS
     */
    private void resendSmsCode(final boolean isCalling) {
        if (StringUtil.isValid(countryInitials)
                && StringUtil.isValid(normalizedPhoneNumber)) {

            MMTracker.getInstance().abacus("signup", "resend", "pin", null,
                    null);

            final ProgressDialog progressDialog = showProgressDialog(VerifyNumberActivity.this
                    .getString(R.string.phone_entry_dialog_message));

            new BackgroundTask() {

                PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

                PhoneNumber phoneNumber = null;

                int countryCode = phoneUtil
                        .getCountryCodeForRegion(countryInitials);

                @Override
                public void work() {
                    if (NetUtil
                            .checkInternetConnection(VerifyNumberActivity.this)) {

                        try {

                            phoneNumber = getPhoneNumber();

                            PBCommandEnvelope envelope = RestfulClient
                                    .getInstance().newPhone(
                                            countryCode,
                                            String.valueOf(phoneNumber
                                                    .getNationalNumber()),
                                            isCalling);

                            if (!envelope.hasError()) {

                                PBCommandAccountPhoneNew phoneNew = envelope
                                        .getAccountPhoneNew();

                                normalizedPhoneNumber = phoneNew
                                        .getPhoneNumber().getE164();
                            } else {
                                PBError pbErr = envelope.getError();
                                MMTracker.getInstance().abacus("signup",
                                        "pb_error", "pin", null,
                                        pbErr.getCode().getNumber());

                                // The phone number was already validated on 
                                // the previous page so we wouldn't expect to
                                // get any errors on the envelop here
                                LogIt.e(this, null, pbErr.getCode(),
                                        pbErr.getReason());
                                fail(getString(R.string.signup_error),
                                        getString(R.string.unexpected_error));
                            }
                        } catch (NumberParseException e) {
                            MMTracker.getInstance().abacus("signup", "e_parse",
                                    "pin", null, null);
                            LogIt.e(VerifyNumberActivity.class, e,
                                    e.getMessage());
                            fail(getString(R.string.signup_error),
                                    getString(R.string.invalid_number));
                        } catch (IOException e) {
                            MMTracker.getInstance().abacus("signup", "e_io",
                                    "pin", null, null);
                            LogIt.e(VerifyNumberActivity.class, e,
                                    e.getMessage());
                            fail(getString(R.string.network_error_title),
                                    getString(R.string.network_error));
                        } catch (ResourceException e) {
                            MMTracker.getInstance().abacus("signup", "error",
                                    "pin", null, e.getStatus().getCode());
                            LogIt.e(VerifyNumberActivity.class, e,
                                    e.getMessage());
                            fail(getString(R.string.network_error_title),
                                    getString(R.string.network_timed_out));
                        } catch (Exception e) {
                            MMTracker.getInstance().abacus("signup", "error_u",
                                    "pin", null, null);
                            LogIt.e(VerifyNumberActivity.class, e,
                                    e.getMessage());
                            fail(getString(R.string.signup_error),
                                    getString(R.string.unexpected_error));
                        }
                    } else {
                        MMTracker.getInstance().abacus("signup", "error_u",
                                "pin", null, null);
                        LogIt.w(VerifyNumberActivity.class, "No network");
                        fail(getString(R.string.network_error_title),
                                getString(R.string.network_error));
                    }

                }

                @Override
                public void done() {
                    progressDialog.dismiss();

                    if (failed()) {
                        alert(getExceptionTitle(), getExceptionMessage());
                        initCountDownTimer();
                    } else {
                        if (!isCalling) {
                            resendSmsConfirmationDialog();
                        }
                    }
                }
            };
        }
    }

    /**
     * Returns the Object PhoneNumber containing the registered
     * phone number
     */
    private PhoneNumber getPhoneNumber() throws NumberParseException {
        return phoneUtil.parse(normalizedPhoneNumber, countryInitials);
    }

    /**
     * Creates an Alert Dialog to notify the user that a SMS message
     * has been sent with a new code
     */
    private void resendSmsConfirmationDialog() {

        String phoneNumber;
        try {
            phoneNumber = String.valueOf(getPhoneNumber().getNationalNumber());

            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle(R.string.signup_pin_sent_title);
            alert.setMessage(String.format(
                    getString(R.string.signup_pin_sms_sent), phoneNumber));
            alert.setPositiveButton(getString(R.string.contact_support),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            LogIt.user(this, "Open send email");
                            MailUtil.sendEmail(VerifyNumberActivity.this,
                                    getString(R.string.support_email), "", "");
                        }
                    });

            alert.setNegativeButton(getString(R.string.okay),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            dialog.cancel();
                        }
                    });
            alert.show();

        } catch (NumberParseException e) {
            LogIt.e(this, e, e.getMessage());
        }
    }

    private class ResendCodeClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            LogIt.user(this, "User clicked in resend SMS");
            // Calls the resendSmsCode method with a no calling argument
            resendSmsCode(false);
        }
    }

}
