package com.littleinc.MessageMe.ui;

import java.io.IOException;

import org.restlet.resource.ResourceException;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ShareCompat.IntentBuilder;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.coredroid.util.BackgroundTask;
import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.actionbar.ActionBarActivity;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.net.RestfulClient;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandUserSearch;
import com.littleinc.MessageMe.util.DatabaseTask;
import com.littleinc.MessageMe.util.DeviceUtil;
import com.littleinc.MessageMe.util.FriendsInviteUtil;
import com.littleinc.MessageMe.util.NetUtil;
import com.littleinc.MessageMe.util.StringUtil;

/**
 * Activity that enables the user to invite more people into his address book
 */
@TargetApi(14)
public class FriendsInviteActivity extends ActionBarActivity {

    private EditText friendsInviteInput;
    
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.friends_invite_title);
        setContentView(R.layout.friends_invite_layout);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            ActionBar actionBar = getActionBar();
            actionBar.setHomeButtonEnabled(true);
            actionBar.setIcon(R.drawable.actionbar_top_icon_back);
        }

        initComponents();
    }

    /**
     * Initializer
     */
    private void initComponents() {
        friendsInviteInput = (EditText) findViewById(R.id.friends_invite_input);
        friendsInviteInput.setOnEditorActionListener(searchActionListener);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.no_button_actionbar, menu);

        // Calling super after populating the menu is necessary here to ensure
        // that the action bar helpers have a chance to handle this event.
        return super.onCreateOptionsMenu(menu);
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

    /**
     * Method called from the layout XML, when
     * the user press the add Facebook friends button
     */
    public void addFacebookFriends(View view) {
        LogIt.user(this, "Pressed add Facebook friends btn");
        alert(R.string.friends_invite_coming_soon, null);
    }

    /**
     * Method called from the layout XML, when
     * the user press the invite your friends button
     */
    public void inviteYourFriends(View view) {
        LogIt.user(this, "Pressed invite your friends btn");
        FriendsInviteUtil.openFriendsInvite(this);
    }

    /**
     * Displays an alert to send an invite by email and open the App chooser to select a supported 
     * application to share the invite
     */
    private void showEmailInviteAlert(final String email) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.friends_invite_no_user_title)
                .setMessage(R.string.friends_invite_no_user_body)
                .setPositiveButton(R.string.friends_invite_send_invite,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                LogIt.user(this, "Pressed send invite option");
                                IntentBuilder
                                        .from(FriendsInviteActivity.this)
                                        .addEmailTo(email)
                                        // message/* mimetype it is frequently desirable, in sending mail, to encapsulate another mail message.
                                        // This mimetype help us to reduce the list of supported applications
                                        .setType("message/rfc822")
                                        .setSubject(
                                                getString(R.string.email_invite_subject))
                                        .setText(
                                                getString(R.string.email_invite_body))
                                        .setChooserTitle(
                                                R.string.friends_invite_send_invite)
                                        .startChooser();
                            }
                        })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                dialog.dismiss();
                            }
                        }).create().show();
    }

    private void doSearch(TextView v) {
        final String query = v.getText().toString();
        LogIt.d(this, "Search for user", query);                

        if (!NetUtil.checkInternetConnection(v.getContext())) {
            LogIt.w(this, "No connection");
            alert(R.string.network_error_title, R.string.network_error);
        } else if (!StringUtil.isValid(query)) {
            LogIt.w(this, "Invalid pin, email or phone");
            alert(R.string.friends_invite_search_error_title,
                    R.string.friends_invite_search_error_invalid);
        } else {
            
            friendsInviteInput.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
            
            new BackgroundTask() {

                private User resultUser = null;

                @Override
                public void work() {
                    try {
                        PBCommandEnvelope commandEnvelope = RestfulClient
                                .getInstance().userSearch(query);
                        PBCommandUserSearch commandUserSearch = commandEnvelope
                                .getUserSearch();

                        if (!commandEnvelope.hasError()
                                && commandUserSearch.getVerifiedResultsCount() > 0) {
                            // Should only get the first verified user back
                            resultUser = User.parseFrom(commandUserSearch
                                    .getVerifiedResultsList().get(0));
                        } else if (!commandEnvelope.hasError()
                                && commandUserSearch
                                        .getUnverifiedResultsCount() > 0) {
                            // Should only get the first unverified user back
                            resultUser = User.parseFrom(commandUserSearch
                                    .getUnverifiedResultsList().get(0));
                        } else {
                            fail(getString(R.string.friends_invite_search_error_title),
                                    getString(R.string.friends_invite_search_error_body));
                        }
                    } catch (ResourceException e) {
                        LogIt.e(this, e, e.getMessage());
                        fail(getString(R.string.network_error_title),
                                getString(R.string.network_error));
                    } catch (IOException e) {
                        LogIt.e(this, e, e.getMessage());
                        fail(getString(R.string.network_error_title),
                                getString(R.string.network_error));
                    } catch (Exception e) {
                        LogIt.e(this, e, e.getMessage());
                        fail(getString(R.string.generic_error_title),
                                getString(R.string.unexpected_error));
                    }
                }

                @Override
                public void done() {
                    friendsInviteInput.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    if (!failed() && resultUser != null) {
                        new DatabaseTask() {

                            @Override
                            public void work() {
                                // If resultUser doesn't exist in the local database, should be added
                                if (!User.exists(resultUser.getUserId())) {
                                    LogIt.d(FriendsInviteActivity.class,
                                            "Adding new user",
                                            resultUser.getUserId());
                                    resultUser.save();
                                }
                            }

                            @Override
                            public void done() {
                                Intent intent = new Intent(
                                        FriendsInviteActivity.this,
                                        ContactProfileActivity.class);
                                intent.putExtra(
                                        MessageMeConstants.RECIPIENT_ID_KEY,
                                        resultUser.getUserId());
                                startActivity(intent);
                            }
                        };
                    } else {
                        if (getExceptionTitle()
                                .equals(getString(R.string.friends_invite_search_error_title))) {
                            if (StringUtil.isEmailValid(query)) {
                                showEmailInviteAlert(query);
                                return;
                            }
                        }

                        alert(getExceptionTitle(), getExceptionMessage());
                    }
                }
            };
        }
    }

    private OnEditorActionListener searchActionListener = new OnEditorActionListener() {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

            if (DeviceUtil.wasActionPressed(v, actionId, event)) {
                UIUtil.hideKeyboard(v);
                doSearch(v);
                return true;
            } else {
                return false;
            }
        }
    };
}