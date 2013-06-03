package com.littleinc.MessageMe.ui;

import java.io.IOException;

import org.restlet.resource.ResourceException;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.coredroid.util.BackgroundTask;
import com.coredroid.util.LogIt;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.actionbar.ActionBarNoServiceActivity;
import com.littleinc.MessageMe.bo.CountryCode;
import com.littleinc.MessageMe.metrics.MMFunnelTracker;
import com.littleinc.MessageMe.metrics.MMTracker;
import com.littleinc.MessageMe.net.RestfulClient;
import com.littleinc.MessageMe.protocol.Commands.PBCommandAccountPhoneNew;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Objects.PBError;
import com.littleinc.MessageMe.protocol.Objects.PBError.ErrorType;
import com.littleinc.MessageMe.util.NetUtil;
import com.littleinc.MessageMe.util.StringUtil;

@TargetApi(14)
public class PhoneEntryActivity extends ActionBarNoServiceActivity {

    private EditText phoneEntry;

    private TextView countryNameTextView;

    private TextView countryCodeTextView;

    private LinearLayout countryContainer;

    private CountryCode mainCountryCode;

    private PhoneNumberUtil phoneUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // tracking
        MMFunnelTracker.getInstance().abacusOnceFunnel(
                MMFunnelTracker.SIGNUP_FUNNEL,
                MessageMeConstants.ONE_WEEK_SECS, "phone_num", "phone", null,
                null);

