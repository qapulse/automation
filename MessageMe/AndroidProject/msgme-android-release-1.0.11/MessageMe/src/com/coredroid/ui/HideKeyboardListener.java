package com.coredroid.ui;

import android.view.View;

import com.coredroid.util.UIUtil;

/**
 * On the ENTER keyboard event, hide the keyboard
 */
public class HideKeyboardListener extends EnterKeyListener {

    @Override
    protected boolean onEnterKey(View v) {
        UIUtil.hideKeyboard(v);
        return true;
    }
}
