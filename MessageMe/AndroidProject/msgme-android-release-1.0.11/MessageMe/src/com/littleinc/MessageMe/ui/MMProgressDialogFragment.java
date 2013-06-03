package com.littleinc.MessageMe.ui;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.coredroid.util.UIUtil;

public class MMProgressDialogFragment extends DialogFragment {

    protected static final String MESSAGE = "message";

    protected static final String CANCELABLE = "cancelable";

    protected static final String MESSAGE_RES_ID = "message_res_id";

    public static MMProgressDialogFragment newInstance(int messageResId,
            boolean cancelable) {
        MMProgressDialogFragment dialogFragment = new MMProgressDialogFragment();

        Bundle args = new Bundle();
        args.putBoolean(CANCELABLE, cancelable);
        args.putInt(MESSAGE_RES_ID, messageResId);
        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    public static MMProgressDialogFragment newInstance(String message,
            boolean cancelable) {
        MMProgressDialogFragment dialogFragment = new MMProgressDialogFragment();

        Bundle args = new Bundle();
        args.putString(MESSAGE, message);
        args.putBoolean(CANCELABLE, cancelable);
        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        ProgressDialog progressDialog = getProgressDialog();
        if (progressDialog == null) {
            return super.onCreateDialog(savedInstanceState);
        } else {
            return progressDialog;
        }
    }

    /**
     * Base implementation for a {@link ProgressDialog} (
     * {@link UIUtil#showProgressDialog(android.content.Context, String)})
     * 
     * Children should override this method to create custom
     * {@link ProgressDialog} instances
     * 
     * If in a child need a custom layout for a dialog should override
     * this and return null then use normal {@link DialogFragment}
     * implementations
     */
    protected ProgressDialog getProgressDialog() {

        Bundle args = getArguments();
        ProgressDialog progressDialog;

        if (args != null) {
            String message = args.containsKey(MESSAGE_RES_ID) ? getString(args
                    .getInt(MESSAGE_RES_ID)) : args.getString(MESSAGE);

            progressDialog = UIUtil.showProgressDialog(getActivity(), message);

            setCancelable(args.getBoolean(CANCELABLE));
            progressDialog.setCancelable(args.getBoolean(CANCELABLE));
        } else {

            // In case that child implementations doesn't set arguments into
            // this fragment an empty progress dialog will be displayed
            progressDialog = UIUtil.showProgressDialog(getActivity(), "");
        }

        return progressDialog;
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
        return allowStateLoss ? transaction.commitAllowingStateLoss() : super
                .show(transaction, tag);
    }
}
