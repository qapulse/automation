package com.littleinc.MessageMe.util;

import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;

import com.littleinc.MessageMe.MessageMeConstants;

/**
 * TextWatcher class to be used in the search activities
 * that uses the search-as-you-type methodology
 *
 */
public class SearchTextWatcher implements TextWatcher {
    
    private Handler mHandler;
    
    public SearchTextWatcher(Handler handler){
        this.mHandler = handler;
    }

    @Override
    public void afterTextChanged(Editable arg0) {}

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,
            int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before,
            int count) {
        mHandler.removeMessages(MessageMeConstants.UPDATE_SEARCH_MESSAGE); // start counting again
        mHandler.sendMessageDelayed(
                mHandler.obtainMessage(MessageMeConstants.UPDATE_SEARCH_MESSAGE, s.toString()),
                MessageMeConstants.TEXT_CHANGE_THRESHOLD);
    }
}