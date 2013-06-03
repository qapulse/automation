package com.littleinc.MessageMe.ui;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.coredroid.util.LogIt;
import com.coredroid.util.StringUtil;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.actionbar.ActionBarNoServiceActivity;
import com.littleinc.MessageMe.util.MMApiUtils;

@TargetApi(14)
public class CreateLoginActivity extends ActionBarNoServiceActivity {

    private EditText emailText;

    private EditText passwordText;

    private String firstName;

    private String lastName;

    private boolean isCallCapable;

    private String phoneSignature;

    private String normalizedPhoneNumber;

    private static final String FACEBOOK_EMAIL = "@facebook.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_login_layout);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            ActionBar actionBar = getActionBar();
            actionBar.setHomeButtonEnabled(true);
            actionBar.setIcon(R.drawable.actionbar_top_icon_back);
        }

        emailText = (EditText) findViewById(R.id.email);

        passwordText = (EditText) findViewById(R.id.password);

        String fbEmail = getIntent().getStringExtra(
                MessageMeConstants.EXTRA_EMAIL);

        firstName = getIntent().getStringExtra(
                MessageMeConstants.EXTRA_FIRST_NAME);

        lastName = getIntent().getStringExtra(
                MessageMeConstants.EXTRA_LAST_NAME);

        isCallCapable = getIntent().getBooleanExtra(
                MessageMeConstants.EXTRA_IS_CALL_CAPABLE, false);

        phoneSignature = getIntent().getStringExtra(
                MessageMeConstants.EXTRA_PHONE_SIGNATURE);

        normalizedPhoneNumber = getIntent().getStringExtra(
                MessageMeConstants.EXTRA_PHONE_NUMBER);

        if (!StringUtil.isEmpty(fbEmail) && !fbEmail.contains(FACEBOOK_EMAIL)) {
            emailText.setText(fbEmail);
        }

        passwordText.setOnEditorActionListener(new DoneKeyListener());

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
            if (!isCallCapable) {
                startLandingPageActivity();
            }
            break;
        case R.id.next_btn:
            LogIt.user(this, "Pressed Next button");

            MMApiUtils.completeUserRegistration(CreateLoginActivity.this,
                    firstName, lastName, emailText.getText().toString(),
                    passwordText.getText().toString(), normalizedPhoneNumber,
                    phoneSignature, isCallCapable, true);

            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startLandingPageActivity() {
        Intent intent = new Intent(this, LandingActivity.class);

        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        finish();
        startActivity(intent);
    }

    private class DoneKeyListener implements EditText.OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                LogIt.user(this, "User pressed Done key");
                MMApiUtils.completeUserRegistration(CreateLoginActivity.this,
                        firstName, lastName, emailText.getText().toString(),
                        passwordText.getText().toString(),
                        normalizedPhoneNumber, phoneSignature, isCallCapable,
                        true);
                return true;
            }
            return false;
        }
    }
}
