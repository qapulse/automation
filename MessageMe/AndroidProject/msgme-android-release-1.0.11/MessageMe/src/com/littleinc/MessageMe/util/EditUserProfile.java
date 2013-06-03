package com.littleinc.MessageMe.util;

import java.io.IOException;

import org.restlet.resource.ResourceException;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.coredroid.util.BackgroundTask;
import com.coredroid.util.LogIt;
import com.coredroid.util.StringUtil;
import com.coredroid.util.UIUtil;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.net.RestfulClient;
import com.littleinc.MessageMe.ui.TabsFragmentActivity;

public class EditUserProfile {

    private String changedFirstName = null;

    private String changedLastName = null;

    private TextView profileName;   

    public EditUserProfile(TextView profileName) {
        this.profileName = profileName;
    }

    public EditUserProfile() {
        this.profileName = null;
    }

    public void createEditProfileDialog(final Context context) {
        LogIt.d(this, "Opens Edit Profile Dialog");
        
        User currentUser = MessageMeApplication.getCurrentUser();

        LinearLayout wrapper = new LinearLayout(context);
        wrapper.setOrientation(1);
        final EditText firstName = new EditText(context);
        final EditText lastName = new EditText(context);

        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(R.string.edit_my_profile);

        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);

        wrapper.addView(firstName, params);
        wrapper.addView(lastName, params);

        firstName.setHint(R.string.first_name_label);
        lastName.setHint(R.string.last_name_label);
        firstName.setSingleLine(true);
        lastName.setSingleLine(true);
        firstName.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        lastName.setImeOptions(EditorInfo.IME_ACTION_DONE);

        if (changedFirstName == null && changedLastName == null) {
            firstName.setText(currentUser.getFirstName());
            lastName.setText(currentUser.getLastName());
        } else {
            // Only happens if the user left one of the fields empty
            // and enters here through the retry confirmation alert
            firstName.setText(changedFirstName);
            lastName.setText(changedLastName);
        }

        alert.setView(wrapper);
        alert.setPositiveButton(context.getString(R.string.save),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String newFirstName = firstName.getText().toString();
                        String newLastName = lastName.getText().toString();
                        saveUserInformation(context, newFirstName, newLastName);
                    }
                });

        alert.setNegativeButton(context.getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                        changedFirstName = null;
                        changedLastName = null;
                    }
                });
        alert.show();
    }

    /**
     * Creates a confirmation dialog when first name or 
     * last name was empty in the edit profile dialog.
     */
    private void retryEditProfileDialog(final Context context) {
        LogIt.d(this, "Opens Retry confirmation dialog");
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(R.string.edit_my_profile);
        alert.setMessage(R.string.required_fields_alert_body);
        alert.setPositiveButton(context.getString(R.string.retry),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        createEditProfileDialog(context);
                    }
                });

        alert.setNegativeButton(context.getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                });
        alert.show();
    }

    /**
     * Sends a POST to the API and save the new local
     * user first name and last name.
     */
    private void saveUserInformation(final Context context, final String newFirstName,
            final String newLastName) {
        LogIt.d(this, "Clicked on Save settings");

        if (StringUtil.isEmpty(newFirstName) || StringUtil.isEmpty(newLastName)) {
            LogIt.d(this, "First name or Last name are empty, calling retry confirmation dialog");
            changedFirstName = newFirstName;
            changedLastName = newLastName;
            retryEditProfileDialog(context);
            return;
        }
    
        final ProgressDialog progressDialog = UIUtil.showProgressDialog(
                context, context.getString(R.string.saving));               

        new BackgroundTask() {
            
            User updateUser = new User();            

            @Override
            public void work() {

                try {                                        
                    updateUser.setFirstName(newFirstName);
                    updateUser.setLastName(newLastName);

                    RestfulClient.getInstance().updateUser(updateUser,
                            MessageMeConstants.USER_NAME);

                } catch (IOException e) {
                    LogIt.e(EditUserProfile.class, e, e.getMessage());
                    fail(context.getString(R.string.network_error));
                } catch (ResourceException e) {
                    LogIt.e(EditUserProfile.class, e, e.getMessage());
                    fail(context.getString(R.string.network_error));
                } catch (Exception e) {
                    LogIt.e(EditUserProfile.class, e, e.getMessage());
                    fail(context.getString(R.string.unexpected_error));
                }

            }

            @Override
            public void done() {
                progressDialog.dismiss();

                if (!failed()) {

                    MessageMeApplication.getCurrentUser().setFirstName(
                            updateUser.getFirstName());
                    MessageMeApplication.getCurrentUser().setLastName(
                            updateUser.getLastName());

                    if (profileName != null) {
                        profileName.setText(MessageMeApplication
                                .getCurrentUser().getDisplayName());
                    }
                    
                    MessageMeApplication.getCurrentUser().save();

                    if (context instanceof TabsFragmentActivity) {
                        // context can be either TabsFragmentActivity or SettingsActivity
                        TabsFragmentActivity activity = (TabsFragmentActivity) context;
                        activity.updateUserInformation();
                    }

                    Toast.makeText(context,
                            context.getString(R.string.succesfull_update),
                            Toast.LENGTH_SHORT).show();
                    LogIt.d(context, "profile succesfully updated");
                } else {
                    Toast.makeText(context, getExceptionMessage(),
                            Toast.LENGTH_SHORT).show();
                    changedFirstName = null;
                    changedLastName = null;
                }
            }
        };
    }
}
