package com.littleinc.MessageMe.ui;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;

import com.littleinc.MessageMe.R;

public class MMAlertDialogFragment extends DialogFragment {

    protected static final String TITLE = "title";

    protected static final String MESSAGE = "message";

    protected static final String TITLE_RES_ID = "title_res_id";

    protected static final String MESSAGE_RES_ID = "message_res_id";

    public static MMAlertDialogFragment newInstance(int titleResId,
            int messageResId) {
        MMAlertDialogFragment dialogFragment = new MMAlertDialogFragment();

        Bundle args = new Bundle();
        args.putInt(TITLE_RES_ID, titleResId);
        args.putInt(MESSAGE_RES_ID, messageResId);
        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    public static MMAlertDialogFragment newInstance(String title, String message) {
        MMAlertDialogFragment dialogFragment = new MMAlertDialogFragment();

        Bundle args = new Bundle();
        args.putString(TITLE, title);
        args.putString(MESSAGE, message);
        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Builder alertDialogBuilder = getAlertDialogBuilder();
        if (alertDialogBuilder == null) {
            return super.onCreateDialog(savedInstanceState);
        } else {
            return alertDialogBuilder.create();
        }
    }

    /**
     * Flag to detect if current dialog can be restored
     * 
     * @return true if the current dialog is available to be restored
     */
    public boolean isAvailableToRestore() {
        return true;
    }

    /**
     * Implementation for base alert dialog (Title, Message, Okay Btn)
     * 
     * Children should override this method to create custom alert dialogs
     * using {@link AlertDialog.Builder}
     * 
     * If in a child need a custom layout for a dialog should override
     * this and return null then use normal {@link DialogFragment}
     * implementations
     */
    protected AlertDialog.Builder getAlertDialogBuilder() {

        Bundle args = getArguments();
        Builder builder = new Builder(getActivity());

        if (args != null) {
            String title = args.containsKey(TITLE_RES_ID) ? getString(args
                    .getInt(TITLE_RES_ID)) : args.getString(TITLE);
            String message = args.containsKey(MESSAGE_RES_ID) ? getString(args
                    .getInt(MESSAGE_RES_ID)) : args.getString(MESSAGE);

            if (!TextUtils.isEmpty(title)) {
                builder.setTitle(title);
            }
            if (!TextUtils.isEmpty(message)) {
                builder.setMessage(message);
            }
        }
        builder.setPositiveButton(R.string.okay, null).create();

        return builder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void show(FragmentManager manager, String tag) {
        show(manager, tag, false);
    }

    /**
     * {@inheritDoc}
     * 
     * @param allowStateLoss If true allows the commit to be executed after an
     * activity's state is saved. e.g {@link FragmentActivity#onActivityResult}
     * other wise a {@link IllegalStateException} could be throw
     * 
     * http://stackoverflow.com/questions/10114324/show-dialogfragment-from-
     * onactivityresult
     */
    public void show(FragmentManager manager, String tag, boolean allowStateLoss) {
        FragmentTransaction transaction = manager.beginTransaction();
        show(transaction, tag, allowStateLoss);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int show(FragmentTransaction transaction, String tag) {
        return show(transaction, tag, false);
    }

    /**
     * Custom show method to avoid {@link IllegalStateException} when this is
     * executed after an activity's state is saved e.g
     * {@link FragmentActivity#onActivityResult}
     * 
     * @param allowStateLoss If true allows the commit to be executed after an
     * activity's state is saved. e.g {@link FragmentActivity#onActivityResult}
     * other wise a {@link IllegalStateException} could be throw
     * 
     * http://stackoverflow.com/questions/10114324/show-dialogfragment-from-
     * onactivityresult
     */
    public int show(FragmentTransaction transaction, String tag,
            boolean allowStateLoss) {
        transaction.add(this, tag);
        return allowStateLoss ? transaction.commitAllowingStateLoss()
                : transaction.commit();
    }
}