        setContentView(R.layout.phone_entry_layout);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            ActionBar actionBar = getActionBar();
            actionBar.setHomeButtonEnabled(true);
            actionBar.setIcon(R.drawable.actionbar_top_icon_back);
        }

        phoneEntry = (EditText) findViewById(R.id.phone_number_text);

        phoneEntry.setOnEditorActionListener(new DoneKeyListener());

        countryNameTextView = (TextView) findViewById(R.id.country_name);

        countryCodeTextView = (TextView) findViewById(R.id.country_code);

        countryContainer = (LinearLayout) findViewById(R.id.country_container);

        countryContainer.setOnClickListener(new CountryTextViewClickListener());

        phoneEntry.setText(setPhoneNumber());

        mainCountryCode = new CountryCode();

        setCurrentCountry();
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
                finish();
                break;
            case R.id.next_btn:
                LogIt.user(this, "Pressed Next button");
                registerPhone();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Checks for the network country ISO and pre-populates the country name
     * field name and country code
     */
    private void setCurrentCountry() {
        TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String countryISO = manager.getNetworkCountryIso();

        for (CountryCode country : CountryListActivity.getCountryList()) {
            if (country.getCountryShortName().equalsIgnoreCase(countryISO)) {
                setCountryInformation(country.getCountryName(),
                        country.getCountryShortName(), country.getCountryCode());
                LogIt.d(this, "Detected country: ", country.getCountryName());
                return;
            }
        }

        if (StringUtil.isEmpty(countryNameTextView.getText().toString())) {
            // This shouldn't happen, but just in case the user country
            // is not listed, we add the default one
            setCountryInformation("United States", "US", 1);
            LogIt.d(this, "Couldn't find country, adding default value");
        }
    }

    private void setCountryInformation(String countryName, String countryISO,
            int countryCode) {
        mainCountryCode.setCountryName(countryName);
        mainCountryCode.setCountryCode(countryCode);
        mainCountryCode.setCountryShortName(countryISO);
        countryNameTextView.setText(countryName);
        countryCodeTextView.setText("+" + String.valueOf(countryCode));
    }

    private class CountryTextViewClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            // We don't specify a title or any legend text for this page
            Intent intent = new Intent(PhoneEntryActivity.this,
                    CountryListActivity.class);
            startActivityForResult(intent,
                    MessageMeConstants.COUNTRY_SELECTION_REQUEST_CODE);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case MessageMeConstants.COUNTRY_SELECTION_REQUEST_CODE:
                switch (resultCode) {
                    case RESULT_OK:
                        mainCountryCode
                                .setCountryName(data
                                        .getStringExtra(MessageMeConstants.EXTRA_COUNTRY_NAME));
                        mainCountryCode
                                .setCountryCode(Integer.valueOf(data
                                        .getStringExtra(MessageMeConstants.EXTRA_CONTRY_CODE)));
                        mainCountryCode
                                .setCountryShortName(data
                                        .getStringExtra(MessageMeConstants.EXTRA_COUNTRY_INITIALS));

                        countryNameTextView.setText(mainCountryCode
                                .getCountryName());
                        countryCodeTextView.setText("+"
                                + String.valueOf(mainCountryCode
                                        .getCountryCode()));
                        break;
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private String setPhoneNumber() {

        TelephonyManager phoneManager = (TelephonyManager) this
                .getSystemService(FragmentActivity.TELEPHONY_SERVICE);
        String localnumber = phoneManager.getLine1Number();

        return localnumber;

    }

    private PhoneNumber getConvertedPhoneNumber(String phone)
            throws NumberParseException {
        PhoneNumber phoneNumber = phoneUtil.parse(phone,
                mainCountryCode.getCountryShortName());

        return phoneNumber;
    }

    private void registerPhone() {
        String typedPhoneNumber = phoneEntry.getText().toString();

        if (StringUtil.isValid(typedPhoneNumber)) {

            final ProgressDialog progressDialog = showProgressDialog(PhoneEntryActivity.this
                    .getString(R.string.phone_entry_dialog_message));

            new BackgroundTask() {

                PhoneNumber phoneNumber = null;

                String normalizedPhoneNumber = "";

                @Override
                public void work() {
                    if (NetUtil
                            .checkInternetConnection(PhoneEntryActivity.this)) {
                        try {

                            phoneUtil = PhoneNumberUtil.getInstance();

                            phoneNumber = getConvertedPhoneNumber(phoneEntry
                                    .getText().toString());

                            String formatedPhoneNumber = phoneUtil.format(
                                    phoneNumber, PhoneNumberFormat.E164);

                            if (!MessageMeApplication.getPreferences()
                                    .getRegisteredPhoneNumber()
                                    .equals(formatedPhoneNumber)) {

                                PBCommandEnvelope envelope = RestfulClient
                                        .getInstance()
                                        .newPhone(
                                                mainCountryCode
                                                        .getCountryCode(),
                                                String.valueOf(phoneNumber
                                                        .getNationalNumber()),
                                                false);

                                if (!envelope.hasError()) {
                                    PBCommandAccountPhoneNew phoneNew = envelope
                                            .getAccountPhoneNew();

                                    normalizedPhoneNumber = phoneNew
                                            .getPhoneNumber().getE164();

                                } else {
                                    PBError pbErr = envelope.getError();

                                    // Error tracking
                                    MMTracker.getInstance().abacus("signup",
                                            "pb_error", "phone", null,
                                            pbErr.getCode().getNumber());

                                    LogIt.w(PhoneEntryActivity.this,
                                            pbErr.getCode(), pbErr.getReason());

                                    if (pbErr.getCode() == ErrorType.INVALID_PHONE) {
                                        fail(getString(R.string.phone_invalid_title),
                                                getString(R.string.phone_invalid_message));
                                    } else if (pbErr.getCode() == ErrorType.UNAVAILABLE_PHONE) {
                                        fail(getString(R.string.signup_error),
                                                getString(R.string.phone_entry_dulplicate_number));
                                    } else {
                                        fail(getString(R.string.signup_error),
                                                envelope.getError().getReason());
                                    }
                                }
                            } else {
                                LogIt.d(PhoneEntryActivity.class,
                                        "Phone number already saved in app preferences, don't register phone number again");
                                normalizedPhoneNumber = MessageMeApplication
                                        .getPreferences()
                                        .getRegisteredPhoneNumber();
                            }
                        } catch (NumberParseException e) {
                            MMTracker.getInstance().abacus("signup", "e_parse",
                                    "phone", null, null);
                            LogIt.e(PhoneEntryActivity.class, e, e.getMessage());
                            fail(getString(R.string.signup_error),
                                    getString(R.string.invalid_number));
                        } catch (IOException e) {
                            MMTracker.getInstance().abacus("signup", "e_io",
                                    "phone", null, null);
                            LogIt.e(PhoneEntryActivity.class, e, e.getMessage());
                            fail(getString(R.string.network_error_title),
                                    getString(R.string.network_error));
                        } catch (ResourceException e) {
                            MMTracker.getInstance().abacus("signup", "error",
                                    "phone", null, e.getStatus().getCode());
                            LogIt.e(PhoneEntryActivity.class, e, e.getMessage());
                            fail(getString(R.string.network_error_title),
                                    getString(R.string.network_timed_out));
                        } catch (Exception e) {
                            MMTracker.getInstance().abacus("signup", "error_u",
                                    "phone", null, null);
                            LogIt.e(PhoneEntryActivity.class, e, e.getMessage());
                            fail(getString(R.string.signup_error),
                                    getString(R.string.unexpected_error));
                        }
                    } else {
                        MMTracker.getInstance().abacus("signup", "error_u",
                                "phone", null, null);
                        LogIt.w(this, "No network");
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

                        Intent intent = new Intent(PhoneEntryActivity.this,
                                VerifyNumberActivity.class);
                        intent.putExtra(MessageMeConstants.EXTRA_PHONE_NUMBER,
                                normalizedPhoneNumber);

                        intent.putExtra(
                                MessageMeConstants.EXTRA_COUNTRY_INITIALS,
                                mainCountryCode.getCountryShortName());

                        MessageMeApplication
                                .getPreferences()
                                .setRegisteredPhoneNumber(normalizedPhoneNumber);

                        startActivity(intent);
                        finish();
                    }
                }
            };
        } else {
            alert(PhoneEntryActivity.this
                    .getString(R.string.required_fields_alert_title),
                    PhoneEntryActivity.this
                            .getString(R.string.required_fields_alert_body));
        }
    }

    private class DoneKeyListener implements EditText.OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                LogIt.user(this, "Pressed Done key");
                registerPhone();
                return true;
            }
            return false;
        }
    }

}
