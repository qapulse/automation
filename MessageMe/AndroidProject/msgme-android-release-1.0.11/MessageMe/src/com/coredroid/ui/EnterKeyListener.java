package com.coredroid.ui;

import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;

/**
 * On the ENTER keyboard event perform an action
 */
public abstract class EnterKeyListener implements OnKeyListener {

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER
                && event.getAction() == KeyEvent.ACTION_UP) {
            return onEnterKey(v);
        }

        return false;
    }

    protected abstract boolean onEnterKey(View v);
}
